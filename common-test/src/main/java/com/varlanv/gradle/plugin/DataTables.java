package com.varlanv.gradle.plugin;

import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Value
public class DataTables {

    List<Boolean> isCiList;
    List<Boolean> configurationCacheList;
    List<Boolean> buildCacheList;
    List<String> gradleVersions;

    public static Stream<DataTable> streamDefault() {
        return getDefault().list().stream();
    }

    public static DataTables getDefault() {
        if (Objects.equals(System.getenv("CI"), "true")) {
            return new DataTables(
                List.of(false),
                List.of(false),
                List.of(false),
                List.of(TestGradleVersions.current()
                )
            );
        } else {
            return new DataTables(
                List.of(true, false),
                List.of(true, false),
                List.of(true, false),
                TestGradleVersions.list()
            );
        }
    }

    public List<DataTable> list() {
        List<DataTable> result = new ArrayList<>();
        gradleVersions.forEach(gradleVersion ->
            isCiList.forEach(isCi ->
                configurationCacheList.forEach(configurationCache ->
                    buildCacheList.forEach(buildCache ->
                        result.add(new DataTable(isCi, configurationCache, buildCache, gradleVersion))
                    )
                )
            )
        );
        return result;
    }
}
