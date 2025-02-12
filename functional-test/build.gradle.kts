plugins {
    `java-gradle-plugin`
    alias(libs.plugins.internalConvention)
}

internalConvention {
    integrationTestName = "functionalTest"
    internalModule = true
}

dependencies {
    implementation(projects.plugin)
}
