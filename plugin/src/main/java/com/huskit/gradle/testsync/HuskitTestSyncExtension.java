package com.huskit.gradle.testsync;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.SetProperty;

import java.util.ArrayList;

interface HuskitTestSyncExtension extends HuskitTestSyncExtensionView {

    SetProperty<String> getUniqueTags();

    ListProperty<String> getTags();

    @Override
    default void tag(CharSequence tag) {
        getTags().add(verifyNotExists(tag));
    }

    @Override
    default void tags(CharSequence... tags) {
        var tagsList = new ArrayList<String>(tags.length);
        for (var tag : tags) {
            tagsList.add(verifyNotExists(tag));
        }
        getTags().addAll(tagsList);
    }

    @Override
    default void tags(Iterable<? extends CharSequence> tags) {
        var tagsList = new ArrayList<String>();
        for (var tag : tags) {
            tagsList.add(verifyNotExists(tag));
        }
        getTags().addAll(tagsList);
    }

    private String verifyNotExists(CharSequence tag) {
        var tagStr = tag.toString();
        var uniqueTags = getUniqueTags().get();
        if (uniqueTags.contains(tagStr)) {
            throw new IllegalArgumentException("Tag [" + tagStr + "] was already added, please check your configuration.");
        }
        getUniqueTags().add(tagStr);
        return tagStr;
    }
}
