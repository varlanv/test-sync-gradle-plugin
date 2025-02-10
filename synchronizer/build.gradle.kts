plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.internalConvention)
}

dependencies {
    compileOnly(libs.junit.platform.launcher)
    compileOnly(projects.constants)
    testCompileOnly(projects.constants)
    testImplementation(libs.junit.platform.launcher)
}

publishing {
    publications {
        named("mavenJava", MavenPublication::class) {
            pom {
                name = "Gradle test synchronizer"
                description = "Helper module for handling test synchronization with gradle test sync plugin"
            }
        }
    }
}
