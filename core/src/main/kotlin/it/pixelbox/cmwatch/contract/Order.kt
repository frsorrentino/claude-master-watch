package it.pixelbox.cmwatch.contract

object Order {
    // waiting, busy e awaiting, idle, gone, poi alfabetico (contract/README.md): awaiting = lavora a un prompt
    // partito dal polso, quindi «busy» per l'orologio (relay, order_sessions()).
    private fun rank(s: SessionState) = when (s) {
        SessionState.WAITING -> 0; SessionState.BUSY -> 1; SessionState.AWAITING -> 1
        SessionState.IDLE -> 2; SessionState.GONE -> 3
    }
    fun sessions(list: List<Session>): List<Session> =
        list.sortedWith(compareBy<Session> { rank(it.state) }.thenBy { it.name.lowercase() })
}

sealed class Freshness {
    data object Fresh : Freshness()
    /** 1.43: stato arrivato da poco, ma raccolto [lagS] secondi prima della pubblicazione. Vale come fresco ovunque, tranne nel menu. */
    data class Slow(val lagS: Int) : Freshness()
    data class Stale(val minutes: Int) : Freshness()
    companion object {
        const val STALE_AFTER_S = 180L
        const val SLOW_LAG_S = 30L
        /** L'età si conta dalla pubblicazione (1.43), o da `ts` con un relay precedente. */
        fun of(s: State, now: Long): Freshness {
            val pub = s.publishedTs
            if (now - pub >= STALE_AFTER_S) return Stale(((now - pub) / 60).toInt())
            val lag = pub - s.ts
            return if (s.publishedAt != null && lag >= SLOW_LAG_S) Slow(lag.toInt()) else Fresh
        }
    }
}

object Durations {
    /** La lettera dei giorni la decide la lingua di chi disegna: «g» in italiano, «d» in inglese (le altre coincidono). */
    fun since(from: Long, now: Long, days: String = "g"): String {
        val s = (now - from).coerceAtLeast(0)
        return when {
            // Numero e unità insieme (U+00A0): a 384 px «12 min» si spezzava fra le due righe (master, 25/09).
            s < 3600 -> "${s / 60} m"
            s < 86400 -> "%d h %02d".format(s / 3600, (s % 3600) / 60)
            else -> "${s / 86400} $days"
        }
    }
}
