import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import kotlin.collections.sorted
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

fun logDelete(id: Int?, name: String) {
    if (!toolConfig.logging) return
    logEvent(LogEvent.DeleteEvent(now(), id, name))
}

fun logLaunch() {
    if (!toolConfig.logging) return
    val modHash = createModHash()
    updateHashFile(modHash)
    logEvent(LogEvent.LaunchEvent(now(), modHash.hash))
}

fun logNote(note: String) = logEvent(LogEvent.NoteEvent(now(), note))

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

private fun createModHash(): ModHash {
    val (modIds, modNames) = toolData.mods.filter { it.enabled }.partition { it.id != null }
    return ModHash(modIds.mapNotNull { it.id }.sorted(), modNames.map { it.creationId ?: it.name }.sorted())
}
