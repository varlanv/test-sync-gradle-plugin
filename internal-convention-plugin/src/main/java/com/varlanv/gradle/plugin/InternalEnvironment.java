package com.varlanv.gradle.plugin;

import lombok.Value;

import java.io.Serializable;

/**
 * Simple utility for applying different configurations based on different environment.
 */
@Value
class InternalEnvironment implements Serializable {

    boolean isCi;
    boolean isTest;

    public static String name() {
        return "__internal_environment__";
    }

    public boolean isLocal() {
        return !isCi;
    }
}
