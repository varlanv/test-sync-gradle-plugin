package com.varlanv.gradle.testsync;

import lombok.Getter;

import java.util.Objects;

@Getter
final class SyncProperty {

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
