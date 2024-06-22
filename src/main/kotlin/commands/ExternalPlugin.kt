package commands

import Column
import Mod
import Table
import confirmation
import modFolder
import save
import toolConfig
import toolData
import yellow
import java.io.File
import java.nio.file.Files


val externalModDescription = """
Tools for managing external plugins
external ls - lists unmanaged plugins by examining your game data folder
external add - adds a single plugin by its id (like SFBGS003). Also see add mod
external add all - Attempts to add _all_ unmanaged plugins found in the data folder

""".trimIndent()

val externalModUsage = """
   external ls
   external add <id>
   external add all
""".trimIndent()

fun externalMod(args: List<String>) {
    val firstArg = args.firstOrNull() ?: ""
    when {
        args.isEmpty() -> println(externalModDescription)
        listOf("ls", "list").contains(firstArg) -> listExternal()
        args.getOrNull(1) == "all" && firstArg == "add" -> addAllExternal()
        firstArg == "add" -> addExternal(args.getOrNull(1)!!, args.getOrNull(2))

        else -> println("Unknown args: ${args.joinToString(" ")}")
    }
}

private fun listExternal() {
    val columns = listOf(
        Column("Esp", 42),
        Column("Index", 15),
        Column("Name", 22),
    )
    val data = getExternalMods().map { (esp, mod) ->
        mapOf(
            "Esp" to esp,
            "Index" to (mod?.index ?: ""),
            "Name" to (mod?.name ?: ""),
        )
    }

    Table(columns, data).print()
}

private fun addAllExternal() {
    val mods = getExternalMods().filter { it.value == null }.keys
    println(yellow("Add unmanaged creations? ") + mods.joinToString(", ") + " (y/n)")
    confirmation = { c ->
        if (c.firstOrNull() == "y") {
            mods.forEach { addExternal(it.split(".")[0]) }
        }
    }
}

fun addExternal(esp: String, name: String? = null) {
    val mods = getExternalMods()
    val cleanKey = mods.keys.firstOrNull { it.lowercase().split(".")[0] == esp.lowercase() }
    if (cleanKey == null) {
        println("Could not find $esp")
        return
    }
    if (mods[esp] != null) {
        println("Mod already managed as ${mods[esp]?.description()}")
        return
    }
    val usedName = (name ?: esp).lowercase()

    val loadOrder = toolData.nextLoadOrder()
    val stagePath = modFolder.path + "/" + usedName.replace(" ", "-")
    val mod = Mod(usedName, stagePath, loadOrder + 1).also {
        it.index = toolData.mods.size
        it.add(Tag.EXTERNAL)
        it.plugins = listOf(cleanKey)
        toolData.mods.add(it)
        save()
    }

    println("Added (${mod.index}) ${mod.name}")
}

private fun getExternalMods(): Map<String, Mod?> {
    val creations = parseCreationPlugins()
    return File(toolConfig.gamePath!! + "/Data").listFiles()!!
        .asSequence()
        .filter {
            it.extension in espTypes
                    && !Files.isSymbolicLink(it.toPath())
                    && !creations.contains(it.name)
        }
        .associate { it.name to modFromEsp(it.name) }
}

private fun modFromEsp(esp: String): Mod? {
    return toolData.mods.firstOrNull { it.plugins.contains(esp) }
}