package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.Night
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.*
import org.junit.Test

/**
 * La sezione Lavoro della pagina Quota rifatta (Franz, 16/09 13:00, «ok tutte»): una grafica per dato invece dello
 * stesso anello dappertutto. Qui i numeri; il disegno sta nell'app. Fixture: ledger-api aspetta una risposta (ctx 62),
 * atlas-shop lavora (18), field-notes è ferma (4), orbit-docs è sparita.
 */
class WorkPanelTest {
    private val state = ContractJson.decodeState(Fixtures.stateQuestion)
    private val now = state.ts

    /** «Adesso»: un segmento per sessione viva, prima chi aspetta te, poi chi lavora, poi chi è ferma. Le sparite no. */
    @Test fun adessoHaUnSegmentoPerSessioneViva() {
        val a = WorkPanel.now(state)
        assertEquals(listOf(WorkPanel.Seg.WAITING, WorkPanel.Seg.WORKING, WorkPanel.Seg.IDLE), a.segments)
        assertEquals(1, a.working); assertEquals(1, a.waiting); assertEquals(1, a.idle)
    }

    @Test fun awaitingContaComeLavoro() {
        val s = state.copy(sessions = state.sessions.map { if (it.name == "field-notes") it.copy(state = SessionState.AWAITING) else it })
        assertEquals(2, WorkPanel.now(s).working)
    }

    /** «Contesto»: le sessioni vive col contesto noto, dalla più piena, al massimo quattro. */
    @Test fun contestoOrdinatoDallaPiuPiena() {
        val c = WorkPanel.contexts(state)
        assertEquals(listOf("ledger-api", "atlas-shop", "field-notes"), c.map { it.name })
        assertEquals(62, c.first().pct)
    }

    @Test fun contestoAlMassimoQuattroRighe() {
        val molte = state.copy(sessions = (1..6).map { i -> state.sessions[1].copy(id = "s$i", name = "s$i", context = i * 10) })
        val c = WorkPanel.contexts(molte)
        assertEquals(4, c.size); assertEquals("s6", c.first().name)
    }

    @Test fun contestoConLeSoglieDelleMisure() {
        val s = state.copy(sessions = state.sessions.map { if (it.name == "ledger-api") it.copy(context = 91) else it })
        assertEquals(BriefCards.Tone.ALERT, WorkPanel.contexts(s).first().tone)
    }

    /** Le domande: quante, la più vecchia con il suo nome e da quanto aspetta, per aprirla con un tocco. */
    @Test fun domandeConLaPiuVecchia() {
        val d = WorkPanel.questions(state, now)!!
        assertEquals(1, d.count); assertEquals("ledger-api", d.oldest); assertEquals("5 m", d.age)
    }

    @Test fun senzaDomandeNienteCard() {
        val s = state.copy(sessions = state.sessions.map { it.copy(question = null) })
        assertNull(WorkPanel.questions(s, now))
    }

    @Test fun notteSoloQuandoCeQualcosa() {
        assertEquals(2, WorkPanel.night(state)!!.queued)
        assertNull(WorkPanel.night(state.copy(night = Night())))
    }

    /** La riga in fondo al posto della card «Aggiornato»: età e macchina, e se il PC è fermo. */
    @Test fun rigaDellAggiornamento() {
        val fresco = WorkPanel.updated(state, Freshness.Fresh, now)
        assertEquals(0, fresco.minutes); assertEquals("crostini-demo", fresco.host); assertFalse(fresco.stale)
        val fermo = WorkPanel.updated(state, Freshness.Stale(7), now + 7 * 60)
        assertEquals(7, fermo.minutes); assertTrue(fermo.stale)
    }
}
