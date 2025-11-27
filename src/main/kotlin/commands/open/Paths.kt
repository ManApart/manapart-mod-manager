package commands.open

import Column
import GameMode
import Table
import gameConfig
import gameMode
import toTable
import toolConfig
import kotlin.collections.sorted

val pathsDescription = """
    List and open various common paths
    If you pass 'cli' it will open in terminal instead of local folder
    paths list - list the values of all paths
    You can override (or create new static) paths in the config (see config command)
    See also: open, cli, and paths commands
""".trimIndent() + "\n\n" +
        GameMode.entries.asSequence().flatMap { mode -> mode.generatedPaths.values.map { mode to it } }.groupBy { it.second.aliases.first() + it.second.type.description }.map { (_, paths) ->
            val modes = paths.map { it.first }.joinToString { it.abbreviation }
            val first = paths.first().second
            val aliases = first.aliases.drop(1).takeIf { it.isNotEmpty() }?.let { ", $it" } ?: ""
            first.aliases.first() + " ($modes)$aliases - " + first.type.description
        }.sorted().joinToString("\n")

val pathsUsage = "paths\n" +
        GameMode.entries.asSequence().flatMap { it.generatedPaths.values }.map { it.aliases.first() }.toSet().sorted().joinToString("\n")

val pathsAliases = (listOf("path") + GameMode.entries.flatMap { mode -> mode.generatedPaths.values.flatMap { it.aliases } }).toSet().toTypedArray()

fun paths(command: String, args: List<String>) {
    if (args.size == 1 && listOf("list", "ls").contains(args.first())) {
        listPaths()
        return
    }
    if (command == "path" && args.size == 1) {
        paths(args.first(), listOf())
        return
    }
    val openType = OpenType.entries.firstOrNull { it.aliases.contains(command) }
    val overridePath = gameConfig[command]
    val gamePath = gameMode.generatedPaths.values.firstOrNull { it.aliases.contains(command) }
    when {
        //TODO - this is broken, needs to be nullable
        overridePath != null -> open(overridePath, command, args.contains("cli"))
        openType != null -> openType.invoke(args)
        gamePath != null -> open(gamePath.path(), gamePath.type.name, args.contains("cli"))
        else -> {
            gameMode.generatedPaths.values.forEach {
                println("${it.aliases.first()} - ${it.type.description}")
            }
        }
    }
}

private fun listPaths() {
    val base = gameMode.generatedPaths.entries.associate { (type, generated) ->
        type.name to generated.path()
    }.toMutableMap()
    gameConfig.paths.forEach { (key, value) -> base[key] = value }
    base.entries.sortedBy { it.key }
        .toTable("Name", 25, "Value", 40).print()
}