package com.varlanv.gradle.plugin;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class TestUtils {

    private static final AtomicReference<Path> projectRoot = new AtomicReference<>();

    public static Path projectRoot() {
        return projectRoot.get();
    }

    public static void setProjectRoot(Supplier<Path> fileSupplier) {
        if (projectRoot.get() == null) {
            synchronized (TestUtils.class) {
                projectRoot.set(fileSupplier.get());
            }
        }
    }
}
