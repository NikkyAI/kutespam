import com.beust.jcommander.JCommander
import com.beust.jcommander.MissingCommandException
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.beust.jcommander.Parameters
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Type
import java.util.Comparator
import kotlin.math.min

class Help {

}

class Version {

}

@Parameters(separators = "=", commandDescription = "Record changes to the repository")
abstract class BaseQuery {
    @Parameter(names = ["--help", "-h"], help = true)
    var help: Boolean = false

//    @Parameter(names = ["--uri"])
//    var uri: Boolean = false

    @Parameter(names = ["--allSource"])
    var allSource: Boolean = false

    @Parameter(names = ["--author"])
    var author: String? = null

    @Parameter(names = ["--caption"])
    var caption: String? = null

    @Parameter(names = ["--source"])
    var source: String? = null

    @Parameter(names = ["--rating"])
    var rating: Rating? = null

    @Parameter(names = ["--keyword"])
    var keywords: List<String> = listOf()

    @Parameter(names = ["--not-keyword"])
    var notKeywords: List<String> = listOf()
}

class Query : BaseQuery() {
    @Parameter(names = ["--limit"])
    var limit: Int = 1

    @Parameter(names = ["--random"])
    var random: Boolean = false
}

class Random : BaseQuery()

class Commands(args: String, val response: suspend (Any?) -> Unit) {
    val commandParts = splitWords(args)
    //    val cm = CommandMain()
    val query = Query()
    val random = Random()
    val help = Help()
    val version = Version()
    val jc = JCommander.newBuilder()
        .programName("${'$'}cutespam")
//        .addObject(cm)
        .addCommand("query", query)
        .addCommand("random", random)
        .addCommand("keywords", object {})
        .addCommand("help", help)
        .addCommand("version", version)
        .build()

    suspend fun process() {
        val command = commandParts.joinToString(" ") {
            if (it.contains(" "))
                "\"$it\""
            else
                it
        }
        println("parsing: $command")
        try {
            jc.parse(*commandParts)
            println("parsedCommand: " + jc.parsedCommand)
            when (jc.parsedCommand) {
                null -> {
//                    if (cm.version) {
//                        response("version: a.b.c")
//                        return
//                    }
                }
                "help" -> {
                    response(jc.shortUsage())
                }
                "version" -> {
                    response("version: 0.0.1")
                }
                "random" -> {
                    if (random.help) {
                        val queryJc = jc.commands["random"]!!
//                        val sb = StringBuilder()
//                        queryJc.usage(sb)
//                        response(sb.toString())
                        response(queryJc.shortUsage())
                        return
                    }

                    val uuids = Cutespam.queryUUIDS(
                        QueryParameter(
                            limit = 1,
                            random = true,
                            author = random.author,
                            caption = random.caption,
                            source = random.source,
                            rating = random.rating,
                            keywords = random.keywords,
                            notKeywords = random.notKeywords
                        ),
                        response
                    )

                    if (uuids.isNotEmpty()) {
                        Cutespam.probeUUIDs(uuids).map { meta ->
                            val source = if (random.allSource) {
                                listOf(meta.source) + (meta.source_other ?: setOf()) + (meta.source_via ?: setOf())
                            } else {
                                listOf(meta.source)
                            }
                            val urls = source.filterNotNull().run {
                                if (isNotEmpty())
                                    joinToString(" ")
                                else
                                    Cutespam.fileOf(meta.uid).path
                            }
                            val keywords = meta.keywords.joinToString(" ")
                            "$urls keywords: $keywords"
                        }.forEach {
                            response(it)
                        }
                    }
                }
                "query" -> {
                    if (query.help) {
                        val queryJc = jc.commands["query"]!!
//                        val sb = StringBuilder()
//                        queryJc.usage(sb)
//                        response(sb.toString())
                        response(queryJc.shortUsage())
                        return
                    }

                    val defaultLimit = 5

                    val uuids = Cutespam.queryUUIDS(
                        QueryParameter(
                            limit = min(query.limit ?: defaultLimit, defaultLimit),
                            random = query.random,
                            author = query.author,
                            caption = query.caption,
                            source = query.source,
                            rating = query.rating,
                            keywords = query.keywords,
                            notKeywords = query.notKeywords
                        ),
                        response
                    )

                    if (uuids.isNotEmpty()) {
                        Cutespam.probeUUIDs(uuids).map { meta ->
                            val source = if (random.allSource) {
                                listOf(meta.source) + (meta.source_other ?: setOf()) + (meta.source_via ?: setOf())
                            } else {
                                listOf(meta.source)
                            }
                            val urls = source.filterNotNull().run {
                                if (isNotEmpty())
                                    joinToString(" ")
                                else
                                    Cutespam.fileOf(meta.uid).path
                            }
                            val keywords = meta.keywords.joinToString(" ")
                            "$urls keywords: $keywords"
                        }.forEach {
                            response(it)
                        }
                    }
                }
                "keywords" -> {
                    val allMeta: List<Metadata> = Cutespam.probeUUIDs(Cutespam.queryUUIDS())
                    println("loaded ${allMeta.size} metadata entries")

                    val keyowrds = allMeta.flatMap { meta ->
                        meta.keywords
                    }.toSet()
                    val allKeywordStrings = keyowrds.sorted().joinToString("\n") {
                        "- $it"
                    }
                    val paste = PasteUtil.paste(
                        Paste(
                            description = "All keywords",
                            sections = listOf(
                                PasteSection(
                                    name = "keywords.md",
                                    syntax = "markdown",
                                    contents = allKeywordStrings
                                )
                            )
                        )
                    )
                    response("keywords: " + paste.link)

                }
            }
        } catch (e: MissingCommandException) {
            e.printStackTrace()
//            val sb = StringBuilder()
//            jc.usage(sb)
//            response(sb.toString())
            response(e.message + "\n" + jc.shortUsage())
        } catch (e: ParameterException) {
            e.printStackTrace()
//            val sb = StringBuilder()
//            jc.usage(sb)
//            response(sb.toString())
            response(e.message + "\n" + jc.shortUsage())
        }
    }

    companion object {
        val regex = "(?<=\")[^\"]*(?=\")|[^\" ]+".toRegex()
        fun splitWords(str: String): Array<String> {
            println("input: $str")
            val matches = regex.findAll(str, 0)
            val strings = matches.map { it.groupValues }
                .flatten().toList()
            println("result: $strings")
            return strings.toTypedArray()
        }
    }
}

fun JCommander.shortUsage(): String {
    val allCommands = commands.values.run {
        if (isEmpty()) "" else {
            joinToString(" | ", prefix = "{ ", postfix = " }") {
                it.programName
            }
        }
    }
    val allParameters = parameters.sortedWith(Comparator { p0, p1 ->
        val a0 = p0.parameterAnnotation
        val a1 = p1.parameterAnnotation
        if (a0 != null && a0.order != -1 && a1 != null && a1.order != -1) {
            Integer.compare(a0.order, a1.order)
        } else if (a0 != null && a0.order != -1) {
            -1
        } else if (a1 != null && a1.order != -1) {
            1
        } else {
            p0.longestName.compareTo(p1.longestName)
        }
    }).filter {
        !it.parameter.hidden()
    }.joinToString(" ") {
        if (it.parameter.required()) {
            it.parameter.names()
                .joinToString(
                    " | ",
                    prefix = "< ",
                    postfix = " ${it.parameterized.genericType.short.let { if (it == "boolean") "" else it }} >"
                )
        } else {
            it.parameter.names()
                .joinToString(
                    " | ",
                    prefix = "[ ",
                    postfix = " ${it.parameterized.genericType.short.let { if (it == "boolean") "" else it }} ]"
                )
        }
    } //.sortWith(param)
    return "Usage: $programName $allParameters $allCommands"
}

val Type.short: String
    get() {
        return toString()
            .replace("class ", "")
            .replace("java.util.", "")
            .replace("java.lang.", "")
            .replace("Integer", "Int")
            .replace("Rating", Rating.values().joinToString(" | ", "{ ", " }") { it.name })
    }

object CommandsMain {
    @JvmStatic
    fun main(args: Array<String>) {
        Commands("-h") { println(it) }
        Commands("version") { println(it) }
        Commands("help") { println(it) }
        Commands("query") { println(it) }
        Commands("query -h") { println(it) }
        Commands("random") { println(it) }
        Commands("random -h") { println(it) }
        Commands("dsfdszf") { println(it) }
        Commands("query --keyword \"nipples\"") { println(it) }
        Commands("query --keyword \"nipples and such\"") { println(it) }
    }
}