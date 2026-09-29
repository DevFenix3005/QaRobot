plugins {
    id("com.rebirth.qarobot.java-library-conventions")
}

dependencies {
    implementation(project(":commons"))
    implementation(libs.selenium.java)
    implementation(libs.lorem)
    implementation(libs.unirest)
    implementation(libs.ascii.table)
    implementation(libs.freemarker)
    implementation(libs.jsonpath)
    implementation(libs.bundles.graal.js)

    testImplementation(project(":commons"))
}

tasks.named<Test>("test") {
    useJUnitPlatform { excludeTags("browser") }
}

tasks.register<Test>("browserSmokeTest") {
    description = "Run Selenium wrapper checks against a local page in a real browser."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("browser") }
    systemProperty("qarobot.headless", "true")
    systemProperty("qarobot.browser", providers.gradleProperty("browser").getOrElse("CHROME"))
    shouldRunAfter(tasks.named("test"))
}
