package it.pixelbox.cmwatch.rules

/**
 * Il rifiuto del PC per un file della chat (contratto 1.24): «too large: <byte>», con la 1.34 anche « max <byte>».
 * Franz, 04/10 21:55: l'app mostrava i byte nudi («too large: 3630371»); così mostra la misura e, se c'è, il limite.
 */
object FileRefusal {
    data class TooLarge(val bytes: Long, val max: Long?)

    private val TOO_LARGE = Regex("""too large:\s*(\d+)(?:\s*max\s*(\d+))?""", RegexOption.IGNORE_CASE)

    fun tooLarge(reason: String): TooLarge? =
        TOO_LARGE.find(reason)?.let { m -> TooLarge(m.groupValues[1].toLong(), m.groupValues[2].toLongOrNull()) }
}
