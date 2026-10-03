package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.rules.Summary.Group
import org.junit.Assert.assertEquals
import org.junit.Test

/** Il menu delle sessioni a pannello (Franz, 03/10 16:44, variante A): i gruppi del riepilogo, senza master e chiuse. */
class SessionsMenuTest {
    private fun s(name: String, st: SessionState = SessionState.IDLE, q: Boolean = false) = Session(
        id = name, name = name, account = "personale", project = name, state = st, since = 0,
        question = if (q) Question("1", QuestionKind.ASK, "?", emptyList(), Tier.LOW, 0) else null,
    )

    @Test fun groupsInOrderOfNeedWithoutTheMasterAndTheClosed() {
        val m = SessionsMenu.of(listOf(s("idle"), s("busy", SessionState.BUSY), s("asks", SessionState.WAITING, q = true), s("master"), s("x", SessionState.GONE)))
        assertEquals(listOf(Group.WAITING, Group.WORKING, Group.STILL), m.groups.map { it.first })
        assertEquals(listOf("asks", "busy", "idle"), m.groups.flatMap { g -> g.second.map { it.name } })
    }

    @Test fun closedAreOnlyCounted() = assertEquals(2, SessionsMenu.of(listOf(s("a"), s("x", SessionState.GONE), s("y", SessionState.GONE))).closed)

    @Test fun aBusySessionWithAQuestionWaits() =
        assertEquals(listOf(Group.WAITING), SessionsMenu.of(listOf(s("b", SessionState.BUSY, q = true))).groups.map { it.first })

    @Test fun theOrderInsideAGroupIsKept() =
        assertEquals(listOf("b", "a"), SessionsMenu.of(listOf(s("b"), s("a"))).groups.single().second.map { it.name })
}
