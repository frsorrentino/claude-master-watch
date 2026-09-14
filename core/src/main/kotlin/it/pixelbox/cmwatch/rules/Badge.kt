package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.SessionState

/**
 * Un badge per sessione (Franz, 12/09 16:27): forma = account (cerchio personale, quadrato arrotondato agenzia),
 * riempimento = `color` della sessione (contratto 1.1; assente o non valido → grigio), glifo di stato monocromo dentro,
 * nero o bianco scelto dal contrasto WCAG calcolato (≥ 4,5:1), non da una tabella.
 */
object Badge {
    enum class Shape { CIRCLE, SQUARE }
    enum class Glyph { PLAY, CHECK, QUESTION, CROSS }
    data class Spec(val shape: Shape, val fill: Int, val glyph: Glyph, val glyphColor: Int)

    const val GREY = 0xFF9B9B9B.toInt()
    const val BLACK = 0xFF000000.toInt()
    const val WHITE = 0xFFF2F4F7.toInt()

    /** Tabella emoji → colore del contratto 1.1 (claude-master, 12/09 16:46); l'emoji dice anche la forma. */
    private val EMOJI_COLOR = mapOf(
        "🟠" to "#F5A623", "🟧" to "#F5A623", "🧡" to "#F5A623", "🟡" to "#F4D03F", "🟨" to "#F4D03F", "💛" to "#F4D03F",
        "🔴" to "#E74C3C", "🟥" to "#E74C3C", "❤️" to "#E74C3C", "🟢" to "#2ECC71", "🟩" to "#2ECC71", "💚" to "#2ECC71",
        "🔵" to "#3B82F6", "🟦" to "#3B82F6", "💙" to "#3B82F6", "🟣" to "#9B59B6", "🟪" to "#9B59B6", "💜" to "#9B59B6",
        "⚪" to "#BDC3C7", "⬜" to "#BDC3C7", "🤍" to "#BDC3C7", "🟤" to "#8D6E63", "🟫" to "#8D6E63", "🤎" to "#8D6E63",
    )
    private val SQUARES = setOf("🟧", "🟨", "🟥", "🟩", "🟦", "🟪", "⬜", "🟫")

    fun of(account: String, color: String?, state: SessionState, icon: String? = null): Spec {
        val emoji = icon?.trim()?.takeIf { it.isNotEmpty() }
        val fill = parse(color) ?: parse(EMOJI_COLOR[emoji]) ?: GREY
        val glyph = when (state) {
            SessionState.BUSY, SessionState.AWAITING -> Glyph.PLAY
            SessionState.IDLE -> Glyph.CHECK
            SessionState.WAITING -> Glyph.QUESTION
            SessionState.GONE -> Glyph.CROSS
        }
        val glyphColor = if (contrast(BLACK, fill) >= 4.5) BLACK else WHITE
        // Forma dall'account (contratto 1.1): tondo = personale, quadrato = qualunque altro account.
        val square = account.lowercase() != "personale"
        return Spec(if (square) Shape.SQUARE else Shape.CIRCLE, fill, glyph, glyphColor)
    }

    /**
     * Il badge respira mentre la sessione lavora, come il pallino dell'app Claude (Franz, 14/09 16:24): ogni sessione
     * al lavoro, non più solo la seguita, che nella lista ha il suo bordo e la campanella.
     */
    fun breathes(state: SessionState): Boolean = state == SessionState.BUSY || state == SessionState.AWAITING

    fun parse(color: String?): Int? {
        val h = color?.trim()?.removePrefix("#") ?: return null
        if (!h.matches(Regex("[0-9a-fA-F]{6}"))) return null
        return (0xFF000000L or h.toLong(16)).toInt()
    }

    /** Rapporto di contrasto WCAG 2.x fra due colori ARGB opachi. */
    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a); val lb = luminance(b)
        val (hi, lo) = if (la >= lb) la to lb else lb to la
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun luminance(argb: Int): Double {
        fun ch(v: Int): Double { val c = v / 255.0; return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4) }
        return 0.2126 * ch((argb shr 16) and 0xFF) + 0.7152 * ch((argb shr 8) and 0xFF) + 0.0722 * ch(argb and 0xFF)
    }
}
