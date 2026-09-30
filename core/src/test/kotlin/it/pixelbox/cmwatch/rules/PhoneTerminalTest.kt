package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneTerminalTest {
    private fun s(st: SessionState) = Session(id = "1", name = "kb", account = "personale", project = "p", state = st, since = 0)
    private val t0 = 1_000L

    @Test fun asksAtOpen() = assertTrue(PhoneTerminal.shouldAsk(s(SessionState.BUSY), lastAskedAt = null, answered = false, now = t0))

    @Test fun waitsForTheAnswer() =
        assertFalse(PhoneTerminal.shouldAsk(s(SessionState.BUSY), lastAskedAt = t0, answered = false, now = t0 + PhoneTerminal.POLL_MS * 2))

    @Test fun asksAgainAfterPoll() {
        assertFalse(PhoneTerminal.shouldAsk(s(SessionState.BUSY), t0, answered = true, now = t0 + PhoneTerminal.POLL_MS - 1))
        assertTrue(PhoneTerminal.shouldAsk(s(SessionState.BUSY), t0, answered = true, now = t0 + PhoneTerminal.POLL_MS))
    }

    @Test fun neverForAClosedSession() {
        assertFalse(PhoneTerminal.shouldAsk(s(SessionState.GONE), lastAskedAt = null, answered = false, now = t0))
        assertFalse(PhoneTerminal.shouldAsk(null, lastAskedAt = null, answered = false, now = t0))
    }

    @Test fun aLostReadDoesNotFreezeTheTerminal() {
        assertFalse(PhoneTerminal.shouldAsk(s(SessionState.BUSY), t0, answered = false, now = t0 + PhoneTerminal.LOST_MS - 1))
        assertTrue(PhoneTerminal.shouldAsk(s(SessionState.BUSY), t0, answered = false, now = t0 + PhoneTerminal.LOST_MS))
    }
}
