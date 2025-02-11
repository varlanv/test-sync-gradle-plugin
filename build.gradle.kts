import kotlin.io.path.readText
import kotlin.io.path.writeText

abstract class IncrementVersion : DefaultTask() {

    @InputDirectory
    abstract fun getRootProjectFile(): DirectoryProperty

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
            "Patch" -> "${currentVersionParts[0]}.${currentVersionParts[1]}.${Integer.valueOf(currentVersionParts[2]) + 1}"
            "Minor" -> "${currentVersionParts[0]}.${Integer.valueOf(currentVersionParts[1]) + 1}.0"
            "Major" -> "${Integer.valueOf(currentVersionParts[0]) + 1}.0.0"
            else -> throw IllegalStateException("Unknown version semantic -> $versionSemantic")
        }

        listOf(
            rootProjectPath.resolve("constants").resolve("src").resolve("main").resolve("java").resolve("com").resolve("varlanv").resolve("gradle").resolve("testsync").resolve("Constants.java"),
            rootProjectPath.resolve("gradle.properties"),
            rootProjectPath.resolve("README.md")
        ).forEach {
            val text = it.readText(Charsets.UTF_8)
            val newText = text.replace(currentVersion, newVersion)
            if (text != newText) {
                it.writeText(newText, Charsets.UTF_8)
            }
        }
    }
}

listOf("Patch", "Minor", "Major").forEach {
    tasks.register("increment${it}Version", IncrementVersion::class) {
        getRootProjectFile().set(project.layout.projectDirectory)
        getVersionSemantic().set(it)
        getCurrentVersion().set(properties["version"].toString())
    }
}
