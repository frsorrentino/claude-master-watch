package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import org.junit.Assert.*
import org.junit.Test

/** L'avviso delle altre sessioni nella chat (Franz, 02/10 20:47, variante A): chi ti aspetta, poi chi ha finito. */
class ElsewhereTest {
    private val now = 1_000_000L
    private fun s(name: String, st: SessionState, question: Question? = null, outcome: Outcome? = null, followed: Boolean = false) =
        Session(id = name, name = name, account = "personal", project = "p", state = st, since = 0, question = question, outcome = outcome, followed = followed)
    private fun q(id: String, asked: Long) = Question(id, QuestionKind.ASK, "Pubblico?", emptyList(), Tier.LOW, asked)
    private fun done(at: Long) = Outcome("Fatto", "Fatto tutto.", at)
    private fun state(vararg ss: Session) = State(v = 1, ts = now, host = "pc", sessions = ss.toList())

    @Test fun aQuestionElsewhereComesFirst() {
        val st = state(s("here", SessionState.IDLE), s("ledger", SessionState.WAITING, question = q("q1", now - 60)), s("atlas", SessionState.IDLE, outcome = done(now - 30), followed = true))
        assertEquals(Elsewhere.Waiting(listOf("ledger")), Elsewhere.alert(st, current = "here", now = now, mine = emptySet(), seen = emptySet()))
    }

    @Test fun theOpenSessionNeverAlertsItself() {
        val st = state(s("ledger", SessionState.WAITING, question = q("q1", now - 60)))
        assertNull(Elsewhere.alert(st, current = "ledger", now = now, mine = emptySet(), seen = emptySet()))
    }

    @Test fun severalWaitingOldestFirst() {
        val st = state(s("b", SessionState.WAITING, question = q("q2", now - 10)), s("a", SessionState.WAITING, question = q("q1", now - 300)))
        assertEquals(Elsewhere.Waiting(listOf("a", "b")), Elsewhere.alert(st, current = "here", now = now, mine = emptySet(), seen = emptySet()))
    }

    // Franz, 02/10 20:47: «ha finito» solo per chi segui o per chi hai scritto dal telefono, se no con 5-6 sessioni non è discreto.
    @Test fun aFinishedTurnOnlyFromFollowedOrWrittenSessions() {
        val other = s("atlas", SessionState.IDLE, outcome = done(now - 30))
        assertNull(Elsewhere.alert(state(other), current = "here", now = now, mine = emptySet(), seen = emptySet()))
        assertEquals(Elsewhere.Finished("atlas", now - 30), Elsewhere.alert(state(other.copy(followed = true)), current = "here", now = now, mine = emptySet(), seen = emptySet()))
        assertEquals(Elsewhere.Finished("atlas", now - 30), Elsewhere.alert(state(other), current = "here", now = now, mine = setOf("atlas"), seen = emptySet()))
    }

    @Test fun aFinishedTurnShowsOnce() {
        val st = state(s("atlas", SessionState.IDLE, outcome = done(now - 30), followed = true))
        val first = Elsewhere.alert(st, current = "here", now = now, mine = emptySet(), seen = emptySet())!!
        assertNull(Elsewhere.alert(st, current = "here", now = now, mine = emptySet(), seen = setOf(first.key)))
    }

    @Test fun anOldOutcomeOrABusySessionIsNotNews() {
        val old = s("atlas", SessionState.IDLE, outcome = done(now - Elsewhere.FRESH_S - 1), followed = true)
        assertNull(Elsewhere.alert(state(old), current = "here", now = now, mine = emptySet(), seen = emptySet()))
        val busy = s("atlas", SessionState.BUSY, outcome = done(now - 30), followed = true)
        assertNull(Elsewhere.alert(state(busy), current = "here", now = now, mine = emptySet(), seen = emptySet()))
    }

    @Test fun theNewestFinishedTurnFirst() {
        val st = state(s("a", SessionState.IDLE, outcome = done(now - 200), followed = true), s("b", SessionState.IDLE, outcome = done(now - 20), followed = true))
        assertEquals("b", (Elsewhere.alert(st, current = "here", now = now, mine = emptySet(), seen = emptySet()) as Elsewhere.Finished).session)
    }
}
