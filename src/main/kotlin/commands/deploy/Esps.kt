package commands.deploy

import Column
import Mod
import Table
import clearConsole
import save
import toolData

val espTypes = listOf("esp", "esm", "esl")

val espDescription = """
    Plugins with a higher load order are loaded later, and override plugins loaded earlier. Given plugin A has an order 5 and plugin B has an order of 1, then A will load AFTER B, and A's records will be used instead of B's in any conflicts. 
    In these examples the first number is the mod index and the second is the sort order you want
    esp ls - view load order of plugins
    esp 1 set 4 - sets mod with index 1 to load order 4. Any plugins with a higher number for load order have their number increased
    esp 1 later - sets the plugin to load after the next plugin (may shift the order number more than one as it shifts until it's after the next plugin).
    esp 1 later 2 - same as above but shifts down by two plugins
    esp 1 sub 2 set 0 - Used to re-order plugins within a single mod. Finds the plugin of mod 1 that is in sub load order 2 and sets it to sub load order 0.
    For general mod order, see order command. The same order number is used for both commands.
""".trimIndent()

val espUsage = """
    esp ls
    esp refresh
    esp refresh 1
    esp 1 first
    esp 1 last
    esp 1 sooner 5
    esp 1 later
    esp 1 set 4
    esp 1 sub 1 set 0
""".trimIndent()

fun esp(command: String, args: List<String>) {
    val arguments = parseArgs(args)
        ?: when {
            args.size == 1 && args.last() == "ls" -> {
                val mods = getModsWithPlugins()
                if (mods.isEmpty()) {
                    println("Found no plugins. Consider running esp refresh")
                } else {
                    display(mods)
                }
                return
            }

            args.firstOrNull() == "refresh" -> {
                val i = args.last().toIntOrNull()
                if (i != null) {
                    toolData.byIndex(i)?.let {
                        it.refreshPlugins()
                        save()
                        println("Refreshed Plugins for mod ${it.description()}")
                    }
                } else {
                    refreshPlugins()
                    save()
                    println("Refreshed Plugins")
                }
                return
            }

            else -> {
                println(orderDescription)
                return
            }
        }
    with(arguments) {
        val mods = getModsWithPlugins()
        when (subCommand) {
            "first" -> setModOrder(toolData.mods, index, 0)
            "last" -> setModOrder(toolData.mods, index, toolData.nextLoadOrder())
            "set" if amount != null -> setModOrder(toolData.mods, index, amount)
            "sooner" if amount != null -> setModOrder(
                toolData.mods, index,
                nextPluginLoadOrder(mods, index, -amount)
            )

            "later" if amount != null -> setModOrder(
                toolData.mods, index,
                nextPluginLoadOrder(mods, index, amount)
            )

            "sooner" -> setModOrder(toolData.mods, index, nextPluginLoadOrder(mods, index, -1))
            "later" -> setModOrder(toolData.mods, index, nextPluginLoadOrder(mods, index, 1))
            "sub" -> setSubOrder(index, amount ?: 0, args.last().toIntOrNull() ?: 0)
            else -> println("Unknown subCommand: ")
        }
        display(getModsWithPlugins())
    }
}

private fun refreshPlugins() {
    toolData.mods.forEach { it.refreshPlugins() }
}

private fun getModsWithPlugins(): Map<Mod, List<String>> {
    return toolData.mods
        .filter { it.enabled }.sortedBy { it.loadOrder }
        .associateWith { mod -> mod.plugins }
        .filter { (_, files) -> files.isNotEmpty() }
}

private fun nextPluginLoadOrder(mods: Map<Mod, List<String>>, modIndexToShift: Int, pluginShiftAmount: Int): Int {
    return nextPlugin(mods, modIndexToShift, pluginShiftAmount)?.loadOrder ?: 0
}

private fun nextPluginIndex(mods: Map<Mod, List<String>>, modIndexToShift: Int, pluginShiftAmount: Int): Int {
    return nextPlugin(mods, modIndexToShift, pluginShiftAmount)?.index ?: -1
}

private fun nextPlugin(mods: Map<Mod, List<String>>, modIndexToShift: Int, pluginShiftAmount: Int): Mod? {
    val sortedMods = mods.keys.toList().sortedBy { it.loadOrder }
    val i = sortedMods.firstOrNull { it.index == modIndexToShift }?.let { sortedMods.indexOf(it) } ?: -1
    return sortedMods.getOrNull(i + pluginShiftAmount)
}

private fun display(mods: Map<Mod, List<String>>) {
    val modFiles = mods.flatMap { (mod, files) -> files.mapIndexed { i, plugin -> mod to Pair(i, plugin) } }
    val espWidth = modFiles.maxOf { it.second.second.length } + 5

    clearConsole()
    val columns = listOf(
        Column("Id", 7),
        Column("Load", 7),
        Column("Index", 7, true),
        Column("Esp", espWidth),
        Column("Mod", 22),
    )
    val data = modFiles.map { (mod, pluginPair) ->
        with(mod) {
            val (i, plugin) = pluginPair
            val idClean = id?.toString() ?: "?"
            val order = if (i == 0) "$loadOrder" else "$loadOrder-$i"
            mapOf(
                "Index" to mod.index,
                "Load" to order,
                "Id" to idClean,
                "Esp" to plugin,
                "Mod" to name,
            )
        }
    }
    Table(columns, data).print()
}

private fun setSubOrder(index: Int, initialOrder: Int, newOrder: Int) {
    toolData.byIndex(index)?.let { mod ->
        val plugin = mod.plugins.getOrNull(initialOrder)
        if (plugin == null) println("No Plugin found at $initialOrder") else {
            println("${mod.description()}: Setting '$plugin' at position $initialOrder to $newOrder")
            val newPlugins = mod.plugins.toMutableList()
            newPlugins.remove(plugin)
            newPlugins.add(newOrder, plugin)
            mod.plugins = newPlugins
            save()
        }
    }
}
