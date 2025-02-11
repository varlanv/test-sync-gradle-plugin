package com.varlanv.gradle.plugin;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class InternalEnvironment {

    boolean isCi;
    boolean isTest;

    public static String name() {
        return "__internal_environment__";
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
