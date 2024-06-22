package commands

import Mod
import toolData
import java.io.File

private enum class SearchType { NAME, CATEGORY, TAG, ALL }

val searchDescription = """
    Search for mods and list them once
    To apply a filter to future lists, see filter
    Search the given text (name or category), by ids, by mods missing ids, and by status
    Searching a string will search name, category, and tags.
    To search ONLY tags, use search tag <tagName>.
    The same can be done to search by category or name as well
""".trimIndent()

val searchModsUsage = """
    search <search text> 
    search <tag>
    search tag <tag> 
    search name <name>
    search category <category>
    search <mod id>
    search enabled
    search disabled
    search staged
    search unstaged
    search endorsed
    search unendorsed
    search abstained
    search missing
""".trimIndent()

fun searchMods(args: List<String> = listOf()) {
    searchMods(false, args)
}

fun searchMods(persist: Boolean, args: List<String> = listOf()) {
    if (args.firstOrNull() == "all" || args.firstOrNull() == "clear") {
        toolData.mods.forEach { it.show = true }
        listMods()
        return
    }
    val enabled = when {
        args.contains("enabled") -> true
        args.contains("disabled") -> false
        else -> null
    }
    val staged = when {
        args.contains("staged") -> true
        args.contains("unstaged") -> false
        else -> null
    }
    val endorsed = when {
        args.contains("endorsed") -> true
        args.contains("abstained") -> false
        else -> null
    }
    val unendorsed = args.contains("unendorsed")
    val missing = when {
        args.contains("missing") -> true
        else -> null
    }
    val id = args.firstOrNull { it.toIntOrNull() != null }
    val flagList = listOf("enabled", "disabled", "staged", "unstaged", "endorsed", "abstained", "unendorsed", "tag", "name", "category")
    val search = args.filter { !flagList.contains(it) && it.toIntOrNull() == null }.joinToString(" ").lowercase()

    val searchType = when {
        args.getOrNull(0) == "name" -> SearchType.NAME
        args.getOrNull(0) == "category" -> SearchType.CATEGORY
        args.getOrNull(0) == "tag" -> SearchType.TAG
        else -> SearchType.ALL
    }

    val mods = toolData.mods.map { mod ->
        val displayed = mod.isDisplayed(enabled, staged, missing, id, endorsed, unendorsed, searchType, search)
        if (persist) mod.show = displayed
        mod to displayed
    }
    display(mods)
}

private fun Mod.isDisplayed(
    enabled: Boolean?,
    staged: Boolean?,
    missing: Boolean?,
    id: String?,
    endorsed: Boolean?,
    unendorsed: Boolean,
    searchType: SearchType,
    search: String
): Boolean {
    return (enabled != null && enabled == this.enabled) ||
            (staged != null && staged == File(filePath).exists()) ||
            (missing != null && missing == (this.id == null)) ||
            (id != null && this.id?.toString()?.contains(id) ?: false) ||
            (endorsed != null && endorsed == this.endorsed) || (unendorsed && this.endorsed == null) ||
            (search.isNotBlank() && stringSearch(searchType, search))
}

private fun Mod.stringSearch(kind: SearchType, search: String): Boolean {
    return when (kind) {
        SearchType.NAME -> name.contains(search)
        SearchType.CATEGORY -> category()?.lowercase()?.contains(search) ?: false
        SearchType.TAG -> tags.any { tag -> tag.lowercase().contains(search) }
        else -> {
            name.contains(search) || (category()?.lowercase()?.contains(search) ?: false) || tags.any { tag -> tag.lowercase().contains(search) }
        }
    }
}
