import kotlin.io.path.readText
import kotlin.io.path.writeText

abstract class IncrementVersion : DefaultTask() {

    @Input
    abstract fun getRootProjectFile(): RegularFileProperty

    @Input
    abstract fun getVersionSemantic(): Property<String>

    @Input
    abstract fun getCurrentVersion(): Property<String>

    @TaskAction
    fun run() {
        val versionSemantic = getVersionSemantic().get()
        val rootProjectPath = getRootProjectFile().get().asFile.toPath()
        val currentVersion = getCurrentVersion().get()
        val currentVersionParts = currentVersion.split('.')
        val newVersion = when (versionSemantic) {
            "patch" -> "${currentVersionParts[0]}.${currentVersionParts[1]}.${Integer.valueOf(currentVersionParts[2]) + 1}"
            "minor" -> "${currentVersionParts[0]}.${Integer.valueOf(currentVersionParts[1]) + 1}.0"
            "major" -> "${Integer.valueOf(currentVersionParts[0]) + 1}.0.0"
            else -> throw IllegalStateException("Unknown version semantic -> $versionSemantic")
        }

        val constantsFile = rootProjectPath.resolve("constants").resolve("src").resolve("main").resolve("java").resolve("com").resolve("varlanv").resolve("gradle").resolve("testsync").resolve("Constants")
        val propertiesFile = rootProjectPath.resolve("gradle.properties")
        val readmeFile = rootProjectPath.resolve("README.md")
        listOf(constantsFile, propertiesFile, readmeFile).forEach {
            val text = it.readText(Charsets.UTF_8)
            val newText = text.replace(currentVersion, newVersion)
            if (text != newText) {
                it.writeText(newText, Charsets.UTF_8)
            }
        }
    }
}

tasks.register("incrementPatchVersion", IncrementVersion::class) {
    getRootProjectFile().set(project.rootDir)
    getVersionSemantic().set("patch")
    getCurrentVersion().set(properties["version"].toString())
}

tasks.register("incrementMinorVersion", IncrementVersion::class) {
    getRootProjectFile().set(project.rootDir)
    getVersionSemantic().set("minor")
    getCurrentVersion().set(properties["version"].toString())
}

tasks.register("incrementMajorVersion", IncrementVersion::class) {
    getRootProjectFile().set(project.rootDir)
    getVersionSemantic().set("major")
    getCurrentVersion().set(properties["version"].toString())
}
