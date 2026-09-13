package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.SessionState

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionsTextTest {
    private val s = ContractJson.decodeState(Fixtures.stateQuestion)
    private val now = 1789210800L

    @Test fun waitingShowsAgeOfTheQuestion() = assertEquals("ledger-api · 5 m", SessionsText.row(s.sessions[0], now))
    @Test fun busyShowsTurnAge() = assertEquals("atlas-shop · 1 m", SessionsText.row(s.sessions[1], now))
    @Test fun idleShowsSinceAge() = assertEquals("field-notes · 1 g", SessionsText.row(s.sessions[2], now))
    @Test fun goneShowsNoAge() = assertEquals("orbit-docs", SessionsText.row(s.sessions[3], now))
    @Test fun headerCounts() = assertEquals("4 sessioni · 1 ❓ · 1 ✗", SessionsText.header(s.sessions, "sessioni"))
}

class SessionsSubTest {
    private val s = ContractJson.decodeState(Fixtures.stateQuestion)

    @Test fun laSessioneFinitaDiceCheEChiusaEDaQuando() {
        val gone = s.sessions.first { it.state == SessionState.GONE }
        assertEquals("chiusa · ${Durations.since(gone.since, gone.since + 7200)}", SessionsText.sub(gone, gone.since + 7200, "chiusa"))
    }

    @Test fun leAltreDannoSoloLEta() {
        val waiting = s.sessions.first { it.state == SessionState.WAITING }
        assertEquals(Durations.since(waiting.question!!.askedAt, waiting.since + 600), SessionsText.sub(waiting, waiting.since + 600, "chiusa"))
    }
}
