plugins {
    id("com.gradle.plugin-publish").version("1.3.0")
    alias(libs.plugins.huskitInternalConvention)
}

gradlePlugin {
    website = "https://github.com/varlanv/gradle-test-sync-plugin"
    vcsUrl = "https://github.com/varlanv/gradle-test-sync-plugin.git"
    plugins {
        create("huskitTestSyncPlugin") {
            id = "com.huskit.gradle.testsync-plugin"
            displayName = "Huskit Test Sync Plugin"
            description = "Plugin for synchronizing parallel tests across multiple Gradle modules"
            implementationClass = "com.huskit.gradle.testsync.HuskitTestSyncPlugin"
            tags = listOf("test", "integrationTest", "junit", "sync", "synchronization", "parallel", "multimodule")
        }
    }
}

dependencies {
    compileOnly(projects.constants)
}
