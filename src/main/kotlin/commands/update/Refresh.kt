package commands.update

import Mod
import cyan
import doCommand
import refreshMod

val refreshDescription = """
    For all mods with ids, attempt to redownload (or grab the file from the downloads folder if it exists) and restage.
    Refreshing can be done by ranges of indexs, or by mod status
    If you're looking to upgrade to a new version, see update and upgrade
""".trimIndent()

val refreshUsage = """
    refresh <index>
    refresh 1 2 4
    refresh 1-3
    refresh all
    refresh empty
    refresh staged
    refresh enabled
    refresh disabled
""".trimIndent()

fun refresh(command: String, args: List<String>) {
    doCommand(args, List<Mod>::refreshMods)
}

private fun List<Mod>.refreshMods() {
    also { println(cyan("Refreshing ${it.size} mods")) }
        .forEach { refreshMod(it) }
    println(cyan("Done Refreshing"))
}
