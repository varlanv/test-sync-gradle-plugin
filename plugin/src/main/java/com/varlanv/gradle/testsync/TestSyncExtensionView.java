package com.varlanv.gradle.testsync;

public interface TestSyncExtensionView {

    void tag(CharSequence tag);

    void tags(CharSequence... tags);

    void tags(Iterable<? extends CharSequence> tags);
}
