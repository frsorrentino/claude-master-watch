package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartRouteTest {
    @Test fun freshLaunchOpensOverview() = assertEquals(StartRoute.Tab.OVERVIEW, StartRoute.tab(restored = null))
    @Test fun rotationKeepsTheTab() = assertEquals(StartRoute.Tab.DIARY, StartRoute.tab(restored = StartRoute.Tab.DIARY))
    @Test fun freshLaunchClosesAnyOpenSheet() = assertNull(StartRoute.openSheet(freshLaunch = true, restored = "kb"))
    @Test fun rotationKeepsTheOpenSheet() = assertEquals("kb", StartRoute.openSheet(freshLaunch = false, restored = "kb"))

    @Test fun shortTripAwayKeepsTheSheet() = assertFalse(StartRoute.resetOnReturn(awayMs = StartRoute.AWAY_RESET_MS - 1))
    @Test fun longAbsenceReopensOnOverview() = assertTrue(StartRoute.resetOnReturn(awayMs = StartRoute.AWAY_RESET_MS))
    // Design 03/10: il riepilogo unico prende il posto di Master e Sessioni; resta il Registro, aperto dal menu.
    @Test fun onlyTheSummaryAndTheRegister() = assertEquals(listOf(StartRoute.Tab.OVERVIEW, StartRoute.Tab.DIARY), StartRoute.Tab.entries.toList())
}
