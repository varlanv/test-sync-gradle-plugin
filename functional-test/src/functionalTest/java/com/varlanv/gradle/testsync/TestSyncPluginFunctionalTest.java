package com.varlanv.gradle.testsync;

import com.varlanv.gradle.plugin.DataTable;
import com.varlanv.gradle.plugin.FunctionalTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

class TestSyncPluginFunctionalTest implements FunctionalTest {

    @ParameterizedTest
    @MethodSource("defaultDataTables")
    @DisplayName("should succeed")
    void should_succeed(DataTable dataTable) {
        runGradleRunnerFixture(
            dataTable,
            List.of("check", "--parallel"),
            fixture -> {
                copyFolderContents(
                    projectRoot().resolve("use-cases").resolve("junit-testsync"),
                    fixture.subjectProjectDir()
                );

                build(fixture.runner());
            });
    }
}