package commands.deploy

import Mod
import StageChange
import commands.view.BUFFER
import cyan
import detectStagingChanges
import doCommand
import save
import toolData
import yellow
import java.io.File

val enableDescription = """
    Enable or disable a mod.
    Enabled mods are symlinked into the game folder the next time the deploy command is run
""".trimIndent()

val enableUsage = """
    enable <index>
    enable 1 2 4
    enable 1-4
    enable all
""".trimIndent()
val disableUsage = """
    disable <index>
    disable 1 2 4
    disable 1-4
    disable all
""".trimIndent()

fun enable(command: String, args: List<String>) = enableMod(true, args)
fun disable(command: String, args: List<String>) = enableMod(false, args)

private fun enableMod(enable: Boolean = true, args: List<String>) {
    if (args.isEmpty()) {
        println(enableDescription)
        return
    } else doCommand(args) { enable(enable) }
    if (args.isNotEmpty()) autoDeploy()
}

private fun List<Mod>.enable(enable: Boolean) {
    val mods = flatMap { enableMod(enable, it) }.toSet()
    BUFFER = mods
    val names = mods.joinToString(", ") { it.name }
    save()
    if (enable) println(cyan("Enabled") + " $names") else println(cyan("Disabled") + " $names")
}

fun enableMod(enable: Boolean, i: Int) = enableMod(enable, toolData.mods[i])
fun enableMod(enable: Boolean, mod: Mod, enableChildren: Boolean = true): Set<Mod> {
    return if (enable && detectStagingChanges(File(mod.filePath)) == StageChange.FOMOD) {
        println("${mod.index} ${yellow(mod.name)} cannot be enabled because it is an unprocessed fomod. Delete the fomod folder in the staging folder to enable. (And pick your options).")
        emptySet()
    } else {
        mod.enabled = enable
        if (enable) {
            //Enable ALL required mods in one go, don't recursively do it
            setOf(mod) + (if (enableChildren) mod.getAllRequiredMods().flatMap { enableMod(true, it, false) } else emptyList())
        } else {
            //Disable all children, recursively
            setOf(mod) + (mod.getDependantMods().flatMap { enableMod(false, it) })
        }
    }
}
