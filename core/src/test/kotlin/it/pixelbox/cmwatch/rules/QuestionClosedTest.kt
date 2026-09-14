package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.Order
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.*
import org.junit.Test

/**
 * Una domanda risposta dal terminale o dal telefono sparisce dallo stato: ogni superficie dell'orologio che si
 * ricalcola dallo stato deve tornare giusta (Franz, 14/09 22:12: «deve sparire ovunque»). La notifica, che ha una
 * memoria propria, la chiude `Wake.Action.CloseQuestion` (vedi `WakeTest`).
 */
class QuestionClosedTest {
    private val q = ContractJson.decodeState(Fixtures.stateQuestion)
    private val answered = q.copy(sessions = q.sessions.map { if (it.name == "ledger-api") it.copy(state = SessionState.BUSY, question = null) else it })
    private val ledger = answered.sessions.first { it.name == "ledger-api" }

    @Test fun laTileNonChiedePiuDiRispondere() {
        val edge = TileTexts.edge(answered, Freshness.Fresh, emptySet())
        assertNotEquals(TileTexts.Edge.Kind.REPLY, edge.kind); assertNotEquals(TileTexts.Edge.Kind.QUESTIONS, edge.kind)
        assertFalse(TileTexts.counts(answered).contains("❓"))
    }

    @Test fun laComplicationNonMostraPiuLaDomanda() {
        assertFalse(ComplicationTexts.short(answered, fresh = true).endsWith("?"))
        assertFalse(ComplicationTexts.long(answered, fresh = true, staleLabel = "PC").startsWith("❓"))
        assertEquals("cmwatch://sessions", ComplicationTexts.tapTarget(answered))
    }

    @Test fun ilBadgeTornaAlloStatoDiLavoro() =
        assertEquals(Badge.Glyph.PLAY, Badge.of(ledger.account, ledger.color, ledger.state, ledger.icon).glyph)

    @Test fun laSessioneNonStaPiuInCimaComeInAttesa() {
        assertEquals("ledger-api", Order.sessions(q.sessions).first().name)
        assertNotEquals("ledger-api", Order.sessions(answered.sessions).first().name)
    }
}
