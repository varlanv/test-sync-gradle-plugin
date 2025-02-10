package com.varlanv.gradle.testsync;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.TestTag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JunitSynchronizingTestListenerTest {

    String syncSysProperty = "fakeSyncSysProperty";
    String syncSysPropertySeparator = Constants.SYNC_PROPERTY_SEPARATOR;
    String syncTagSeparator = Constants.TAG_SEPARATOR;

    @AfterEach
    void cleanup() {
        System.clearProperty(syncSysProperty);
    }

    @Nested
    class SyncTagsFromSystemPropertyTest {

        @Test
        void when_syncSysProperty_is_null__then_array_is_empty() {
            var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                syncSysProperty,
                syncSysPropertySeparator,
                syncTagSeparator
            );

            assertThat(subject.array()).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "  "})
        void when_syncSysProperty_is_empty__then_array_is_empty(String syncSysPropertyValue) {
            System.setProperty(syncSysProperty, syncSysPropertyValue);
            var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                syncSysProperty,
                syncSysPropertySeparator,
                syncTagSeparator
            );

            assertThat(subject.array()).isEmpty();
        }

        @Test
        void when_syncSysProperty_has_no_separator__then_array_is_empty() {
            System.setProperty(syncSysProperty, "qwe");
            var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                syncSysProperty,
                syncSysPropertySeparator,
                syncTagSeparator
            );

            assertThat(subject.array()).isEmpty();
        }

        @Test
        void when_syncSysProperty_has_one_property_separator__and_right_side_is_empty__then_array_is_empty() {
            System.setProperty(syncSysProperty, "qwe" + syncSysPropertySeparator);
            var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                syncSysProperty,
                syncSysPropertySeparator,
                syncTagSeparator
            );

            assertThat(subject.array()).isEmpty();
        }

        @Test
        void when_syncSysProperty_has_one_property_and_lock_file_exists__then_array_has_one_element() {
            useTempFile(
                file -> {
                    var tag = "qwe";
                    System.setProperty(syncSysProperty, tag + syncTagSeparator + file.toAbsolutePath());
                    var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                        syncSysProperty,
                        syncSysPropertySeparator,
                        syncTagSeparator
                    );

                    assertThat(subject.array())
                        .hasSize(1)
                        .satisfiesExactly(
                            syncTag -> {
                                assertThat(syncTag.fileName()).isEqualTo(file.toAbsolutePath().toString());
                                assertThat(syncTag.testTag()).isEqualTo(TestTag.create(tag));
                                assertThat(syncTag.syncFileChannel()).isNotNull();
                            }
                        );
                }
            );
        }

        @Test
        void when_syncSysProperty_has_one_property_and_lock_file_not_exists__then_array_is_empty() {
            var fileRef = new AtomicReference<Path>();
            useTempFile(fileRef::set);
            var file = fileRef.get();
            assertThat(file).doesNotExist();
            var tag = "qwe";
            System.setProperty(syncSysProperty, tag + syncTagSeparator + file.toAbsolutePath());
            var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                syncSysProperty,
                syncSysPropertySeparator,
                syncTagSeparator
            );

            assertThat(subject.array()).isEmpty();
        }

        @Test
        void when_syncSysProperty_has_two_properties_and_only_one_lock_file_exists__then_array_is_empty() {
            var fileRef = new AtomicReference<Path>();
            useTempFile(fileRef::set);
            var nonExistingFile = fileRef.get();
            assertThat(nonExistingFile).doesNotExist();
            var tag1 = "qwe1";
            var tag2 = "qwe2";
            useTempFile(
                file -> {
                    System.setProperty(syncSysProperty, tag1 + syncTagSeparator + file.toAbsolutePath()
                        + syncSysPropertySeparator + tag2 + syncTagSeparator + nonExistingFile.toAbsolutePath()
                    );
                    var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                        syncSysProperty,
                        syncSysPropertySeparator,
                        syncTagSeparator
                    );

                    assertThat(subject.array()).isEmpty();
                }
            );
            var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                syncSysProperty,
                syncSysPropertySeparator,
                syncTagSeparator
            );

            assertThat(subject.array()).isEmpty();
        }
    }

    @SneakyThrows
    void useTempFile(ThrowingConsumer<Path> action) {
        var file = Files.createTempFile("junit", "gradlesynctest");
        file.toFile().deleteOnExit();
        try {
            action.accept(file);
        } finally {
            Files.delete(file);
        }
    }
}
