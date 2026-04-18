package commands.view

import Column
import ENABLED
import FOLDER
import Mod
import THUMBS_DOWN
import THUMBS_UP
import Table
import UPDATE
import clearConsole
import toolData
import truncate
import java.io.File

val listDescription = """
    List Mod details
    You can give a start and amount if you want to list just a subsection
    list 10 30 would list 30 mods, starting with the 10th mod
    list load will sort mods by load order instead of index
""".trimIndent()

val listUsage = """
    List
    list <start> <amount>
    List load
""".trimIndent()

enum class ListSort(val comparator: (Mod) -> Int) {
    INDEX({ it.index }),
    LOAD({ it.loadOrder }),
}

fun listMods(command: String, args: List<String> = listOf()) {
    val ranges = args.mapNotNull { it.toIntOrNull() }
    val sort = if (args.contains("load")) ListSort.LOAD else ListSort.INDEX
    when {
        ranges.isNotEmpty() -> displayAmount(ranges, sort)
        else -> displayShown(toolData.mods.map { it to it.show }, sort)
    }
}

private fun displayAmount(ranges: List<Int>, sort: ListSort) {
    val start = ranges.first()
    val amount = ranges.getOrNull(1) ?: 20
    var shownCount = 0
    val shownMods = toolData.mods.map { mod ->
        val shown = if (mod.show && mod.index >= start && shownCount < amount) {
            shownCount++
            true
        } else false
        mod to shown
    }
    displayShown(shownMods, sort)
}

fun displayShown(mods: List<Pair<Mod, Boolean>>, sort: ListSort = ListSort.INDEX) = display(mods.filter { it.second }.map { it.first }, sort)
fun display(mods: List<Mod>, sort: ListSort = ListSort.INDEX) {
    clearConsole()
    val columns = listOf(
        Column("Id", 7),
        Column("Version", 12),
        Column("Load", 7, true),
        Column("Status", 10),
        Column("Category", 20),
        Column("Index", 7, true),
        Column("Name", 60),
        Column("Tags", 30),
    )
    val data = mods.sortedBy(sort.comparator).map { mod ->
        with(mod) {
            val enabledCheck = if (enabled) ENABLED else "  "
            val endorsedCheck = when (endorsed) {
                true -> THUMBS_UP
                false -> THUMBS_DOWN
                else -> "  "
            }
            val idClean = id?.toString() ?: "?"
            val versionClean = when {
                version != null && latestVersion != null && version != latestVersion -> "$UPDATE${version.truncate()}"
                version != null -> "  " + version.truncate(8)
                latestVersion != null -> "$UPDATE?"
                else -> "  ?"
            }
            val staged = if (File(filePath).exists()) FOLDER else "  "
            val category = category()?.take(19) ?: "?"
            mapOf(
                "Index" to mod.index,
                "Status" to "$staged $enabledCheck $endorsedCheck",
                "Load" to loadOrder,
                "Id" to idClean,
                "Version" to versionClean,
                "Category" to category,
                "Name" to name,
                "Tags" to tags.joinToString(", "),
            )
        }
    }
    Table(columns, data).print()
}
