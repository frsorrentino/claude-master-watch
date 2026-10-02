package it.pixelbox.cmwatch.rules

/**
 * Le tabelle markdown nel testo di Claude (Franz, 02/10 21:07: con barre e trattini non si leggevano; approvato alle
 * 21:11). Il testo si divide in blocchi: una tabella stretta resta una griglia, una larga diventa una scheda per riga, con
 * la prima colonna come titolo e le altre come «intestazione: valore», senza celle vuote; a voce si leggono le schede.
 * Una riga logica occupa la riga fisica: nessuna colonna spezzata in frammenti.
 */
object MarkdownTable {
    sealed interface Block
    data class Text(val text: String) : Block
    data class Table(val header: List<String>, val rows: List<List<String>>) : Block
    data class Card(val title: String, val lines: List<String>)

    /** Una griglia regge al massimo tre colonne di celle corte su un telefono. */
    const val COMPACT_COLS = 3
    const val COMPACT_CELL = 18

    private val SEPARATOR = Regex("""^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)*\|?\s*$""")
    private val PIPE = Regex("""(?<!\\)\|""")
    private val EMPTY = setOf("", "—", "–", "-")

    fun blocks(src: String): List<Block> {
        val lines = src.split('\n')
        val out = mutableListOf<Block>()
        val text = mutableListOf<String>()
        fun flush() {
            text.joinToString("\n").trim('\n').takeIf { it.isNotBlank() }?.let { out += Text(it) }
            text.clear()
        }
        var i = 0
        while (i < lines.size) {
            if (isRow(lines[i]) && i + 1 < lines.size && SEPARATOR.matches(lines[i + 1])) {
                val header = cells(lines[i])
                val rows = mutableListOf<List<String>>()
                var j = i + 2
                while (j < lines.size && isRow(lines[j])) {
                    val r = cells(lines[j])
                    rows += List(header.size) { k -> r.getOrElse(k) { "" } }
                    j++
                }
                flush()
                out += Table(header, rows)
                i = j
            } else {
                text += lines[i]
                i++
            }
        }
        flush()
        return out
    }

    fun compact(t: Table): Boolean =
        t.header.size <= COMPACT_COLS && (listOf(t.header) + t.rows).all { r -> r.all { it.length <= COMPACT_CELL } }

    fun cards(t: Table): List<Card> = t.rows.map { r ->
        Card(r.firstOrNull().orEmpty(), (1 until t.header.size).mapNotNull { k -> r.getOrNull(k)?.takeIf { it !in EMPTY }?.let { "${t.header[k]}: $it" } })
    }

    /** Il testo da leggere a voce: le tabelle come le loro schede, una riga per riga della tabella. */
    fun spoken(src: String): String = blocks(src).joinToString("\n\n") { b ->
        when (b) {
            is Text -> b.text
            is Table -> cards(b).joinToString("\n") { c -> (listOf(c.title) + c.lines).filter { it.isNotBlank() }.joinToString(". ") + "." }
        }
    }

    private fun isRow(line: String): Boolean = line.trim().let { it.startsWith("|") && it.count { c -> c == '|' } >= 2 }

    private fun cells(line: String): List<String> =
        line.trim().removePrefix("|").removeSuffix("|").split(PIPE).map { it.trim().replace("\\|", "|") }
}
