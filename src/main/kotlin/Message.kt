import kotlinx.serialization.*

@Serializable
data class Message(
    val text: String,
    val channel: String = "",
    val username: String = "",
    val userid: String = "",
    val avatar: String = "",
    val account: String = "",
    val event: String = "",
    val protocol: String = "",
    val gateway: String = "",
    val parent_id: String = "",
    val timestamp: String = "",
    val id: String = "",
    val Extra: String? = null
)