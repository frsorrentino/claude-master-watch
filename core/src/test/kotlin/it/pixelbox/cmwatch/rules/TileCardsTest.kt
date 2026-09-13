package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.rules.TileTexts.Card
import org.junit.Assert.*
import org.junit.Test

class TileCardsTest {
    private val q = ContractJson.decodeState(Fixtures.stateQuestion)
    private val idle = ContractJson.decodeState(Fixtures.stateIdle)

    @Test fun onlyNonZeroCountersAtMostThree() {
        // fixture 1: ledger-api waiting (con domanda), atlas-shop busy, field-notes idle, orbit-docs gone
        assertEquals(listOf(Card(Card.Kind.ACTIVE, 2), Card(Card.Kind.IDLE, 1), Card(Card.Kind.GONE, 1)), TileTexts.cards(q, emptySet()))
        assertEquals(listOf(Card(Card.Kind.IDLE, 1)), TileTexts.cards(idle, emptySet()))
    }

    @Test fun waitingCountsAsActiveWhenTheQuestionWasSeen() {
        val seen = setOf(q.sessions[0].question!!.id)
        assertEquals(listOf(Card(Card.Kind.ACTIVE, 2), Card(Card.Kind.IDLE, 1), Card(Card.Kind.GONE, 1)), TileTexts.cards(q, seen))
    }

    @Test fun neverMoreThanThree() {
        val many = q.copy(sessions = q.sessions + q.sessions.map { it.copy(id = it.id + "x", name = it.name + "x", state = SessionState.AWAITING, question = null) })
        assertTrue(TileTexts.cards(many, emptySet()).size <= 3)
    }

    @Test fun badgeUsesTheContractEmojiElseTheStateGlyph() {
        assertEquals("🟦", TileTexts.badge(q.sessions[0]))
        assertEquals("❓", TileTexts.badge(q.sessions[0].copy(icon = null)))
        assertEquals("▶", TileTexts.badge(q.sessions[1].copy(icon = null, question = null)))
        assertEquals("✗", TileTexts.badge(q.sessions[3].copy(icon = null)))
    }

    @Test fun questionAgeLine() {
        assertEquals("ferma da 5 m", TileTexts.waitingFor(q.sessions[0], 1789210800, "ferma da %s"))
    }
}
