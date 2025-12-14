package commands.config

import logLaunch
import runCommand
import java.io.File

val startCKDescription = """
    Run the Creation Kit Editor
""".trimIndent()

val startSKUsage = """
    start
""".trimIndent()

fun startCK(command: String, args: List<String>) {
    File(".").runCommand("steam steam://rungameid/2722710", true)
    println("Starting...")
    logLaunch()
}
