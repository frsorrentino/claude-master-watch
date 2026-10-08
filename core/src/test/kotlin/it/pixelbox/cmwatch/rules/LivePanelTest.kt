package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import org.junit.Assert.assertEquals
import org.junit.Test

// La pillola della live aperta (B2, Franz 08/10 21:47): una riga per sessione, prima chi chiede, poi chi lavora.
class LivePanelTest {
    private val st = ContractJson.decodeState(Fixtures.stateQuestion)

    @Test fun oneRowPerOpenSessionAskingFirstThenWorking() {
        val rows = LivePanel.rows(st, 1_789_210_700L + 12 * 60)
        assertEquals(listOf("ledger-api", "atlas-shop", "field-notes"), rows.map { it.name })
        assertEquals(LivePanel.Kind.ASKING, rows[0].kind)
        assertEquals("Run the test suite", rows[1].what); assertEquals(12, rows[1].minutes)
    }
}
