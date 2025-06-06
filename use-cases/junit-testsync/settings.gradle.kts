pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "junit-testsync"

fun pathToRoot(dir: File, parts: MutableList<String> = mutableListOf()): String {
    val targetFolderNameMarket = "internal-convention-plugin"
    return when {
        dir.resolve(targetFolderNameMarket).exists() -> parts.joinToString("") { "../" }
        dir.parentFile != null -> pathToRoot(dir.parentFile, parts.apply { add(dir.name) })
        else -> throw IllegalStateException("Cannot find a directory containing $targetFolderNameMarket")
    }
}

// only include parent project for manual testing
if (!providers.provider({ System.getenv("FUNCTIONAL_SPEC_RUN") }).isPresent) {
    includeBuild(pathToRoot(rootProject.projectDir))
}

include(
    "single-tag:base-single-tag",
    "single-tag:single-tag-1",
    "single-tag:single-tag-2",
    "single-tag:single-tag-3",
    "single-tag:single-tag-4",
    "single-tag:single-tag-5",
    "single-tag:single-tag-6",
    "single-tag:single-tag-7",
    "single-tag:single-tag-8",
    "single-tag:single-tag-9",
    "single-tag:single-tag-10",
    "multi-tag:base-multi-tag",
    "multi-tag:multi-tag-1",
    "multi-tag:multi-tag-2",
    "multi-tag:multi-tag-3",
    "multi-tag:multi-tag-4",
    "multi-tag:multi-tag-5",
    "multi-tag:multi-tag-6",
    "multi-tag:multi-tag-7",
    "multi-tag:multi-tag-8",
    "multi-tag:multi-tag-9",
    "multi-tag:multi-tag-10",
    "mixed-tag:base-mixed-tag",
    "mixed-tag:mixed-tag-1",
    "mixed-tag:mixed-tag-2",
    "mixed-tag:mixed-tag-3",
    "mixed-tag:mixed-tag-4",
    "mixed-tag:mixed-tag-5",
    "mixed-tag:mixed-tag-6",
    "mixed-tag:mixed-tag-7",
    "mixed-tag:mixed-tag-8",
    "mixed-tag:mixed-tag-9",
    "mixed-tag:mixed-tag-10"
)
