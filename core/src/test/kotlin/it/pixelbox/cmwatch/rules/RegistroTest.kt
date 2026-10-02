package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.EventKind
import org.junit.Assert.*
import org.junit.Test

/** Il Registro (mockup approvato da Franz, 02/10 09:10): il resoconto della notte e i diari, una riga per lavoro o progetto. */
class RegistroTest {
    private val events = ContractJson.decodeEvents(Fixtures.events)

    @Test fun nightReportBecomesOneRowPerJobWithoutPaths() {
        val r = Registro.night(events.first { it.kind == EventKind.NIGHT_REPORT }.body)
        assertEquals(2, r.size)
        assertEquals(Registro.Job(true, "atlas-shop", 812, "Fixed the three flaky tests, two were real."), r[0])
        assertEquals(Registro.Job(false, "ledger-api", 3600, "timed out (night.item_timeout_s = 3600)"), r[1])
    }

    @Test fun unknownNightReportGivesNoRows() {
        assertTrue(Registro.night("Notte tranquilla, niente da dire").isEmpty())
    }

    @Test fun recapBodyBecomesOneRowPerProject() {
        val rows = Registro.recap(events.first { it.kind == EventKind.RECAP }.body)
        assertEquals(listOf("ledger-api", "atlas-shop", "field-notes"), rows.map { it.project })
        assertEquals("Migrations 008-011 applied, tests green", rows[1].text)
    }

    @Test fun minutesRoundUp() {
        assertEquals(14, Registro.minutes(812))
        assertEquals(60, Registro.minutes(3600))
    }
}
