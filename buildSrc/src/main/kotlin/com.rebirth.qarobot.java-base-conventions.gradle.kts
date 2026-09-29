plugins {
    idea
    java
}

group = "com.rebirth.qarobot"
version = "2.0"

// Convention plugins access the consuming project's catalog through the public API.
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

dependencies {
    implementation(libs.findLibrary("dagger").get())
    annotationProcessor(libs.findLibrary("dagger-compiler").get())

    implementation(libs.findLibrary("guava").get())
    implementation(libs.findBundle("log4j").get())

    compileOnly(libs.findLibrary("lombok").get())
    annotationProcessor(libs.findLibrary("lombok").get())

    testCompileOnly(libs.findLibrary("lombok").get())
    testAnnotationProcessor(libs.findLibrary("lombok").get())
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter-api").get())
    testRuntimeOnly(libs.findLibrary("junit-jupiter-engine").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())

    implementation(libs.findLibrary("jakarta-xml-bind").get())
    runtimeOnly(libs.findLibrary("jaxb-runtime").get())
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    failOnNoDiscoveredTests.set(false)
}
