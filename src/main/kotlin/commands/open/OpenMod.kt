package commands.open

import Mod
import commands.CommandType
import commands.getIndicesOrRange
import gameMode
import runCommand
import toolConfig
import toolData
import java.awt.Desktop
import java.io.File
import java.net.URI

val openCliDescription = """
    Open various folders and filepaths as well as mods
    Use paths command to see paths you can open
    See also: local, open, and paths commands
""".trimIndent()

val openLocalDescription = """
    Open various folders and filepaths as well as mods
    If you pass 'cli' it will open in terminal instead of local folder
    Use paths command to see paths you can open
    See also: open, cli, and paths commands
""".trimIndent()

val openDescription = """
    Open a mod on nexus or visit several other sites
    See also: local, cli, and paths commands
""".trimIndent() +
        OpenType.entries.sortedBy { it.aliases.first() }.joinToString("\n") { it.aliases.first() + " - " + it.description }

val openUsage = """
    open <index>
""".trimIndent() + "\n" +
        OpenType.entries.sortedBy { it.aliases.first() }.joinToString("\n") { it.aliases.first() }

val cliUsage = """
    cli <index> 
    cli <path>
""".trimIndent()

val localUsage = """
    local <index> *cli
    local <path>
""".trimIndent()

val openAliases = (listOf("o") + OpenType.entries.flatMap { it.aliases }).toTypedArray()

enum class OpenType(
    val aliases: List<String>,
    val description: String,
    val invoke: (List<String>) -> Unit
) {
    MANUAL(listOf("manual", "man"), "Online Manual of commands", { openInWeb("https://manapart.github.io/starfield-mod-manager-site/manual.html") }),
    SITE(listOf("site"), "Open manager's main site", { openInWeb("https://manapart.github.io/starfield-mod-manager-site/index.html") }),
    SOURCE(listOf("git", "github", "source"), "Open source for mod manager", { openInWeb("https://github.com/ManApart/starfield-mod-manager") }),
    NEXUS(listOf("nexus"), "Open the mod on nexus mods", ::openNexus),
}

fun open(command: String, args: List<String>) {
    val commandType = CommandType.entries.firstOrNull { it.name.lowercase() == command || it.aliases.contains(command) } ?: CommandType.OPEN
    val openInWeb = commandType == CommandType.OPEN
    val cli = commandType == CommandType.CLI || args.contains("cli")

    val openType = OpenType.entries.firstOrNull { it.aliases.contains(command) }
    val gamePath = gameMode.generatedPaths.values.firstOrNull { it.aliases.contains(command) || (args.size == 1 && it.aliases.contains(args.first())) }
    if (openType != null) {
        openType.invoke(args)
    } else if (gamePath != null) {
        open(gamePath.path(), gamePath.type.name, cli)
    } else {
        openMod(openInWeb, cli, command, args)
    }
}

private fun openNexus(args: List<String>) {
    if (args.size != 2 || args[1].toIntOrNull() != null) {
        println("Must pass a mod index to open")
        return
    }
    val modId = toolData.byIndex(args[1].toInt())?.id
    if (modId == null) {
        println("Unable to find mod")
        return
    }
    openInWeb("https://www.nexusmods.com/starfield/mods/${modId}")
}

private fun openMod(web: Boolean = true, cliCommand: Boolean, command: String, args: List<String>) {
    if (args.isEmpty() || (args.size == 1 && args.first() == "cli")) {
        val commandType = CommandType.entries.firstOrNull {it.name.lowercase() == command} ?: CommandType.OPEN
        println(commandType.description + "\n")
        println(commandType.usage)
        return
    }
    val cli = cliCommand || args.contains("cli")
    val mods = args.filter { it != "cli" }.getIndicesOrRange(toolData.mods.size).mapNotNull { toolData.mods.getOrNull(it) }
    when {
        mods.isEmpty() -> println(openDescription)
        web -> mods.forEach { openInWeb(it) }
        else -> mods.forEach { openLocal(it, cli) }
    }
}

fun openInWeb(mod: Mod) {
    if (mod.id == null) {
        println("Can't open ${mod.name} because it doesn't have an id.")
    } else {
        openInWeb(mod.url(), mod.name)
    }
}

fun openInWeb(url: String, urlName: String = url) {
    try {
        Desktop.getDesktop().browse(URI(url))
    } catch (e: Exception) {
        println("Unable to open $urlName in web")
    }
}

fun openLocal(mod: Mod, cli: Boolean) = open(mod.filePath, mod.name, cli)

fun open(path: String, name: String, cli: Boolean) {
    try {
        if (cli) {
            val cleanPath = if (path.endsWith(".txt")) File(path).parent else path
            val command = toolConfig.openInTerminalCommand?.replace("{pwd}", cleanPath) ?: "gnome-terminal"
            File(cleanPath).runCommand(command)
        } else {
            Desktop.getDesktop().open(File(path))
        }
    } catch (e: Exception) {
        println("Unable to open $name on disk ($path)")
    }
}
