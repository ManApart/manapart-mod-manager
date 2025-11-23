package commands.config

import gameMode
import logLaunch
import runCommand
import java.io.File

val startCKDescription = """
    Run the steam game
""".trimIndent()

val startSKUsage = """
    start
""".trimIndent()

fun startCK(command: String, args: List<String>) {
    File(".").runCommand("steam steam://rungameid/2722710", true)
    println("Starting...")
    logLaunch()
}
