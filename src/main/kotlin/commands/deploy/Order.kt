package commands.deploy

import Column
import Mod
import Table
import red
import save
import sun.util.calendar.CalendarUtils.mod
import toolData

val orderDescription = """
    Mods with a higher load order are loaded later, and override mods loaded earlier. Given mod A has an order 5 and mod B has an order of 1, then A will load AFTER B, and A's files will be used instead of B's in any file conflicts. 
    In these examples the first number is the mod index and the second is the sort order you want
    order 1 set 4 - sets mod with index 1 to load order 4. Any mods with a higher number for load order have their number increased
    order 1 - view any conflicts mod index 1 has with any other mods
    To manage load order specifically for plugins, see esps command. The same order number is used for both commands.
    order 1 optimize - Moves every mod to load after their latest requirement. Dry run gives you a preview. You may want to backup your data first in case you don't like the new order.
    Optimize is best effort and may need manual correction
""".trimIndent()

val orderUsage = """
    order 1
    order 1 first
    order 1 last
    order 1 sooner 5
    order 1 later
    order 1 set 4
    order 1 optimize
    order 1 optimize dry
""".trimIndent()

data class Args(val index: Int, val subCommand: String, val amount: Int?)

fun order(command: String, args: List<String>) {
    val arguments = parseArgs(args)
    val isOpt = args.firstOrNull() == "optimize" || args.firstOrNull() == "op"
    when {
        isOpt && args.contains("dry") -> optimizeMods(true)
        isOpt -> optimizeMods(false)
        arguments == null && args.size == 1 && args.last().toIntOrNull() != null -> {
            toolData.mods.getOrNull(args.last().toInt())?.let { showOverrides(it) }
        }

        arguments == null -> println(orderDescription)
        else ->
            with(arguments) {
                when (subCommand) {
                    "first" -> setModOrder(toolData.mods, index, 0)
                    "last" -> setModOrder(toolData.mods, index, toolData.nextLoadOrder())
                    "set" if amount != null -> setModOrder(toolData.mods, index, amount)
                    "sooner" if amount != null -> setModOrder(toolData.mods, index, index - amount)
                    "later" if amount != null -> setModOrder(toolData.mods, index, index + amount)
                    "sooner" -> setModOrder(toolData.mods, index, index - 1)
                    "later" -> setModOrder(toolData.mods, index, index + 1)
                    else -> println("Unknown subCommand: ")
                }
            }
    }
}

fun parseArgs(args: List<String>): Args? {
    val index = args.getOrNull(0)?.toIntOrNull()
    val subCommand = args.getOrNull(1)
    val amount = args.getOrNull(2)?.toIntOrNull()
    return if (index != null && subCommand != null) {
        Args(index, subCommand, amount)
    } else null
}

fun setModOrder(mods: List<Mod>, modIndex: Int, position: Int) {
    if (position < 0) return
    val mod = mods.getOrNull(modIndex)
    if (mod == null) {
        println(red("No mod found at $modIndex"))
        return
    }
    mod.getRequiredMods().firstOrNull { it.loadOrder > position }?.let {
        println(red("${mod.indexName()} ($position) must load AFTER ${it.indexName()} (${it.loadOrder})"))
        return
    }
    mod.getDependantMods().firstOrNull { it.loadOrder < position }?.let {
        println(red("${mod.indexName()} ($position) must load BEFORE ${it.indexName()} (${it.loadOrder})"))
        return
    }
    println("Setting mod $modIndex at position ${mod.loadOrder} to position $position")
    val oldOrder = mod.loadOrder
    mods.filter { it.loadOrder > oldOrder }.forEach { it.loadOrder -= 1 }
    mods.filter { it.loadOrder >= position }.forEach { it.loadOrder += 1 }
    mod.loadOrder = position
    save()
}

private fun optimizeMods(dryRun: Boolean) {
    val columns = listOf(
        Column("Index", 7),
        Column("Current Load", 15),
        Column("New Load", 15),
        Column("Name", 60),
        Column("After", 60),
    )
    val data = mutableListOf<Map<String, Any>>()
    toolData.mods.forEach { mod ->
        val reqs = mod.getRequiredMods()
        if (reqs.isEmpty()) return@forEach
        val req = reqs.maxBy { it.loadOrder }
        val pos = req.loadOrder + 1
        if (mod.loadOrder == pos) return@forEach
        data.add(mapOf(
            "Index" to mod.index,
            "Current Load" to mod.loadOrder,
            "New Load" to pos,
            "Name" to mod.name,
            "After" to req.name
        ))
        if (!dryRun) {
            setModOrder(toolData.mods, mod.index, pos)
        }
    }
    Table(columns, data).print()
}
