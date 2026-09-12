package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class SpeakRulesTest {
    @Test fun buttonAboveThresholdOrForOutcomeAnswerQuestion() {
        assertFalse(SpeakRules.showButton("corto", SpeakRules.Kind.PLAIN, 120))
        assertTrue(SpeakRules.showButton("x".repeat(121), SpeakRules.Kind.PLAIN, 120))
        assertTrue(SpeakRules.showButton("corto", SpeakRules.Kind.OUTCOME, 120))
        assertTrue(SpeakRules.showButton("corto", SpeakRules.Kind.QUESTION, 120))
        assertTrue(SpeakRules.showButton("corto", SpeakRules.Kind.ANSWER, 120))
        assertFalse(SpeakRules.showButton("", SpeakRules.Kind.OUTCOME, 120))
    }
}

class TerminalTextTest {
    @Test fun atMostThirtyWholeLines() {
        val lines = TerminalText.lines((1..40).joinToString("\n") { "riga $it" })
        assertEquals(30, lines.size); assertEquals("riga 11", lines.first()); assertEquals("riga 40", lines.last())
        assertEquals(listOf("$ pytest -q tests", "42 passed in 3.1s"), TerminalText.lines("$ pytest -q tests\n42 passed in 3.1s\n"))
    }
}

class TimelineTextTest {
    private val ev = ContractJson.decodeEvents(Fixtures.events)
    private val zone = ZoneId.of("Europe/Rome")

    @Test fun groupedByDayNewestFirst() {
        val g = TimelineText.groups(ev, zone)
        assertEquals(listOf("12 set"), g.map { it.day })
        assertEquals(6, g[0].rows.size)
        assertEquals("13:01 · 1 yes · ledger-api · risposto da watch-pixel5", g[0].rows[0])
        assertEquals("12:55 · ❓ ledger-api · Deploy ready, waiting for the client's ok. Deploy now?", g[0].rows[1])
    }

    @Test fun filterBySession() {
        val g = TimelineText.groups(ev, zone, session = "atlas-shop")
        assertEquals(2, g[0].rows.size)
    }

    @Test fun rowWithoutBodyHasNoTrailingSeparator() {
        val gone = ev.first { it.kind == EventKind.GONE }
        assertEquals("10:00 · ✗ orbit-docs", TimelineText.row(gone, zone))
    }
}

class LaunchRulesTest {
    private val s = ContractJson.decodeState(Fixtures.stateQuestion)
    @Test fun onlyPublishedPaths() {
        assertTrue(LaunchRules.allowed("/home/demo/workspaces/personal/atlas-shop", s.projects))
        assertFalse(LaunchRules.allowed("/tmp/evil", s.projects))
        assertFalse(LaunchRules.allowed("", s.projects))
    }
}

class FollowRulesTest {
    private val s = ContractJson.decodeState(Fixtures.stateQuestion)
    @Test fun ongoingOnlyWhileTheFollowedSessionWorks() {
        assertNull(FollowRules.ongoing(s))                                    // ledger-api seguita ma waiting
        val busy = s.copy(sessions = s.sessions.map { if (it.name == "ledger-api") it.copy(state = SessionState.BUSY, question = null) else it })
        assertEquals("ledger-api", FollowRules.ongoing(busy)?.name)
        assertEquals("▶ ledger-api · 7 m", FollowRules.status(busy.sessions[0].copy(state = SessionState.BUSY, question = null), 1789210800))
    }
}

class QuotaTextTest {
    private val labels = QuotaText.Labels(week = "settimana", reset = "reset", stale = "dato vecchio", none = "—")
    @Test fun lines() {
        val q = QuotaAccount(h5 = 11, w7 = 36, resetW7 = 1789610400, stale = false)
        assertEquals("personale · 11 %", QuotaText.h5Line("personale", q, labels))
        assertEquals("settimana 36 % · reset gio 04:00", QuotaText.w7Line(q, labels, ZoneId.of("Europe/Rome")))
        val none = QuotaAccount(h5 = null, w7 = 75, resetW7 = 1789444800, stale = true)
        assertEquals("agenzia · — · dato vecchio", QuotaText.h5Line("agenzia", none, labels))
        assertEquals(0f, QuotaText.fraction(none.h5)); assertEquals(0.75f, QuotaText.fraction(75))
    }
}

class RecapTextTest {
    private val s = ContractJson.decodeState(Fixtures.stateQuestion)
    @Test fun rows() {
        val r = RecapText.rows(s.recap)
        assertEquals("atlas-shop · Migrazioni 008-011 applicate, test verdi", r[0].done)
        assertEquals("→ Rivedere i seed e la pagina admin", r[0].next)
        assertEquals("2 · —", RecapText.night(s.night, "—"))
        assertEquals("0 · atlas-shop", RecapText.night(s.night.copy(queued = 0, running = "atlas-shop"), "—"))
    }
}
