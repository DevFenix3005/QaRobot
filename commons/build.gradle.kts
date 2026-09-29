plugins {
    id("com.rebirth.qarobot.java-common-conventions")
}

dependencies {
    // Jackson annotations and mapper types are part of the shared model API.
    api(platform(libs.jackson.bom))
    api(libs.bundles.jackson)
    implementation(libs.ascii.table)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}


val xjc = configurations.create("xjc")

dependencies {
    xjc(libs.jaxb.xjc)
}

val generatedJaxb = layout.buildDirectory.dir("generated/sources/xjc/main")
val qaSchema = layout.projectDirectory.file("src/main/resources/com/rebirth/qarobot/commons/xsd/qarobot_v2.xsd")

val generateQaXml = tasks.register<JavaExec>("generateQaXml") {
    description = "Generate the QaRobot XML model from its schema."
    classpath = xjc
    mainClass.set("com.sun.tools.xjc.XJCFacade")
    inputs.file(qaSchema)
    outputs.dir(generatedJaxb)
    doFirst {
        generatedJaxb.get().asFile.mkdirs()
    }
    args("-d", generatedJaxb.get().asFile.absolutePath,
            "-p", "com.rebirth.qarobot.commons.models.dtos.qarobot",
            "-encoding", "UTF-8", "-no-header", qaSchema.asFile.absolutePath)
}

sourceSets.main {
    java.srcDir(generatedJaxb)
}
tasks.named<JavaCompile>("compileJava") { dependsOn(generateQaXml) }
