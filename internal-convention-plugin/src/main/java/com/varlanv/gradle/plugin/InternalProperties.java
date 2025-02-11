package com.varlanv.gradle.plugin;

import lombok.RequiredArgsConstructor;
import org.gradle.api.artifacts.VersionCatalog;
import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.api.provider.Provider;

@RequiredArgsConstructor
class InternalProperties {

    VersionCatalog versionCatalog;

    public static String name() {
        return "__internal_properties__";
    }

    public String getLib(String name) {
        return versionCatalog.findLibrary(name)
            .map(maybeLib -> maybeLib.map(lib -> String.format("%s:%s:%s", lib.getGroup(), lib.getName(), lib.getVersion())))
            .map(Provider::getOrNull)
            .orElseThrow(() -> new IllegalArgumentException("No library found for name: " + name));

    }

    public String getVersion(String name) {
        return versionCatalog.findVersion(name)
            .map(VersionConstraint::getRequiredVersion)
            .orElseThrow(() -> new IllegalArgumentException("No version found for name: " + name));
    }
}
