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

    // Segnalazione 01/10 20:12: lo scorrimento laterale cambia scheda, nell'ordine della barra in basso.
    @Test fun swipeMovesBetweenTheThreeTabs() {
        assertEquals(StartRoute.Tab.SESSIONS, StartRoute.swipe(StartRoute.Tab.OVERVIEW, toNext = true))
        assertEquals(StartRoute.Tab.DIARY, StartRoute.swipe(StartRoute.Tab.SESSIONS, toNext = true))
        assertEquals(StartRoute.Tab.SESSIONS, StartRoute.swipe(StartRoute.Tab.DIARY, toNext = false))
        assertEquals(StartRoute.Tab.DIARY, StartRoute.swipe(StartRoute.Tab.DIARY, toNext = true))
        assertEquals(StartRoute.Tab.OVERVIEW, StartRoute.swipe(StartRoute.Tab.OVERVIEW, toNext = false))
    }
}
