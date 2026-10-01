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

    // Dal vivo 30/09 23:00: senza risposta in 20 s il messaggio spesso è arrivato lo stesso; non è «non consegnato».
    @Test fun timeoutIsUncertainNotFailed() = assertEquals(Status.UNCERTAIN, ChatRules.status(m, PendingStatus.FAILED, null, s(SessionState.IDLE)))

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

    // Lo stato preciso di ogni invio (Franz, 30/09 22:13): ogni passaggio si vede, e un fallimento dice perché.
    @Test fun uploadingWhileTheImageGoesUp() =
        assertEquals(Status.UPLOADING, ChatRules.status(m, null, null, s(SessionState.IDLE), ChatRules.Upload.Going))

    @Test fun uploadFailureIsFailedWithItsReason() {
        val up = ChatRules.Upload.Failed("image too large")
        assertEquals(Status.FAILED, ChatRules.status(m, null, null, s(SessionState.IDLE), up))
        assertEquals("image too large", ChatRules.reason(null, null, up))
    }

    @Test fun sentOnceWrittenOnTheBus() = assertEquals(Status.SENT, ChatRules.status(m, PendingStatus.SENT, null, s(SessionState.IDLE)))

    @Test fun offlineWaitsForTheNetwork() = assertEquals(Status.OFFLINE, ChatRules.status(m, PendingStatus.QUEUED, null, s(SessionState.IDLE)))

    @Test fun rejectedCarriesTheRelayReason() =
        assertEquals("kb is not running: nothing sent", ChatRules.reason(null, ok.copy(ok = false, text = "kb is not running: nothing sent"), null))

    @Test fun lostHasNoReasonFromThePc() = assertNull(ChatRules.reason(PendingStatus.FAILED, null, null))

    // Revisione 30/09: il relay mette la sessione in «awaiting» appena consegna il prompt; conta come turno partito.
    @Test fun awaitingCountsAsStarted() {
        val a = ChatRules.advance(m, s(SessionState.AWAITING), now = t + 3)
        assertEquals(t + 3, a.startedAt)
    }

    // Una domanda di permesso a metà turno non chiude il turno.
    @Test fun waitingKeepsTheTurnOpen() {
        val started = m.copy(startedAt = t + 1)
        assertNull(ChatRules.advance(started, s(SessionState.WAITING), now = t + 60).doneAt)
    }

    // Un messaggio fallito resta fallito: il turno dopo non lo prende, neanche ore dopo.
    @Test fun failedMessagesDoNotAdvance() {
        val failed = m.copy(failed = "kb is not running")
        assertEquals(failed, ChatRules.advance(failed, s(SessionState.BUSY, turn = t + 30), now = t + 40))
        assertEquals(Status.FAILED, ChatRules.status(failed, null, null, s(SessionState.IDLE)))
        assertEquals("kb is not running", ChatRules.reason(null, null, null, failed))
    }

    // Un turno partito molto dopo l'invio non è di quel messaggio.
    @Test fun lateTurnsAreNotClaimed() {
        val a = ChatRules.advance(m, s(SessionState.BUSY, turn = t + ChatRules.CLAIM_S + 1), now = t + ChatRules.CLAIM_S + 5)
        assertNull(a.startedAt)
    }

    // Invio programmato (piano 30/09, Task 4): scritto ora, parte alla ripartenza della quota.
    private val later = m.copy(scheduledFor = t + 3600)

    @Test fun scheduledIsItsOwnStatus() {
        assertEquals(Status.SCHEDULED, ChatRules.status(later, null, null, s(SessionState.BUSY, turn = t - 600)))
        // Un turno che parte prima dell'invio non è suo.
        assertNull(ChatRules.advance(later, s(SessionState.BUSY, turn = t + 5), now = t + 10).startedAt)
    }

    /** Telefono spento all'ora giusta: parte appena possibile, una volta sola. */
    @Test fun scheduledSendsOnceWhenLate() {
        assertTrue(ChatRules.due(listOf(later), now = t + 60).isEmpty())
        assertEquals(listOf("c1"), ChatRules.due(listOf(later, m), now = t + 3 * 3600).map { it.id })
        val sent = later.copy(sentAt = t + 3 * 3600)
        assertTrue(ChatRules.due(listOf(sent), now = t + 4 * 3600).isEmpty())
        assertNotEquals(Status.SCHEDULED, ChatRules.status(sent, PendingStatus.SENDING, null, s(SessionState.IDLE)))
    }

    /** «Manda stanotte»: la cartella del progetto della sessione, dai progetti dello stato. */
    @Test fun nightDirFromTheSessionProject() {
        val st = State(v = 1, ts = t, host = "pc", projects = listOf(
            Project("/home/demo/workspaces/personal/atlas-shop", "atlas-shop", "personal"),
            Project("/home/demo/workspaces/work/clients/ledger-api", "ledger-api", "work"),
        ))
        val ledger = s(SessionState.IDLE).copy(name = "ledger-api", project = "work/clients/ledger-api", account = "work")
        assertEquals("/home/demo/workspaces/work/clients/ledger-api", ChatRules.nightDir(st, ledger))
        assertNull(ChatRules.nightDir(st, ledger.copy(project = "work/other")))
    }
}
