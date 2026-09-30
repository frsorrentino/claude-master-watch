package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartRouteTest {
    @Test fun freshLaunchOpensOverview() = assertEquals(StartRoute.Tab.OVERVIEW, StartRoute.tab(restored = null))
    @Test fun rotationKeepsTheTab() = assertEquals(StartRoute.Tab.DIARY, StartRoute.tab(restored = StartRoute.Tab.DIARY))
    @Test fun freshLaunchClosesAnyOpenSheet() = assertNull(StartRoute.openSheet(freshLaunch = true, restored = "kb"))
    @Test fun rotationKeepsTheOpenSheet() = assertEquals("kb", StartRoute.openSheet(freshLaunch = false, restored = "kb"))
}
