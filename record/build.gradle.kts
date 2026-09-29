plugins {
    id("com.rebirth.qarobot.java-library-conventions")
}

dependencies {
    implementation(project(":commons"))
    implementation(libs.monte.screen.recorder)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
