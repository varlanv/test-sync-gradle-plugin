package com.varlanv.gradle.testsync;

import lombok.val;
import org.gradle.api.NonNullApi;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

import java.util.ArrayList;
import java.util.Set;

@NonNullApi
interface TestSyncExtension extends TestSyncExtensionView {

    SetProperty<String> getUniqueTags();

    ListProperty<String> getTags();

    Property<Boolean> getVerboseConfiguration();

    Property<Boolean> getVerboseSynchronizer();

    @Override
    default void tag(CharSequence tag) {
        getTags().add(verifyNotExists(tag, getUniqueTags().get()));
    }

    @Override
    default void tags(CharSequence... tags) {
        val tagsList = new ArrayList<String>(tags.length);
        val uniqueTags = getUniqueTags().get();
        for (val tag : tags) {
            tagsList.add(verifyNotExists(tag, uniqueTags));
        }
        getTags().addAll(tagsList);
    }

    @Override
    default void tags(Iterable<? extends CharSequence> tags) {
        val tagsList = new ArrayList<String>();
        val uniqueTags = getUniqueTags().get();
        for (val tag : tags) {
            tagsList.add(verifyNotExists(tag, uniqueTags));
        }
        getTags().addAll(tagsList);
    }

    @Override
    default void verboseConfiguration(boolean verbose) {
        getVerboseConfiguration().set(verbose);
    }

    @Override
    default void verboseSynchronizer(boolean verbose) {
        getVerboseSynchronizer().set(verbose);
    }

    default String verifyNotExists(CharSequence tag, Set<String> uniqueTags) {
        val tagStr = tag.toString();
        if (uniqueTags.contains(tagStr)) {
            throw new IllegalArgumentException("Test synchronization tag [" + tagStr + "] was already added, please check your configuration.");
        }
        getUniqueTags().add(tagStr);
        return tagStr;
    }
}
