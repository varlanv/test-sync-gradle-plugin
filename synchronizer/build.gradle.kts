plugins {
    java
    alias(libs.plugins.huskitInternalConvention)
}

dependencies {
    compileOnly(libs.junit.platform.engine)
    compileOnly(libs.junit.platform.launcher)
    compileOnly(libs.junit.jupiter.api)
    compileOnly(projects.constants)
}
