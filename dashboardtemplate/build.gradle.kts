import com.github.gradle.node.npm.task.NpmTask

plugins {
    alias(libs.plugins.node)
}

tasks.register<NpmTask>("startProject") {
    args.set(listOf("start"))
}

val npmCi = tasks.register<NpmTask>("npmCi") {
    args.set(listOf("ci"))
}

tasks.register<NpmTask>("buildProject") {
    dependsOn(npmCi)
    args.set(listOf("run", "build"))
    inputs.files("package.json", "package-lock.json", "webpack.config.js")
    inputs.dir("src")
    outputs.dir("dist")
}
