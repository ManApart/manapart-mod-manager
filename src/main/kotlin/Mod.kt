import commands.edit.Tag
import commands.deploy.espTypes
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File

@Serializable
data class Mod(
    var name: String,
    var filePath: String,
    var loadOrder: Int,
    var id: Int? = null,
    var plugins: List<String> = emptyList(),
    var creationId: String? = null,
    var deployTarget: PathType = gameConfig.defaultDeployTarget,
    var downloadPath: String? = null,
    var fileId: Int? = null,
    var latestFileId: Int? = null,
    var version: String? = null,
    var latestVersion: String? = null,
    var enabled: Boolean = false,
    var categoryId: Int? = null,
    var endorsed: Boolean? = null,
    val requiredIds: MutableSet<Int> = mutableSetOf(),
    val requiredNames: MutableSet<String> = mutableSetOf(),
    val tags: MutableSet<String> = mutableSetOf(),
) {
    @Transient
    var show: Boolean = true

    @Transient
    var index: Int = 0

    fun getModPaths(): List<String> {
        val modRoot = File(filePath).absolutePath + "/"
        return getModFiles().map { it.absolutePath.replace(modRoot, "") }
    }

    fun getModFiles(): List<File> {
        return File(filePath).getFiles {
            !it.path.contains("${filePath}/fomod", ignoreCase = true)
                    && !it.path.contains("${filePath}/optional", ignoreCase = true)
        }
    }

    fun url() = "https://www.nexusmods.com/${gameMode.urlName}/mods/$id"

    fun updateAvailable() = latestVersion != null && latestVersion != version

    fun idName(): String {
        return if (id == null) name else "$id $name"
    }

    fun description(): String {
        return "$index (${id ?: "?"}) $name"
    }

    fun category(): String? {
        return categoryId?.let { gameConfig.categories[it] }
    }

    fun add(tag: Tag) {
        tags.add(tag.tag)
        save()
    }

    fun remove(tag: Tag) {
        tags.remove(tag.tag)
        save()
    }

    fun hasTag(tag: Tag) = hasTag(tag.tag)
    fun hasTag(tag: String) = tags.contains(tag)


    fun require(mod: Mod) {
        val modId = mod.id
        if (modId != null) requiredIds.add(modId) else requiredNames.add(mod.name)
        save()
    }

    fun removeRequired(mod: Mod) {
        requiredIds.remove(mod.id)
        requiredNames.remove(mod.name)
        save()
    }

    fun getDependantMods() = toolData.mods.filter { child -> (id != null && child.requiredIds.contains(id) || child.requiredNames.contains(name))  }

    fun getRequiredMods() = requiredIds.mapNotNull { toolData.byId(it) } + requiredNames.mapNotNull { toolData.byName(it) }

    fun getAllRequiredMods(depth: Int = 100): List<Mod> {
        return getRequiredMods().flatMap { listOf(it) + it.getAllRequiredMods(depth - 1) }.toSet().toList()
    }

    fun refreshPlugins() {
        if (!hasTag(Tag.EXTERNAL)) {
            val newPlugins = discoverPlugins()

            if (plugins.toSet().sorted() != newPlugins.toSet().sorted()) {
                plugins = newPlugins
            }
        }
    }

    fun discoverPlugins() = getModFiles().filter { it.extension.lowercase() in espTypes }.map { it.name }
}

fun File.getFiles(filterFunction: (File) -> Boolean = { true }): List<File> {
    val fileList = listFiles() ?: arrayOf()
    val (folders, files) = fileList.partition { it.isDirectory }
    return files.filter { filterFunction(it) } + folders.flatMap { it.getFiles(filterFunction) }
}

fun File.getFolders(filterFunction: (File) -> Boolean = { true }): List<File> {
    val fileList = listFiles() ?: arrayOf()
    val folders = fileList.filter { it.isDirectory }
    return folders.filter { filterFunction(it) } + folders.flatMap { it.getFolders(filterFunction) }
}
