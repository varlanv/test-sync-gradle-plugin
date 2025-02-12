package com.varlanv.gradle.plugin;

import org.gradle.api.provider.Property;

/**
 * Extension for further configure default convention plugin if any module requires non-defaults.
 */
public interface InternalConventionExtension {

    static String name() {
        return "internalConvention";
    }

    Property<String> getIntegrationTestName();

    Property<Boolean> getInternalModule();
}
