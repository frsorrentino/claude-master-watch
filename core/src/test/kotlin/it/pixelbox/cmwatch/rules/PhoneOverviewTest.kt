package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class PhoneOverviewTest {
    private val state = ContractJson.decodeState(Fixtures.stateQuestion)
    private val now = state.ts + 60
    private val zone = ZoneId.of("Europe/Rome")
    private fun build(samples: Map<String, List<QuotaHistory.Sample>> = emptyMap(), stale: Boolean = false) =
        PhoneOverview.build(state, emptyList(), samples, now, zone, stale)

    @Test fun ringsPersonalFirstWithWeek() {
        val rings = build().rings
        assertEquals(listOf("personal", "work"), rings.map { it.account })
        assertTrue(rings[0].personal)
        assertEquals(36, rings[0].w7)
    }

    @Test fun noSamplesNoPaceButRingStays() {
        val ring = build().rings.first()
        assertNull(ring.pace)
        assertEquals(11, ring.h5)
    }

    // Franz, 02/10 16:51 («dati di 4 min fa», senza ore): il reset resta, il ritmo no (si proietterebbe da dati vecchi).
    @Test fun staleKeepsResetTimeButNotThePace() {
        val samples = mapOf("personal" to listOf(QuotaHistory.Sample(now - 1_800, 5), QuotaHistory.Sample(now - 60, 11)))
        assertNotNull(build(samples).rings.first().pace)
        val ring = build(samples, stale = true).rings.first()
        assertNotNull(ring.resetAt)
        assertNull(ring.pace)
    }

    @Test fun nowCountsMatchTheBoard() {
        val n = build().now
        assertEquals(state.sessions.count { it.state != SessionState.GONE }, n.waiting + n.working + n.idle)
    }

    @Test fun contextRowsCarryModelAndEffort() {
        val row = build().contexts.first { it.name == "ledger-api" }
        assertEquals(62, row.pct)
        assertEquals("Opus 5", row.model)
        assertEquals("high", row.effort)
    }

    @Test fun updatedCarriesAgeAndHost() {
        val u = build(stale = true).updated
        assertEquals(1, u.minutes)
        assertEquals("crostini-demo", u.host)
        assertTrue(u.stale)
    }

    @Test fun weekPillCarriesItsReset() {
        assertEquals(1789610400L, build().rings.first().weekResetAt)
        assertEquals(1789610400L, build(stale = true).rings.first().weekResetAt)
    }

    @Test fun staleAccountIsFlagged() {
        val rings = build().rings
        assertFalse(rings.first { it.account == "personal" }.stale)
        assertTrue(rings.first { it.account == "work" }.stale)
    }
}
