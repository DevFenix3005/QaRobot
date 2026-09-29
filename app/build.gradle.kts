plugins {
    id("com.rebirth.qarobot.java-application-conventions")
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
    systemProperty("qarobot.home", projectDir.absolutePath)
    val workspace = providers.gradleProperty("workspace")
    if (workspace.isPresent) {
        systemProperty("qarobot.workspace", file(workspace.get()).absolutePath)
    }
}

application {
    mainClass.set("com.rebirth.qarobot.app.main.Main")
}

distributions {
    named("main") {
        distributionBaseName.set("QaRobot")
        contents {
            from(projectDir.absolutePath) {
                include("dashboardtemplate/**")
                include("webdrivers/**")
            }
        }
    }
}
