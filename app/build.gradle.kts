import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption

plugins {
    id("com.rebirth.qarobot.java-application-conventions")
}

val prepareDashboard = tasks.register<Sync>("prepareDashboard") {
    description = "Build and assemble the dashboard resources included in distributions."
    dependsOn(":dashboardtemplate:buildProject")
    from(layout.projectDirectory.dir("dashboardtemplate")) { include("index.ftl") }
    from(project(":dashboardtemplate").layout.projectDirectory.dir("dist")) {
        include("*.css", "*.js", "*.png", "*.txt")
    }
    into(layout.buildDirectory.dir("dashboardtemplate"))
}

dependencies {
    implementation(project(":commons"))
    implementation(project(":scraping"))
    implementation(project(":record"))

    implementation(libs.selenium.java)
    implementation(libs.monte.screen.recorder)
    implementation(libs.lorem)
    implementation(libs.unirest)
    implementation(libs.rxjava)
    implementation(libs.freemarker)
    implementation(libs.flatlaf)
    implementation(libs.system.theme.detector)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    systemProperty("qarobot.guiSmoke", providers.gradleProperty("guiSmoke").getOrElse("false"))
}

tasks.named<JavaExec>("run") {
    workingDir(rootProject.projectDir)
    systemProperty("qarobot.home", projectDir.absolutePath)
    val workspace = providers.gradleProperty("workspace")
    if (workspace.isPresent) {
        systemProperty("qarobot.workspace", file(workspace.get()).absolutePath)
    }
}

application {
    mainClass.set("com.rebirth.qarobot.app.main.Main")
    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED",
        "-Dstdout.encoding=UTF-8",
        "-Dstderr.encoding=UTF-8"
    )
}

tasks.named<CreateStartScripts>("startScripts") {
    doLast {
        // Expanded paths for every dependency can exceed cmd.exe's 8191-character limit.
        windowsScript.writeText(windowsScript.readText().replace(Regex("(?m)^set CLASSPATH=[^\\r\\n]*")) {
            "set CLASSPATH=%APP_HOME%\\lib\\*"
        })
    }
}

distributions {
    named("main") {
        distributionBaseName.set("QaRobot")
        contents {
            from(prepareDashboard) { into("dashboardtemplate") }
            from(rootProject.layout.projectDirectory.dir("examples")) { into("examples") }
            from(rootProject.layout.projectDirectory.file("README.md")) { into("docs") }
            from(projectDir.absolutePath) {
                include("webdrivers/**")
            }
        }
    }
}

// jpackage uses the JDK running Gradle and creates platform-specific launchers.
val nativeApplicationName = "QaRobot"
val nativeApplicationVersion = version.toString()
val nativePackagingDirectory = layout.buildDirectory.dir("jpackage")
val nativeInputDirectory = nativePackagingDirectory.map { it.dir("input") }
val nativeImageDirectory = nativePackagingDirectory.map { it.dir("image/$nativeApplicationName") }
val nativeInstallerDirectory = nativePackagingDirectory.map { it.dir("installer") }
val nativeInstallerFile = layout.buildDirectory.file(
    "distributions/$nativeApplicationName-$nativeApplicationVersion-windows-x64.exe"
)
val nativeIcon = layout.projectDirectory.file("packaging/QaRobot.ico")
val consoleLauncherProperties = layout.projectDirectory.file("packaging/windows-cli.properties")
val nativeMainJar = tasks.named<Jar>("jar").flatMap { it.archiveFileName }
val jpackageExecutable = File(System.getProperty("java.home"), "bin/jpackage.exe")

fun windowsPackageVersion(applicationVersion: String): String {
    val components = applicationVersion.substringBefore('-').split('.').toMutableList()
    if (components.size == 2) components.add("0")
    val limits = listOf(255L, 255L, 65535L)
    val numbers = components.map { it.toLongOrNull() }
    if (numbers.size != 3 || numbers.withIndex().any { (index, number) ->
            number == null || number < 0 || number > limits[index]
        }) {
        throw GradleException("La version del instalador Windows debe estar entre 0.0.0 y 255.255.65535.")
    }
    return numbers.joinToString(".")
}

fun validateWindowsPackagingHost() {
    if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
        throw GradleException("El instalador Windows debe generarse desde Windows.")
    }
    if (System.getProperty("os.arch").lowercase() !in setOf("amd64", "x86_64")) {
        throw GradleException("El instalador Windows requiere un JDK para x64.")
    }
    if (Runtime.version().feature() < 25 || !jpackageExecutable.isFile) {
        throw GradleException("Configura JAVA_HOME con un JDK 25 o posterior que incluya jpackage.")
    }
}

fun checkedNativeOutputPath(file: File): Path {
    val buildRoot = layout.buildDirectory.get().asFile.canonicalFile.toPath()
    val target = file.toPath().toAbsolutePath().normalize()
    val resolved = if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) target.toRealPath()
        else file.canonicalFile.toPath()
    check(resolved.startsWith(buildRoot) && resolved != buildRoot) {
        "La salida de jpackage debe estar dentro del directorio build de app."
    }
    return target
}

fun makeNativeOutputWritable(file: File) {
    val target = checkedNativeOutputPath(file)
    // jpackage marks its Windows executables read-only, including the installer.
    if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
        Files.setAttribute(target, "dos:readonly", false, LinkOption.NOFOLLOW_LINKS)
    }
}

fun cleanNativeOutput(directory: File) {
    val target = checkedNativeOutputPath(directory)
    if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return
    // Do not follow links; validate every existing entry before modifying any of them.
    val entries = Files.walk(target).use { it.toList() }
    entries.forEach { checkedNativeOutputPath(it.toFile()) }
    try {
        entries.sortedByDescending { it.nameCount }.forEach { entry ->
            makeNativeOutputWritable(entry.toFile())
            Files.delete(entry)
        }
    } catch (error: java.io.IOException) {
        throw GradleException("No se pudo limpiar $directory. Cierra QaRobot antes de volver a empaquetar.", error)
    }
}

val installedDistribution = tasks.named<Sync>("installDist")
val prepareJpackageInput = tasks.register<Sync>("prepareJpackageInput") {
    group = "distribution"
    description = "Assemble application libraries and resources for native Windows packaging."
    dependsOn(installedDistribution)
    // jpackage discovers the non-modular classpath from JARs in the input root.
    from(installedDistribution.map { it.destinationDir.resolve("lib") }) { include("*.jar") }
    from(installedDistribution) {
        exclude("lib/**", "bin/**")
        includeEmptyDirs = false
    }
    into(nativeInputDirectory)
}

val jpackageImage = tasks.register<Exec>("jpackageImage") {
    group = "distribution"
    description = "Create Windows GUI and CLI launchers with a bundled Java runtime (JDK 25+ required)."
    dependsOn(prepareJpackageInput)
    inputs.dir(nativeInputDirectory)
    inputs.files(nativeIcon, consoleLauncherProperties)
    inputs.property("applicationVersion", nativeApplicationVersion)
    inputs.property("mainClass", application.mainClass)
    inputs.property("javaHome", System.getProperty("java.home"))
    inputs.property("javaRuntimeVersion", System.getProperty("java.runtime.version"))
    inputs.property("architecture", System.getProperty("os.arch"))
    outputs.dir(nativeImageDirectory)

    doFirst {
        validateWindowsPackagingHost()
        val packageVersion = windowsPackageVersion(nativeApplicationVersion)
        val imageDirectory = nativeImageDirectory.get().asFile
        cleanNativeOutput(imageDirectory)
        commandLine(
            jpackageExecutable.absolutePath,
            "--type", "app-image",
            "--name", nativeApplicationName,
            "--app-version", packageVersion,
            "--vendor", "QaRobot",
            "--description", "QaRobot - automatizacion de pruebas con Selenium",
            "--input", nativeInputDirectory.get().asFile.absolutePath,
            "--dest", imageDirectory.parentFile.absolutePath,
            "--main-jar", nativeMainJar.get(),
            "--main-class", application.mainClass.get(),
            "--icon", nativeIcon.asFile.absolutePath,
            "--add-launcher", "QaRobot-cli=${consoleLauncherProperties.asFile.absolutePath}",
            "--java-options", "--enable-native-access=ALL-UNNAMED",
            "--java-options", "-Dstdout.encoding=UTF-8",
            "--java-options", "-Dstderr.encoding=UTF-8",
            "--java-options", "-Dqarobot.home=\$APPDIR",
            "--jlink-options", "--strip-native-commands --strip-debug --no-man-pages --no-header-files --bind-services"
        )
    }
    doLast {
        listOf("QaRobot.exe", "QaRobot-cli.exe").forEach { launcher ->
            makeNativeOutputWritable(nativeImageDirectory.get().file(launcher).asFile)
        }
    }
}

tasks.register<Exec>("jpackageInstaller") {
    group = "distribution"
    description = "Create a Windows x64 installer with shortcuts and uninstaller (WiX required)."
    dependsOn(jpackageImage)
    inputs.dir(nativeImageDirectory)
    inputs.property("applicationVersion", nativeApplicationVersion)
    inputs.property("javaHome", System.getProperty("java.home"))
    inputs.property("javaRuntimeVersion", System.getProperty("java.runtime.version"))
    outputs.file(nativeInstallerFile)

    doFirst {
        validateWindowsPackagingHost()
        val packageVersion = windowsPackageVersion(nativeApplicationVersion)
        val installerDirectory = nativeInstallerDirectory.get().asFile
        cleanNativeOutput(installerDirectory)
        commandLine(
            jpackageExecutable.absolutePath,
            "--type", "exe",
            "--name", nativeApplicationName,
            "--app-version", packageVersion,
            "--vendor", "QaRobot",
            "--description", "QaRobot - automatizacion de pruebas con Selenium",
            "--app-image", nativeImageDirectory.get().asFile.absolutePath,
            "--dest", installerDirectory.absolutePath,
            "--about-url", "https://github.com/DevFenix3005/QaRobot",
            "--win-per-user-install",
            "--win-dir-chooser",
            "--win-menu", "--win-menu-group", "QaRobot",
            "--win-shortcut", "--win-shortcut-prompt",
            // Keep this identifier stable so future versions upgrade the same application.
            "--win-upgrade-uuid", "54745339-85e0-4dc8-a8cb-5bcdd0b26015"
        )
    }
    doLast {
        val generatedInstaller = nativeInstallerDirectory.get().file(
            "$nativeApplicationName-${windowsPackageVersion(nativeApplicationVersion)}.exe"
        ).asFile
        check(generatedInstaller.isFile) { "jpackage no genero el instalador esperado: $generatedInstaller" }
        val destination = checkedNativeOutputPath(nativeInstallerFile.get().asFile)
        Files.createDirectories(destination.parent)
        try {
            makeNativeOutputWritable(generatedInstaller)
            makeNativeOutputWritable(destination.toFile())
            Files.move(generatedInstaller.toPath(), destination, StandardCopyOption.REPLACE_EXISTING)
        } catch (error: java.io.IOException) {
            throw GradleException("No se pudo reemplazar $destination. Cierra el instalador antes de volver a empaquetar.", error)
        }
    }
}
