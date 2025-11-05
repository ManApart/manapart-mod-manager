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

fun logLaunch() {
    if (!toolConfig.logging) return
    val (modIds, modNames) = toolData.mods.filter { it.enabled }.partition { it.id != null }

    logEvent(LogEvent.LaunchEvent(now(), modIds.mapNotNull { it.id }, modNames.map { it.creationId ?: it.name }))
}

private fun logEvent(event: LogEvent) {
    val file = File(
        gameMode.configPath
            .replace("-config", "-log")
            .replace(".json", ".jsonl")
    )
        .also { if (!it.exists()) it.createNewFile() }

    file.appendText(jsonLogMapper.encodeToString(event) +"\n")
}


@OptIn(ExperimentalTime::class)
private fun now(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
