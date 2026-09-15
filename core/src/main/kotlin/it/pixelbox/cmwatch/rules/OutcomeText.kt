package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Outcome

/**
 * Titolo e testo della schermata Esito (Franz, 14/09 13:20 e 14:09): un titolo di senso compiuto in carattere più
 * piccolo, e sotto solo quello che il titolo non dice già. Il titolo è la riga «Esito:» o «Watch:» intera presa dalla
 * coda del messaggio in `full`; se quella riga non c'è ma `full` comincia come `short`, la prima frase intera di `full`;
 * altrimenti `short`, che il PC taglia a 200 caratteri a fine parola (contratto 1.6).
 */
object OutcomeText {
    private val RIGA = Regex("^[\\s*_>-]*(?:Esito|Watch)\\s*:\\s*[*_]*\\s*(.+?)[*_\\s]*$", RegexOption.IGNORE_CASE)
    private val FINE = Regex("[.!?…](\\s|$)")
    private const val MAX = 220

    /** Oltre questa lunghezza il titolo dell'Esito scende a `bodyLarge`: in `titleMedium` una frase lunga riempiva lo schermo. */
    private const val BIG_MAX = 60

    /** Titolo grande solo per la frase breve (Franz, 14/09 17:40): dal contratto 1.6 la riga d'esito arriva a 200 caratteri. */
    fun bigTitle(title: String): Boolean = title.length <= BIG_MAX

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

    /**
     * Il corpo della card nella Scheda. `full` è la coda del messaggio (contratto 1.6): quando è lunga quanto un taglio
     * e comincia con la minuscola è partita a metà frase, e allora si parte dalla prima frase intera (Franz, 15/09 17:18,
     * «di un'altra versione non blocca.» in cima alla card). Il messaggio intero sta nella Risposta.
     */
    fun cardBody(o: Outcome): String? {
        val b = body(o) ?: return null
        if (o.full.trim().length < CODA_MIN) return b
        val primo = b.firstOrNull { it.isLetterOrDigit() } ?: return b
        if (!primo.isLowerCase()) return b
        val fine = FINE_FRASE.find(b) ?: return b
        return b.substring(fine.range.last + 1).trim().ifEmpty { b }
    }

    /** Sotto questa lunghezza `full` non è stato tagliato: il PC taglia la coda a 600 o, sotto pressione, a 300. */
    private const val CODA_MIN = 250
    private val FINE_FRASE = Regex("[.!?…]\\s+|\\n")

    private fun riga(o: Outcome): String? =
        o.full.lines().mapNotNull { RIGA.find(it.trim())?.groupValues?.get(1)?.trim() }.lastOrNull { it.isNotEmpty() }
}
