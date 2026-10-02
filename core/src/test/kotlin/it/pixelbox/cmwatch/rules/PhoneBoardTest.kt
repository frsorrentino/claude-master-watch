package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import org.junit.Assert.*
import org.junit.Test

class PhoneBoardTest {
    private fun s(id: String, name: String, st: SessionState, account: String = "personale") =
        Session(id = id, name = name, account = account, project = "p", state = st, since = 0)

    private val state = State(v = 1, ts = 100, host = "pc", sessions = listOf(
        s("1", "docs", SessionState.IDLE), s("2", "kb", SessionState.BUSY), s("3", "ledger", SessionState.WAITING),
        s("4", "old", SessionState.GONE), s("5", "watch", SessionState.AWAITING), s("6", "kb", SessionState.BUSY, account = "lavoro"),
    ), quota = mapOf(
        "lavoro" to QuotaAccount(h5 = 18, resetH5 = 500, kind = "work"),
        "personale" to QuotaAccount(h5 = 62, resetH5 = 400, kind = "personal"),
    ))

    @Test fun groupsInOrderAwaitingCountsAsWorking() {
        val g = PhoneBoard.sections(state)
        assertEquals(listOf(PhoneBoard.Group.WAITING, PhoneBoard.Group.WORKING, PhoneBoard.Group.IDLE, PhoneBoard.Group.CLOSED), g.map { it.group })
        assertEquals(listOf("2", "6", "5"), g[1].sessions.map { it.id })
    }

    @Test fun sameNameOnTwoAccountsStaysTwoCards() {
        val working = PhoneBoard.sections(state).first { it.group == PhoneBoard.Group.WORKING }
        assertEquals(2, working.sessions.count { it.name == "kb" })
    }

    @Test fun emptyGroupsAreLeftOut() {
        val only = state.copy(sessions = listOf(s("1", "docs", SessionState.IDLE)))
        assertEquals(listOf(PhoneBoard.Group.IDLE), PhoneBoard.sections(only).map { it.group })
    }

    // Franz, 02/10 06:39: la master ha la sua scheda, in Sessioni non si ripete.
    @Test fun masterLeftOutOfTheList() {
        val withMaster = state.copy(sessions = state.sessions + s("7", ContextActions.MASTER, SessionState.IDLE))
        assertTrue(PhoneBoard.sections(withMaster, withMaster = false).flatMap { it.sessions }.none { it.name == ContextActions.MASTER })
        assertEquals(1, PhoneBoard.sections(withMaster).flatMap { it.sessions }.count { it.name == ContextActions.MASTER })
    }

    @Test fun quotaPersonalFirst() {
        val q = PhoneBoard.quotaRows(state, now = 100)
        assertEquals(listOf("personale", "lavoro"), q.map { it.account })
        assertEquals(PhoneBoard.QuotaRow("personale", true, 62, 400, false), q[0])
    }

    @Test fun staleOrMissingResetInventsNoTime() {
        val q = PhoneBoard.quotaRows(now = 100, state = state.copy(quota = mapOf("personale" to QuotaAccount(h5 = 40, resetH5 = 400, stale = true))))
        assertNull(q[0].resetAt); assertTrue(q[0].stale)
    }

    @Test fun passedResetShowsNoTime() =
        assertNull(PhoneBoard.quotaRows(state, now = 450).first { it.account == "personale" }.resetAt)

    @Test fun oldSnapshotShowsNoTime() =
        assertNull(PhoneBoard.quotaRows(state, now = 100, dataStale = true).first { it.account == "personale" }.resetAt)
}
