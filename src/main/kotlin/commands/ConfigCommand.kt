package commands

import GamePath
import cyan
import gameConfig
import jsonMapper
import kotlinx.serialization.encodeToString
import lastFullInput
import nexus.getGameInfo
import save
import toolConfig
import java.io.File
import kotlin.reflect.KMutableProperty0

val configDescription = """
    Used to configure the mod manager itself. Saved in the config.json (and game specific starfield-config.json etc) files located next to the jar
    open-in-terminal-command is optional and only needed if you don't use `gnome-terminal`. This value will be used as the command when opening folders in the terminal. Will be run in the relevant folder, but if you need to specify the directory in the command, you can use `{pwd}` and it will be replaced by the relevant path.
    verbose gives additional output (for debugging) if set to true
    autodeploy automatically runs deploy when enabling or disabling mods. Defaults to true
    use-my-docs optionally allows deploying mod files under Data to my documents instead of the game folder. (Defaults to false) 
    categories is used to download category names from nexus
    config path is used to set game specific paths
    config paths tells you what paths are needed
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
    |config use-my-docs <true/false> 
    |config categories
    |config version
""".trimMargin()

fun config(command: String, args: List<String>) {
    val path = args.getOrNull(1)?.lowercase()?.let { a -> GamePath.entries.firstOrNull { it.name.lowercase() == a } }
    when {
        args.isEmpty() -> {
            println("Running in ${File(".").absolutePath}")
            println("Main Config:\n" + jsonMapper.encodeToString(toolConfig))
            println("Game Config:\n" + jsonMapper.encodeToString(gameConfig))
        }

        args.size == 1 && args.last() == "paths" -> describePaths()

        args.size == 3 && args[0] == "path" && path != null -> {
            val newPath = lastFullInput.replace("config path ${path.name}", "", true).replace("\"", "").trim().let { if (it.endsWith("/")) it.substring(0, it.length-1) else it }
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

        args.first() == "verbose" -> updateFlag(args, toolConfig::verbose)
        args.first() == "autodeploy" -> updateFlag(args, toolConfig::autoDeploy)
        args.first() == "use-my-docs" -> updateFlag(args, toolConfig::useMyDocs)
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
