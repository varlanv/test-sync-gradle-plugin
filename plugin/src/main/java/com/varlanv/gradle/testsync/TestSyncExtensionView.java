package com.varlanv.gradle.testsync;

import org.gradle.api.NonNullApi;

/**
 * Configuration options for gradle plugin {@code com.varlanv.testsync}
 */
@NonNullApi
public interface TestSyncExtensionView {

    /**
     * Adds one tag to the list of synchronization tags.
     *
     * @param tag test tag
     * @throws IllegalArgumentException if tag was already added previously
     */
    void tag(CharSequence tag);

    /**
     * Adds multiple tags to the list of synchronization tags.
     *
     * @param tags test tag vararg
     * @throws IllegalArgumentException if any of the tags were already added previously
     */
    void tags(CharSequence... tags);

    /**
     * Adds multiple tags to the list of synchronization tags.
     *
     * @param tags test tags collection
     * @throws IllegalArgumentException if any of the tags were already added previously
     */
    void tags(Iterable<? extends CharSequence> tags);

    /**
     * Configure whether verbose logging during plugin configuration should be enabled.
     * Default is false.
     *
     * @param verbose true/false
     */
    void verboseConfiguration(boolean verbose);

    /**
     * Configure whether verbose logging of underlying synchronizer during test execution should be enabled.
     * Default is false.
     *
     * @param verbose true/false
     */
    void verboseSynchronizer(boolean verbose);
}
