plugins {
    `java-library`
    alias(libs.plugins.internalConvention)
}

internalConvention {
    internalModule = true
}

dependencies {
    implementation(libs.junit.platform.engine)
    implementation(libs.junit.platform.launcher)
    implementation(libs.junit.jupiter.api)
    implementation(libs.assertj.core)
    api(libs.apache.commons.lang)
    api(libs.apache.commons.io)
    api(gradleApi())
    api(gradleTestKit())
}
