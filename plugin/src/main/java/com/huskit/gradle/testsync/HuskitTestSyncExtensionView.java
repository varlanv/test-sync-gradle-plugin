package com.huskit.gradle.testsync;

public interface HuskitTestSyncExtensionView {

    void tag(CharSequence tag);

    void tags(CharSequence... tags);

    void tags(Iterable<? extends CharSequence> tags);
}
