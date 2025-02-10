package com.varlanv.gradle.testsync;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.SetProperty;

import java.util.ArrayList;
import java.util.Set;

interface TestSyncExtension extends TestSyncExtensionView {

    SetProperty<String> getUniqueTags();

    ListProperty<String> getTags();

    @Override
    default void tag(CharSequence tag) {
        getTags().add(verifyNotExists(tag, getUniqueTags().get()));
    }

    @Override
    default void tags(CharSequence... tags) {
        var tagsList = new ArrayList<String>(tags.length);
        var uniqueTags = getUniqueTags().get();
        for (var tag : tags) {
            tagsList.add(verifyNotExists(tag, uniqueTags));
        }
        getTags().addAll(tagsList);
    }

    @Override
    default void tags(Iterable<? extends CharSequence> tags) {
        var tagsList = new ArrayList<String>();
        var uniqueTags = getUniqueTags().get();
        for (var tag : tags) {
            tagsList.add(verifyNotExists(tag, uniqueTags));
        }
        getTags().addAll(tagsList);
    }

    private String verifyNotExists(CharSequence tag, Set<String> uniqueTags) {
        var tagStr = tag.toString();
        if (uniqueTags.contains(tagStr)) {
            throw new IllegalArgumentException("Test synchronization tag [" + tagStr + "] was already added, please check your configuration.");
        }
        getUniqueTags().add(tagStr);
        return tagStr;
    }
}
