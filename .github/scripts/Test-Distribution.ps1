# Run with PowerShell 7 on Windows or Linux after :app:installDist.
param(
    [Parameter(Mandatory = $true)]
    [string] $DistributionPath,
    [Parameter(Mandatory = $true)]
    [string] $OutputPath
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$distributionDirectory = (Resolve-Path -LiteralPath $DistributionPath).Path
$outputDirectory = [System.IO.Path]::GetFullPath($OutputPath)
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

function Invoke-QaRobot {
    param([string[]] $Arguments, [int] $ExpectedExitCode)

    if ($IsWindows) {
        & (Join-Path $distributionDirectory 'bin/app.bat') @Arguments | Out-Host
    } else {
        & bash (Join-Path $distributionDirectory 'bin/app') @Arguments | Out-Host
    }
    if ($LASTEXITCODE -ne $ExpectedExitCode) {
        throw "QaRobot exited with code $LASTEXITCODE; expected $ExpectedExitCode."
    }
}

function Assert-Report {
    param([string] $ScenarioDirectory, [string] $JUnitPath, [bool] $ExpectFailure)

    $htmlReports = @(Get-ChildItem -LiteralPath $ScenarioDirectory -Filter index.html -File -Recurse)
    if ($htmlReports.Count -eq 0) { throw "No HTML report was generated in $ScenarioDirectory." }
    [xml] $junit = Get-Content -LiteralPath $JUnitPath -Raw
    if ($junit.SelectNodes('//testcase').Count -eq 0) { throw "JUnit report has no test cases: $JUnitPath" }
    $hasFailure = $junit.SelectNodes('//testcase/failure | //testcase/error').Count -gt 0
    if ($hasFailure -ne $ExpectFailure) { throw "Unexpected JUnit outcome in $JUnitPath." }
}

# Starting outside the installation checks that the launcher finds its dashboard assets.
$workingDirectory = Join-Path $outputDirectory 'working directory'
New-Item -ItemType Directory -Path $workingDirectory -Force | Out-Null
Push-Location -LiteralPath $workingDirectory
try {
    Invoke-QaRobot -Arguments @('--help') -ExpectedExitCode 0

    $passedDirectory = Join-Path $outputDirectory 'passed'
    $passedJUnit = Join-Path $outputDirectory 'passed.xml'
    Invoke-QaRobot -Arguments @(
        'run', (Join-Path $distributionDirectory 'examples/dynamic-waits.xml'),
        '--headless', '--browser', 'chrome', '--output', $passedDirectory, '--junit', $passedJUnit
    ) -ExpectedExitCode 0
    Assert-Report -ScenarioDirectory $passedDirectory -JUnitPath $passedJUnit -ExpectFailure $false

    # The suite must keep going after failure-evidence and finish offline-smoke successfully.
    $failedDirectory = Join-Path $outputDirectory 'failed'
    $failedJUnit = Join-Path $outputDirectory 'failed.xml'
    Invoke-QaRobot -Arguments @(
        'run', (Join-Path $distributionDirectory 'examples'),
        '--headless', '--browser', 'chrome', '--output', $failedDirectory, '--junit', $failedJUnit
    ) -ExpectedExitCode 1
    Assert-Report -ScenarioDirectory $failedDirectory -JUnitPath $failedJUnit -ExpectFailure $true
    [xml] $suite = Get-Content -LiteralPath $failedJUnit -Raw
    $cases = @($suite.SelectNodes('//testcase'))
    $passedCases = @($suite.SelectNodes('//testcase[not(failure) and not(error) and not(skipped)]'))
    $failedCases = @($suite.SelectNodes('//testcase[failure or error]'))
    if ($cases.Count -ne 3 -or $passedCases.Count -ne 2 -or $failedCases.Count -ne 1) {
        throw 'The examples suite must report three cases: two passed and one failed.'
    }
    if (-not $cases[1].GetAttribute('name').EndsWith('failure-evidence.xml') -or
        -not $cases[2].GetAttribute('name').EndsWith('offline-smoke.xml') -or
        $null -ne $cases[2].SelectSingleNode('failure | error | skipped')) {
        throw 'The examples suite did not successfully run offline-smoke after failure-evidence.'
    }
    foreach ($fileName in @('failure-0001.png', 'failure-0001.txt', 'failure-0002.png', 'failure-0002.txt')) {
        $evidence = @(Get-ChildItem -LiteralPath $failedDirectory -Filter $fileName -File -Recurse)
        if ($evidence.Count -eq 0 -or $evidence[0].Length -eq 0) {
            throw "Missing or empty failure evidence: $fileName"
        }
    }
} finally {
    Pop-Location
}

Write-Host 'Distribution smoke tests passed: CLI, dynamic waits, suite continuation, failure exit code, JUnit, HTML and evidence.'
# The intentional failed scenario must not leak its native exit code to the CI shell.
$global:LASTEXITCODE = 0
