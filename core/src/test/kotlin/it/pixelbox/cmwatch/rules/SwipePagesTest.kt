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

    // Dal vivo 03/10 16:40: scelta dal menu in alto, la sessione tornava subito quella di prima. La pagina vista quando il
    // contenuto si riattiva è quella vecchia: non è uno scorrimento e non deve cambiare la sessione aperta.
    @Test fun theFirstSettledPageNeverChangesTheOpenSession() =
        assertEquals(SwipePages.Move.Stay, SwipePages.afterSettle(listOf(null, "a", "b"), settled = 1, open = "b", initial = true))

    @Test fun aSwipeToAnotherSessionOpensIt() =
        assertEquals(SwipePages.Move.Open("b"), SwipePages.afterSettle(listOf(null, "a", "b"), settled = 2, open = "a", initial = false))

    @Test fun aSwipeToTheFirstPageGoesBackToTheSummary() =
        assertEquals(SwipePages.Move.Open(null), SwipePages.afterSettle(listOf(null, "a", "b"), settled = 0, open = "a", initial = false))

    @Test fun settlingOnTheOpenSessionChangesNothing() =
        assertEquals(SwipePages.Move.Stay, SwipePages.afterSettle(listOf(null, "a", "b"), settled = 2, open = "b", initial = false))

    // Segnalazione del 07/10 16:09: dopo un riordino il pager restava sulla pagina di una sessione mentre quella aperta era
    // un'altra, e la chat a schermo non si rileggeva più. `step` decide in un punto solo fra lo scorrimento del dito e il
    // riallineamento, così l'uno non annulla l'altro.
    private val pp = listOf(null, "team-supervisor-app", "motion-graphic-video")

    @Test fun aReorderLeavingThePagerOnAnotherSessionGoesBackToTheOpenOne() =
        assertEquals(SwipePages.Step.ScrollTo(1), SwipePages.step(prevSettled = 2, settled = 2, scrolling = false, pages = pp, open = "team-supervisor-app"))

    @Test fun aSwipeOpensTheSessionItStopsOn() =
        assertEquals(SwipePages.Step.Open("motion-graphic-video"), SwipePages.step(prevSettled = 1, settled = 2, scrolling = false, pages = pp, open = "team-supervisor-app"))

    @Test fun aSwipeBackToTheSummaryOpensIt() =
        assertEquals(SwipePages.Step.Open(null), SwipePages.step(prevSettled = 1, settled = 0, scrolling = false, pages = pp, open = "team-supervisor-app"))

    @Test fun theMenuChoiceMovesThePager() =
        assertEquals(SwipePages.Step.ScrollTo(2), SwipePages.step(prevSettled = 1, settled = 1, scrolling = false, pages = pp, open = "motion-graphic-video"))

    @Test fun whenTheContentComesBackTheOldPageDoesNotUndoTheChoice() =
        assertEquals(SwipePages.Step.ScrollTo(2), SwipePages.step(prevSettled = null, settled = 1, scrolling = false, pages = pp, open = "motion-graphic-video"))

    @Test fun whileTheFingerScrollsNothingMoves() =
        assertEquals(SwipePages.Step.None, SwipePages.step(prevSettled = 1, settled = 1, scrolling = true, pages = pp, open = "motion-graphic-video"))

    @Test fun alignedNothingToDo() =
        assertEquals(SwipePages.Step.None, SwipePages.step(prevSettled = 1, settled = 1, scrolling = false, pages = pp, open = "team-supervisor-app"))

    @Test fun anOpenSessionThatIsNoPageIsLeftAlone() =
        assertEquals(SwipePages.Step.None, SwipePages.step(prevSettled = 1, settled = 1, scrolling = false, pages = pp, open = "zeta"))

    // Franz, 09/10 15:46: al rilascio la pagina scattava. Aperta, la sessione «ha finito» diventa letta e cambia gruppo: con
    // la regia rifatta a ogni stato le pagine si riordinavano sotto il dito. Mentre si scorre l'ordine resta quello di prima.
    @Test fun theOrderStaysWhileSwipingAndNewSessionsGoAtTheEnd() {
        val prev = listOf(null, "a", "b", "c")
        assertEquals(listOf(null, "a", "b", "c"), SwipePages.stable(prev, listOf(null, "b", "a", "c")))
        assertEquals(listOf(null, "a", "c", "d"), SwipePages.stable(prev, listOf(null, "d", "c", "a")))
        assertEquals(listOf(null, "b", "a"), SwipePages.stable(emptyList(), listOf(null, "b", "a")))
    }
}
