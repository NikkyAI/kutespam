import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

/**
 * Created by nikky on 09/07/18.
 * @author Nikky
 */

@Serializable
data class Paste(
    val encrypted: Boolean = false,
    val description: String,
    val sections: List<PasteSection>
)

@Serializable
data class PasteSection(
    val name: String,
    val syntax: String = "text",
    val contents: String
)

@Serializable
data class PasteResponse(
    val id: String,
    val link: String,
    val success: Boolean
)

object PasteUtil {
    private const val DEFAULT_KEY = "uKJoyicVJFnmpnrIZMklOURWxrCKXYaiBWOzPmvon"

    private val json = Json(JsonConfiguration.Stable)
//        .builder()
//        .registerTypeAdapter {
//            PasteResponse(
//                id = it.getReified("id") ?: "",
//                link = it.getReified<String>("link")
//                    ?.replace("\\/", "/")
//                    ?: "invalid"
//            )
//        }

    suspend fun paste(paste: Paste, key: String = DEFAULT_KEY): PasteResponse {
        val apiKey = key.takeIf { it.isNotBlank() } ?: DEFAULT_KEY

        val content = MatterBridge.client.post<String>("https://api.paste.ee/v1/pastes") {
            header("X-Auth-Token", apiKey)
            body = TextContent(
                text = MatterBridge.json.stringify(Paste.serializer(), paste),
                contentType = ContentType.Application.Json
            )
        }
        println(content)
        val pasteResponse = MatterBridge.json.parse(PasteResponse.serializer(), content)
        return pasteResponse.copy(link = pasteResponse.link.replace("\\/", "/"))
    }
}

fun main() = runBlocking {
    val paste = PasteUtil.paste(
        Paste(
            description = "",
            sections = listOf(
                PasteSection(
                    name = "response",
                    syntax = "autodetect",
                    contents = "some\ntest\ncontent"
                )
            )
        )
    )
    println(paste)
//    val response = MatterBridge.client.post<String>("https://api.paste.ee/v1/pastes/file") {
//        header("X-Auth-Token", "uKJoyicVJFnmpnrIZMklOURWxrCKXYaiBWOzPmvon")
//        body = MultiPartFormDataContent(

//            formData {
//                append("files[]", "Hello\nWorld".toByteArray())
//                append("names[]", "hello.txt")
//                append("syntaxes[]", "txt")
//        }
//        )
//    }
//    println(response.replace("\\/", "/"))
}