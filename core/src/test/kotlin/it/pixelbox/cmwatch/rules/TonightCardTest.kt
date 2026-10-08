package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Test

// La scheda Stanotte (mockup approvato da Franz l'08/10 alle 20:43): un lavoro per riga con il progetto e cosa deve fare,
// quello in corso per primo con da quanto; i conteggi per il titolo.
class TonightCardTest {
    private val st = ContractJson.decodeState(Fixtures.stateQuestion)

    @Test fun theQueueInTheOrderItWasAdded() {
        val m = TonightCard.of(st.night, 1_789_220_000L)
        assertEquals(listOf("atlas-shop", "ledger-api"), m.items.map { it.name })
        assertEquals(0, m.running); assertEquals(2, m.queued)
        assertEquals(listOf(null, null), m.items.map { it.runningMin })
    }

    @Test fun theRunningOneComesFirstWithItsMinutes() {
        val items = st.night.items!!.let { l -> listOf(l[0], l[1].copy(started = 1_789_219_000L)) }
        val m = TonightCard.of(st.night.copy(items = items, queued = 1, running = items[1].id), 1_789_220_380L)
        assertEquals(listOf("ledger-api", "atlas-shop"), m.items.map { it.name })
        assertEquals(23, m.items[0].runningMin)
        assertEquals(1, m.running); assertEquals(1, m.queued)
    }

    @Test fun noItemsIsAnEmptyCard() = assertEquals(0, TonightCard.of(st.night.copy(items = emptyList(), queued = 0), 0).items.size)
}
