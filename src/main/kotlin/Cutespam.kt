import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonParsingException
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import java.io.File
import java.lang.Exception
import java.util.UUID

data class QueryParameter(
    val uri: Boolean = false,
    val limit: Int? = null,
    val random: Boolean = false,
    val author: String? = null,
    val caption: String? = null,
    val source: String? = null,
    val rating: Rating? = null,
    val keywords: List<String> = listOf(),
    val notKeywords: List<String> = listOf()
) {
    val arguments: Array<String>
    inline get() {
        val arguments = mutableListOf<String>()
        if (uri) arguments += listOf("--uri")
        if (limit != null) arguments += listOf("--limit", "$limit")
        if (random) arguments += listOf("--random")
        if (author != null) arguments += listOf("--author", author)
        if (caption != null) arguments += listOf("--caption", caption)
        if (source != null) arguments += listOf("--source", source)
        if (rating != null) arguments += listOf("--rating", rating.name)
        if (keywords.isNotEmpty()) arguments += listOf("--keyword") + keywords
        if (notKeywords.isNotEmpty()) arguments += listOf("--not-keyword") + notKeywords
        return arguments.toTypedArray()
    }
}

object Cutespam {
    fun query(
        parameter: QueryParameter = QueryParameter(),
        response: suspend (Any?) -> Unit = { println(it) }
    ): List<String> {
        val arguments = parameter.arguments
        val commandParts = arrayOf("cutespam", "query", "--json", *arguments)
        val (output, err) = runCommand(*commandParts)
            ?: return listOf()

        if(err.isNotBlank()) {
            runBlocking {
                println("pasting..")
                println(err)
                val paste = PasteUtil.paste(
                    Paste(
                        description = "error while executing " +
                                commandParts.joinToString(" ") {
                                    if(it.contains(" "))
                                        '"'+it+'"'
                                    else
                                        it
                                },
                        sections = listOf(
                            PasteSection(
                                name = "traceback",
                                syntax = "autodetect",
                                contents = err + "\n"
                            )
                        )
                    )
                )
                response("cannot process: "+ paste.link)
            }
            return emptyList()
        }

        return try {
            val json = Json(JsonConfiguration.Stable)
            json.parse(String.serializer().list, output)
        }catch(e: JsonParsingException) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun queryFiles(
        parameter: QueryParameter = QueryParameter()
    ): List<File> = query(parameter.copy(uri = true))
        .map {
            File(it.substringAfter("file://"))
        }

    fun queryUUIDS(
        parameter: QueryParameter = QueryParameter(),
        response: suspend (Any?) -> Unit = { println(it) }
    ): List<UUID> = query(
        parameter,
        response
    ).map {
        UUID.fromString(it)
    }

    fun probeUUIDs(
        uuids: List<UUID>,
        response: suspend (Any?) -> Unit = { println(it) }
    ): List<Metadata> {
        val commandParts = arrayOf(
            "cutespam", "probe", "--json",
            *(uuids.map { it.toString() }.toTypedArray())
        )
        val (output, err) = runCommand(*commandParts) ?: return listOf()

        if(err.isNotBlank()) {
            runBlocking {
                println("pasting..")
                println(err)
                val paste = PasteUtil.paste(
                    Paste(
                        description = "error while executing " +
                                commandParts.joinToString(" ") {
                                    if(it.contains(" "))
                                        '"'+it+'"'
                                    else
                                        it
                                },
                        sections = listOf(
                            PasteSection(
                                name = "traceback",
                                syntax = "autodetect",
                                contents = err + "\n"
                            )
                        )
                    )
                )
                response("cannot process: "+ paste.link)
            }
            return emptyList()
        }

        val json = Json(JsonConfiguration.Stable)
        return json.parse(Metadata.serializer().list, output)
    }

    fun fileOf(uuid: UUID): File {
        val (output, err) =
            runCommand("cutespam", "open", "--uri", uuid.toString()) ?: throw Exception("cannot execute open --uuid")
        return File(output.substringAfter("file://"))
    }
//    fun probeFiles(paths: List<File>): List<Metadata> {
//        val output =
//            runCommand("cutespam", "probe", "--json", *paths.map { it.path }.toTypedArray()) ?: return listOf()
//        val json = Json(JsonConfiguration.Stable)
//        return json.process(Metadata.serializer().list, output)
//    }

//    fun autoComplete(vararg incomplete: String): List<UUID> {
//        val output =
//            runCommand("cutespam", "probe", *incomplete) ?: return listOf()
//        return output.lines().map { UUID.fromString(it) }
//    }

    fun runCommand(
        vararg command: String,
        workingDir: File = File(".")
    ): Pair<String, String>? {
        try {
            println("executing: " + command.joinToString(" ") {
                if (it.contains(" ")) "\"$it\"" else it
            })
            val proc = ProcessBuilder(*command)
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            val out = proc.inputStream.bufferedReader().readText()
            val err = proc.errorStream.bufferedReader().readText()
            return out to err
//        proc.waitFor(60, TimeUnit.SECONDS)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        //    val chunkedUUIDS = uuids.chunked(1000)
//    val pool = newFixedThreadPoolContext(3, "pool")
//    val metas = chunkedUUIDS.map { uuids ->
//        async(pool) {
//            val results = probe(*uuids.toTypedArray())
//            println("processed: ${results.map { it.uid }}")
//            results
//        }
//    }.awaitAll().flatten()
        val allMeta: List<Metadata> = probeUUIDs(queryUUIDS())
        println("loaded ${allMeta.size} metadata entries")

        val keyowrds = allMeta.flatMap { meta ->
            meta.keywords
        }.toSet()
        keyowrds.sorted().forEach {
            println(it)
        }

        val paths = queryFiles(QueryParameter(uri = true, keywords = listOf("nipples")))


        val uuids = queryUUIDS(QueryParameter(keywords = listOf("nipples")))
        val nipples = probeUUIDs(uuids)
        println("nipples")
//        nipples.forEach {
//            println(it.source)
//        }

        allMeta.groupBy { meta -> meta.rating }.forEach { (rating, metaList) ->
            println("rating: $rating")
            metaList.forEach {meta ->
                println(meta.source ?: fileOf(meta.uid).path)
            }
        }

        println("captions")
        allMeta.mapNotNull { it.caption }.toSet()
            .sorted().forEach {
                println(it)
            }
        allMeta.filter { it.source == null }
            .apply {
                forEach {
                    val file = fileOf(it.uid)
                    println(file.path)
                }
                println("entries missing source: $size")
            }

        Unit
    }
}
