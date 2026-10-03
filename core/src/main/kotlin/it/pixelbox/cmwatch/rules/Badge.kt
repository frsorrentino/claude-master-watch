package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.SessionState

/**
 * Un badge per sessione (Franz, 12/09 16:27): forma = account (cerchio personale, quadrato arrotondato agenzia),
 * riempimento = `color` della sessione (contratto 1.1; assente o non valido → grigio), glifo di stato monocromo dentro,
 * nero o bianco scelto dal contrasto WCAG calcolato (≥ 4,5:1), non da una tabella.
 */
object Badge {
    enum class Shape { CIRCLE, SQUARE }
    /**
     * Icone uniche ovunque (Franz, 03/10 09:01, dalla consulenza del 02/10): mano = ti aspetta, fulmine = al lavoro, pausa =
     * ferma, croce = chiusa. Il ▶ resta solo alla lettura vocale.
     */
    enum class Glyph { HAND, ZAP, PAUSE, CROSS }
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

    fun of(account: String, color: String?, state: SessionState, icon: String? = null, kind: String? = null): Spec {
        val emoji = icon?.trim()?.takeIf { it.isNotEmpty() }
        val fill = parse(color) ?: parse(EMOJI_COLOR[emoji]) ?: GREY
        val glyph = when (state) {
            SessionState.BUSY, SessionState.AWAITING -> Glyph.ZAP
            SessionState.IDLE -> Glyph.PAUSE
            SessionState.WAITING -> Glyph.HAND
            SessionState.GONE -> Glyph.CROSS
        }
        val glyphColor = if (contrast(BLACK, fill) >= 4.5) BLACK else WHITE
        // Forma dall'account: tondo = personale, quadrato = lavoro. Dal contratto 1.8 lo dice il tipo, non il nome.
        val square = !Accounts.personal(account, kind)
        return Spec(if (square) Shape.SQUARE else Shape.CIRCLE, fill, glyph, glyphColor)
    }

    /**
     * Il tracciato di ogni glifo, a tratto in un riquadro 24×24 (Lucide, ISC: «hand», «zap», «x»; la pausa sono due
     * barre più lunghe di quelle di Lucide, leggibili dentro il badge). Una sola sorgente per il badge del telefono e
     * dell'orologio, le notifiche e le icone di stato.
     */
    fun paths(g: Glyph): List<String> = when (g) {
        Glyph.HAND -> listOf(
            "M18 11V6a2 2 0 0 0-2-2a2 2 0 0 0-2 2",
            "M14 10V4a2 2 0 0 0-2-2a2 2 0 0 0-2 2v2",
            "M10 10.5V6a2 2 0 0 0-2-2a2 2 0 0 0-2 2v8",
            "M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.86-5.99-2.34l-3.6-3.6a2 2 0 0 1 2.83-2.82L7 15",
        )
        Glyph.ZAP -> listOf("M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z")
        Glyph.PAUSE -> listOf("M9 6v12", "M15 6v12")
        Glyph.CROSS -> listOf("M18 6 6 18", "M6 6l12 12")
    }

    data class Labels(
        val waiting: String, val busy: String, val idle: String, val gone: String, val awaiting: String,
        val personal: String, val work: String,
    )

    /** Quello che il badge dice a chi guarda, detto a TalkBack (S07): lo stato del glifo e l'account della forma. */
    fun description(account: String, kind: String?, state: SessionState, l: Labels): String {
        val stato = when (state) {
            SessionState.WAITING -> l.waiting
            SessionState.BUSY -> l.busy
            SessionState.AWAITING -> l.awaiting
            SessionState.IDLE -> l.idle
            SessionState.GONE -> l.gone
        }
        return "$stato, ${if (Accounts.personal(account, kind)) l.personal else l.work}"
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
