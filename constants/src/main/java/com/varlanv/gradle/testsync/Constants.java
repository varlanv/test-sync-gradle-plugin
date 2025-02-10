package com.varlanv.gradle.testsync;

/**
 * Compile-time constants.
 * Should be included as compileOnly dependency to avoid publishing of extra artifacts.
 */
class Constants {

    static final String PLUGIN_VERSION = "0.0.1";
    static final String SYNCHRONIZER_DEPENDENCY = "com.varlanv.testsync-gradle-plugin:synchronizer:" + PLUGIN_VERSION;
    static final String EXTENSION_NAME = "testSync";
    static final String BUILD_SERVICE_NAME = "__internal_test_sync_plugin_bs__";
    static final String SYNC_FILE_NAME_BASE = "syncfile_";
    static final String PLUGIN_NAME = "com.varlanv.gradle.testsync-plugin";
    static final String TAG_SEPARATOR = "_:_:_";
    static final String SYNC_PROPERTY_SEPARATOR = ":___:";
    static final String SYNC_PROPERTY = "com.varlanv.gradle.build.sync";
    static final String SYNC_FOLDER_PREFIX = "varlanvtestsync_";
}
