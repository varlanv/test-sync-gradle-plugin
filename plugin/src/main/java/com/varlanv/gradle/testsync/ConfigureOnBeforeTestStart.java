package com.varlanv.gradle.testsync;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.testing.Test;

@RequiredArgsConstructor
class ConfigureOnBeforeTestStart implements Action<Task> {

    TestSyncExtension testSyncExtension;
    Provider<TestSynchronizerBuildService> syncBuildServiceProvider;

    @Override
    public void execute(Task task) {
        val tags = testSyncExtension.getTags().get();
        if (!tags.isEmpty()) {
            if (!(task instanceof Test)) {
                throw new IllegalArgumentException(
                    String.format(
                        "Task must be of type '[%s], but received: [%s]'. This is likely caused by a bug in the plugin [%s].",
                        Test.class.getName(), task.getClass().getName(), Constants.PLUGIN_NAME
                    )
                );
            }
            val test = (Test) task;
            val buildService = syncBuildServiceProvider.get();
            val syncProperty = buildService.buildSyncProperty(testSyncExtension);
            if (syncProperty.property().isEmpty()) {
                if (testSyncExtension.getVerboseConfiguration().get()) {
                    task.getLogger().error(
                        "No sync file created for tags {} and seed [{}]. This is likely caused by a bug in the plugin [{}].",
                        tags, syncProperty.seed(), Constants.PLUGIN_NAME
                    );
                }
            } else {
                test.systemProperty(
                    Constants.SYNC_PROPERTY,
                    syncProperty.property()
                );
                task.getLogger().info(
                    "Running test task with seed [{}] and sync property [{}]",
                    syncProperty.seed(), syncProperty.property()
                );
            }
        }
    }
}
