
package com.varlanv.gradle.plugin;

public record DataTable(boolean isCi,
                        boolean configurationCache,
                        Boolean buildCache,
                        String gradleVersion) {
}