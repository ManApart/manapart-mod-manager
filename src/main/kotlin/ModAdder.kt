import kotlinx.coroutines.runBlocking
import nexus.*
import java.io.File
import kotlin.math.max

fun addModById(id: Int, fileId: Int? = null, forceRedownload: Boolean = false) {
    val existing = toolData.byId(id)
    if (existing != null) {
        println("Mod already exists. Refreshing...")
        refreshMod(existing, forceRedownload, fileId)
        return
    }
    val mod = runBlocking { fetchModInfo(id, fileId) } ?: return
    val downloaded = downloadMod(mod, forceRedownload, fileId)
    if (downloaded == null) {
        println(red("Failed to download ${mod.name}"))
    } else {
        addModFile(mod, downloaded, mod.name, mod.latestVersion)
    }
}

fun refreshMod(mod: Mod, forceRedownload: Boolean = false, fileId: Int? = null) {
    val downloaded = if (forceRedownload || mod.downloadPath?.let { File(it) }?.exists() != true) {
        if (mod.id == null) {
            println(red("Unable to refresh ${mod.name} because it has no id or local file"))
            null
        } else {
            downloadMod(mod, forceRedownload, fileId)
        }
    } else {
        File(mod.downloadPath!!)
    }

    if (downloaded == null) {
        println(red("Failed to download ${mod.name}"))
    } else {
        addModFile(mod, downloaded, mod.name, mod.latestVersion)
    }
}

private fun downloadMod(mod: Mod, forceRedownload: Boolean, fileId: Int? = null): File? {
    println("Downloading ${mod.id}: ${mod.name}")
    val cleanName = mod.name.replace(" ", "-")
    val downloadUrl = runBlocking { getDownloadUrl(toolConfig.apiKey!!, mod.id!!, fileId ?: mod.fileId!!) }
    verbose("Downloading with url $downloadUrl")
    if (downloadUrl == null) {
        println(red("Unable to get download url for ${mod.name}"))
        return null
    }
    val destination = "$HOME/Downloads/${gameMode.modFolder}/$cleanName${parseFileExtension(downloadUrl)}"
    return downloadMod(downloadUrl, destination, forceRedownload)
}

suspend fun fetchModInfo(id: Int, fileId: Int? = null): Mod? {
    val modInfo = getModDetails(toolConfig.apiKey!!, id)
        ?: return null.also { println(red("Unable to get mod info for $id")) }
    val modFileId = fileId ?: getModFiles(toolConfig.apiKey!!, id)?.getPrimaryFile()

    if (modFileId == null) {
        println(red("Could not find primary file for $id"))
        return null
    }

    val modName = modInfo.name.lowercase()
    val cleanName = modName.cleanModName()
    val filePath = modFolder.path + "/" + cleanName
    toolData.createOrUpdate(modInfo.mod_id, modName, filePath)
    toolData.update(modInfo, true, modFileId)
    return toolData.byId(modInfo.mod_id)!!.also { save() }
}

suspend fun updateModInfo(id: Int) {
    val modInfo = getModDetails(toolConfig.apiKey!!, id)
    if (modInfo == null) {
        println(red("Unable to get mod info for ${toolData.byId(id)?.description() ?: id}"))
        return
    }
    val modFileId = getModFiles(toolConfig.apiKey!!, id)?.getPrimaryFile()
    if (modFileId == null) {
        println(red("Could not find primary file for $id"))
        return
    }

    toolData.update(modInfo, false, modFileId)
    save()
}

private val versionComparator = Comparator { fileA: ModFileInfoFile, fileB: ModFileInfoFile ->
    val versionA = fileA.version.split(".").mapNotNull { it.toIntOrNull() }
    val versionB = fileB.version.split(".").mapNotNull { it.toIntOrNull() }
    val max = max(versionA.size, versionB.size)
    (0..max).forEach { i ->
        val a = versionA.getOrNull(i) ?: 0
        val b = versionB.getOrNull(i) ?: 0
        when {
            a > b -> return@Comparator -1
            a < b -> return@Comparator 1
            else -> {}
        }
    }
    0
}

fun ModFileInfo.getPrimaryFile(): Int? {
    return files.let { files ->
        if (files.size == 1) files.first().file_id else {
            files.firstOrNull { it.is_primary }?.file_id
        } ?: files.sortedWith(versionComparator).firstOrNull()?.file_id
    }
}

fun addModByNexusProtocol(url: String) {
    val request = parseDownloadRequest(url)
    val modInfo = runBlocking { getModDetails(toolConfig.apiKey!!, request.modId) }
    if (modInfo == null) {
        println(red("Unable to download $url"))
        return
    }
    val modName = modInfo.name.lowercase()
    val cleanName = modName.cleanModName()
    val filePath = modFolder.path + "/" + cleanName
    toolData.createOrUpdate(modInfo.mod_id, modName, filePath)
    toolData.update(modInfo, true, request.fileId)
    val mod = toolData.byId(modInfo.mod_id)!!
    save()
    println("Downloading $modName")
    val downloadUrl = runBlocking { getDownloadUrl(toolConfig.apiKey!!, request) }
    if (downloadUrl == null) {
        println(red("Unable to get download url for $modName"))
        return
    }
    val destination = "$HOME/Downloads/${gameMode.modFolder}/$cleanName${parseFileExtension(downloadUrl)}"
    val downloaded = downloadMod(downloadUrl, destination)
    if (downloaded == null) {
        println(red("Failed to download ${mod.name}"))
    } else {
        addModFile(mod, downloaded, modName)
        logFetch(listOf(request.modId))
    }
}

fun addModByFile(filePath: String, nameOverride: String?) {
    val name = nameOverride ?: File(filePath).nameWithoutExtension
    val sourceFile = File(filePath)

    val existing = toolData.byName(name, true)
    val mod = if (existing != null) existing else {
        val loadOrder = toolData.nextLoadOrder()
        val stagePath = modFolder.path + "/" + name.replace(" ", "-")
        Mod(name, stagePath, loadOrder + 1).also {
            it.index = toolData.mods.size
            toolData.mods.add(it)
            save()
        }
    }

    addModFile(mod, sourceFile, name)
    logFetch(name)
}

fun addModFile(mod: Mod, sourceFile: File, modName: String, version: String? = mod.latestVersion) {
    if (!sourceFile.exists()) {
        println(red("Could not find ${sourceFile.absolutePath}"))
        return
    }
    if (mod.downloadPath != sourceFile.absolutePath) {
        mod.downloadPath = sourceFile.absolutePath
        save()
    }
    val stageFile = File(mod.filePath)
    val stageExists = stageFile.exists()
    if (stageMod(sourceFile, stageFile, modName)) {
        mod.refreshPlugins()
        if (stageExists) {
            println("Updated (${mod.index}) ${mod.name}")
        } else {
            println("Added (${mod.index}) ${mod.name}")
        }
    } else {
        println(red("Failed to add mod ${mod.name}"))
    }
    mod.version = version
}

fun String.cleanModName() = replace(" ", "-").replace("[^A-Za-z\\-]".toRegex(), "")
