package commands.add

import Changes
import commands.edit.Tag
import cyan
import jsonMapper
import lastFullInput
import red
import toolData

val importDescription = """
    Import Data from Mod Viewer
    Viewer found at https://manapart.github.io/starfield-mod-manager-site/viewer.html
    Imports json and applies the changes
""".trimIndent()

val importUsage = """
    import <json>
""".trimIndent()

fun importData(command: String, args: List<String> = listOf()) {
    val input = if (lastFullInput.startsWith("import")) lastFullInput.substring("import ".length) else lastFullInput.substring(2)
    println("Importing $input")
    var changes: Changes? = null
    try {
        changes = jsonMapper.decodeFromString<Changes>(input)
    } catch (e: Exception) {
        println(red("Unable to parse changes. Is it proper json?"))
    }
    if (changes != null) {
        with(changes) {
            println("${adds.size} adds, ${deletes.size} deletes and ${tagsAdded.size + tagsRemoved.size} tag updates")

            (deletes + tagsRemoved.keys + tagsAdded.keys).toSet().map { it to toolData.byUniqueId(it, true) }.filter { it.second == null }.takeIf { it.isNotEmpty() }?.let { missing -> println("Missing ids: ${missing.joinToString{it.first}}") }

            if (adds.isNotEmpty()) fetchModsById(adds)

            deletes.mapNotNull { toolData.byUniqueId(it, true) }.forEach { mod ->
                if (mod.hasTag(Tag.CREATION)) rmCreation(mod, true) else delete(mod)
            }

            tagsRemoved.forEach { (id, tags) ->
                toolData.byUniqueId(id, true)?.tags?.removeAll(tags.toSet())
            }

            tagsAdded.forEach { (id, tags) ->
                toolData.byUniqueId(id, true)?.tags?.addAll(tags)
            }
            println(cyan("Import complete"))
        }
    }
}
