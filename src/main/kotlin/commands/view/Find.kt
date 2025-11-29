package commands.view

import cyan
import toolData

val findDescription = """
    Find all files that have the given text in their path
    If you use find esp, the search will only look at plugin names
""".trimIndent()
val findUsage = """
    Find files
    find <search text> 
    find esp <search text>
""".trimIndent()

fun find(command: String, args: List<String> = listOf()) {
    when {
        args.size > 1 && args.first() == "esp" -> searchPlugins(args)
        args.isNotEmpty() -> searchFiles(args)
        else -> println("Please provide a search term")
    }
}

private fun searchPlugins(args: List<String>) {
    toolData.mods
        .map { mod -> mod to mod.plugins.filter { plugin -> args.any { plugin.lowercase().contains(it) } } }
        .filter { it.second.isNotEmpty() }
        .sortedBy { it.first.index }
        .also { if (it.isEmpty()) println("No plugins found") }
        .forEach { (mod, matches) ->
            println("${mod.index} ${cyan(mod.name)} (${matches.size} matches)")
            matches.forEach { println("\t$it") }
        }
}

private fun searchFiles(args: List<String>) {
    toolData.mods
        .map { mod -> mod to mod.getModFiles().map { it.path }.filter { file -> args.any { file.lowercase().contains(it) } } }
        .filter { it.second.isNotEmpty() }
        .sortedBy { it.first.index }
        .also { if (it.isEmpty()) println("No files found") }
        .forEach { (mod, matches) ->
            println("${mod.index} ${cyan(mod.name)} (${matches.size} matches)")
            matches.forEach { println("\t$it") }

        }
}
