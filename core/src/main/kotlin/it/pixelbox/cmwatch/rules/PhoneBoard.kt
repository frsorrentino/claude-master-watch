package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*

/** La regia del telefono (design 29/09): gruppi di card e righe di quota, dallo stato. */
object PhoneBoard {
    enum class Group { WAITING, WORKING, IDLE, CLOSED }
    data class Section(val group: Group, val sessions: List<Session>)
    data class QuotaRow(val account: String, val personal: Boolean, val pct: Int?, val resetAt: Long?, val stale: Boolean)

    private fun group(s: SessionState) = when (s) {
        SessionState.WAITING -> Group.WAITING
        SessionState.BUSY, SessionState.AWAITING -> Group.WORKING
        SessionState.IDLE -> Group.IDLE
        SessionState.GONE -> Group.CLOSED
    }

    /** `withMaster = false` per la lista Sessioni: la master ha la sua scheda (Franz, 02/10). */
    fun sections(state: State, withMaster: Boolean = true): List<Section> {
        val byGroup = Order.sessions(state.sessions.filter { withMaster || it.name != ContextActions.MASTER }).groupBy { group(it.state) }
        return Group.entries.mapNotNull { g -> byGroup[g]?.let { Section(g, it) } }
    }

    /**
     * Personale prima, poi alfabetico. Nessuna ora inventata: un dato vecchio (del relay o dell'intero stato) o un
     * azzeramento già passato non mostrano l'ora (revisione 29/09).
     */
    fun quotaRows(state: State, now: Long, dataStale: Boolean = false): List<QuotaRow> =
        state.quota.map { (name, q) ->
            val reset = q.resetH5?.takeIf { !q.stale && !dataStale && it > now }
            QuotaRow(name, Accounts.isPersonalQuota(name, q), q.h5, reset, q.stale)
        }.sortedWith(compareBy<QuotaRow> { !it.personal }.thenBy { it.account })
}
