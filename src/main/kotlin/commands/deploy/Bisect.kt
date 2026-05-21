package commands.deploy

import Column
import ENABLED
import Mod
import Table
import cyan
import green
import toolData

val bisectDescription = """
    Allows you to bisect your enabled mods (do a binary search)
    Use this to find a single mod causing a crash, mod conflicts, or which mod is running slow etc
    It's recommended to save mods to a profile before running bisect so you can go back to before the bisect
    Bisect will auto deploy if you have that config setting on
    Bisect enable/disable should take requirements into account, so it may not always be 50% each step
    Bisect itself is in memory only and doesn't save changes to disk
""".trimIndent()

val bisectUsage = """
    bisect new
    bisect hit (h)
    bisect miss (m)
    bisect back (b)
    bisect list (ls)
    bisect example
""".trimIndent()

private data class Step(val enabled: Set<Mod>, val disabled: Set<Mod> = setOf()) {
    fun apply() {
        enabled.forEach { enableMod(true, it) }
        disabled.forEach { enableMod(false, it) }
        autoDeploy()
    }
}

private val steps = mutableListOf<Step>()

fun bisect(command: String, args: List<String>) {
    when (args.firstOrNull()) {
        "new", "n" -> createBisect()
        "back", "b" -> back()
        "hit", "h" -> hit()
        "miss", "m" -> miss()
        "list", "ls" -> list()
        "example", "ex" -> printExample()
        else -> println(bisectDescription)
    }
}

private fun list() {
    val last = steps.lastOrNull()
    if (last == null) {
        println("No step")
        return
    }
    val columns = listOf(
        Column("Enabled", 10),
        Column("Mod", 40),
    )
    val mods = last.enabled.map { Triple(it.index, it.idName(), ENABLED) } + last.disabled.map { Triple(it.index, it.idName(), "") }
    val data = mods.sortedBy { it.first }.map { (_, name, enabled) ->
        mapOf(
            "Enabled" to enabled,
            "Mod" to name,
        )
    }
    Table(columns, data).print()
}

private fun createBisect() {
    steps.clear()
    steps.add(Step(toolData.mods.filter { it.enabled }.toSet()))
    println("Created")
}

private fun back() {
    val last = steps.lastOrNull()
    steps.remove(last)
    if (last == null) {
        println("No previous step")
        return
    }
    steps.lastOrNull()?.apply()
    println("Reverted")
}

private fun hit() {
    val last = steps.lastOrNull()
    if (last == null) {
        println("No step. Create a bisect to start")
        return
    }
    val newDisabled = last.enabled.take(last.enabled.size / 2).toSet()
    val newStep = Step(last.enabled - newDisabled, last.disabled + newDisabled)
    steps.add(newStep)
    newStep.apply()
    println("Hit")
}

private fun miss() {
    val last = steps.lastOrNull()
    if (last == null) {
        println("No step. Create a bisect to start")
        return
    }
    val newDisabled = last.disabled.take(last.enabled.size / 2).toSet()
    val flippedEnabled = last.disabled - newDisabled
    val newEnabled = flippedEnabled + last.enabled
    val newStep = Step(newEnabled, newDisabled)
    steps.add(newStep)
    newStep.apply()
    println("Miss")
}


private fun printExample() {
    val columns = listOf(
        Column("Mod", 5),
        Column("Enabled", 10),
    )

    println("Assume the bad mod is mod 1, and it makes us crash on load. Mod 5 was already disabled, so it's not part of our bisect.")
    printSection("bisect new", "Bisect sets up for tracking", "We load the game and crash", columns, row(true, true, true, true, false))
    printSection("bisect hit", "We tell bisect we failed. It disables half the mods", "We launch and don't crash", columns, row(false, false, true, true, false))
    printSection(
        "bisect miss",
        "The bad mod was disabled, so we didn't crash. Which disabled mod was it?",
        "Bisect flips the enabled status of the mods in the last bisect, then bisects that smaller list (1 and 2)",
        columns,
        row(false, true, true, true, false)
    )
    println("We launch, so we know the bad mod is mod 1")
}

private fun printSection(command: String, label: String, result: String, columns: List<Column>, row: List<Map<String, Any>>) {
    println("─────────────────")
    println(label + "\n")
    println(green(command))
    Table(columns, row).print()
    println("\n" + result)
    println()
}

private fun row(vararg enabled: Boolean): List<Map<String, String>> {
    return enabled.mapIndexed { i, b ->
        mapOf(
            "Enabled" to if (b) ENABLED else "",
            "Mod" to "${i + 1}",
        )
    }
}
