package com.varlanv.gradle.testsync;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.experimental.PackagePrivate;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

@Getter
@RequiredArgsConstructor
final class SyncTagProperty {

    @Getter
    long seed;
    @NonFinal
    @PackagePrivate
    volatile State state;
    Lock lock;

    Optional<State> state() {
        return Optional.ofNullable(state);
    }

    @Getter
    @RequiredArgsConstructor
    public static final class State {

        String tag;
        Path syncFolderPath;
        Path syncFilePath;
        String syncFilePathStr;
    }
}
