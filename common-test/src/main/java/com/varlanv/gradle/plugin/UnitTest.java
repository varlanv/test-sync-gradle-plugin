package com.varlanv.gradle.plugin;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;

@Tags({@Tag(BaseTest.UNIT_TEST_TAG), @Tag(BaseTest.FAST_TEST_TAG)})
public interface UnitTest extends BaseTest {
}
