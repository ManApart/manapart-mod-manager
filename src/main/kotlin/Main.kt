import commands.CommandType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.Locale

lateinit var toolData: Data
lateinit var toolConfig: MainConfig
lateinit var gameConfig: GameConfig
lateinit var modFolder: File
var gameMode = GameMode.STARFIELD
val HOME = System.getProperty("user.home")!!
private var confirmation: ((List<String>) -> Unit)? = null
var testingMode = false

val jsonMapper = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

fun main(args: Array<String>) {
    checkForUpgrade()
    loadData()
    println(cyan("\n${gameMode.displayName} Mod Manager"))
    if (args.isEmpty()) {
        initialCommand()
        while (true) {
            readLine(readlnOrNull())
            while (confirmation != null) {
                confirmation?.let { confirm ->
                    readlnOrNull()?.parseArgs()?.let {
                        confirmation = null
                        confirm(it)
                    }
                }
            }
        }
    } else {
        readLine(args.joinToString(" "))
    }
}

private fun checkForUpgrade() {
    File(mainConfigPath()).takeIf { it.exists() }?.let {
        jsonMapper.decodeFromString<JsonObject>(it.readText())
    }?.let { legacyConfig ->
        if (legacyConfig.containsKey("gamePath") || legacyConfig.containsKey("categories")) {
            if (canUpgradeConfig(legacyConfig)) {
                try {
                    upgradeConfig(legacyConfig)
                } catch (e: Exception) {
                    println(red(e.toString()))
                    throw IllegalStateException("Failed to upgrade config. Please see https://manapart.github.io/starfield-mod-manager-site/setup.html#Upgrade to upgrade your config to the new format")
                }
            } else {
                throw IllegalStateException("Config file setup has changed. Please see https://manapart.github.io/starfield-mod-manager-site/setup.html#Upgrade to upgrade your config to the new format")
            }
        }
    }
}

private fun canUpgradeConfig(legacyConfig: JsonObject): Boolean {
    return listOf("gamePath", "appDataPath", "apiKey")
        .all { legacyConfig.containsKey(it) }
}

private fun upgradeConfig(legacyConfig: JsonObject) {
    toolConfig = jsonMapper.decodeFromJsonElement<MainConfig>(legacyConfig)
    gameConfig = jsonMapper.decodeFromJsonElement<GameConfig>(legacyConfig)
    gameConfig[GamePath.GAME] = legacyConfig["gamePath"]!!.jsonPrimitive.content
    gameConfig[GamePath.COMPAT_DATA] = legacyConfig["appDataPath"]!!.jsonPrimitive.content.let { it.substring(0, it.indexOf("/pfx")) }

    toolData = (File("data.json").takeIf { it.exists() } ?: File(gameMode.dataJsonPath).takeIf { it.exists() })?.let {
        jsonMapper.decodeFromString(it.readText())
    } ?: Data()
    save()
    println("Upgraded config and settings")
}

fun loadData() {
    toolConfig = File(mainConfigPath()).takeIf { it.exists() }?.let {
        jsonMapper.decodeFromString(it.readText())
    } ?: MainConfig()
    gameMode = toolConfig.mode
    modFolder = File(gameMode.modFolder)

    gameConfig = File(gameMode.configPath).takeIf { it.exists() }?.let {
        jsonMapper.decodeFromString(it.readText())
    } ?: GameConfig()

    toolData = File(gameMode.dataJsonPath).takeIf { it.exists() }?.let {
        jsonMapper.decodeFromString(it.readText())
    } ?: Data()
    toolData.updateSorts()
}

fun save() {
    if (testingMode) return
    File(mainConfigPath()).writeText(jsonMapper.encodeToString(toolConfig))
    File(gameMode.configPath).writeText(jsonMapper.encodeToString(gameConfig))
    File(gameMode.dataJsonPath).writeText(jsonMapper.encodeToString(toolData))
}

fun saveMainConfigOnly() {
    if (testingMode) return
    File(mainConfigPath()).writeText(jsonMapper.encodeToString(toolConfig))
}

fun initialCommand() {
    val missing = GamePath.entries.filter { !gameConfig.paths.containsKey(it.name) }
    if (missing.isEmpty()) {
        CommandType.LIST.apply("ls", listOf())
    } else {
        println(yellow("The following paths are missing:"))
        missing.forEach { it.describe() }
    }
}

fun verbose(message: String) {
    if (toolConfig.verbose) println(message)
}

fun confirm(skip: Boolean = false, warnMessage: String? = null, block: () -> Unit) {
    if (skip) block() else {
        warnMessage?.let { println("$it (y/n)") }
        confirmation = { c ->
            if (c.firstOrNull() == "y" || c.firstOrNull() == "yes") {
                block()
            } else if (c.size != 1 || c.first() !in listOf("no", "n")) {
                println("Unable to understand response. Bailing out")
            }
        }
    }
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
