package it.pixelbox.cmwatch.rules

/**
 * La formattazione dei messaggi della chat (Franz, 01/10 20:17: «**prova**» si vedeva con gli asterischi): il testo
 * senza i segni del markdown e gli intervalli da disegnare. Grassetto `**x**` e `__x__`, corsivo `*x*`, codice `` `x` ``
 * e blocchi ```` ``` ````, link `[testo](url)`, titoli `#` in grassetto, elenchi `-`/`*` con il pallino. Un segno senza
 * chiusura resta com'è; dentro il codice niente si interpreta; `_` singolo non è corsivo (nomi come tool_note).
 */
object Markdown {
    enum class Kind { BOLD, ITALIC, CODE, LINK }
    data class Span(val kind: Kind, val range: IntRange, val url: String? = null)
    data class Styled(val text: String, val spans: List<Span>)

    private val LIST = Regex("""^(\s*)[-*]\s+""")
    private val HEADING = Regex("""^#{1,6}\s+""")
    private val LINK = Regex("""^\[([^\]\n]+)]\((https?://[^)\s]+)\)""")

    fun parse(src: String): Styled {
        val out = StringBuilder()
        val spans = mutableListOf<Span>()
        val lines = src.split('\n')
        var i = 0
        var first = true
        fun newline() { if (!first) out.append('\n'); first = false }
        while (i < lines.size) {
            val line = lines[i]
            if (line.trimStart().startsWith("```")) {
                val end = (i + 1 until lines.size).firstOrNull { lines[it].trimStart().startsWith("```") }
                if (end != null) {
                    val body = lines.subList(i + 1, end).joinToString("\n")
                    newline()
                    val start = out.length
                    out.append(body)
                    if (body.isNotEmpty()) spans += Span(Kind.CODE, start until out.length)
                    i = end + 1
                    continue
                }
            }
            newline()
            var rest = line
            val heading = HEADING.find(rest)
            if (heading != null) rest = rest.substring(heading.value.length)
            LIST.find(rest)?.let { m -> out.append(m.groupValues[1]).append("• "); rest = rest.substring(m.value.length) }
            val start = out.length
            inline(rest, out, spans)
            if (heading != null && out.length > start) spans += Span(Kind.BOLD, start until out.length)
            i++
        }
        return Styled(out.toString(), spans)
    }

    private fun inline(s: String, out: StringBuilder, spans: MutableList<Span>) {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '`') {
                val end = s.indexOf('`', i + 1)
                if (end > i + 1) { add(Kind.CODE, s.substring(i + 1, end), out, spans); i = end + 1; continue }
            }
            if (c == '[') {
                val m = LINK.find(s.substring(i))
                if (m != null) {
                    val start = out.length
                    inline(m.groupValues[1], out, spans)
                    spans += Span(Kind.LINK, start until out.length, m.groupValues[2])
                    i += m.value.length
                    continue
                }
            }
            val double = s.startsWith("**", i) || (s.startsWith("__", i) && boundary(s, i - 1))
            if (double) {
                val mark = s.substring(i, i + 2)
                val end = s.indexOf(mark, i + 2)
                if (end > i + 2 && !s[i + 2].isWhitespace() && (mark == "**" || boundary(s, end + 2))) {
                    val start = out.length
                    inline(s.substring(i + 2, end), out, spans)
                    spans += Span(Kind.BOLD, start until out.length)
                    i = end + 2
                    continue
                }
            }
            if (c == '*' && i + 1 < s.length && !s[i + 1].isWhitespace() && s[i + 1] != '*') {
                val end = s.indexOf('*', i + 1)
                if (end > i + 1 && !s[end - 1].isWhitespace()) {
                    val start = out.length
                    inline(s.substring(i + 1, end), out, spans)
                    spans += Span(Kind.ITALIC, start until out.length)
                    i = end + 1
                    continue
                }
            }
            out.append(c)
            i++
        }
    }

    /** `__` vale grassetto solo fuori da una parola: mcp__chrome-bridge__click resta com'è. */
    private fun boundary(s: String, at: Int) = at < 0 || at >= s.length || !s[at].isLetterOrDigit()

    private fun add(kind: Kind, text: String, out: StringBuilder, spans: MutableList<Span>) {
        val start = out.length
        out.append(text)
        spans += Span(kind, start until out.length)
    }
}
