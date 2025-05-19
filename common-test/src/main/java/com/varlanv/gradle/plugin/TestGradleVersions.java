package com.varlanv.gradle.plugin;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestGradleVersions {

    static List<String> list() {
        return List.of(
            current(),
            latest7()
//            latest6()
        );
    }

    static String current() {
        return latest8();
    }

    static String latest7() {
        return "7.6.1";
    }

    static String latest8() {
        return "8.14";
    }

    static String latest6() {
        return "6.9.4";
    }
}
