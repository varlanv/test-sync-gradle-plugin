plugins {
    alias(libs.plugins.huskitInternalConvention)
}

gradlePlugin {
    plugins {
        create("huskitTestSyncPlugin") {
            id = "com.huskit.gradle.testsync-plugin"
            implementationClass = "com.huskit.gradle.testsync.HuskitTestSyncPlugin"
        }
    }
}

dependencies{
    compileOnly(projects.constants)
}
