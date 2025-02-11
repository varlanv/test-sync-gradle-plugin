package com.varlanv.gradle.testsync;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.gradle.api.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
class SynchronizerRequest {

    Logger log;
    TestSyncExtension extension;
    ConcurrentMap<String, SyncTagProperty> tagToSyncPropMap;
    Path tempFolderPath;
    long seed;

    @SneakyThrows
    SyncProperty handle() {
        val tags = extension.getTags().get();
        if (tags.isEmpty()) {
            return new SyncProperty(seed);
        }
        val syncFolderPath = tempFolderPath.resolve(Constants.SYNC_FOLDER_PREFIX + seed);
        if (Files.notExists(syncFolderPath)) {
            Files.createDirectories(syncFolderPath);
        }

        val syncPropertiesStates = new ArrayList<SyncTagProperty.State>(tags.size());
        for (val tag : tags) {
            val syncTagProperty = tagToSyncPropMap.computeIfAbsent(
                tag,
                key -> new SyncTagProperty(
                    seed,
                    new ReentrantLock()
                )
            );
            try {
                syncTagProperty.lock().lock();
                val state = syncTagProperty.state();
                if (!state.isPresent()) {
                    val syncFilePath = syncFolderPath.resolve(Constants.SYNC_FILE_NAME_BASE + tag);
                    val syncFilePathStr = syncFilePath.toString();
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
        val syncProperties = new ArrayList<String>(syncPropertiesStates.size());
        for (val syncPropertiesState : syncPropertiesStates) {
            syncProperties.add(syncPropertiesState.tag() + Constants.TAG_SEPARATOR + syncPropertiesState.syncFilePathStr());
        }
        val finalProperty = String.join(Constants.SYNC_PROPERTY_SEPARATOR, syncProperties);
        log.debug("Initialized state: seed -> [{}], sync property -> [{}]", seed, finalProperty);
        return new SyncProperty(seed, finalProperty);

    }
}
