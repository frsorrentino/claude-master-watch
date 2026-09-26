package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.data.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewStateTest {
    private val s = ContractJson.decodeState(Fixtures.stateQuestion)
    private val snap = Snapshot(s, Freshness.Fresh)

    @Test fun unpairedGoesToPairing() =
        assertEquals(Screen.Pairing, ViewState.reduce(snap, paired = false, chosen = Screen.Sessions, seen = emptySet()))
    // Demo senza PC (Franz, 26/09 18:09): l'orologio non accoppiato con la Demo accesa esce dal pairing.
    @Test fun unpairedDemoLeavesPairing() =
        assertEquals(Screen.Sessions, ViewState.reduce(snap, paired = false, chosen = Screen.Pairing, seen = setOf("q-1789210500-1"), demo = true))
    @Test fun unpairedDemoKeepsChosenScreen() =
        assertEquals(Screen.Settings, ViewState.reduce(snap, paired = false, chosen = Screen.Settings, seen = setOf("q-1789210500-1"), demo = true))
    @Test fun unpairedDemoStillOpensQuestion() =
        assertEquals(Screen.Question("ledger-api"), ViewState.reduce(snap, paired = false, chosen = Screen.Pairing, seen = emptySet(), demo = true))
    @Test fun openQuestionWins() =
        assertEquals(Screen.Question("ledger-api"), ViewState.reduce(snap, true, Screen.Sessions, emptySet()))
    @Test fun seenQuestionDoesNotReopen() =
        assertEquals(Screen.Sessions, ViewState.reduce(snap, true, Screen.Sessions, setOf("q-1789210500-1")))
    @Test fun answeredElsewhereClosesQuestion() {
        val idle = Snapshot(ContractJson.decodeState(Fixtures.stateIdle), Freshness.Fresh)
        assertEquals(Screen.Sessions, ViewState.reduce(idle, true, Screen.Question("ledger-api"), emptySet()))
    }
    @Test fun staleKeepsScreen() =
        assertEquals(Screen.Session("atlas-shop"), ViewState.reduce(snap.copy(freshness = Freshness.Stale(5)), true, Screen.Session("atlas-shop"), setOf("q-1789210500-1")))
    @Test fun noStateGoesToSessions() =
        assertEquals(Screen.Sessions, ViewState.reduce(Snapshot(null, Freshness.Stale(0)), true, Screen.Question("x"), emptySet()))
}
