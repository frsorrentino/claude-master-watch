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
    data class Stale(val minutes: Int) : Freshness()
    companion object {
        const val STALE_AFTER_S = 180L
        fun of(stateTs: Long, now: Long): Freshness {
            val age = now - stateTs
            return if (age < STALE_AFTER_S) Fresh else Stale((age / 60).toInt())
        }
    }
}

object Durations {
    /** La lettera dei giorni la decide la lingua di chi disegna: «g» in italiano, «d» in inglese (le altre coincidono). */
    fun since(from: Long, now: Long, days: String = "g"): String {
        val s = (now - from).coerceAtLeast(0)
        return when {
            s < 3600 -> "${s / 60} m"
            s < 86400 -> "%d h %02d".format(s / 3600, (s % 3600) / 60)
            else -> "${s / 86400} $days"
        }
    }
}
