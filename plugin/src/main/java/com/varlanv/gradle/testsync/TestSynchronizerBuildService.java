package com.varlanv.gradle.testsync;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.NonFinal;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class TestSynchronizerBuildService implements BuildService<BuildServiceParameters.None>, AutoCloseable, Serializable {

    private static final Logger log = Logging.getLogger(TestSynchronizerBuildService.class);
    /**
     * Temporary directory path.
     */
    @Nullable
    private static final String TEMP_FOLDER_PATH_STR = System.getProperty("java.io.tmpdir");
    /**
     * Unique number with lifetime of one gradle build.
     */
    transient long seed = ThreadLocalRandom.current().nextLong();
    /**
     * Map of user test tag to synchronization property. Lifetime of this map is one gradle build.
     */
    transient ConcurrentMap<String, SyncTagProperty> tagToSyncPropMap = new ConcurrentHashMap<>();

    @Getter
    @RequiredArgsConstructor
    static final class SyncTagProperty {

        @Getter
        long seed;
        @NonFinal
        volatile State state;
        Lock lock;

        Optional<State> state() {
            return Optional.ofNullable(state);
        }

        @RequiredArgsConstructor
        public static final class State {

            String tag;
            Path syncFolderPath;
            Path syncFilePath;
            String syncFilePathStr;
        }
    }

    @Getter
    static final class SyncProperty {

        long seed;
        String property;

        SyncProperty(long seed, String property) {
            this.seed = seed;
            this.property = Objects.requireNonNull(property);
        }

        SyncProperty(long seed) {
            this(seed, "");
        }
    }

    @SneakyThrows
    SyncProperty buildSyncProperty(TestSyncExtension extension) {
        if (TEMP_FOLDER_PATH_STR == null || TEMP_FOLDER_PATH_STR.isBlank()) {
            throw new IllegalStateException(
                String.format(
                    "System property `java.io.tmpdir` is not set. "
                        + "Plugin `%s` requires a valid temp folder path. "
                        + "Please ensure that property `System.getProperty(\"java.io.tmpdir\") is set"
                        + "(should be set by default unless explicitly disabled).",
                    Constants.PLUGIN_NAME
                )
            );
        }
        var tempFolderPath = Path.of(TEMP_FOLDER_PATH_STR);
        var tags = extension.getTags().get();
        if (tags.isEmpty()) {
            return new SyncProperty(seed);
        }
        var syncFolderPath = tempFolderPath.resolve(Constants.SYNC_FOLDER_PREFIX + seed);
        if (Files.notExists(syncFolderPath)) {
            Files.createDirectories(syncFolderPath);
        }

        var syncPropertiesStates = new ArrayList<SyncTagProperty.State>(tags.size());
        for (var tag : tags) {
            var syncTagProperty = tagToSyncPropMap.computeIfAbsent(
                tag,
                key -> new SyncTagProperty(
                    seed,
                    new ReentrantLock()
                )
            );
            try {
                syncTagProperty.lock().lock();
                var state = syncTagProperty.state();
                if (state.isEmpty()) {
                    var syncFilePath = syncFolderPath.resolve(Constants.SYNC_FILE_NAME_BASE + tag);
                    var syncFilePathStr = syncFilePath.toString();
                    if (Files.notExists(syncFilePath)) {
                        Files.createFile(syncFilePath);
                    }
                    syncTagProperty.state = new SyncTagProperty.State(tag, syncFolderPath, syncFilePath, syncFilePathStr);
                    syncPropertiesStates.add(syncTagProperty.state);
                } else {
                    syncPropertiesStates.add(state.get());
                }
            } finally {
                syncTagProperty.lock().unlock();
            }
        }
        var syncProperties = new ArrayList<String>(syncPropertiesStates.size());
        for (var syncPropertiesState : syncPropertiesStates) {
            syncProperties.add(syncPropertiesState.tag + Constants.TAG_SEPARATOR + syncPropertiesState.syncFilePathStr);
        }
        var finalProperty = String.join(Constants.SYNC_PROPERTY_SEPARATOR, syncProperties);
        log.debug("Initialized state: seed -> [{}], sync property -> [{}]", seed, finalProperty);
        return new SyncProperty(seed, finalProperty);
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
        for (var syncFolderPath : tagToSyncPropMap.values()) {
            if (syncFolderPath.state().isPresent()) {
                var state = syncFolderPath.state().get();
                try {
                    Files.deleteIfExists(state.syncFilePath);
                    folderRef = state.syncFolderPath;
                } catch (Exception e) {
                    log.error("Failed to delete sync file [{}] - {}", state.syncFilePathStr, e.getMessage());
                }
            }
        }
        return Optional.ofNullable(folderRef);
    }
}
