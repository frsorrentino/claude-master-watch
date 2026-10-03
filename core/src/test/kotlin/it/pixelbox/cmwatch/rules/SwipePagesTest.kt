package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import org.junit.Assert.assertEquals
import org.junit.Test

/** Lo scorrimento laterale (Franz, 03/10 15:59): il riepilogo è sempre la prima pagina, poi le sessioni. */
class SwipePagesTest {
    private fun s(name: String, st: SessionState = SessionState.IDLE) = Session(id = name, name = name, account = "personale", project = name, state = st, since = 0)
    private fun st(vararg ss: Session) = State(v = 1, ts = 0, host = "pc", sessions = ss.toList())

    @Test fun theSummaryIsAlwaysTheFirstPage() {
        val pages = SwipePages.of(st(s("a"), s("b")), open = null)
        assertEquals(null, pages.first())
        assertEquals(setOf("a", "b"), pages.drop(1).toSet())
    }

    @Test fun withoutAStateOnlyTheSummary() = assertEquals(listOf<String?>(null), SwipePages.of(null, open = null))

    @Test fun theMasterIsAPageOnlyWhenOpen() {
        assertEquals(listOf(null, "a"), SwipePages.of(st(s("master"), s("a")), open = null))
        assertEquals(listOf(null, "a", "master"), SwipePages.of(st(s("master"), s("a")), open = "master"))
    }

    @Test fun aClosedSessionIsAPageOnlyWhenOpen() {
        assertEquals(listOf(null, "a"), SwipePages.of(st(s("a"), s("x", SessionState.GONE)), open = null))
        assertEquals(listOf(null, "a", "x"), SwipePages.of(st(s("a"), s("x", SessionState.GONE)), open = "x"))
    }
}
