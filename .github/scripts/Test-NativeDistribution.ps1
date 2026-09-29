# Run with PowerShell 7 on Windows after :app:jpackageImage. This does not install anything.
param(
    [Parameter(Mandatory = $true)]
    [string] $ApplicationPath,
    [Parameter(Mandatory = $true)]
    [string] $OutputPath
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
if (-not $IsWindows) { throw 'The native Windows distribution must be tested on Windows.' }
$applicationDirectory = (Resolve-Path -LiteralPath $ApplicationPath).Path
$outputDirectory = [System.IO.Path]::GetFullPath($OutputPath)
$launcher = Join-Path $applicationDirectory 'QaRobot-cli.exe'
foreach ($relativePath in @('QaRobot.exe', 'QaRobot-cli.exe', 'runtime/bin/server/jvm.dll', 'app/examples/offline-smoke.xml')) {
    if (-not (Test-Path -LiteralPath (Join-Path $applicationDirectory $relativePath) -PathType Leaf)) {
        throw "Missing native application file: $relativePath"
    }
}
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

function Invoke-NativeQaRobot {
    param([string[]] $Arguments, [int] $ExpectedExitCode)
    & $launcher @Arguments | Out-Host
    if ($LASTEXITCODE -ne $ExpectedExitCode) {
        throw "Native QaRobot exited with code $LASTEXITCODE; expected $ExpectedExitCode."
    }
}

function Test-NativeGui {
    if (-not ('QaRobot.Ci.NativeWindow' -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;

namespace QaRobot.Ci {
    public static class NativeWindow {
        private delegate bool EnumWindowCallback(IntPtr handle, IntPtr parameter);
        [DllImport("user32.dll")]
        private static extern bool EnumWindows(EnumWindowCallback callback, IntPtr parameter);
        [DllImport("user32.dll")]
        private static extern uint GetWindowThreadProcessId(IntPtr handle, out uint processId);
        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        private static extern int GetWindowText(IntPtr handle, StringBuilder text, int capacity);
        [DllImport("user32.dll")]
        private static extern bool ShowWindow(IntPtr handle, int command);
        [DllImport("user32.dll", SetLastError = true)]
        private static extern bool PostMessage(IntPtr handle, uint message, IntPtr wParam, IntPtr lParam);
        [DllImport("user32.dll", SetLastError = true)]
        private static extern IntPtr SendMessageTimeout(IntPtr handle, uint message, IntPtr wParam,
            IntPtr lParam, uint flags, uint timeout, out IntPtr result);

        public static IntPtr FindMainWindow(int[] processIds) {
            var owners = new HashSet<int>(processIds);
            IntPtr mainWindow = IntPtr.Zero;
            EnumWindows((handle, parameter) => {
                uint processId;
                GetWindowThreadProcessId(handle, out processId);
                if (!owners.Contains((int)processId)) return true;
                var title = new StringBuilder(512);
                GetWindowText(handle, title, title.Capacity);
                if (!title.ToString().StartsWith("QaRobot [", StringComparison.Ordinal)) return true;
                ShowWindow(handle, 0); // Preserve a hidden smoke test even if Swing shows its frame.
                mainWindow = handle;
                return false;
            }, IntPtr.Zero);
            return mainWindow;
        }

        public static bool IsResponsive(IntPtr handle) {
            IntPtr result;
            return SendMessageTimeout(handle, 0, IntPtr.Zero, IntPtr.Zero, 2, 5000, out result) != IntPtr.Zero;
        }

        public static bool Close(IntPtr handle) {
            return PostMessage(handle, 0x0010, IntPtr.Zero, IntPtr.Zero); // WM_CLOSE
        }
    }
}
'@
    }

    $guiExecutable = Join-Path $applicationDirectory 'QaRobot.exe'
    $guiProcess = Start-Process -FilePath $guiExecutable -WorkingDirectory $workingDirectory `
        -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $outputDirectory 'gui-stdout.log') `
        -RedirectStandardError (Join-Path $outputDirectory 'gui-stderr.log')
    $ownedProcesses = @{ $guiProcess.Id = $guiProcess.StartTime.ToUniversalTime() }

    function Update-OwnedProcesses {
        # jpackage's launcher starts a second QaRobot.exe that owns the JVM/window.
        # Follow only descendants of this launch, matching its executable and birth time.
        $candidates = @(Get-CimInstance Win32_Process -Filter "Name = 'QaRobot.exe'" | Where-Object {
            $_.ExecutablePath -eq $guiExecutable
        })
        do {
            $discovered = $false
            foreach ($candidate in $candidates) {
                $childId = [int]$candidate.ProcessId
                $parentId = [int]$candidate.ParentProcessId
                $created = $candidate.CreationDate.ToUniversalTime()
                if (-not $ownedProcesses.ContainsKey($childId) -and $ownedProcesses.ContainsKey($parentId) -and
                    $created -ge $ownedProcesses[$parentId]) {
                    $ownedProcesses[$childId] = $created
                    $discovered = $true
                }
            }
        } while ($discovered)
    }

    function Get-OwnedRunningProcesses {
        foreach ($ownedId in @($ownedProcesses.Keys)) {
            $process = Get-Process -Id $ownedId -ErrorAction SilentlyContinue
            if ($process -and -not $process.HasExited -and
                [Math]::Abs(($process.StartTime.ToUniversalTime() - $ownedProcesses[$ownedId]).TotalMilliseconds) -lt 1) {
                $process
            }
        }
    }

    try {
        $window = [IntPtr]::Zero
        $deadline = [DateTime]::UtcNow.AddSeconds(30)
        do {
            Update-OwnedProcesses
            $running = @(Get-OwnedRunningProcesses)
            if ($running.Count -gt 0) {
                $window = [QaRobot.Ci.NativeWindow]::FindMainWindow([int[]]$running.Id)
            }
            if ($window -ne [IntPtr]::Zero) { break }
            Start-Sleep -Milliseconds 200
        } while ([DateTime]::UtcNow -lt $deadline)
        if ($window -eq [IntPtr]::Zero) { throw 'The native GUI did not create its main window within 30 seconds.' }
        if (-not [QaRobot.Ci.NativeWindow]::IsResponsive($window)) {
            throw 'The native GUI main window did not respond.'
        }
        if (-not [QaRobot.Ci.NativeWindow]::Close($window)) { throw 'Could not send WM_CLOSE to the native GUI.' }
        $deadline = [DateTime]::UtcNow.AddSeconds(15)
        do {
            Update-OwnedProcesses
            if (@(Get-OwnedRunningProcesses).Count -eq 0) { break }
            Start-Sleep -Milliseconds 200
        } while ([DateTime]::UtcNow -lt $deadline)
        if (@(Get-OwnedRunningProcesses).Count -ne 0) { throw 'The native GUI did not exit after closing its main window.' }
    } finally {
        Update-OwnedProcesses
        # Failure cleanup is restricted to the processes created by this smoke test.
        Get-OwnedRunningProcesses | Stop-Process -Force -ErrorAction SilentlyContinue
        $guiProcess.Dispose()
    }
}

# Test from an unrelated directory and hide all PATH entries exposing a Java
# executable. The launcher must use runtime/ inside the application image.
$workingDirectory = Join-Path $outputDirectory 'working directory'
New-Item -ItemType Directory -Path $workingDirectory -Force | Out-Null
$originalJavaHome = $env:JAVA_HOME
$originalPath = $env:PATH
$javaOptions = @{}
foreach ($name in @('JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS')) {
    $javaOptions[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
Push-Location -LiteralPath $workingDirectory
try {
    $env:JAVA_HOME = $null
    $env:PATH = (($originalPath -split [System.IO.Path]::PathSeparator) | Where-Object {
        $entry = [Environment]::ExpandEnvironmentVariables($_.Trim('"'))
        $entry -and -not (Test-Path -LiteralPath (Join-Path $entry 'java.exe')) -and
            -not (Test-Path -LiteralPath (Join-Path $entry 'javaw.exe'))
    }) -join [System.IO.Path]::PathSeparator
    foreach ($name in $javaOptions.Keys) {
        [Environment]::SetEnvironmentVariable($name, $null, 'Process')
    }
    if (Get-Command java.exe -CommandType Application -ErrorAction SilentlyContinue) {
        throw 'The smoke test must run without an external Java executable on PATH.'
    }

    Invoke-NativeQaRobot -Arguments @('--help') -ExpectedExitCode 0
    $reportDirectory = Join-Path $outputDirectory 'passed'
    $junitPath = Join-Path $outputDirectory 'passed.xml'
    Invoke-NativeQaRobot -Arguments @(
        'run', (Join-Path $applicationDirectory 'app/examples/offline-smoke.xml'),
        '--headless', '--browser', 'chrome', '--output', $reportDirectory, '--junit', $junitPath
    ) -ExpectedExitCode 0
    [xml] $junit = Get-Content -LiteralPath $junitPath -Raw
    if ($junit.SelectNodes('//testcase').Count -ne 1 -or
        $junit.SelectNodes('//testcase/failure | //testcase/error | //testcase/skipped').Count -ne 0) {
        throw 'The native launcher must produce one successful offline-smoke JUnit case.'
    }
    $htmlReports = @(Get-ChildItem -LiteralPath $reportDirectory -Filter index.html -File -Recurse)
    if ($htmlReports.Count -ne 1 -or $htmlReports[0].Length -eq 0) {
        throw 'The native launcher did not produce the expected HTML report.'
    }
    Test-NativeGui
} finally {
    $env:JAVA_HOME = $originalJavaHome
    $env:PATH = $originalPath
    foreach ($name in $javaOptions.Keys) {
        [Environment]::SetEnvironmentVariable($name, $javaOptions[$name], 'Process')
    }
    Pop-Location
}

Write-Host 'Native Windows smoke passed: bundled Java, CLI, Chrome, JUnit, HTML, GUI startup and graceful close.'
$global:LASTEXITCODE = 0
