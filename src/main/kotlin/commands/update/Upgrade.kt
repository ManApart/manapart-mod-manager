package commands.update

import Mod
import addModById
import commands.view.displayShown
import cyan
import doCommand
import toolData

val upgradeDescription = """
    For all mods with newer versions, attempt to stage the latest version.
    If you want to check for new versions, see update
    If you're looking to just redownload or restage a file at the current version, see refresh
""".trimIndent()

val upgradeUsage = """
    upgrade <index>
    upgrade 1 2 4
    upgrade 1-3
    upgrade all
""".trimIndent()

fun upgrade(command: String, args: List<String>) {
    if (args.isEmpty()) {
        println(upgradeDescription + "\n")
        val updatable = toolData.mods.map { it to it.updateAvailable() }
        displayShown(updatable)
    } else {
        doCommand(args, List<Mod>::upgradeMods)
    }
}

private fun List<Mod>.upgradeMods(){
    filter { it.id != null && it.updateAvailable()}
        .also { println(cyan("Upgrading ${it.size} mods")) }
        .forEach { addModById(it.id!!, it.latestFileId, true) }
        println(cyan("Done Upgrading"))
}
