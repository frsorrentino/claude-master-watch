package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Outcome

/**
 * Titolo e testo della schermata Esito (Franz, 14/09 13:20 e 14:09): un titolo di senso compiuto in carattere più
 * piccolo, e sotto solo quello che il titolo non dice già. Il titolo è la riga «Esito:» o «Watch:» intera presa dalla
 * coda del messaggio in `full`; se quella riga non c'è ma `full` comincia come `short`, la prima frase intera di `full`;
 * altrimenti `short`, che il PC taglia a 60 caratteri.
 */
object OutcomeText {
    private val RIGA = Regex("^[\\s*_>-]*(?:Esito|Watch)\\s*:\\s*[*_]*\\s*(.+?)[*_\\s]*$", RegexOption.IGNORE_CASE)
    private val FINE = Regex("[.!?…](\\s|$)")
    private const val MAX = 220

    fun headline(o: Outcome): String {
        riga(o)?.let { return it }
        val base = o.short.trimEnd('…', ' ')
        val full = o.full.trim()
        if (base.isNotEmpty() && full.startsWith(base)) {
            val f = FINE.find(full, base.length.coerceAtMost(full.length))
            val frase = (if (f != null) full.substring(0, f.range.first + 1) else full).trim()
            if (frase.length <= MAX) return frase
        }
        return o.short
    }

    /** Il testo sotto il titolo, senza ripeterlo; null se non resta niente. */
    fun body(o: Outcome): String? {
        val titolo = headline(o)
        val full = o.full.trim()
        val resto = if (riga(o) != null) full.lines().filterNot { RIGA.matches(it.trim()) }.joinToString("\n").trim()
        else full.removePrefix(titolo).trim()
        return resto.takeIf { it.isNotEmpty() && it != titolo }
    }

    private fun riga(o: Outcome): String? =
        o.full.lines().mapNotNull { RIGA.find(it.trim())?.groupValues?.get(1)?.trim() }.lastOrNull { it.isNotEmpty() }
}
