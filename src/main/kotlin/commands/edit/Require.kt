package commands.edit

import Mod
import red
import toolData

val requireDescription = """
    Mark a mod as requiring another mod
    When a mod is enabled, any required mods are also enabled
    When a mod is disabled, any mods depending on that mod are also disabled
    require lists direct requirements of this mod
    require all lists all mods that have requirements and their first level required
    require <index> all lists requirements recursively so you can see the full tree
    require child shows children who require this mod
""".trimIndent()

val requireUsage = """
    require all
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
        args.isEmpty() -> println("Specify the index of a mod to see requirements")
        args.first() == "all" -> printAllRequirements()
        mod == null -> println("Specify the index of a mod to see requirements")
        subCommand == "all" -> printRequirements(mod, true)
        subCommand == "child" && args.contains("all") -> printChildren(mod, true)
        subCommand == "child" -> printChildren(mod, false)
        subCommand == null -> printRequirements(mod, false)
        other == null -> println("You need to specify the mod to add/remove")
        subCommand == "add" -> {
            mod.require(other)
            println("${mod.name} now requires ${other.idName()}")
        }

        subCommand == "remove" || subCommand == "rm" -> {
            mod.removeRequired(other)
            println("${mod.name} no longer requires ${other.idName()}")
        }
    }
}

private fun printAllRequirements() {
    toolData.mods.filter { it.hasRequiredMods() }.forEach { printRequirements(it, false) }
}

private fun printRequirements(mod: Mod, all: Boolean) {
    val mods = mod.getRequiredMods()
    if (mods.isEmpty()) println("${mod.name} doesn't require any mods") else {
        println(mod.indexName())
        mods.forEachIndexed { i, req ->
            printRequirements(req, i == mods.lastIndex, 1, " ", all)
        }
    }
}

private fun printRequirements(mod: Mod, isLast: Boolean, depth: Int, prefix: String = " ", recursive: Boolean = true) {
    if (depth > 100) {
        println(red("Dependency chain greater than 100. Do you have a required mod loop?"))
        return
    }
    val branch = if (isLast) "└─ " else "├─ "
    println(prefix + branch + mod.indexName())
    if (recursive) {
        val mods = mod.getRequiredMods()
        val childPrefix = prefix + if (isLast) "    " else "│   "
        mods.forEachIndexed { index, req ->
            printRequirements(req, index == mods.lastIndex, depth + 1, childPrefix)
        }
    }
}

private fun printChildren(mod: Mod, all: Boolean) {
    val mods = mod.getDependantMods()
    if (mods.isEmpty()) println("${mod.name} isn't needed by any mods") else {
        println(mod.indexName())
        mods.forEachIndexed { i, child ->
            printChildren(child, i == mods.lastIndex, 1, " ", all)
        }
    }
}

private fun printChildren(mod: Mod, isLast: Boolean, depth: Int, prefix: String = " ", recursive: Boolean = true) {
    if (depth > 100) {
        println(red("Dependency chain greater than 100. Do you have a required mod loop?"))
        return
    }
    val branch = if (isLast) "└─ " else "├─ "
    println(prefix + branch + mod.indexName())
    if (recursive) {
        val mods = mod.getDependantMods()
        val childPrefix = prefix + if (isLast) "    " else "│   "
        mods.forEachIndexed { index, req ->
            printChildren(req, index == mods.lastIndex, depth + 1, childPrefix)
        }
    }
}
