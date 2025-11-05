import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class LogEvent {
    abstract val at: LocalDateTime

    @Serializable
    @SerialName("launch")
    data class LaunchEvent(override val at: LocalDateTime, val ids: List<Int> = emptyList(), val names: List<String> = emptyList()) : LogEvent()

    @Serializable
    @SerialName("fetch")
    data class FetchEvent(override val at: LocalDateTime, val ids: List<Int> = emptyList()) : LogEvent()

    @Serializable
    @SerialName("delete")
    data class DeleteEvent(override val at: LocalDateTime, val ids: List<Int> = emptyList(), val names: List<Int> = emptyList()) : LogEvent()

}
