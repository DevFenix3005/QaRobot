import com.github.gradle.node.npm.task.NpmTask

plugins {
    alias(libs.plugins.node)
}

tasks.register<NpmTask>("startProject") {
    args.set(listOf("start"))
}

tasks.register<NpmTask>("buildProject") {
    args.set(listOf("run", "build"))
}
