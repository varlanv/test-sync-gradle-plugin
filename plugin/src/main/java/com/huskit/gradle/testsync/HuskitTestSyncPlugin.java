package com.huskit.gradle.testsync;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

public class HuskitTestSyncPlugin implements Plugin<Project> {

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
        var testSyncExtension = project.getObjects().newInstance(HuskitTestSyncExtension.class);
        project.getExtensions().add(
            Constants.EXTENSION_NAME,
            testSyncExtension
        );
        project.afterEvaluate(
            p -> p.getTasks().withType(
                Test.class,
                test -> {
                    test.usesService(buildServiceProvider);
                    test.doFirst(
                        new ConfigureOnBeforeTestStart(
                            testSyncExtension,
                            buildServiceProvider
                        )
                    );
                    p.getDependencies().add(
                        test.getName() + "Implementation",
                        Constants.SYNCHRONIZER_DEPENDENCY
                    );
                }
            )
        );
    }
}
