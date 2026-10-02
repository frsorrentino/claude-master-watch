package it.pixelbox.cmwatch.data

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.rules.ChatRules
import it.pixelbox.cmwatch.rules.Sent
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ChatLogTest {
    @get:Rule val tmp = TemporaryFolder()
    private var now = 1_000_000L
    private fun log() = ChatLog(java.io.File(tmp.root, "chat.json")) { now }
    private val m = Sent("c1", "kb", "hello", sentAt = now)

    @Test fun addPersistsAcrossInstances() {
        log().add(m)
        assertEquals(listOf(m), log().forSession("kb"))
    }

    @Test fun advanceMovesToDone() {
        val l = log(); l.add(m)
        val out = Outcome("ok", "All good.", at = now + 10)
        now += 20
        l.advance(State(v = 1, ts = now, host = "pc", sessions = listOf(
            Session(id = "1", name = "kb", account = "personal", project = "p", state = SessionState.IDLE, since = 0, outcome = out),
        )))
        assertEquals("All good.", log().forSession("kb").single().outcomeFull)
    }

    @Test fun attachmentSurvivesTheRoundTrip() {
        log().add(m.copy(id = "c2", attachment = "/data/chat/c2.jpg"))
        assertEquals("/data/chat/c2.jpg", log().forSession("kb").single { it.id == "c2" }.attachment)
    }

    @Test fun corruptFileStartsEmpty() {
        java.io.File(tmp.root, "chat.json").writeText("{not json")
        assertTrue(log().forSession("kb").isEmpty())
    }

    @Test fun oldMessagesPrunedOnAdvance() {
        val l = log(); l.add(m)
        now += ChatRules.KEEP_S + 1
        l.advance(null)
        assertTrue(log().forSession("kb").isEmpty())
    }

    @Test fun failureIsSavedOnTheMessage() {
        log().add(m)
        log().markFailed("c1", "kb is not running")
        assertEquals("kb is not running", log().forSession("kb").single().failed)
    }

    /** Il pannello di un comando slash resta sul messaggio, come il motivo di un fallimento (dal vivo 02/10 14:51). */
    @Test fun panelIsSavedOnTheMessage() {
        log().add(m)
        log().markPanel("c1", "Total cost: \$0.42")
        assertEquals("Total cost: \$0.42", log().forSession("kb").single().panel)
    }

    /** Due giri del lavoro in background insieme: il programmato si prende una volta sola, e resta preso dopo un riavvio. */
    @Test fun claimDueOnce() {
        val l = log(); l.add(m.copy(id = "s1", scheduledFor = now + 3600))
        assertTrue(l.claimDue().isEmpty())
        now += 7200
        assertEquals(listOf("s1"), l.claimDue().map { it.id })
        assertTrue(l.claimDue().isEmpty())
        assertTrue(log().claimDue().isEmpty())
        assertEquals(now, log().forSession("kb").single().sentAt)
    }

    /** Revisione finale 01/10 (C1): un invio non riuscito torna programmato e si riprende al giro dopo. */
    @Test fun unclaimMakesItDueAgain() {
        val l = log(); l.add(m.copy(id = "s1", scheduledFor = now + 60))
        now += 120
        l.claimDue()
        l.unclaim("s1")
        assertEquals(listOf("s1"), l.claimDue().map { it.id })
    }
}
