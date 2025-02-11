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
        var extensions = project.getExtensions();
        var objects = project.getObjects();
        var gradle = project.getGradle();
        var sharedServices = gradle.getSharedServices();
        var dependencies = project.getDependencies();
        var tasks = project.getTasks();
        var buildServiceProvider = sharedServices.registerIfAbsent(
            Constants.BUILD_SERVICE_NAME,
            TestSynchronizerBuildService.class,
            spec -> {
            }
        );
        var testSyncExtension = objects.newInstance(TestSyncExtension.class);
        extensions.add(
            TestSyncExtensionView.class,
            Constants.EXTENSION_NAME,
            testSyncExtension
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
