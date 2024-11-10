package io.huskit.gradle.plugin;

import org.gradle.api.provider.Property;

public interface HuskitInternalConventionExtension {

    static String name() {
        return "huskitConvention";
    }

    Property<String> getIntegrationTestName();
}
