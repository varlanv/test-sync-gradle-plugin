package io.huskit.gradle.plugin.internal;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InternalEnvironment {

    boolean isCi;
    boolean isTest;

    public static String name() {
        return "__huskit_internal_environment__";
    }

    public boolean isCi() {
        return isCi;
    }

    public boolean isLocal() {
        return !isCi;
    }

    public boolean isTest() {
        return isTest;
    }
}
