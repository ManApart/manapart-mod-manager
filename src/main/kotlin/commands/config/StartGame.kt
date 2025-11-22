package commands.config

import gameMode
import logLaunch
import runCommand
import java.io.File

val startGameDescription = """
    Run the steam game
""".trimIndent()

val startGameUsage = """
    start
""".trimIndent()

fun startGame(command: String, args: List<String>) {
    File(".").runCommand("steam steam://rungameid/${gameMode.steamId}", true)
    println("Starting...")
    logLaunch()
}
