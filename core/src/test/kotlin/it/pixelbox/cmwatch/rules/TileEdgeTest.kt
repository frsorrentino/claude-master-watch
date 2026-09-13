package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.rules.TileTexts.Edge
import org.junit.Assert.assertEquals
import org.junit.Test

class TileEdgeTest {
    private val q = ContractJson.decodeState(Fixtures.stateQuestion)
    private val idle = ContractJson.decodeState(Fixtures.stateIdle)

    @Test fun oneQuestionAsksToAnswer() =
        assertEquals(Edge(Edge.Kind.REPLY, 1), TileTexts.edge(q, Freshness.Fresh, emptySet()))

    @Test fun moreQuestionsCountThem() {
        val two = q.copy(sessions = q.sessions.map { if (it.name == "field-notes") q.sessions[0].copy(id = "x", name = "field-notes") else it })
        assertEquals(Edge(Edge.Kind.QUESTIONS, 2), TileTexts.edge(two, Freshness.Fresh, emptySet()))
    }

    @Test fun seenQuestionsDoNotCount() {
        val seen = setOf(q.sessions[0].question!!.id)
        assertEquals(Edge(Edge.Kind.ACTIVE, 2), TileTexts.edge(q, Freshness.Fresh, seen))
    }

    @Test fun nothingActiveOrStaleFallsBackToSessions() {
        assertEquals(Edge(Edge.Kind.SESSIONS, 0), TileTexts.edge(idle, Freshness.Fresh, emptySet()))
        assertEquals(Edge(Edge.Kind.SESSIONS, 0), TileTexts.edge(q, Freshness.Stale(5), emptySet()))
        assertEquals(Edge(Edge.Kind.SESSIONS, 0), TileTexts.edge(null, Freshness.Fresh, emptySet()))
    }
}
