package it.pixelbox.cmwatch.contract

import it.pixelbox.cmwatch.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderTest {
    @Test fun shuffledSessionsComeBackInContractOrder() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        val shuffled = s.sessions.reversed()
        assertEquals(listOf("ledger-api", "atlas-shop", "field-notes", "orbit-docs"), Order.sessions(shuffled).map { it.name })
    }

    @Test fun sameStateSortsByName() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        val a = s.sessions[2].copy(name = "zeta"); val b = s.sessions[2].copy(name = "alpha")
        assertEquals(listOf("alpha", "zeta"), Order.sessions(listOf(a, b)).map { it.name })
    }

    @Test fun awaitingRanksWithBusy() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        val awaiting = s.sessions[2].copy(name = "aaa-await", state = SessionState.AWAITING)
        assertEquals(listOf("ledger-api", "aaa-await", "atlas-shop", "field-notes", "orbit-docs"), Order.sessions(s.sessions + awaiting).map { it.name })
    }

    @Test fun freshnessThreeMinutes() {
        assertEquals(Freshness.Fresh, Freshness.of(1000, 1000 + 179))
        assertEquals(Freshness.Stale(3), Freshness.of(1000, 1000 + 180))
        assertEquals(Freshness.Stale(65), Freshness.of(1000, 1000 + 65 * 60 + 5))
    }

    @Test fun durations() {
        assertEquals("0 m", Durations.since(100, 130))
        assertEquals("2 m", Durations.since(100, 100 + 150))
        assertEquals("1 h 05", Durations.since(0, 65 * 60))
        assertEquals("3 g", Durations.since(0, 3 * 86400 + 100))
    }
}
