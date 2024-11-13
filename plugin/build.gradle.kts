plugins {
    alias(libs.plugins.gradlePluginPublish)
    alias(libs.plugins.huskitInternalConvention)
}

gradlePlugin {
    website = "https://github.com/varlanv/gradle-test-sync-plugin"
    vcsUrl = "https://github.com/varlanv/gradle-test-sync-plugin.git"
    plugins {
        create("huskitTestSyncPlugin") {
            id = "org.huskit.testsync-gradle-plugin"
            displayName = "Huskit Test Sync Plugin"
            description = "Plugin for synchronizing parallel tests across multiple Gradle modules"
            implementationClass = "org.huskit.gradle.testsync.HuskitTestSyncPlugin"
            tags = listOf("test", "integrationTest", "junit", "sync", "synchronization", "parallel", "multimodule")
        }
    }
}

huskitConvention {
    integrationTestName = "functionalTest"
}

dependencies {
    compileOnly(projects.constants)
    compileOnly(libs.junit.platform.engine)
    compileOnly(libs.junit.platform.launcher)
    compileOnly(libs.junit.jupiter.api)
}
