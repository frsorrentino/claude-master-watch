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

    @Test fun staleHidesResetTime() {
        assertNotNull(build().rings.first().resetAt)
        assertTrue(build(stale = true).rings.all { it.resetAt == null })
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
}
