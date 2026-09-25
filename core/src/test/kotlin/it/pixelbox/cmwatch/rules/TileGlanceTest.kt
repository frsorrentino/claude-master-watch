package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TileGlanceTest {
    private val q = ContractJson.decodeState(Fixtures.stateQuestion)
    private val idle = ContractJson.decodeState(Fixtures.stateIdle)
    private val l = TileTexts.Labels(none = "Nessuna sessione", stale = "PC fermo da %d min", works = "lavora")

    @Test fun questionIsTheOnlyThingThatMatters() {
        val g = TileTexts.glance(q, Freshness.Fresh, 1789210800, l)
        assertEquals("1 ❓ · 1 ▶ · 1 ✓ · 1 ✗", g.counts)
        assertEquals("ledger-api", g.name)
        assertEquals("Deploy ready, waiting for the client's ok. Deploy now?", g.body)
        assertEquals(TileTexts.Button.REPLY, g.button); assertEquals(TileTexts.Accent.QUESTION, g.accent)
        assertEquals("cmwatch://question/ledger-api", g.target)
    }

    @Test fun withoutQuestionsTheFollowedOrMostRecentSession() {
        val g = TileTexts.glance(idle, Freshness.Fresh, 1789214000L + 360, l)
        assertEquals("1 ✓", g.counts); assertEquals("atlas-shop", g.name)
        assertEquals("Seeds and admin page reviewed", g.body)                  // idle: l'esito breve
        assertEquals(TileTexts.Button.SESSIONS, g.button); assertEquals(TileTexts.Accent.IDLE, g.accent)
        assertEquals("cmwatch://session/atlas-shop", g.target)
        val busy = q.copy(sessions = q.sessions.map { it.copy(question = null, state = if (it.name == "ledger-api") SessionState.BUSY else it.state, tool = if (it.name == "ledger-api") "Bash pytest -q" else it.tool) })
        val b = TileTexts.glance(busy, Freshness.Fresh, 1789210800, l)
        assertEquals("ledger-api", b.name)                                    // seguita
        assertEquals("▶ Bash pytest -q · 7 m", b.body); assertEquals(TileTexts.Accent.BUSY, b.accent)
    }

    @Test fun staleAndEmpty() {
        val s = TileTexts.glance(ContractJson.decodeState(Fixtures.stateStale), Freshness.Stale(12), 0, l)
        assertNull(s.name); assertEquals("PC fermo da 12 min", s.body); assertEquals(TileTexts.Accent.STALE, s.accent)
        assertEquals("", s.counts)
        val e = TileTexts.glance(null, Freshness.Stale(0), 0, l)
        assertEquals("Nessuna sessione", e.body); assertEquals(TileTexts.Button.SESSIONS, e.button); assertEquals("cmwatch://sessions", e.target)
    }
}
