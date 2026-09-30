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
}
