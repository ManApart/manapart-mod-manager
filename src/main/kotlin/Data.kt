import nexus.ModInfo
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Profile(val name: String, var ids: List<Int>, var filePaths: List<String>) {
    fun modCount() = ids.size + filePaths.size
}

@Serializable
data class Data(
    val mods: MutableList<Mod> = mutableListOf(),
    val profiles: MutableList<Profile> = mutableListOf(),
) {
    fun byId(id: Int) = mods.firstOrNull { it.id == id }
    fun byCreationId(id: String) = mods.firstOrNull { it.creationId == id }
    fun byIndex(i: Int, silent: Boolean = false) = mods.getOrNull(i).also { if (it == null && !silent) println("No Mod found for $i") }
    fun byName(name: String, silent: Boolean = false) = mods.firstOrNull { it.name == name }.also { if (it == null && !silent) println("No Mod found for $name") }
    fun byFilePath(path: String, silent: Boolean = false) = mods.firstOrNull { it.filePath == path }.also { if (it == null && !silent) println("No Mod found for $path") }
    fun byUniqueId(id: String, silent: Boolean = false) = (id.toIntOrNull()?.let { byId(it) } ?: byFilePath(id, true) ?: byName(id, true)).also { if (it == null && !silent) println("No mod found for $id") }
    fun nextLoadOrder() = (mods.maxOfOrNull { it.loadOrder } ?: -1) + 1
    fun updateSorts() = toolData.mods.forEachIndexed { i, mod -> mod.index = i }

    fun createOrUpdate(id: Int, name: String, filePath: String) {
        byId(id)?.also {
            it.name = name
            it.filePath = filePath
        } ?: Mod(name, filePath, nextLoadOrder(), id).also {
            it.index = mods.size
            mods.add(it)
        }
    }

    fun update(info: ModInfo, updateCurrentVersion: Boolean = true, updatedFileId: Int? = null) {
        byId(info.mod_id)?.apply {
            if (updateCurrentVersion) version = info.version
            latestVersion = info.version
            categoryId = info.category_id
            endorsed = info.endorsement.endorseStatus.isEndorsed()
            updatedFileId?.let {
                if (updateCurrentVersion) fileId = it
                latestFileId = it
            }
        }
    }

    fun profileByIndex(i: Int) = profiles.getOrNull(i).also { if (it == null) println("No Profile found for $i") }
}
