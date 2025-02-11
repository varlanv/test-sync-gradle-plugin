package com.varlanv.gradle.testsync;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.testing.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@RequiredArgsConstructor
class ConfigureOnBeforeTestStart implements Action<Task> {

    TestSyncExtension testSyncExtension;
    Provider<TestSynchronizerBuildService> syncBuildServiceProvider;
    Provider<Path> pluginDirPathProvider;

    @Override
    public void execute(Task task) {
        if (!(task instanceof Test)) {
            throw new IllegalArgumentException(
                String.format(
                    "Task must be of type '[%s], but received: [%s]'. This is likely caused by a bug in the plugin [%s].",
                    Test.class.getName(), task.getClass().getName(), Constants.PLUGIN_NAME
                )
            );
        }
        val test = (Test) task;
        setupSyncJar();
        setupSyncProperties(test);
    }

    @SneakyThrows
    private void setupSyncJar() {
        val pluginDir = Files.createDirectories(pluginDirPathProvider.get());
        val targetJarPath = pluginDir.resolve(Constants.SYNCHRONIZER_JAR);
        if (Files.notExists(targetJarPath)) {
            try (val in = Objects.requireNonNull(ConfigureOnBeforeTestStart.class.getResourceAsStream(Constants.SYNCHRONIZER_JAR_RESOURCE))) {
                Files.copy(in, targetJarPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void setupSyncProperties(Test test) {
        val tags = testSyncExtension.getTags().get();
        if (!tags.isEmpty()) {
            val buildService = syncBuildServiceProvider.get();
            val syncProperty = buildService.buildSyncProperty(testSyncExtension);
            if (syncProperty.property().isEmpty()) {
                if (testSyncExtension.getVerboseConfiguration().get()) {
                    test.getLogger().error(
                        "No sync file created for tags {} and seed [{}]. This is likely caused by a bug in the plugin [{}].",
                        tags, syncProperty.seed(), Constants.PLUGIN_NAME
                    );
                }
            } else {
                test.systemProperty(
                    Constants.SYNC_PROPERTY,
                    syncProperty.property()
                );
                test.getLogger().info(
                    "Running test task with seed [{}] and sync property [{}]",
                    syncProperty.seed(), syncProperty.property()
                );
            }
        }
    }
}
