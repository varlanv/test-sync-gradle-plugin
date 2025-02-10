package com.varlanv.gradle;

import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@DisplayNameGeneration(BaseSyncTest.DisplayName.class)
public abstract class BaseSyncTest {

    @Test
    void unsynchronized_test() {
        // no-op
    }

    @Tag("mytag")
    @RepeatedTest(10)
    void synchronized_test() throws Exception {
        var testSyncFilePath = System.getProperty("testSingleSyncFile");
        Assertions.assertNotNull(testSyncFilePath);
        Assertions.assertFalse(testSyncFilePath.isBlank());
        var syncFile = Paths.get(testSyncFilePath);
        Assertions.assertTrue(Files.exists(syncFile));

        var fileContent = Files.readString(syncFile);
        Assertions.assertEquals("", fileContent);

        Files.write(syncFile, "synced".getBytes());

        var newFileContent = Files.readString(syncFile);
        Assertions.assertEquals("synced", newFileContent);

        Files.write(syncFile, "".getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
    }

    static class DisplayName extends DisplayNameGenerator.Standard {

        @Override
        public String generateDisplayNameForClass(Class<?> testClass) {
            return testClass.getSimpleName() + " - " + System.getProperty("gradleProjectName");
        }
    }
}
