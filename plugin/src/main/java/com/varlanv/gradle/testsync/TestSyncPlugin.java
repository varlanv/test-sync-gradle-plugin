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
        val dependencies = project.getDependencies();
        val tasks = project.getTasks();
        val testSyncExtension = objects.newInstance(TestSyncExtension.class);
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
                test.usesService(buildServiceProvider);
                test.doFirst(
                    new ConfigureOnBeforeTestStart(
                        testSyncExtension,
                        buildServiceProvider
                    )
                );
                dependencies.add(
                    test.getName() + "RuntimeOnly",
                    Constants.SYNCHRONIZER_DEPENDENCY
                );
            }
        );
    }
}
