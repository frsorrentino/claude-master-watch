package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.data.PendingStatus
import it.pixelbox.cmwatch.rules.ChatRules.Status
import org.junit.Assert.*
import org.junit.Test

class ChatRulesTest {
    private val t = 1_000_000L
    private val m = Sent("c1", "kb", "run the tests", sentAt = t)
    private fun s(st: SessionState, turn: Long? = null, outcome: Outcome? = null) =
        Session(id = "1", name = "kb", account = "personal", project = "p", state = st, since = 0, turnStarted = turn, outcome = outcome)
    private val ok = CmdResult("c1", ok = true, text = "delivered", at = t + 1)

    @Test fun sendingUntilThePcAnswers() = assertEquals(Status.SENDING, ChatRules.status(m, PendingStatus.SENDING, null, s(SessionState.IDLE)))

    @Test fun failedWhenRejected() =
        assertEquals(Status.FAILED, ChatRules.status(m, null, ok.copy(ok = false, text = "kb is not running"), s(SessionState.GONE)))

    @Test fun failedWhenLost() = assertEquals(Status.FAILED, ChatRules.status(m, PendingStatus.FAILED, null, s(SessionState.IDLE)))

    @Test fun deliveredWhenIdle() = assertEquals(Status.DELIVERED, ChatRules.status(m, null, ok, s(SessionState.IDLE)))

    @Test fun queuedBehindARunningTurn() =
        assertEquals(Status.QUEUED, ChatRules.status(m, null, ok, s(SessionState.BUSY, turn = t - 600)))

    @Test fun workingWhenItsTurnStarts() {
        val a = ChatRules.advance(m, s(SessionState.BUSY, turn = t + 3), now = t + 5)
        assertEquals(t + 3, a.startedAt)
        assertEquals(Status.WORKING, ChatRules.status(a, null, ok, s(SessionState.BUSY, turn = t + 3)))
    }

    @Test fun doneWithItsOutcome() {
        val started = ChatRules.advance(m, s(SessionState.BUSY, turn = t + 3), now = t + 5)
        val out = Outcome("Tests green", "All 40 tests green.", at = t + 90)
        val done = ChatRules.advance(started, s(SessionState.IDLE, outcome = out), now = t + 95)
        assertEquals(t + 95, done.doneAt)
        assertEquals("All 40 tests green.", done.outcomeFull)
        assertEquals(Status.DONE, ChatRules.status(done, null, ok, s(SessionState.IDLE, outcome = out)))
    }

    @Test fun outcomeOfAnEarlierTurnIsNotAttached() {
        val started = ChatRules.advance(m, s(SessionState.BUSY, turn = t + 3), now = t + 5)
        val old = Outcome("Old", "Old turn.", at = t - 100)
        val done = ChatRules.advance(started, s(SessionState.IDLE, outcome = old), now = t + 95)
        assertNotNull(done.doneAt)
        assertNull(done.outcomeFull)
    }

    @Test fun fastTurnSeenOnlyByItsOutcome() {
        val out = Outcome("Done", "Done quickly.", at = t + 4)
        val done = ChatRules.advance(m, s(SessionState.IDLE, outcome = out), now = t + 30)
        assertEquals(t + 4, done.doneAt)
        assertEquals("Done quickly.", done.outcomeFull)
    }

    @Test fun oldOutcomeDoesNotFinishANewMessage() {
        val old = Outcome("Old", "Old turn.", at = t - 100)
        val a = ChatRules.advance(m, s(SessionState.IDLE, outcome = old), now = t + 30)
        assertNull(a.doneAt)
        assertEquals(Status.DELIVERED, ChatRules.status(a, null, ok, s(SessionState.IDLE, outcome = old)))
    }

    @Test fun smallClockSkewStillCountsTheTurn() {
        val a = ChatRules.advance(m, s(SessionState.BUSY, turn = t - 5), now = t + 2)
        assertEquals(Status.WORKING, ChatRules.status(a, null, ok, s(SessionState.BUSY, turn = t - 5)))
    }

    @Test fun advanceIsIdempotentOnceDone() {
        val done = m.copy(startedAt = t + 1, doneAt = t + 9, outcomeFull = "x")
        assertEquals(done, ChatRules.advance(done, s(SessionState.BUSY, turn = t + 50), now = t + 60))
    }

    @Test fun pruneAfterSevenDays() {
        val list = listOf(m, m.copy(id = "old", sentAt = t - ChatRules.KEEP_S - 1))
        assertEquals(listOf("c1"), ChatRules.prune(list, now = t).map { it.id })
    }
}
