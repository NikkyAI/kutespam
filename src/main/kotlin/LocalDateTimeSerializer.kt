import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Serializer(forClass = LocalDateTime::class)
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnn")
    override fun serialize(encoder: Encoder, obj: LocalDateTime) {
        encoder.encodeString(formatter.format(obj))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val input = decoder.decodeString()
        val date = try {
            LocalDateTime.from(formatter.parse(input))
        }catch(e: DateTimeParseException) {
            try {
                LocalDateTime.from(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .parse(input))
            }catch(e: DateTimeParseException) {
                throw SerializationException("cannot process LocalDataTime from $")
            }
        }
        return date
    }
}