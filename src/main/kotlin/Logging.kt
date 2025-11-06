import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

val jsonLogMapper = kotlinx.serialization.json.Json {
    classDiscriminator = "type"
    encodeDefaults = false
    prettyPrint = false
}

fun logFetch(name: String) {
    if (!toolConfig.logging) return
    logEvent(LogEvent.FetchEvent(now(), names = listOf(name)))
}

fun logFetch(ids: List<Int>) {
    if (!toolConfig.logging) return
    logEvent(LogEvent.FetchEvent(now(), ids))
}

fun logDelete(ids: List<Int>, names: List<String>) {
    if (!toolConfig.logging) return
    logEvent(LogEvent.DeleteEvent(now(), ids, names))
}

fun logLaunch() {
    if (!toolConfig.logging) return
    val (modIds, modNames) = toolData.mods.filter { it.enabled }.partition { it.id != null }

    val modHash = ModHash(modIds.mapNotNull { it.id }, modNames.map { it.creationId ?: it.name })
    updateHashFile(modHash)
    logEvent(LogEvent.LaunchEvent(now(), modHash.hash))
}

private fun updateHashFile(modHash: ModHash) {
    val hashFile = File(
        gameMode.configPath.replace("-config", "-hashes")
    ).also {
        if (!it.exists()) {
            it.createNewFile()
            it.writeText("{}")
        }
    }

    val hashes = jsonMapper.decodeFromString<Map<String, ModHash>>(hashFile.readText()).toMutableMap()
    hashes[modHash.hash] = modHash
    hashFile.writeText(jsonLogMapper.encodeToString(hashes))
}

private fun logEvent(event: LogEvent) {
    val file = File(
        gameMode.configPath.replace("-config.json", "-log.jsonl")
    ).also { if (!it.exists()) it.createNewFile() }

    file.appendText(jsonLogMapper.encodeToString(event) + "\n")
}

@OptIn(ExperimentalTime::class)
private fun now(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
