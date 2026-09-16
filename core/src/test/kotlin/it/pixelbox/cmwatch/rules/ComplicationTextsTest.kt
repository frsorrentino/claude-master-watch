package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.assertEquals
import org.junit.Test

class ComplicationTextsTest {
    private val q = ContractJson.decodeState(Fixtures.stateQuestion)
    private val idle = ContractJson.decodeState(Fixtures.stateIdle)
    private val stale = ContractJson.decodeState(Fixtures.stateStale)

    @Test fun shortText() {
        assertEquals("1?", ComplicationTexts.short(q, fresh = true))
        assertEquals("✓", ComplicationTexts.short(idle, fresh = true))
        assertEquals("▶1", ComplicationTexts.short(idle.copy(sessions = listOf(idle.sessions[0].copy(state = SessionState.BUSY))), fresh = true))
        assertEquals("PC", ComplicationTexts.short(stale, fresh = false))
        assertEquals("—", ComplicationTexts.short(null, fresh = false))
    }

    @Test fun longText() {
        assertEquals("❓ ledger-api · Deploy now?", ComplicationTexts.long(q, fresh = true, staleLabel = "PC fermo"))
        assertEquals("▶ 0 · ✓ 1", ComplicationTexts.long(idle, fresh = true, staleLabel = "PC fermo"))
        assertEquals("PC fermo", ComplicationTexts.long(stale, fresh = false, staleLabel = "PC fermo"))
    }

    @Test fun gistKeepsWholeShortTextElseLastQuestion() {
        assertEquals("Deploy now?", ComplicationTexts.gist("Deploy ready, waiting for the client's ok. Deploy now?", 30))
        assertEquals("Ok?", ComplicationTexts.gist("Ok?", 30))
        assertEquals("Una frase molto lunga senza", ComplicationTexts.gist("Una frase molto lunga senza domanda alla fine e senza punti", 30))
    }

    // Senza «%» sull'anello (Franz, 16/09 13:44): la batteria accanto mostra «51», e l'arco dice già che è una parte di 100.
    @Test fun ranged() {
        val r = ComplicationTexts.ranged(q, "personal")
        assertEquals(11f, r.value); assertEquals(100f, r.max); assertEquals("11", r.text); assertEquals("5h", r.title); assertEquals(false, r.week)
        // `work` nella fixture ha solo la settimana: l'anello la mostra con la sua sigla.
        val w = ComplicationTexts.ranged(q, "work")
        assertEquals("75", w.text); assertEquals("7d", w.title)
        val none = ComplicationTexts.ranged(q, "nessuno")
        assertEquals(0f, none.value); assertEquals("—", none.text); assertEquals(null, none.week); assertEquals(null, none.title)
    }

    private fun twoLines(r: ComplicationTexts.Ranged) = "${r.text}/${r.title}"

    // Franz, 15/09 16:44: di notte e nel weekend le 5 ore stanno a zero, l'anello passa alla settimana con la sua sigla.
    private fun withQuota(h5: Int?, w7: Int?) =
        q.copy(quota = q.quota.mapValues { (k, v) -> if (k == "personal") v.copy(h5 = h5, w7 = w7) else v })

    @Test fun rangedAZeroPassaAllaSettimana() {
        val r = ComplicationTexts.ranged(withQuota(0, 34), "personal")
        assertEquals(34f, r.value); assertEquals("34/7d", twoLines(r)); assertEquals(true, r.week)
    }

    @Test fun rangedSenzaCinqueOrePassaAllaSettimana() =
        assertEquals("34/7d", twoLines(ComplicationTexts.ranged(withQuota(null, 34), "personal")))

    @Test fun rangedAZeroSenzaSettimanaRestaSulleCinqueOre() =
        assertEquals("0/5h", twoLines(ComplicationTexts.ranged(withQuota(0, null), "personal")))

    @Test fun rangedSenzaLettureMostraIlTrattino() =
        assertEquals("—", ComplicationTexts.ranged(withQuota(null, null), "personal").text)

    @Test fun tapTargets() {
        assertEquals("cmwatch://question/ledger-api", ComplicationTexts.tapTarget(q))
        assertEquals("cmwatch://sessions", ComplicationTexts.tapTarget(idle))
    }
}
