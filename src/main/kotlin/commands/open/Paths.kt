package commands.open

import GameMode
import gameMode
import kotlin.collections.sorted

val pathsDescription = """
    List and open various common paths
    If you pass 'cli' it will open in terminal instead of local folder
    See also: open, cli, and paths commands
""".trimIndent() +
        GameMode.entries.asSequence().flatMap { mode -> mode.generatedPaths.values.map { mode to it } }.groupBy { it.second.aliases.first() + it.second.type.description }.map { (_, paths) ->
            val modes = paths.map { it.first }.joinToString{it.abbreviation}
            val first = paths.first().second
            val aliases = first.aliases.drop(1).takeIf { it.isNotEmpty() }?.let { ", $it" } ?: ""
            first.aliases.first() +" ($modes)$aliases - " + first.type.description
        }.sorted().joinToString("\n")

val pathsUsage = "paths\n"+
        GameMode.entries.asSequence().flatMap { it.generatedPaths.values }.map { it.aliases.first() }.toSet().sorted().joinToString("\n")

val pathsAliases = (listOf("path") + GameMode.entries.flatMap { mode -> mode.generatedPaths.values.flatMap { it.aliases } }).toSet().toTypedArray()

fun paths(command: String, args: List<String>) {
    if (command == "path" && args.size == 1){
        paths(args.first(), listOf())
        return
    }
    val openType = OpenType.entries.firstOrNull { it.aliases.contains(command) }
    val gamePath = gameMode.generatedPaths.values.firstOrNull { it.aliases.contains(command) }
    if (openType != null) {
        openType.invoke(args)
    } else if (gamePath != null) {
        open(gamePath.path(), gamePath.type.name, args.contains("cli"))
    } else {
        gameMode.generatedPaths.values.forEach {
            println("${it.aliases.first()} - ${it.type.description}")
        }
    }
}
