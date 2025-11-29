import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class ModHash(val ids: List<Int> = listOf(), val names: List<String> = listOf()){
    @Transient
    val hash = "${ids.hashCode()}-${names.hashCode()}"
}

@Serializable
sealed class LogEvent {
    abstract val at: LocalDateTime

    @Serializable
    @SerialName("launch")
    data class LaunchEvent(override val at: LocalDateTime, val hash: String) : LogEvent()

    @Serializable
    @SerialName("fetch")
    data class FetchEvent(override val at: LocalDateTime, val ids: List<Int> = emptyList(), val names: List<String> = emptyList()) : LogEvent()

    @Serializable
    @SerialName("delete")
    data class DeleteEvent(override val at: LocalDateTime, val id: Int? = null, val name: String) : LogEvent()

    @Serializable
    @SerialName("note")
    data class NoteEvent(override val at: LocalDateTime, val note: String) : LogEvent()
}
