package commands.edit

import Mod
import red
import toolData

val requireDescription = """
    Mark a mod as requiring another mod
    When a mod is enabled, any required mods are also enabled
    When a mod is disabled, any mods depending on that mod are also disabled
    require lists direct requirements of this mod
    require all lists requirements recursively so you can see the full tree
    require child shows children who require this mod
""".trimIndent()

val requireUsage = """
    require <index>
    require <index> all
    require <index> child
    require <index> add 456
    require <index> remove 456
""".trimIndent()

fun require(command: String, args: List<String>) {
    val mod = args.firstOrNull()?.toIntOrNull()?.let { toolData.byIndex(it) }
    val other = args.lastOrNull()?.toIntOrNull()?.let { toolData.byIndex(it) }
    val subCommand = args.firstOrNull { it.toIntOrNull() == null }
    when {
        args.isEmpty() || mod == null -> println("Specify the index of a mod to see requirements")
        subCommand == "all" -> printAllRequirements(mod)
        subCommand == "child" -> printChildren(mod)
        subCommand == null -> printRequirements(mod)
        other == null -> println("You need to specify the mod to add/remove")
        subCommand == "add" -> {
            mod.require(other)
            println("${mod.name} now requires ${other.idName()}")
        }

        subCommand == "remove" -> {
            mod.removeRequired(other)
            println("${mod.name} no longer requires ${other.idName()}")
        }
    }
}

private fun printRequirements(mod: Mod) {
    val mods = mod.getRequiredMods()
    if (mods.isEmpty()) println("${mod.name} doesn't require any mods") else {
        println(mod.indexName())
        println("\t" + mods.joinToString("\n\t") { it.indexName() })
    }
}

private fun printAllRequirements(mod: Mod) {
    if (mod.getRequiredMods().isEmpty()) println("${mod.name} doesn't require any mods") else {
        printAllRequirements(mod, 0)
    }
}

private fun printAllRequirements(mod: Mod, depth: Int) {
    if (depth > 100) {
        println(red("Dependency chain greater than 100. Do you have a required mod loop?"))
        return
    }
    println("\t".repeat(depth) + mod.indexName())
    mod.getRequiredMods().forEach { printAllRequirements(it, depth + 1) }
}

private fun printChildren(mod: Mod) {
    val mods = mod.getDependantMods()
    if (mods.isEmpty()) println("${mod.name} isn't needed by any mods") else {
        println(mod.indexName())
        println("\t" + mods.joinToString("\n\t") { it.indexName() })
    }
}
