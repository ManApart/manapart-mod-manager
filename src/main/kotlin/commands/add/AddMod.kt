package commands.add

import addBlank
import addModByFile
import addModById
import addModByNexusProtocol
import cyan
import logFetch
import red
import urlToId

val addModDescription = """
   Add a new mod, including downloading it
   You can add mods by nexus mod manager url, by mod url, by mod id, or even by path to a local zip
   For urls and ids, you can add multiple at once, space separated
   You can also add a new blank mod with new
   Example nexus mod manager url: nxm://starfield/mods/4183/files/12955?key=abc&expires=1697023374&user_id=111
   Example mod url: https://www.nexusmods.com/starfield/mods/4183?tab=files
   Example adding a creation: add SFBGS003
   See also creation, external
""".trimIndent()

val addModUsage = """
   add <nxm://starfield/...>
   add <creation-id>
   add 4183
   add 4183 4182 4181
   add <path-to-mod-zip> <name-of-mod>*
   add new <name-of-mod>*
""".trimIndent()

fun addMod(command: String, args: List<String>) {
    val firstArg = args.firstOrNull() ?: ""
    val firstLower = firstArg.lowercase()
    when {
        args.isEmpty() -> println(addModDescription)
        firstArg.startsWith("nxm") -> addModByNexusProtocol(firstArg)
        firstArg.toIntOrNull() != null -> addModByIds(args.mapNotNull { it.toIntOrNull() })
        firstArg.startsWith("http") -> addModByUrls(args)
        args.size == 2 && firstArg == "new" -> addBlank(args[1])
        listOf("/", "./").any { firstArg.startsWith(it) } -> addModByFile(args[0], args.getOrNull(1))
        firstLower.startsWith("TM_") -> addCreation(firstArg)
        firstLower.startsWith("sfbgs") && parseCreationPlugins().map { it.lowercase() }.any { it.contains(firstLower) } -> addCreation(firstArg)
        firstLower.startsWith("sfbgs") -> addExternal(firstArg)

        else -> println("Unknown args: ${args.joinToString(" ")}")
    }
}

private fun addModByUrls(urls: List<String>) {
    val ids = urls.mapNotNull { url -> url.urlToId().also { if (it == null) println(red("Could not find id for $url")) } }
    ids.forEach { addModById(it) }
    logFetch(ids)
    println(cyan("Done adding"))
}

private fun addModByIds(ids: List<Int>) {
    ids.forEach { addModById(it) }
    logFetch(ids)
    println(cyan("Done adding"))
}
