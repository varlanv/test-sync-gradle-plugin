plugins {
    `java-library`
    alias(libs.plugins.huskitInternalConvention)
}

dependencies {
    compileOnly(libs.junit.platform.launcher)
    compileOnly(projects.constants)
    testCompileOnly(projects.constants)
    testImplementation(libs.junit.platform.launcher)
}
