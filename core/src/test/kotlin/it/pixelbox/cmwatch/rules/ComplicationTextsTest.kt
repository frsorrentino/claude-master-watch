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

    @Test fun ranged() {
        val r = ComplicationTexts.ranged(q, "personal")
        assertEquals(11f, r.value); assertEquals(100f, r.max); assertEquals("11 %", r.text)
        val none = ComplicationTexts.ranged(q, "work")
        assertEquals(0f, none.value); assertEquals("—", none.text)
    }

    @Test fun tapTargets() {
        assertEquals("cmwatch://question/ledger-api", ComplicationTexts.tapTarget(q))
        assertEquals("cmwatch://sessions", ComplicationTexts.tapTarget(idle))
    }
}
