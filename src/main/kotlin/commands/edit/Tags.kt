package commands.edit

import Mod
import confirm
import cyan
import red
import save
import toolData
import yellow

enum class Tag(val tag: String) {
    CREATION("Creation"),
    EXTERNAL("External"),
    SKIP_VALIDATE("Skip-Validate"),
}

val tagDescription = """
Add and remove tags
tag ls - Lists all tags and number of mods using that tag
""".trimIndent()

val tagUsage = """
   tag 1 add essential
   tag 1 rm essential
   tag 1 3 4 add cool
   tag 1 rm 0
   tag ls
""".trimIndent()

fun tag(command: String, args: List<String>) {
    val modIds = args.takeWhile { it.toIntOrNull() != null }.mapNotNull { it.toIntOrNull() }
    val subCommand = args.firstOrNull { it.toIntOrNull() == null }
    val tagArg = subCommand?.let { sub -> args.indexOf(sub) }?.let { args.getOrNull(it + 1) }
    val mods = modIds.mapNotNull { toolData.byIndex(it) }
    val remove = subCommand == "remove" || subCommand == "rm"
    when {
        args.isEmpty() || args.first() == "ls" -> printTags()
        mods.isEmpty() || modIds.size != mods.size -> println("Must provide the index of a valid mod to update")
        subCommand == "add" -> addTag(mods, tagArg)
        remove && mods.size > 1 && tagArg?.toIntOrNull() != null -> println("When removing by index you can only remove tags from one mod at a time")
        remove && tagArg?.toIntOrNull() != null -> removeTag(mods, tagArg.toInt())
        remove && tagArg != null -> removeTag(mods, tagArg)

        else -> println("Unknown args: ${args.joinToString(" ")}")
    }
}

private fun printTags() {
    val tags = toolData.mods.flatMap { it.tags }.groupBy { it }
    if (tags.isEmpty()) {
        println("No tags")
    }
    println(tags.entries.joinToString("\n") { (key, amount) -> "$key (${amount.size})" })
}

private fun addTag(mods: List<Mod>, tag: String?) {
    if (tag == null) {
        println("No tag value found to add")
        return
    }
    mods.forEach { it.tags.add(tag) }
    save()
    println("Added $tag to ${mods.joinToString(", ") { it.name }}")
}

private fun removeTag(mods: List<Mod>, tagId: Int) = removeTag(mods, mods.first().tags.elementAt(tagId))

private fun removeTag(mods: List<Mod>, tag: String) {
    val modWithMissingTags = mods.firstOrNull { !it.tags.contains(tag) }
    if (modWithMissingTags != null) {
        println(red("Tag ") + cyan("'$tag'") + red(" doesn't exist in ") + cyan("'${modWithMissingTags.tags.joinToString(", ")}'") + ". (Command is case sensitive.)")
        return
    }
    confirm(false, yellow("Remove tag $tag? ")) {
        mods.forEach { it.tags.remove(tag) }
        save()
        println("Removed $tag from ${mods.joinToString(", ") { it.name }}")
    }
}
