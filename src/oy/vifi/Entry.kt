package oy.vifi

import org.apache.commons.lang3.StringEscapeUtils
import java.io.File
import java.net.URL

val URL_PATTERN = Regex("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
const val OUTPUT_FOLDER = "output"
//default max quote per page from the website
const val MAX_QUOTE_PER_PAGE = 50
const val QUOTE_AUTHOR_PREFIX = "~~"

val logs = mutableListOf<String>()

fun main() {
    printlnAndLog("START")
    createOutputFolder()
    val people = readPeopleList(File("final_quotes.html"))
    writePeopleToFile(people)

    people
        .also {
            it.forEach {
                if (it.quoteUrl.contains("deleted")) {
                    println("Will ignore ${it.quoteUrl}")
                }
            }
        }
        .filter { !it.quoteUrl.contains("deleted") }
        .mapNotNull {
            if (!it.quoteUrl.matches(URL_PATTERN)) {
                val correctUrl = URL_PATTERN.find(it.quoteUrl)?.value
                if (correctUrl != null && it.quoteUrl.matches(URL_PATTERN)) {
                    it.copy(quoteUrl = correctUrl)
                }
                null
            } else {
                it
            }
        }
        .forEach { person ->
            //read first quote page to get number of total quotes
            val quotePageText = getText(person.quoteUrl)
            //val quotePageText = File("scratch.txt").readText()
            val numberOfQuoteText = Regex("<.{1,5}>\\d+\\s+wallpapers</.+>").find(quotePageText)?.value ?: return
            //<h2>500 wallpapers</h2>
            val numberOfQuote = numberOfQuoteText.replace(Regex("(</?.{1,5}>|wallpapers)"), "").trim().toInt()
            printlnAndLog("Reading ${person.name} - $numberOfQuote quotes...")
            val firstQuotePage = readQuotes(quotePageText)

            val nextPageQuotes = if (numberOfQuote > MAX_QUOTE_PER_PAGE) {
                val numberOfPage =
                    (numberOfQuote / MAX_QUOTE_PER_PAGE) + (if (numberOfQuote % MAX_QUOTE_PER_PAGE != 0) +1 else 0)
                (2..numberOfPage).map { pageIndex ->
                    readQuotes(getText("${person.quoteUrl}/page/$pageIndex"))
                }.toList()
            } else {
                emptyList<List<Quote>>()
            }
            val quoteList = firstQuotePage.toMutableList().plus(nextPageQuotes.flatten())
            writeQuotesToFile(person, quoteList)
        }

    printlnAndLog("DONE")

    File("$OUTPUT_FOLDER/logs.txt").writeText(logs.joinToString("\n"))
}

private fun readPeopleList(file: File): List<People> {
    if (!file.exists()) {
        printlnAndLog("[ERROR] File ${file.absolutePath} is missing")
        return emptyList()
    }

    printlnAndLog("Reading authors...")
    val textInFile = file.readText()

    val peoplePattern = Regex("<div class=\"gridblock-title\">.*</a></div>")

    val peopleList = peoplePattern.findAll(textInFile).map { it.value }.toList()

    //<div class="gridblock-title"><a href="https://quotefancy.com/jenny-valentine-quotes">Jenny Valentine Quotes</a></div>
    val quoteUrlPattern = Regex("href=\".*\"")
    val peopleNamePattern = Regex("\">[^<].*</a>")

    return peopleList
        .map { people ->
            val quoteUrl = quoteUrlPattern.find(people)?.value?.replace(Regex("(href=|\")"), "")
            val name = peopleNamePattern.find(people)?.value?.replace(Regex("(\">|</a>|[qQ]uotes)"), "")

            if (name == null || quoteUrl == null) {
                printlnAndLog("[WARNING] Read name=$name,url=$quoteUrl")
                return@map null
            }

            return@map People(name.trim(), quoteUrl.trim())
        }
        .filterNotNull()
        .toList()
        .also {
            printlnAndLog("Read ${it.size} persons")
        }
}

private fun createOutputFolder() {
    val folder = File("$OUTPUT_FOLDER/")
    printlnAndLog("[OUTPUT] ${folder.absolutePath}/")
    if (!folder.exists()) {
        printlnAndLog("Creating $OUTPUT_FOLDER folder...")
        folder.mkdirs()
        printlnAndLog("Created")
    }
}

private fun getText(urlStr: String): String {
    printlnAndLog("[GET] $urlStr")
    return URL(urlStr).readText()
}

private fun readQuotes(text: String): List<Quote> {
    /*
    <div class="q-wrapper">
        <p class="quote-p">“<a class="quote-a" href="https://quotefancy.com/quote/199926/Frank-Ocean-Work-hard-in-silence-let-your-success-be-your-noise">Work hard in silence, let your success be your noise.</a>”</p>
        <p class="author-p">— Frank Ocean</p>
    </div>
     */
    val blockPattern = Regex("<div class=\"q-wrapper\">.*\n.*\n.*\n.*</div>")
    val quotePhrasePattern = Regex("<a class=\"quote-a\".*</a>")
    val authorPhrasePattern = Regex("<p class=\"author-p\">.*</p>")

    return blockPattern
        .findAll(text)
        .map { it.value }
        .map { blockText ->
            val quote = quotePhrasePattern.find(blockText)?.value
                ?.let { it.replace(Regex("(<.*\">|</a>)"), "") }
                ?.let { it.unescapeHtml4() }

            val author = authorPhrasePattern.find(blockText)?.value
                ?.let { it.replace(Regex("(<.*\">—?\\s?|</p>)"), "") }
                ?.let { it.unescapeHtml4() }

            if (quote == null) {
                printlnAndLog("[WARNING] quote=$quote")
                return@map null
            }

            return@map Quote(quote, author)
        }
        .filterNotNull()
        .toList()
}

private fun writeQuotesToFile(author: People, quotes: List<Quote>) {
    val file = "output/${getName(author.name)}.md"
    printlnAndLog("[WRITE] to $file ${quotes.size} quotes")
    val text = quotes.map {
        it.text + if (it.author.equals(author.name, true)) "" else " $QUOTE_AUTHOR_PREFIX${it.author}"
    }
    File(file).writeText(text.joinToString("\n", postfix = "\n\n${quotes.size} quotes", transform = { " - $it" }))
}

fun writePeopleToFile(people: List<People>) {
    val file = "output/authors.md"
    File(file).writeText(people.joinToString("\n") { "- ${it.name} - ${it.quoteUrl}" })
}

private fun getName(name: String): String {
    return name.toLowerCase().replace(Regex("[\\s\"]"), "-")
}

private fun String.unescapeHtml4(): String = StringEscapeUtils.unescapeHtml4(this)

private fun printlnAndLog(text: String) {
    println(text)
    logs.add(text)
}

data class People(val name: String, val quoteUrl: String)
data class Quote(val text: String, val author: String?)
