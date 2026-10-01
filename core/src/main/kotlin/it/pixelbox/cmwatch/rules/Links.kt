package it.pixelbox.cmwatch.rules

/**
 * I link nei testi della chat, da rendere toccabili (Franz, 01/10 20:05: il link del mockup nella risposta non si
 * apriva). Solo `http(s)://`: un dominio senza schema resta testo. La punteggiatura in coda (punto, virgola, «»,
 * parentesi chiusa senza la sua aperta) non fa parte del link.
 */
object Links {
    private val URL = Regex("""https?://[^\s<>"«»\[\]{}]+""")
    private const val TRAILING = ".,;:!?'’\"»)"

    fun find(text: String): List<IntRange> = URL.findAll(text).mapNotNull { m ->
        var end = m.range.last
        while (end > m.range.first && text[end] in TRAILING) {
            val url = text.substring(m.range.first, end + 1)
            // Una ) chiusa da una ( dentro il link ne fa parte: Roma_(città).
            if (text[end] == ')' && url.count { it == '(' } >= url.count { it == ')' }) break
            end--
        }
        (m.range.first..end).takeIf { end - m.range.first >= "http://x".length - 1 }
    }.toList()
}
