package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.State
import org.junit.Assert.*
import org.junit.Test

/** La coda «Ti aspettano» (piano 30/09, Task 1): tutte le domande aperte in fila, dalla più vecchia. */
class AttentionQueueTest {
    private val base = ContractJson.decodeState(Fixtures.stateQuestion)
    private val q = base.sessions.first { it.question != null }

    /** Tre sessioni con una domanda ciascuna, chieste a 300, 100 e 200. */
    private fun three(): State = base.copy(sessions = listOf(
        q.copy(id = "a", name = "a", question = q.question!!.copy(id = "qa", askedAt = 300)),
        q.copy(id = "b", name = "b", question = q.question!!.copy(id = "qb", askedAt = 100)),
        q.copy(id = "c", name = "c", question = q.question!!.copy(id = "qc", askedAt = 200)),
        q.copy(id = "d", name = "d", question = null),
    ))

    @Test fun oldestFirst() {
        assertEquals(listOf("qb", "qc", "qa"), AttentionQueue.items(three()).map { it.questionId })
        assertEquals("b", AttentionQueue.items(three()).first().session)
    }

    @Test fun nextAfterCurrent() {
        assertEquals("qc", AttentionQueue.next(three(), "qb")?.questionId)
        assertEquals("qb", AttentionQueue.next(three(), null)?.questionId)
    }

    /** L'ultima della fila risposta: si ricomincia dalla più vecchia rimasta, mai null se ce n'è ancora una. */
    @Test fun afterTheLastComesTheOldest() {
        assertEquals("qb", AttentionQueue.next(three(), "qa")?.questionId)
    }

    /** Risposta al PC mentre la coda è aperta: la domanda corrente sparisce, `next` dà la più vecchia rimasta. */
    @Test fun answeredElsewhereLeavesTheQueue() {
        val s = three().let { st -> st.copy(sessions = st.sessions.map { if (it.name == "c") it.copy(question = null) else it }) }
        assertEquals("qb", AttentionQueue.next(s, "qc")?.questionId)
        assertFalse(AttentionQueue.items(s).any { it.questionId == "qc" })
    }

    @Test fun emptyWhenNoQuestions() {
        val s = base.copy(sessions = base.sessions.map { it.copy(question = null) })
        assertTrue(AttentionQueue.items(s).isEmpty())
        assertNull(AttentionQueue.next(s, null))
    }
}
