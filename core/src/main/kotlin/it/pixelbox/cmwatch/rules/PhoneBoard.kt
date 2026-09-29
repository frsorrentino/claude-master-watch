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

    fun sections(state: State): List<Section> {
        val byGroup = Order.sessions(state.sessions).groupBy { group(it.state) }
        return Group.entries.mapNotNull { g -> byGroup[g]?.let { Section(g, it) } }
    }

    /** Personale prima, poi alfabetico. Un dato vecchio non mostra l'ora dell'azzeramento: potrebbe essere passata. */
    fun quotaRows(state: State): List<QuotaRow> =
        state.quota.map { (name, q) ->
            QuotaRow(name, Accounts.isPersonalQuota(name, q), q.h5, if (q.stale) null else q.resetH5, q.stale)
        }.sortedWith(compareBy<QuotaRow> { !it.personal }.thenBy { it.account })
}
