package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Il widget Master (spec 01/10): l'ultimo messaggio o la domanda della master, e le sessioni come chip. */
class MasterWidgetModelTest {
    private fun s(name: String, state: SessionState = SessionState.IDLE, q: Question? = null, outcome: Outcome? = null) =
        Session(id = name, name = name, account = "personale", project = name, state = state, since = 0, question = q, outcome = outcome)
    private fun st(vararg ss: Session) = State(v = 1, ts = 0, host = "pc", sessions = ss.toList())
    private val out = Outcome("push fatto", "Ho pushato i 3 commit.\n\nProssimi: installa · prova\nWatch: push fatto", 100)

    @Test fun lastMessageWithoutServiceLines() {
        val m = MasterWidgetModel.build(st(s("master", outcome = out), s("kb")), maxChips = 6, outcomeLabel = "Esito")
        assertEquals("master", m.master?.name); assertFalse(m.asking)
        assertEquals("Ho pushato i 3 commit.\nEsito: push fatto", m.text); assertEquals(100L, m.at)
    }

    @Test fun questionTakesThePlace() {
        val q = Question("1", QuestionKind.ASK, "Lancio kb?", emptyList(), Tier.LOW, 200)
        val m = MasterWidgetModel.build(st(s("master", SessionState.WAITING, q = q, outcome = out)), maxChips = 6, outcomeLabel = "Esito")
        assertTrue(m.asking); assertEquals("Lancio kb?", m.text); assertEquals(200L, m.at)
    }

    @Test fun chipsWithoutTheMasterWaitingFirstAndMore() {
        val m = MasterWidgetModel.build(st(s("master"), s("a"), s("b", SessionState.BUSY), s("c", SessionState.WAITING), s("d", SessionState.GONE)), maxChips = 2, outcomeLabel = "Esito")
        assertEquals(listOf("c", "b"), m.chips.map { it.name }); assertEquals(1, m.more)
    }

    @Test fun noStateOrNoMaster() {
        val none = MasterWidgetModel.build(null, maxChips = 6, outcomeLabel = "Esito")
        assertNull(none.master); assertNull(none.text); assertTrue(none.chips.isEmpty())
        val gone = MasterWidgetModel.build(st(s("master", SessionState.GONE), s("kb")), maxChips = 6, outcomeLabel = "Esito")
        assertNull(gone.master); assertEquals(listOf("kb"), gone.chips.map { it.name })
    }

    // Revisione finale 02/10 (#8): il widget non mostra i segni del markdown.
    @Test fun noMarkdownSymbols() {
        val m = MasterWidgetModel.build(st(s("master", outcome = Outcome("x", "Fatto il **push** di `3d353f4`.", 1))), maxChips = 0, outcomeLabel = "Esito")
        assertEquals("Fatto il push di 3d353f4.", m.text)
    }
}
