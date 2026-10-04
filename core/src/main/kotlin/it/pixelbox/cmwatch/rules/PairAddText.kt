package it.pixelbox.cmwatch.rules

/**
 * Contratto 1.31 (Franz, 04/10 13:47: «pairing dal telefono senza PC»): i rifiuti di `pair_add` riconosciuti dal testo del
 * relay (inglese anche col relay in italiano, tranne la chiave mancante) e il tempo che resta all'invito.
 */
object PairAddText {
    enum class Refusal { BUSY, NO_KEY, FULL, FAILED, OTHER }

    private val FULL = Regex("""already \d+ devices""")

    fun refusal(text: String): Refusal = when {
        "a pairing is already open" in text -> Refusal.BUSY
        text.startsWith("pair --add:") -> Refusal.NO_KEY
        FULL.containsMatchIn(text) -> Refusal.FULL
        text.startsWith("pairing not started") -> Refusal.FAILED
        else -> Refusal.OTHER
    }

    /** Il motivo dopo «pairing not started: », da mostrare com'è. */
    fun reason(text: String): String = text.substringAfter("pairing not started:", text).trim()

    /** «m:ss» fino alla scadenza, mai sotto zero. */
    fun left(exp: Long, now: Long): String {
        val s = (exp - now).coerceAtLeast(0)
        return "%d:%02d".format(s / 60, s % 60)
    }
}
