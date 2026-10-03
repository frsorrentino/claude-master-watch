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
    // Franz, 03/10 16:30: la master non è una pagina come le altre; aprirla porta la home sulla sua chat.
    @Test fun theMasterOpensAtHome() {
        assertTrue(StartRoute.masterAtHome("master"))
        assertFalse(StartRoute.masterAtHome("atlas-shop"))
        assertFalse(StartRoute.masterAtHome(null))
    }

    // Dal vivo 03/10 19:57: chiusa una sessione, la sua pagina restava nera. Si torna alla home se la sessione aperta non
    // c'è più, o se è chiusa dopo un «Chiudi la sessione» dato dal telefono; una chiusa per conto suo resta, con «Riapri».
    @Test fun aClosedOrVanishedSessionGoesBackHome() {
        fun s(n: String, st: it.pixelbox.cmwatch.contract.SessionState) = it.pixelbox.cmwatch.contract.Session(id = n, name = n, account = "personale", project = n, state = st, since = 0)
        val live = listOf(s("a", it.pixelbox.cmwatch.contract.SessionState.IDLE), s("x", it.pixelbox.cmwatch.contract.SessionState.GONE))
        assertTrue(StartRoute.stillOpen("a", live, leaving = null))
        assertFalse(StartRoute.stillOpen("b", live, leaving = null))
        assertTrue(StartRoute.stillOpen("x", live, leaving = null))
        assertFalse(StartRoute.stillOpen("x", live, leaving = "x"))
    }
}
