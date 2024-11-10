package com.huskit.gradle.testsync;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.NonFinal;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class TestSynchronizerBuildService implements BuildService<BuildServiceParameters.None>, AutoCloseable, Serializable {

    private static final Logger log = Logging.getLogger(TestSynchronizerBuildService.class);
    private static final String TEMP_FOLDER_PATH_STR = System.getProperty("java.io.tmpdir");
    transient long seed = ThreadLocalRandom.current().nextLong();
    transient ConcurrentHashMap<String, SyncTagProperty> tagToSyncPropMap = new ConcurrentHashMap<>();

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

    static final class SyncProperty {

        @Getter
        long seed;
        String syncProperty;

        SyncProperty(long seed, String syncProperty) {
            this.seed = seed;
            this.syncProperty = Objects.requireNonNull(syncProperty);
        }

        SyncProperty(long seed) {
            this.seed = seed;
            this.syncProperty = null;
        }

        Optional<String> property() {
            return Optional.ofNullable(syncProperty);
        }
    }

    @SneakyThrows
    SyncProperty buildSyncProperty(HuskitTestSyncExtension extension) {
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
        log.info("Initialized state: seed -> [{}], sync property -> [{}]", seed, finalProperty);
        return new SyncProperty(seed, finalProperty);
    }

    @Override
    @SneakyThrows
    public void close() {
        var folderRef = new AtomicReference<Path>();
        for (var syncFolderPath : tagToSyncPropMap.values()) {
            syncFolderPath.state().ifPresent(
                state -> {
                    try {
                        Files.deleteIfExists(state.syncFilePath);
                        folderRef.compareAndSet(null, state.syncFolderPath);
                    } catch (Exception e) {
                        log.error("Failed to delete sync file [{}] - {}", state.syncFilePathStr, e.getMessage());
                    }
                }
            );
        }
        if (folderRef.get() != null) {
            try {
                Files.deleteIfExists(folderRef.get());
            } catch (Exception e) {
                log.error("Failed to delete sync folder [{}] - {}", folderRef.get(), e.getMessage());
            }
        }
    }
}
