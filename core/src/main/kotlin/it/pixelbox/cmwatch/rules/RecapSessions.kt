package it.pixelbox.cmwatch.rules

/**
 * Il recap della giornata letto riga per riga (segnalazione 03/10 17:52: nel Registro un blocco per sessione con l'icona
 * giusta, come nella home). Segue `render_short` del plugin: un titolo; le sezioni in maiuscolo (ferme su una domanda,
 * aperte, chiuse oggi, e quelle che non conosce); in ognuna una riga per sessione, «icona nome (link)» seguita da
 * «· strumento» o «: riassunto», e sotto le righe rientrate (la domanda, «↳ prossimo: …»). Legge anche il formato vecchio,
 * in inglese, della fixture del contratto. Le righe che non sono di una sessione restano righe della loro sezione.
 */
object RecapSessions {
    enum class Kind { WAITING, OPEN, CLOSED, OTHER }

    data class Entry(
        val icon: String, val name: String, val url: String? = null, val text: String? = null, val tool: String? = null,
        val next: String? = null, val detail: String? = null,
    )

    data class Section(val kind: Kind, val title: String, val entries: List<Entry>, val lines: List<String>)

    data class View(val title: String, val sections: List<Section>)

    private val HEADERS = mapOf(
        "FERME SU UNA DOMANDA" to Kind.WAITING, "WAITING ON A QUESTION" to Kind.WAITING,
        "APERTE" to Kind.OPEN, "OPEN" to Kind.OPEN,
        "CHIUSE OGGI" to Kind.CLOSED, "CHIUSI OGGI" to Kind.CLOSED, "CLOSED TODAY" to Kind.CLOSED,
    )
    private val ENTRY = Regex("""^(\S+)\s+(.+)$""")
    private val URL = Regex("""\s*\((https?://[^)\s]+)\)""")
    private val OTHER_LINE = Regex("""^(altro|other)\s*:""", RegexOption.IGNORE_CASE)
    private val NEXT = Regex("""^↳\s*(?:prossimo|next)\s*:\s*""", RegexOption.IGNORE_CASE)

    /** Le emoji tonde sono dell'account personale, le quadrate di quello di lavoro, come nei badge di Telegram. */
    fun personal(icon: String): Boolean = icon.trim() !in SQUARES

    private val SQUARES = setOf("🟥", "🟧", "🟨", "🟩", "🟦", "🟪", "🟫", "⬛", "⬜", "◼", "◻", "▪", "▫")

    fun parse(body: String): View {
        val lines = body.lines()
        val title = lines.firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        val sections = mutableListOf<Section>()
        var kind = Kind.OTHER; var header = ""
        var entries = mutableListOf<Entry>(); var other = mutableListOf<String>()
        fun close() { if (entries.isNotEmpty() || other.isNotEmpty()) sections += Section(kind, header, entries.toList(), other.toList()) }
        lines.drop(lines.indexOfFirst { it.isNotBlank() } + 1).forEach { raw ->
            val line = raw.replace("⁠", "")
            val t = line.trim()
            when {
                t.isEmpty() -> {}
                HEADERS[t] != null || isHeader(t) -> { close(); kind = HEADERS[t] ?: Kind.OTHER; header = t; entries = mutableListOf(); other = mutableListOf() }
                // Una riga rientrata appartiene alla sessione sopra: il prossimo passo o la domanda.
                line.startsWith(" ") && entries.isNotEmpty() -> {
                    val last = entries.removeAt(entries.lastIndex)
                    entries += if (NEXT.containsMatchIn(t)) last.copy(next = t.replace(NEXT, ""))
                    else last.copy(detail = listOfNotNull(last.detail, t).joinToString("\n"))
                }
                kind != Kind.OTHER && isEntry(t) -> entries += entry(t)
                // «altro: a, b» (le chiuse senza sostanza) resta nella sua sezione.
                kind != Kind.OTHER && OTHER_LINE.containsMatchIn(t) -> other += t
                else -> {
                    // Il resto (totali, «Tra le sessioni») fa una sezione di righe sole, in fondo.
                    if (kind != Kind.OTHER) { close(); kind = Kind.OTHER; header = ""; entries = mutableListOf(); other = mutableListOf() }
                    other += t
                }
            }
        }
        close()
        return View(title, sections)
    }

    private fun isHeader(t: String) = t.length > 2 && t == t.uppercase() && t.any { it.isLetter() } && !isEntry(t)

    private fun isEntry(t: String): Boolean {
        val m = ENTRY.find(t) ?: return false
        return m.groupValues[1].none { it.isLetterOrDigit() }
    }

    private fun entry(t: String): Entry {
        val m = ENTRY.find(t)!!
        val icon = m.groupValues[1]
        var rest = m.groupValues[2]
        val url = URL.find(rest)?.groupValues?.get(1)
        rest = rest.replace(URL, "")
        val colon = rest.indexOf(": ")
        val dot = rest.indexOf(" · ")
        return when {
            dot >= 0 && (colon < 0 || dot < colon) -> Entry(icon, rest.substring(0, dot).trim(), url, tool = rest.substring(dot + 3).trim())
            colon >= 0 -> Entry(icon, rest.substring(0, colon).trim(), url, text = rest.substring(colon + 2).trim())
            else -> Entry(icon, rest.trim(), url)
        }
    }
}
