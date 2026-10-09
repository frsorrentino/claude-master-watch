package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.RecapItem
import org.junit.Assert.assertEquals
import org.junit.Test

// La sezione Recap (approvata l'08/10 alle 21:08): le azioni delle sessioni vive e del recap, con a chi vanno.
class RecapActionsTest {
    private val st = ContractJson.decodeState(Fixtures.stateQuestion)

    @Test fun sessionActionsGoToTheSessionAndRecapOnesToTheLiveProjectOrTheMaster() {
        val withGone = st.copy(recap = st.recap.copy(items = st.recap.items + RecapItem("orbit-docs", "Pricing page drafted", "Publish the pricing page")))
        val a = RecapActions.of(withGone)
        // atlas-shop: le sue azioni; il recap di atlas-shop va a lei; orbit-docs è chiusa: alla master.
        val orbit = a.single { it.from == "orbit-docs" }
        assertEquals("master", orbit.to); assertEquals("Riprendi orbit-docs: Publish the pricing page", orbit.send)
        assertEquals("atlas-shop", a.first { it.text == "Review the seeds and the admin page" }.to)
        assertEquals(a.size, a.map { NextSteps.key(it.text) }.distinct().size)
        // Il tasto dice da dove viene: le azioni del recap lo sanno, quelle delle sessioni no.
        assertEquals(true, orbit.recap); assertEquals(true, a.first { it.text == "Review the seeds and the admin page" }.recap)
        assertEquals(false, a.first { it.text == "ok to deploy on staging" }.recap)
    }

    // Franz, 09/10 17:21 («queste azioni senza contesto non sono utili», scelta A): nel Recap restano solo i «prossimo» del
    // recap del giorno; i «Prossimi:» delle sessioni stanno sulla loro scheda, accanto all'esito che li spiega.
    @Test fun withoutSessionsOnlyTheRecapNextSteps() {
        val a = RecapActions.of(st, withSessions = false)
        assertEquals(true, a.isNotEmpty())
        assertEquals(true, a.all { it.recap })
        assertEquals(null, a.firstOrNull { it.text == "ok to deploy on staging" })
    }
}
