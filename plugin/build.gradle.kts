plugins {
    alias(libs.plugins.gradlePluginPublish)
    alias(libs.plugins.internalConvention)
}

gradlePlugin {
    website = "https://github.com/varlanv/test-sync-gradle-plugin"
    vcsUrl = "https://github.com/varlanv/test-sync-gradle-plugin"
    plugins {
        create("testSyncPlugin") {
            id = "com.varlanv.testsync"
            displayName = "Test Sync Plugin"
            description = "Plugin for synchronizing parallel tests across multiple Gradle modules"
            implementationClass = "com.varlanv.gradle.testsync.TestSyncPlugin"
            tags = listOf("test", "integrationTest", "junit", "sync", "synchronization", "parallel", "multimodule")
        }
    }
}

internalConvention {
    integrationTestName = "functionalTest"
}

dependencies {
    compileOnly(projects.constants)
    compileOnly(libs.junit.platform.engine)
    compileOnly(libs.junit.platform.launcher)
    compileOnly(libs.junit.jupiter.api)
}

tasks.named<Jar>("jar") {
    dependsOn(":synchronizer:jar")
    from(project.rootDir.toPath()
        .resolve("synchronizer")
        .resolve("build")
        .resolve("libs")
        .resolve("synchronizer-${providers.gradleProperty("version").get()}.jar"))
}
