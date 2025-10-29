package commands.view

import Column
import Mod
import Table
import capitalize
import commands.getIndicesOrRange
import toolData

val detailDescription = """
   View all mod detail
""".trimIndent()
val detailUsage = """
   detail <mod id>
""".trimIndent()

fun detailMod(command: String, args: List<String>) {
    if (args.isEmpty()) {
        println(detailDescription)
    } else {
        args.getIndicesOrRange(toolData.mods.size)
            .mapNotNull { toolData.byIndex(it) }
            .forEach { viewDetail(it) }
    }
}

private fun viewDetail(mod: Mod) {
    val columns = listOf(
        Column("Field", 20),
        Column("Value", 60),
    )
    val data = mod.toString().drop(4).dropLast(1).split(", ").map { l ->
        val parts = l.split("=")
        mapOf("Field" to parts[0].capitalize(), "Value" to parts[1])
    } + listOf(mapOf("Field" to "Category Name", "Value" to (mod.category() ?: "")))
    println(mod.name)
    Table(columns, data.sortedBy { it["Field"] }).print()
}
