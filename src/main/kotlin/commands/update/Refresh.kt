package commands.update

import Mod
import cyan
import doCommand
import refreshMod

val refreshDescription = """
    For all mods with ids, attempt to redownload (or grab the file from the downloads folder if it exists) and restage.
    Refreshing can be done by ranges of indexs, or by mod status
    Refreshing will re-download the existing fileId if it exists
    If you're looking to upgrade to a new version, see update and upgrade
    Even if a version has not changed, the latest fileId may have (and is updated by update command). To get the latest, use upgrade
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
    filter { it.creationId == null }
        .also { if (it.isEmpty()) println("No mods to refresh (Creations are not refreshed)") else println(cyan("Refreshing ${it.size} mods")) }
        .also { list -> list.forEach { refreshMod(it) } }
        .also { if (it.isNotEmpty()) println(cyan("Done Refreshing")) }
}
