plugins {
    `java-gradle-plugin`
}

val isCiBuild = providers.environmentVariable("CI").orNull != null

if (isCiBuild) {
    java {
        version = JavaVersion.VERSION_11
    }
} else {
    java {
        toolchain {
            vendor.set(JvmVendorSpec.AZUL)
            languageVersion.set(JavaLanguageVersion.of(11))
        }
    }
}

repositories {
    if (!isCiBuild) {
        mavenLocal()
    }
    mavenCentral()
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.lombok)
    implementation(libs.junit.platform.launcher)
    annotationProcessor(libs.lombok)
}

gradlePlugin {
    plugins {
        create("huskitInternalGradleConventionPlugin") {
            id = libs.plugins.huskitInternalConvention.get().pluginId
            implementationClass = "io.huskit.gradle.plugin.InternalConventionPlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
