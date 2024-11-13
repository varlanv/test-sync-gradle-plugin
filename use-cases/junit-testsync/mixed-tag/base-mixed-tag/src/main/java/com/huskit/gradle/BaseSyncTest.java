package org.huskit.gradle;

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

    @RepeatedTest(10)
    @Tags({@Tag("my_mixed_tag_1"), @Tag("my_mixed_tag_2"), @Tag("my_mixed_tag_3")})
    void synchronized_test() throws Exception {
        var testSyncFilePath = System.getProperty("testMixedSyncFile");
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
