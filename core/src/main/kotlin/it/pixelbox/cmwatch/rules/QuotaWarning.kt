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

    data class Warn(val account: String, val pct: Int, val resetAt: Long, val projected: Boolean)

    /** `samples`: i campioni delle 5 ore dell'account della sessione. */
    fun of(state: State, session: Session, samples: List<QuotaHistory.Sample>, now: Long): Warn? {
        val (name, q) = state.quota.entries.firstOrNull { it.key.equals(session.account, ignoreCase = true) } ?: return null
        if (q.stale) return null
        val pct = q.h5 ?: return null
        val reset = q.resetH5?.takeIf { it > now } ?: return null
        if (pct >= THRESHOLD) return Warn(name, pct, reset, projected = false)
        val projected = QuotaHistory.pace(samples, reset, now).projected ?: return null
        return if (projected >= 100) Warn(name, pct, reset, projected = true) else null
    }
}
