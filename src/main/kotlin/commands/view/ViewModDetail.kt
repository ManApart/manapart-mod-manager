package commands.view

import Column
import Mod
import Table
import commands.getIndicesOrRange
import jsonMapperVerbose
import kotlinx.serialization.json.JsonObject
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
    val data = jsonMapperVerbose.decodeFromString<JsonObject>(jsonMapperVerbose.encodeToString(mod)).entries.map { (key, value) ->
        mapOf("Field" to key, "Value" to value.toString())
    } + listOf(mapOf("Field" to "Category Name", "Value" to (mod.category() ?: "")))
    println(mod.name)
    Table(columns, data.sortedBy { it["Field"] }).print()
}
