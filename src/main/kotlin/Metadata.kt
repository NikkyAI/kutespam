import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Metadata(
    @Serializable(with = UUIDSerializer::class)
    val uid: UUID,
    val hash: String,
    val caption: String? = null,
    val authors: List<String>? = null,
    val keywords: Set<String> = setOf(),
    val source: String? = null,
    @Serializable(with = UUIDSerializer::class)
    val group_id: UUID? = null,
    val collections: Set<String> = setOf(),
    val rating: String?, // TODO: fix serializable Rating
    @Serializable(with = LocalDateTimeSerializer::class)
    val date: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val last_updated: LocalDateTime,
    val source_other: Set<String>? = null,
    val source_via: Set<String>? = null
)

enum class Rating { s, n, q, e }

//    @Serializer(forClass = Rating::class)
//    companion object {
//        override val descriptor: SerialDescriptor
//            get() = StringDescriptor
//
//        override fun deserialize(decoder: Decoder): Rating {
//            val code = decoder.decodeString()
//            return values().find { it.code == code }
//                ?: throw SerializationException("cannot find Rating for $code")
//        }
//
//        override fun serialize(encoder: Encoder, obj: Rating) {
//            encoder.encodeString(obj.code)
//        }
//    }

fun main() {
    @Serializable
    data class Wrapper(val rating: Rating)
    println(Json(JsonConfiguration.Stable).stringify(Wrapper.serializer(), Wrapper(Rating.e)))
}
