import com.huskit.gradle.testsync.HuskitTestSyncExtensionView

plugins {
    id("com.huskit.gradle.testsync-plugin").version("1.0.0-SNAPSHOT-1").apply(false)
}

abstract class TestBuildService : BuildService<BuildServiceParameters.None>, AutoCloseable {

    private val testSingleSyncFile: File = File.createTempFile("huskit", "gradle_test_single_sync")
    private val testMultiSyncFile: File = File.createTempFile("huskit", "gradle_test_multi_sync")
    private val testMixedSyncFile: File = File.createTempFile("huskit", "gradle-test_mixed_sync")

    fun testSingleSyncFile(): File {
        return testSingleSyncFile
    }

    fun testMultiSyncFile(): File {
        return testMultiSyncFile
    }

    fun testMixedSyncFile(): File {
        return testMixedSyncFile
    }

    override fun close() {
        testSingleSyncFile.delete()
        testMultiSyncFile.delete()
        testMixedSyncFile.delete()
    }
}

subprojects {
    project.apply(mapOf("plugin" to "java-library"))
    project.apply(mapOf("plugin" to "com.huskit.gradle.testsync-plugin"))

    val buildService = gradle.sharedServices.registerIfAbsent(
        "testFileBuildService",
        TestBuildService::class.java
    )

    repositories {
        mavenLocal()
        mavenCentral()
    }

    val projPath = providers.provider { project.path }
    tasks.withType<Test> {
        usesService(buildService)
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
        }
        outputs.upToDateWhen { false }
        doFirst {
            systemProperty("gradleProjectName", projPath.get())
            systemProperty("testSingleSyncFile", buildService.get().testSingleSyncFile().absolutePath)
            systemProperty("testMultiSyncFile", buildService.get().testMultiSyncFile().absolutePath)
            systemProperty("testMixedSyncFile", buildService.get().testMixedSyncFile().absolutePath)
        }
    }

    val junitApiDep = "org.junit.jupiter:junit-jupiter-api:5.11.3"
    val junitEngineDep = "org.junit.jupiter:junit-jupiter-engine:5.11.3"
    if (project.name.contains("single-tag")) {
        if (project.name == "base-single-tag") {
            project.dependencies.add("api", junitApiDep)
            project.dependencies.add("api", junitEngineDep)
        } else {
            dependencies.add("implementation", project(":single-tag:base-single-tag"))
        }
        extensions.configure<HuskitTestSyncExtensionView> {
            tags("mytag")
        }
    } else if (project.name.contains("multi-tag")) {
        if (project.name == "base-multi-tag") {
            project.dependencies.add("api", junitApiDep)
            project.dependencies.add("api", junitEngineDep)
        } else {
            dependencies.add("implementation", project(":multi-tag:base-multi-tag"))

        }
        extensions.configure<HuskitTestSyncExtensionView> {
            tags("mytag1", "mytag2")
        }
    } else if (project.name.contains("mixed-tag")) {
        if (project.name == "base-mixed-tag") {
            project.dependencies.add("api", junitApiDep)
            project.dependencies.add("api", junitEngineDep)
        } else {
            dependencies.add("implementation", project(":mixed-tag:base-mixed-tag"))
        }
        extensions.configure<HuskitTestSyncExtensionView> {
            tags("my_mixed_tag_1", "my_mixed_tag_2", "my_mixed_tag_3")
        }
    }
}
