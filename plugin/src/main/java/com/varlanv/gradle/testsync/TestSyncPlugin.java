package com.varlanv.gradle.testsync;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

/**
 * Plugin entrypoint.
 */
public class TestSyncPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        var buildServiceProvider = project.getGradle()
            .getSharedServices()
            .registerIfAbsent(
                Constants.BUILD_SERVICE_NAME,
                TestSynchronizerBuildService.class,
                spec -> {
                }
            );
        var testSyncExtension = project.getObjects().newInstance(TestSyncExtension.class);
        project.getExtensions().add(
            TestSyncExtensionView.class,
            Constants.EXTENSION_NAME,
            testSyncExtension
        );
        var dependencies = project.getDependencies();
        project.getTasks().withType(
            Test.class
        ).configureEach(
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
