plugins {
    `java-gradle-plugin`
    alias(libs.plugins.internalConvention)
    alias(libs.plugins.testKonvence)
}

internalConvention {
    integrationTestName = "functionalTest"
    internalModule = true
}

dependencies {
    implementation(projects.plugin)
}
