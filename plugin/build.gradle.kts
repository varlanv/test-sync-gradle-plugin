plugins {
    alias(libs.plugins.gradlePluginPublish)
    alias(libs.plugins.internalConvention)
}

gradlePlugin {
    website = "https://github.com/varlanv/gradle-test-sync-plugin"
    vcsUrl = "https://github.com/varlanv/gradle-test-sync-plugin.git"
    plugins {
        create("testSyncPlugin") {
            id = "com.varlanv.testsync-gradle-plugin"
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
