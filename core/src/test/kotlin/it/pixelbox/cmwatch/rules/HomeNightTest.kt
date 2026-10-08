package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.NightReportRef
import org.junit.Assert.*
import org.junit.Test

/** Il riquadro Notte in testa alla home (mockup approvato da Franz l'08/10 alle 12:57). */
class HomeNightTest {
    @Test fun theStateSaysWhichReportIsTheLatest() {
        val raw = """{"v":1,"ts":1,"host":"pc","night":{"queued":0,"items":[],"report":{"date":"2026-10-07","generated_at":1791346889}}}"""
        assertEquals(NightReportRef("2026-10-07", 1791346889), ContractJson.decodeState(raw).night.report)
        assertNull(ContractJson.decodeState("""{"v":1,"ts":1,"host":"pc"}""").night.report)
    }

    @Test fun shownUntilThatNightIsOpened() {
        val ref = NightReportRef("2026-10-07", 1791346889)
        assertTrue(HomeNight.show(ref, opened = null))
        assertTrue(HomeNight.show(ref, opened = "2026-10-06"))
        assertFalse(HomeNight.show(ref, opened = "2026-10-07"))
        assertFalse(HomeNight.show(null, opened = null))
    }
}
