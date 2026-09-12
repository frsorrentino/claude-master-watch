package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.rules.Wake.Action
import it.pixelbox.cmwatch.rules.Wake.NotifyKind
import org.junit.Assert.*
import org.junit.Test

class WakeTest {
    private val q = ContractJson.decodeState(Fixtures.stateQuestion)
    private val idle = ContractJson.decodeState(Fixtures.stateIdle)

    @Test fun newQuestionNotifies() {
        val a = Wake.plan(idle, q)
        assertTrue(a.contains(Action.Notify(NotifyKind.QUESTION, "ledger-api")))
        assertTrue(a.last() == Action.RefreshComplications && a[a.size - 2] == Action.RefreshTile)
    }

    @Test fun sameQuestionDoesNotNotifyTwice() {
        assertTrue(Wake.plan(q, q).none { it is Action.Notify })
    }

    @Test fun firstStateNotifiesOpenQuestions() {
        assertTrue(Wake.plan(null, q).contains(Action.Notify(NotifyKind.QUESTION, "ledger-api")))
    }

    @Test fun newOutcomeOnlyForTheFollowedSession() {
        // atlas-shop non è seguita in fixture 1: il suo esito nuovo non notifica
        val prev = q.copy(sessions = q.sessions.map { if (it.name == "atlas-shop") it.copy(outcome = null) else it })
        assertTrue(Wake.plan(prev, q).none { it == Action.Notify(NotifyKind.OUTCOME, "atlas-shop") })
        val followed = q.copy(sessions = q.sessions.map { if (it.name == "atlas-shop") it.copy(followed = true) else it })
        assertTrue(Wake.plan(prev, followed).contains(Action.Notify(NotifyKind.OUTCOME, "atlas-shop")))
    }

    @Test fun goneNotifiesOnce() {
        val prev = q.copy(sessions = q.sessions.map { if (it.name == "orbit-docs") it.copy(state = SessionState.IDLE) else it })
        assertTrue(Wake.plan(prev, q).contains(Action.Notify(NotifyKind.GONE, "orbit-docs")))
        assertTrue(Wake.plan(q, q).none { it == Action.Notify(NotifyKind.GONE, "orbit-docs") })
        // sparita anche se non c'è più nella lista
        val vanished = q.copy(sessions = q.sessions.filter { it.name != "field-notes" })
        assertTrue(Wake.plan(q, vanished).contains(Action.Notify(NotifyKind.GONE, "field-notes")))
    }

    @Test fun quotaThresholdCrossedOnce() {
        val hot = idle.copy(quota = idle.quota + ("personale" to QuotaAccount(h5 = 96, w7 = 38, resetW7 = 1L)))
        assertTrue(Wake.plan(idle, hot).contains(Action.Notify(NotifyKind.QUOTA, "personale")))
        assertTrue(Wake.plan(hot, hot).none { it is Action.Notify })
    }
}
