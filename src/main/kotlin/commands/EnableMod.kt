package commands

import StageChange
import cyan
import detectStagingChanges
import save
import toolConfig
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
    when {
        args.isEmpty() -> println(enableDescription)
        args.size == 1 && args.first().contains("-") -> enableRange(enable, args)
        args.size == 1 && args.first() == "all" -> enableRange(enable, listOf("0-${toolData.mods.size - 1}"))
        else -> enableList(enable, args)
    }
    if (args.isNotEmpty() && toolConfig.autoDeploy) deploy("", listOf())
}

private fun enableList(enable: Boolean, args: List<String>) {
    val names = args.getIndices(toolData.mods.size).mapNotNull { i ->
        enableMod(enable, i)
    }.joinToString(", ")
    save()
    if (enable) println(cyan("Enabled")+ " $names") else println(cyan("Disabled")+ " $names")
}

fun enableMod(enable: Boolean, i: Int): String? {
    val mod = toolData.mods[i]
    return if (enable && detectStagingChanges(File(mod.filePath)) == StageChange.FOMOD) {
        println("$i ${yellow(mod.name)} cannot be enabled because it is an unprocessed fomod. Delete the fomod folder in the staging folder to enable. (And pick your options).")
        null
    } else {
        mod.enabled = enable
        mod.name
    }
}

private fun enableRange(enable: Boolean, args: List<String>) {
    val range = args.getRange(toolData.mods.size)
    if (range.isNotEmpty()) {
        val names = range.mapNotNull { i ->
            enableMod(enable, i)
        }.joinToString(", ")
        save()
        if (enable) println(cyan("Enabled")+ " $names") else println(cyan("Disabled")+ " $names")
    }
}
