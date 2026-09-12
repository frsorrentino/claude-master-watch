package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
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
