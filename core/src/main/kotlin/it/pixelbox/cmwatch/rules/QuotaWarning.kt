package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.State

/**
 * L'avviso della quota sopra la barra di scrittura (piano 30/09, Task 3): la finestra di 5 ore dell'account della
 * sessione è al 90 % o il ritmo attuale la esaurisce prima del reset. Mai con un dato vecchio, senza lettura delle 5 ore
 * o senza un reset futuro: un avviso inventato farebbe rimandare un lavoro per niente.
 */
object QuotaWarning {
    const val THRESHOLD = 90

    /**
     * Il giudizio sul ritmo aspetta prove sufficienti (segnalazione 01/10 21:58: all'1 % avvisava già): campioni che
     * coprono almeno mezz'ora e la finestra almeno al 20 %. Prima, due campioni vicini fanno sembrare enorme qualunque ritmo.
     */
    const val MIN_SPAN_S = 30 * 60L
    const val MIN_PCT = 20

    data class Warn(val account: String, val pct: Int, val resetAt: Long, val projected: Boolean)

    /** `samples`: i campioni delle 5 ore dell'account della sessione. */
    fun of(state: State, session: Session, samples: List<QuotaHistory.Sample>, now: Long): Warn? {
        val (name, q) = state.quota.entries.firstOrNull { it.key.equals(session.account, ignoreCase = true) } ?: return null
        if (q.stale) return null
        val pct = q.h5 ?: return null
        val reset = q.resetH5?.takeIf { it > now } ?: return null
        if (pct >= THRESHOLD) return Warn(name, pct, reset, projected = false)
        if (pct < MIN_PCT) return null
        val pace = QuotaHistory.pace(samples, reset, now)
        val span = pace.points.takeIf { it.size >= 2 }?.let { it.last().ts - it.first().ts } ?: 0
        if (span < MIN_SPAN_S) return null
        val projected = pace.projected ?: return null
        return if (projected >= 100) Warn(name, pct, reset, projected = true) else null
    }
}
