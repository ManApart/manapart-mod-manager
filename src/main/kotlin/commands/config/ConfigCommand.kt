package commands.config

import GamePath
import commands.update.viewAppVersion
import gameConfig
import gameMode
import jsonMapper
import kotlinx.serialization.encodeToString
import lastFullInput
import nexus.getGameInfo
import red
import save
import toolConfig
import java.io.File
import kotlin.reflect.KMutableProperty0

val configDescription = """
    Used to configure the mod manager itself. Saved in the config.json (and game specific starfield-config.json etc) files located next to the jar
    open-in-terminal-command is optional and only needed if you don't use `gnome-terminal`. This value will be used as the command when opening folders in the terminal. Will be run in the relevant folder, but if you need to specify the directory in the command, you can use `{pwd}` and it will be replaced by the relevant path.
    verbose gives additional output (for debugging) if set to true
    autodeploy automatically runs deploy when enabling or disabling mods. Defaults to true
    deploytarget - set the default path for mods to deploy to. Defaults to the Data directory but could be used to deploy to app data etc
    logging - log events like launch (and load order) or first time fetching a mod etc, on by default
    categories - download game specific category names from nexus
    config path is used to set game specific paths
    config paths tells you what paths are needed
    config path can be used to override built in paths as well (also see paths command)
    config path delete can be used to delete an overridden path
    If your paths have spaces, make sure to quote them
    version gives the commit that the app was built from
""".trimIndent()

val configUsage = """
    |config paths
    ${GamePath.entries.joinToString("\n") { "|config path $it <path-to-folder>" }}
    |config api-key <key-from-nexus>
    |config open-in-terminal-command <path-to-folder> 
    |config verbose <true/false>
    |config autodeploy <true/false>
    |config deploytarget <PATHTYPE>
    |config logging <true/false>
    |config categories
    |config version
""".trimMargin()

fun config(command: String, args: List<String>) {
    when {
        args.isEmpty() -> {
            println("Running ${gameMode.name} in ${File(".").absolutePath}")
            println("Main Config:\n" + jsonMapper.encodeToString(toolConfig))
            println("Game Config:\n" + jsonMapper.encodeToString(gameConfig))
        }

        args.size == 1 && args.last() == "paths" -> describePaths()

        args.size == 3 && args[0] == "path" && args[1] == "delete" -> {
            val toDelete = args[2].uppercase()
            if (gameConfig.paths.containsKey(toDelete)) {
                val pathValue = gameConfig[toDelete]
                gameConfig.paths.remove(toDelete)
                save()
                println(red("Deleted $toDelete: $pathValue"))
            } else {
                println(red("Unable to find path $toDelete"))
            }
        }

        args.size == 3 && args[0] == "path" -> {
            val path = args[1]
            val newPath = lastFullInput.replace("config path $path", "", true).replace("\"", "").trim().let { if (it.endsWith("/")) it.substring(0, it.length - 1) else it }
            gameConfig[path] = newPath
            println("Updated $path to ${gameConfig[path]}")
            save()
        }

        args.size == 2 && args.first() == "open-in-terminal-command" -> {
            toolConfig.openInTerminalCommand = args.last()
            println("Updated terminal command to ${toolConfig.openInTerminalCommand}")
            save()
        }

        args.size == 2 && args.first() == "api-key" -> {
            toolConfig.apiKey = args.last()
            println("Updated api key to ${toolConfig.apiKey}")
            save()
        }

        args.size == 1 && args.first() == "categories" -> {
            getGameInfo(toolConfig.apiKey!!)?.let { info ->
                if (info.categories.isNotEmpty()) {
                    gameConfig.categories = info.categories.associate { it.category_id to it.name }
                    println("Saved ${gameConfig.categories.size} categories")
                    save()
                }
            }
        }

        args.first() == "deploytarget" && args.size == 2 -> {
            val newTarget = PathType.entries.firstOrNull { it.name.lowercase() == args[1] }
            if (newTarget != null) {
                gameConfig.defaultDeployTarget = newTarget
                println("Set default deploy target to ${gameConfig.defaultDeployTarget}")
                save()
            } else println("${args[1]} does not exist in ${PathType.entries}")
        }

        args.first() == "deploytarget" -> println("Deploy target options are: ${PathType.entries}")
        args.first() == "verbose" -> updateFlag(args, toolConfig::verbose)
        args.first() == "autodeploy" -> updateFlag(args, toolConfig::autoDeploy)
        args.first() == "version" -> viewAppVersion()

        else -> println("Unknown args: ${args.joinToString(" ")}")
    }
}

private fun updateFlag(args: List<String>, flag: KMutableProperty0<Boolean>) {
    val newValue = when (args.getOrNull(1)) {
        "true" -> true
        "false" -> false
        else -> !flag.get()
    }
    flag.set(newValue)
    println("Updated ${flag.name} to ${flag.get()}")
    save()
}

private fun describePaths() {
    GamePath.entries.forEach { it.describe() }
}
