import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.response.HttpResponse
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowViaChannel
import kotlinx.coroutines.io.readUTF8Line
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import java.io.File

@Serializable
class MatterBridge(
    val url: String,
    val token: String = ""
) {
    @UseExperimental(FlowPreview::class)
    fun messageFlow() = flowViaChannel<Message>(-1) { channel ->
        println("connecting to matterbridge")

        launch(Dispatchers.IO) {
            client.get<HttpResponse>("$url/api/stream") {
                if (token.isNotEmpty()) {
                    header("Authorization", "Bearer $token")
                }
            }.use { response ->
                val contentChannel = response.content
                while (true) {
                    if (contentChannel.availableForRead > 0) {
                        val line = contentChannel.readUTF8Line()
                        if (line != null) {
                            val message = json.parse<Message>(Message.serializer(), line)
                            channel.send(message)
                        }
                    } else {
                        delay(100)
                    }
                }
            }
        }
    }

    suspend fun receiveMessages(): List<Message> {
        val messageText = client.get<String>("$url/api/messages") {
            if (token.isNotEmpty()) {
                header("Authorization", "Bearer $token")
            }
        }
        return json.parse(Message.serializer().list, messageText)
    }

    suspend fun send(message: Message) {
        val status = client.post<HttpStatusCode>("$url/api/message") {
            if (token.isNotEmpty()) {
                header("Authorization", "Bearer $token")
            }
            val jsonContent = json.stringify(Message.serializer(), message)
            body = TextContent(jsonContent, ContentType.Application.Json)
        }
        println("status: $status")
    }

//    suspend fun replyTo(message: Message, block: Message.() -> Unit) {
//        val responseMessage =
//    }

    companion object {
        val client = HttpClient(engineFactory = CIO)
        val json = Json(JsonConfiguration(encodeDefaults = false))

        @JvmStatic
        @UseExperimental(FlowPreview::class)
        fun main(args: Array<String>) = runBlocking {
            val bridgeJson = File("bridge.json").readText()
            val bridge = json.parse(MatterBridge.serializer(), bridgeJson)

            val dbProc= ProcessBuilder("cutespam-db")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

            val outJob = launch(Dispatchers.IO) {
                dbProc.inputStream.bufferedReader().use {
                    it.lines().forEach {
                        println("db: $it")
                    }
                }
            }
//            val errJob = launch {
//                dbProc.errorStream.bufferedReader().lines().forEach {
//                    System.err.println("db_err: $it")
//                }
//            }


            val oldMessages = bridge.receiveMessages()
            oldMessages.forEach {
                println("skipping: '${it.text}' by ${it.username}")
            }

//            delay(1000)
//            dbProc.destroy()
//            runBlocking {
//                dbJob.join()
//            }

            Runtime.getRuntime().addShutdownHook(Thread {
                println("Shutdown hook ran!")
                dbProc.waitFor()
                runBlocking {
                    outJob.cancelAndJoin()
                }
            })

            bridge.messageFlow().collect { msg ->
                println("received: $msg")
                if (msg.text.startsWith("$")) {
                    val command = msg.text.substringAfter('$')
                    Commands(command) { message ->
                        var msgString = message.toString()
                        if (msgString.lines().size > 2) {
                            val paste = PasteUtil.paste(
                                Paste(
                                    description = "",
                                    sections = listOf(
                                        PasteSection(
                                            name = "response.txt",
                                            syntax = "txt",
                                            contents = msgString+"\n"
                                        )
                                    )
                                )
                            )
                            msgString = paste.link
                        }
                        println("message: $msgString")
                        bridge.send(
                            Message(
                                text = msgString,
                                channel = msg.channel,
                                username = "kyutebot",
                                userid = "kyutebot",
                                gateway = msg.gateway
                            )
                        )
                    }.process()
                }
                if (msg.text.startsWith("echo ")) {
                    bridge.send(
                        Message(
                            text = "response: " + msg.text.substringAfter("echo "),
                            channel = msg.channel,
                            username = "EchoBot",
                            userid = "echobot",
                            gateway = msg.gateway
                        )
                    )
                }
            }
        }
    }
}
