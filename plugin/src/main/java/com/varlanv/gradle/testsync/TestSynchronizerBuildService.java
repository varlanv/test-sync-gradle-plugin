package com.varlanv.gradle.testsync;

import lombok.SneakyThrows;
import lombok.val;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

public abstract class TestSynchronizerBuildService implements BuildService<BuildServiceParameters.None>, AutoCloseable, Serializable {

    private static final Logger log;
    /**
     * Temporary directory path.
     */
    private static final Path tempFolderPath;

    static {
        val tmpDirPathStr = System.getProperty("java.io.tmpdir");
        if (tmpDirPathStr == null || tmpDirPathStr.isEmpty()) {
            throw new IllegalStateException(
                String.format(
                    "System property `java.io.tmpdir` is not set. "
                        + "Plugin `%s` requires a valid temp folder path. "
                        + "Please ensure that property `System.getProperty(\"java.io.tmpdir\")` is set"
                        + "(should be set by default unless explicitly disabled).",
                    Constants.PLUGIN_NAME
                )
            );
        }
        tempFolderPath = Paths.get(tmpDirPathStr);
        log = Logging.getLogger(TestSynchronizerBuildService.class);
    }

    /**
     * Unique number with lifetime of one gradle build.
     */
    transient long seed = ThreadLocalRandom.current().nextLong();
    /**
     * Map of user test tag to synchronization property. Lifetime of this map is one gradle build.
     */
    transient ConcurrentMap<String, SyncTagProperty> tagToSyncPropMap = new ConcurrentHashMap<>();


    @SneakyThrows
    SyncProperty buildSyncProperty(TestSyncExtension extension) {
        return new SynchronizerRequest(
            log,
            extension,
            tagToSyncPropMap,
            tempFolderPath,
            seed
        ).handle();
    }

    @Override
    @SneakyThrows
    public void close() {
        deleteFilesAndReturnFolder().ifPresent((folder) -> {
            try {
                Files.deleteIfExists(folder);
            } catch (Exception e) {
                log.error("Failed to delete sync folder [{}] - {}", folder, e.getMessage());
            }
        });
    }

    private Optional<Path> deleteFilesAndReturnFolder() {
        Path folderRef = null;
        for (val syncFolderPath : tagToSyncPropMap.values()) {
            if (syncFolderPath.state().isPresent()) {
                val state = syncFolderPath.state().get();
                try {
                    Files.deleteIfExists(state.syncFilePath());
                    folderRef = state.syncFolderPath();
                } catch (Exception e) {
                    log.error("Failed to delete sync file [{}] - {}", state.syncFilePathStr(), e.getMessage());
                }
            }
        }
        return Optional.ofNullable(folderRef);
    }
}
