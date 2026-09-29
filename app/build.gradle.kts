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
