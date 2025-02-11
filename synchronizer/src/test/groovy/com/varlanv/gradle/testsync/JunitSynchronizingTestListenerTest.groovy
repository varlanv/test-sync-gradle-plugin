package com.varlanv.gradle.testsync

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.ThrowingConsumer
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.platform.engine.TestTag

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference

class JunitSynchronizingTestListenerTest {

    String syncSysProperty = "fakeSyncSysProperty"
    String syncSysPropertySeparator = Constants.SYNC_PROPERTY_SEPARATOR
    String syncTagSeparator = Constants.TAG_SEPARATOR

    @AfterEach
    void cleanup() {
        System.clearProperty(syncSysProperty)
    }

    @Nested
    class SyncTagsFromSystemPropertyTest {

        @Test
        void "should return empty array when syncSysProperty is null"() {
            var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                syncSysProperty,
                syncSysPropertySeparator,
                syncTagSeparator
            )
            assert subject.array().length == 0
        }

        @ParameterizedTest
        @ValueSource(strings = ["", "  "])
        void "should return empty array when syncSysProperty is empty or blank"(String syncSysPropertyValue) {
            System.setProperty(syncSysProperty, syncSysPropertyValue)
            var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                syncSysProperty,
                syncSysPropertySeparator,
                syncTagSeparator
            )

            assert subject.array().length == 0
        }

        @Test
        void "should return empty array when syncSysProperty has no separator"() {
            System.setProperty(syncSysProperty, "qwe")
            var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                syncSysProperty,
                syncSysPropertySeparator,
                syncTagSeparator
            )

            assert subject.array().length == 0
        }

        @Test
        void "should return empty array when syncSysProperty has one property separator and right side is empty"() {
            System.setProperty(syncSysProperty, "qwe" + syncSysPropertySeparator)
            var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                syncSysProperty,
                syncSysPropertySeparator,
                syncTagSeparator
            )

            assert subject.array().length == 0
        }

        @Test
        void "should return array with one element when syncSysProperty has one property and lock file exists"() {
            useTempFile(
                file -> {
                    var tag = "qwe"
                    System.setProperty(syncSysProperty, tag + syncTagSeparator + file.toAbsolutePath())
                    var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                        syncSysProperty,
                        syncSysPropertySeparator,
                        syncTagSeparator
                    )

                    assert subject.array().length == 1
                    assert subject.array()[0].fileName() == file.toAbsolutePath().toString()
                    assert subject.array()[0].testTag() == TestTag.create(tag)
                    assert subject.array()[0].syncFileChannel() != null
                }
            )
        }

        @Test
        void "should return empty array when syncSysProperty has one property and lock file not exists"() {
            var fileRef = new AtomicReference<Path>()
            useTempFile(fileRef::set)
            var file = fileRef.get()
            assert Files.notExists(file)
            var tag = "qwe"
            System.setProperty(syncSysProperty, tag + syncTagSeparator + file.toAbsolutePath())
            var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                syncSysProperty,
                syncSysPropertySeparator,
                syncTagSeparator
            )

            assert subject.array().length == 0
        }

        @Test
        void "should return empty array when syncSysProperty has two properties and only one lock file exists"() {
            var fileRef = new AtomicReference<Path>()
            useTempFile(fileRef::set)
            var nonExistingFile = fileRef.get()
            assert Files.notExists(nonExistingFile)
            var tag1 = "qwe1"
            var tag2 = "qwe2"
            useTempFile(
                file -> {
                    System.setProperty(syncSysProperty, tag1 + syncTagSeparator + file.toAbsolutePath()
                        + syncSysPropertySeparator + tag2 + syncTagSeparator + nonExistingFile.toAbsolutePath()
                    )
                    var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                        syncSysProperty,
                        syncSysPropertySeparator,
                        syncTagSeparator
                    )

                    assert subject.array().length == 0
                }
            )
            var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                syncSysProperty,
                syncSysPropertySeparator,
                syncTagSeparator
            )

            assert subject.array().length == 0
        }
    }

    static void useTempFile(ThrowingConsumer<Path> action) {
        var file = Files.createTempFile("junit", "gradlesynctest")
        file.toFile().deleteOnExit()
        try {
            action.accept(file)
        } finally {
            Files.delete(file)
        }
    }
}
