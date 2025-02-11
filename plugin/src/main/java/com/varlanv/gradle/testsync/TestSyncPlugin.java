package com.varlanv.gradle.testsync;

import lombok.val;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

/**
 * Plugin entrypoint.
 */
public class TestSyncPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        val extensions = project.getExtensions();
        val objects = project.getObjects();
        val gradle = project.getGradle();
        val sharedServices = gradle.getSharedServices();
        val tasks = project.getTasks();
        val testSyncExtension = objects.newInstance(TestSyncExtension.class);
        val layout = project.getLayout();
        val buildDirectory = layout.getBuildDirectory();
        testSyncExtension.getVerboseSynchronizer().convention(false);
        testSyncExtension.getVerboseConfiguration().convention(false);
        extensions.add(
            TestSyncExtensionView.class,
            Constants.EXTENSION_NAME,
            testSyncExtension
        );
        val buildServiceProvider = sharedServices.registerIfAbsent(
            Constants.BUILD_SERVICE_NAME,
            TestSynchronizerBuildService.class,
            spec -> {
            }
        );

        tasks.withType(Test.class).configureEach(
            test -> {
                test.setClasspath(test.getClasspath().plus(buildDirectory.files("tmp/testsyncplugin/" + Constants.SYNCHRONIZER_JAR)));
                test.usesService(buildServiceProvider);
                test.doFirst(
                    new ConfigureOnBeforeTestStart(
                        testSyncExtension,
                        buildServiceProvider,
                        buildDirectory.map(dir -> dir.getAsFile().toPath().resolve("tmp").resolve("testsyncplugin").toAbsolutePath())
                    )
                );
            }
        );
    }
}
