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
   tag 1 rm 0
   tag ls
""".trimIndent()

fun tag(command: String, args: List<String>) {
    val i = args.firstOrNull()?.toIntOrNull()
    val mod = i?.let { toolData.byIndex(it) }
    val subCommand = args.getOrNull(1)?.replace("rm", "remove")
    val tagArg = args.getOrNull(2)
    when {
        args.isEmpty() || args.first() == "ls" -> printTags()
        mod == null -> println("Must provide the index of a valid mod to update")
        subCommand == "add" -> addTag(mod, tagArg)
        subCommand == "remove" && tagArg?.toIntOrNull() != null -> removeTag(mod, tagArg.toInt())
        subCommand == "remove" && tagArg != null -> removeTag(mod, tagArg)

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

private fun addTag(mod: Mod, tag: String?) {
    if (tag == null) {
        println("No tag value found to add")
        return
    }
    mod.tags.add(tag)
    save()
    println("Added $tag to ${mod.name}")
}

private fun removeTag(mod: Mod, tagId: Int) = removeTag(mod, mod.tags.elementAt(tagId))

private fun removeTag(mod: Mod, tag: String) {
    if (!mod.tags.contains(tag)) {
        println(red("Tag ") + cyan("'$tag'") + red(" doesn't exist in ") + cyan("'${mod.tags.joinToString(", ")}'") + ". (Command is case sensitive.)")
        return
    }
    confirm(false, yellow("Remove tag $tag? ")) {
        mod.tags.remove(tag)
        save()
        println("Removed $tag from ${mod.name}")
    }
}
