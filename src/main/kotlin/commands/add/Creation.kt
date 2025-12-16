package commands.add

import Column
import Mod
import Table
import cleanModName
import confirm
import gameConfig
import jsonMapper
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject
import modFolder
import red
import save
import toolData
import verbose
import yellow
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import GamePath.*
import commands.edit.Tag
import commands.deploy.deleteLink
import commands.deploy.espTypes
import gameMode
import logFetch

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Creation(
    @JsonNames("AchievementSafe") val achievementSafe: Boolean,
    @JsonNames("Files") val files: List<String>,
    @JsonNames("Title") val title: String,
    @JsonNames("Version") val version: String,
) {
    var creationId: String? = null
    val modFileId = files.firstOrNull { it.contains("esm") }?.replace(".esm", "")
}

val creationDescription = """
   Tools for managing creations just like other mods
   creation ls - lists unmanaged creations by examining your game content catalog
   creation add - adds a single creation by its mod file id (like SFBGS021) or content id (like TM_31ccf130-4852-417b-842a-9d82672028e4). Also see add mod
   creation add all - Attempts to add _all_ unmanaged creations found in the content catalog
   creation rm - unmanage this creation, dumping the files back into the data directory (deletes the "mod" but unmanages the files)
   creation forget - delete the creation's entry in your content catalog, so it doesn't show up as unmanaged (remove the creation before forgetting it)
   creation refresh - copy any creation related files from the data directory, overriding existing mod files. Used to update a creation if there are new files
""".trimIndent()

val creationUsage = """
   creation ls
   creation add <id>
   creation add all
   creation rm <index>
   creation rm all
   cration forget <id>
   creation refresh <index>
   creation refresh all
   
""".trimIndent()

fun creation(command: String, args: List<String>) {
    val firstArg = args.firstOrNull() ?: ""
    val i = args.getOrNull(1)?.toIntOrNull()
    val mod = i?.let { toolData.byIndex(it) }
    when {
        args.isEmpty() -> println(creationDescription)
        listOf("ls", "list").contains(firstArg) -> listCreations()
        args.getOrNull(1) == "all" && firstArg == "add" -> addAllCreations()
        args.getOrNull(1) == "all" && firstArg == "rm" -> rmAllCreations()
        args.getOrNull(1) == "all" && firstArg == "refresh" -> refreshAllCreations()
        firstArg == "add" -> addCreation(args[1])
        firstArg == "forget" -> forgetCreation(args[1])
        firstArg == "rm" -> when {
            mod == null -> println("Unable to find a valid mod at index $i")
            !mod.hasTag(Tag.CREATION) -> println("${mod.description()} is not a creation.")
            else -> rmCreation(mod)
        }

        firstArg == "refresh" -> when {
            mod == null -> println("Unable to find a valid mod at index $i")
            !mod.hasTag(Tag.CREATION) -> println("${mod.description()} is not a creation.")
            else -> refreshCreation(mod)
        }

        else -> println("Unknown args: ${args.joinToString(" ")}")
    }
}

fun parseCreationCatalog(): Map<String, Creation> {
    val rawLines = File(gameMode.path(PathType.APP_DATA) + "/ContentCatalog.txt").readLines()
    val parsable = jsonMapper.decodeFromString<JsonObject>(rawLines.joinToString("\n")).filter { it.key != "ContentCatalog" }.toMap().let { jsonMapper.encodeToString(it) }
    return jsonMapper.decodeFromString<Map<String, Creation>>(parsable).also { it.entries.forEach { (id, creation) -> creation.creationId = id } }
}

fun updateCreationCatalog(creations: List<Creation>) {
    val rawLines = File(gameMode.path(PathType.APP_DATA) + "/ContentCatalog.txt").readLines()
    val header = "{\n\t\"ContentCatalog\": " + jsonMapper.decodeFromString<JsonObject>(rawLines.joinToString("\n"))["ContentCatalog"].let { jsonMapper.encodeToString(it) } + ","

    val creationsString = jsonMapper.encodeToString(creations.associateBy { it.creationId }).drop(1)
    val fullText = header + creationsString + "\n"

    File(gameMode.path(PathType.APP_DATA) + "/ContentCatalog.txt").writeText(fullText)
}

fun parseCreationPlugins(creationCatalog: Map<String, Creation> = parseCreationCatalog()): List<String> {
    return creationCatalog.values.flatMap { creation -> creation.files.filter { file -> espTypes.any { file.endsWith(it) } } }
}

private fun listCreations() {
    val columns = listOf(
        Column("Creation Id", 42),
        Column("Mod File Id", 35),
        Column("Managed", 15),
        Column("Mod Index", 10),
        Column("Title", 22),
    )
    val data = parseCreationCatalog().values.map { creation ->
        val mod = creation.creationId?.let { toolData.byCreationId(it) }
        mapOf(
            "Creation Id" to (creation.creationId ?: ""),
            "Mod File Id" to (creation.modFileId ?: ""),
            "Managed" to (mod != null),
            "Mod Index" to (mod?.index ?: ""),
            "Title" to (mod?.name ?: creation.title),
        )
    }

    Table(columns, data).print()
}

private fun addAllCreations(force: Boolean = false) {
    val creations = parseCreationCatalog().values.filter { it.creationId == null || toolData.byCreationId(it.creationId!!) == null }
    if (creations.isEmpty()) {
        println("No creations found")
    } else {
        confirm(force, yellow("Add unmanaged creations? ") + creations.joinToString(", ") { it.title }) {
            creations.forEach { addCreation(it) }
            println("Added Creations")
        }
    }
}

fun addCreation(creationId: String): Creation? {
    val creations = parseCreationCatalog()
    val creation = creations.getCreationById(creationId)
    creation?.let { addCreation(it) }
    return creation
}

fun addCreation(creation: Creation) {
    val files = creation.files.map { File(gameConfig[GAME] + "/Data/$it") }
    if (files.isEmpty()) {
        println(red("No files found for ${creation.title}"))
    }

    val existing = toolData.byName(creation.title.lowercase(), true)
    val mod = if (existing != null) existing else {
        val loadOrder = toolData.nextLoadOrder()
        val stagePath = modFolder.path + "/" + creation.title.cleanModName()
        Mod(creation.title.lowercase(), stagePath, loadOrder + 1).also {
            it.index = toolData.mods.size
            it.creationId = creation.creationId
            it.add(Tag.CREATION)
            it.refreshPlugins()
            toolData.mods.add(it)
            save()
        }
    }

    val dataFolderFiles = File(gameConfig[GAME] + "/Data").listFiles()!!
    val dest = File(mod.filePath + "/Data").also { it.mkdirs() }
    files.forEach { initialFile ->
        var file: File? = initialFile
        //Catalog can have incorrect file casing, so we should perform a backup search
        if (!initialFile.exists()) {
            val search = initialFile.name.lowercase()
            file = dataFolderFiles.firstOrNull { it.name.lowercase() == search }
        }
        if (file == null || !file.exists()) {
            verbose(red(initialFile.absolutePath + " does not exist"))
            println(red("Unable to find creation files in data. Are you sure it's properly downloaded?"))
            rmCreation(mod, true)
            return
        } else {
            Files.move(file.toPath(), File(dest.path + "/" + file.name).toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    if (existing != null) {
        println("Updated (${mod.index}) ${mod.name}")
    } else {
        println("Added (${mod.index}) ${mod.name}")
        logFetch(mod.creationId ?: mod.name)
    }
}

private fun Map<String, Creation>.getCreationById(id: String) = this[id]
    ?: values.firstOrNull { it.modFileId.equals(id, ignoreCase = true) }
    ?: (keys.firstOrNull { it.equals(id, true) }?.let { this[it] })

private fun rmAllCreations(force: Boolean = false) {
    val creations = toolData.mods.filter { it.creationId != null }
    confirm(force, yellow("Remove All Creations? ") + creations.joinToString(", ") { it.name }) {
        creations.forEach { rmCreation(it, true) }
    }
}

fun rmCreation(mod: Mod, force: Boolean = false) {
    confirm(force, yellow("Remove Creation ${mod.description()} ")) {
        val modRoot = File(mod.filePath).absolutePath + "/"
        mod.getModFiles()
            .forEach { file ->
                val linkFile = file.absolutePath.replace(modRoot, "")
                deleteLink(linkFile, mapOf())
                val destFile = File(file.absolutePath.replace(modRoot, gameConfig[GAME] + "/"))
                if (!destFile.exists()) {
                    Files.move(file.toPath(), destFile.toPath())
                }
            }
        delete(mod)
    }
}

private fun forgetCreation(creationId: String) {
    val creations = parseCreationCatalog()
    val creation = creations.getCreationById(creationId)
    if (creation == null) {
        println(red("Could not find creation $creationId"))
    } else {
        updateCreationCatalog(creations.minus(creation.creationId).values.toList())
        println("Forgot creation $creationId")
    }
}

private fun refreshAllCreations() {
    confirm(false, "Refresh All Creations?") {
        rmAllCreations(true)
        addAllCreations(true)
    }
}

private fun refreshCreation(mod: Mod) {
    confirm(false, yellow("Refresh Creation ${mod.description()} ")) {
        rmCreation(mod)
        mod.creationId?.let { addCreation(it) }
    }
}
