import kotlinx.serialization.Serializable
import GamePath.*

@Serializable
data class MainConfig(
    var mode: GameMode = GameMode.STARFIELD,
    var chunkSize: Int = 5,
    var apiKey: String? = null,
    var useMyDocs: Boolean = false,
    var verbose: Boolean = false,
    var autoDeploy: Boolean = true,
    var logging: Boolean = true,
    var openInTerminalCommand: String? = null,
)

@Serializable
data class GameConfig(
    var useMyDocs: Boolean = false,
    var categories: Map<Int, String> = mapOf(),
    val paths: MutableMap<String, String> = mutableMapOf(),
) {
    operator fun get(type: String) = paths[type.uppercase()]
    operator fun get(type: GamePath) = paths[type.name] ?: throw IllegalStateException("Missing $type use 'config path $type <path>' to set it")

    operator fun set(type: String, value: String) {
        paths[type.uppercase()] = value
    }

    operator fun set(type: GamePath, value: String) {
        paths[type.name] = value
    }
}
