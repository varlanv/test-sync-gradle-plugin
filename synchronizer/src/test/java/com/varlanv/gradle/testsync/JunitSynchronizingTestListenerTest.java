package com.varlanv.gradle.testsync;

import com.varlanv.gradle.plugin.BaseTest;
import com.varlanv.gradle.plugin.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.TestTag;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class JunitSynchronizingTestListenerTest implements BaseTest {

    String syncSysProperty = "fakeSyncSysProperty";
    String syncSysPropertySeparator = Constants.SYNC_PROPERTY_SEPARATOR;
    String syncTagSeparator = Constants.TAG_SEPARATOR;

    @AfterEach
    void cleanup() {
        System.clearProperty(syncSysProperty);
    }

    @Nested
    class SyncTagsFromSystemPropertyTest implements UnitTest {

        @Test
        @DisplayName("should return empty array when 'syncSysProperty' is null")
        void should_return_empty_array_when_syncSysProperty_is_null() {
            var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                syncSysProperty,
                syncSysPropertySeparator,
                syncTagSeparator
            );

            assertThat(subject.array()).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "  "})
        @DisplayName("should return empty array when 'syncSysProperty' is empty or blank")
        void should_return_empty_array_when_syncSysProperty_is_empty_or_blank(String syncSysPropertyValue) {
            System.setProperty(syncSysProperty, syncSysPropertyValue);
            var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                syncSysProperty,
                syncSysPropertySeparator,
                syncTagSeparator
            );

            assertThat(subject.array()).isEmpty();
        }

        @Test
        @DisplayName("should return empty array when 'syncSysProperty' has no separator")
        void should_return_empty_array_when_syncSysProperty_has_no_separator() {
            System.setProperty(syncSysProperty, "qwe");
            var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                syncSysProperty,
                syncSysPropertySeparator,
                syncTagSeparator
            );

            assertThat(subject.array()).isEmpty();
        }

        @Test
        @DisplayName("should return empty array when 'syncSysProperty' has one property separator and right side is empty")
        void should_return_empty_array_when_syncSysProperty_has_one_property_separator_and_right_side_is_empty() {
            System.setProperty(syncSysProperty, "qwe" + syncSysPropertySeparator);
            var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                syncSysProperty,
                syncSysPropertySeparator,
                syncTagSeparator
            );

            assertThat(subject.array()).isEmpty();
        }

        @Test
        @DisplayName("should return array with one element when 'syncSysProperty' has one property and lock file exists")
        void should_return_array_with_one_element_when_syncSysProperty_has_one_property_and_lock_file_exists() {
            useTempFile(file -> {
                var tag = "qwe";
                System.setProperty(syncSysProperty, tag + syncTagSeparator + file.toAbsolutePath());
                var subject = new JunitSynchronizingTestListener.SyncTagsFromSystemProperty(
                    syncSysProperty,
                    syncSysPropertySeparator,
                    syncTagSeparator
                );

                assertThat(subject.array()).hasSize(1);
                assertThat(subject.array()[0].fileName()).isEqualTo(file.toAbsolutePath().toString());
                assertThat(subject.array()[0].testTag()).isEqualTo(TestTag.create(tag));
                assertThat(subject.array()[0].syncFileChannel()).isNotNull();
            });
        }

        @Test
        @DisplayName("should return empty array when 'syncSysProperty' has one property and lock file not exists")
        void should_return_empty_array_when_syncSysProperty_has_one_property_and_lock_file_not_exists() {
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
        @DisplayName("should return empty array when 'syncSysProperty' has two properties and only one lock file exists")
        void should_return_empty_array_when_syncSysProperty_has_two_properties_and_only_one_lock_file_exists() {
            var fileRef = new AtomicReference<Path>();
            useTempFile(fileRef::set);
            var nonExistingFile = fileRef.get();
            assertThat(nonExistingFile).doesNotExist();
            var tag1 = "qwe1";
            var tag2 = "qwe2";
            useTempFile(file -> {
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
}
