package commands.edit

import gameConfig
import red
import save
import toTable
import toolData

val categoryDescription = """
    List categories and set the category of a mod
    Use category to list categories
    Use category plus a mod index to update a mod's category
    If you don't have categories, use config categories to fetch them
    Also see mod command
""".trimIndent()

val categoryUsage = """
    category
    category <index> <category id> 
    category <index> <category name> 
""".trimIndent()

fun category(command: String, args: List<String>) {
    val i = args.firstOrNull()?.toIntOrNull()
    val catId = args.lastOrNull()?.toIntOrNull()
    val catByName = args.lastOrNull()?.let { catIdByName(it) }
    when {
        args.isEmpty() -> listCategories()
        args.size == 2 && i != null && catId != null -> changeCategory(i, catId)
        args.size == 2 && i != null && catByName != null -> changeCategory(i, catByName)
        args.size == 2 -> println(red("Unable to understand ${args.last()}. ") + "Try using the id instead of name.")
        else -> listCategories()
    }
}

private fun listCategories() {
    gameConfig.categories.toTable("Id", 5, "Name", 20).print()
}

private fun catIdByName(name: String): Int? {
    return gameConfig.categories.entries.firstOrNull { it.value.lowercase() == name }?.key
}

fun changeCategory(modIndex: Int, newCategory: Int) {
    toolData.byIndex(modIndex)?.let { mod ->
        val old = mod.category()
        mod.categoryId = newCategory
        save()
        println("Updated ${mod.name} from $old to ${mod.category()}")
    }
}