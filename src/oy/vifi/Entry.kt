package oy.vifi

import org.apache.commons.lang3.StringEscapeUtils
import java.io.File
import java.net.URL

val PEOPLE = arrayOf(
    "Albert Einstein",
    ""
)
const val OUTPUT_FOLDER = "output"
val DOT_SPACE = Regex("([. '])")
val QUOTE_PATTERN = Regex("<p class=\"quote-p\">.*</p>")
val QUOTE_FIRST_PART_PATTERN = Regex("<p class=\"quote-p\">“<a class=.*\">")
val QUOTE_SECOND_PART_PATTERN = Regex("</a>”</p>")

fun main() {
    createOutputFolder()
//    readPeople()

//    PEOPLE.forEach { person ->
//        println("Parsing $person quotes...")
//        val quotesList = (1..10).map { parsing(getText("https://quotefancy.com/${getName(person)}-quotes/page/$it")) }.toList()
//        writeQuotes(person, quotesList.flatten())
//    }
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

private fun parsing(text: String): List<String> {
    return QUOTE_PATTERN
        .findAll(text)
        .map { it.groupValues[0] }
        .map { it.replace(QUOTE_FIRST_PART_PATTERN, "") }
        .map { it.replace(QUOTE_SECOND_PART_PATTERN, "") }
        .map { StringEscapeUtils.unescapeHtml4(it) }
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

private fun getName(name: String) = name.toLowerCase().replace(DOT_SPACE, "-")

