package oy.vifi

import org.apache.commons.lang3.StringEscapeUtils
import java.io.File
import java.net.URL

const val OUTPUT_FOLDER = "output"
const val MAX_QUOTE_PER_PAGE = 50

fun main() {
    println("START")
    createOutputFolder()
    val people = readPeopleList(File("final_quotes.html"))

    people[0].let { person ->
        //        val quotePageText = getText(person.quoteUrl)
        val quotePageText = File("scratch.txt").readText()
        val numberOfQuoteText = Regex("<.{1,5}>\\d+\\s+wallpapers</.+>").find(quotePageText)?.value ?: return
        //<h2>7 wallpapers</h2>
        println(numberOfQuoteText)
        val numberOfQuote = numberOfQuoteText.replace(Regex("(</?.{1,5}>|wallpapers)"), "").trim().toInt()
        println("Reading ${person.name}-${person.quoteUrl} for $numberOfQuote quotes...")
        val firstQuotePage = readQuotes(quotePageText)
        println(firstQuotePage)

        val nextPageQuotes = if (numberOfQuote > MAX_QUOTE_PER_PAGE) {
            val numberOfPage = (numberOfQuote / MAX_QUOTE_PER_PAGE) + (numberOfQuote % MAX_QUOTE_PER_PAGE)
        } else {
            emptyList<Quote>()
        }
        //val quotesList = (1..10).map { parsing(getText("https://quotefancy.com/${getName(person)}-quotes/page/$it")) }.toList()
        //writeQuotes(person, quotesList.flatten())
    }

    println("DONE")
}

private fun readPeopleList(file: File): List<People> {
    if (!file.exists()) {
        println("[ERROR] File ${file.absolutePath} is missing")
        return emptyList()
    }

    println("Reading authors...")
    val textInFile = file.readText()

    val peoplePattern = Regex("<div class=\"gridblock-title\">.*</a></div>")

    val peopleList = peoplePattern.findAll(textInFile).map { it.value }.toList()

    //<div class="gridblock-title"><a href="https://quotefancy.com/jenny-valentine-quotes">Jenny Valentine Quotes</a></div>
    val quoteUrlPattern = Regex("href=\".*\"")
    val peopleNamePattern = Regex("quotes\">.*</a>")

    return peopleList
        .map { people ->
            val quoteUrl = quoteUrlPattern.find(people)?.value?.replace(Regex("(href=|\")"), "")
            val name = peopleNamePattern.find(people)?.value?.replace(Regex("(\">|</a>|[qQ]uotes)"), "")

            if (name == null || quoteUrl == null){
                println("[WARNING] Read name=$name,url=$quoteUrl")
                return@map null
            }

            return@map People(name.trim(), quoteUrl.trim())
        }
        .filterNotNull()
        .toList()
        .also {
            println("Read ${it.size} persons")
        }
}

private fun createOutputFolder() {
    val folder = File("$OUTPUT_FOLDER/")
    println("[OUTPUT] ${folder.absolutePath}")
    if (!folder.exists()) {
        println("Creating $OUTPUT_FOLDER folder...")
        folder.mkdirs()
        println("Created")
    }
}

private fun getText(urlStr: String): String {
    println("[GET] $urlStr")
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
                ?.also { it.replace(Regex("(<.*\">|</a>)"), "") }
                ?.also { it.unescapeHtml4() }

            val author = authorPhrasePattern.find(blockText)?.value
                ?.also { it.replace(Regex(""), "") }
                ?.also { it.unescapeHtml4() }

            if (quote == null) {
                return@map null
            }

            return@map Quote(quote, author)
        }
        .filterNotNull()
        .toList()
}

private fun writeQuotes(person: String, quotes: List<String>) {
    val file = "output/${getName(person)}.md"
    println("[WRITE] to $file ${quotes.size} quotes")
    val text = quotes.joinToString("\n", postfix = "\n\n${quotes.size} quotes", transform = { " - $it" })
    File(file)
        .also {
            if (!it.exists()) {
                it.createNewFile()
            }
        }
        .writeText(text)
}

private fun getName(name: String): String {
    val DOT_SPACE = Regex("([. '])")
    return name.toLowerCase().replace(DOT_SPACE, "-")
}

private fun String.unescapeHtml4(): String = StringEscapeUtils.unescapeHtml4(this)

data class People(val name: String, val quoteUrl: String)
data class Quote(val text: String, val author: String?)
