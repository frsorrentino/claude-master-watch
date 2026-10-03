package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.rules.Summary.Group
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Il riepilogo unico (design 03/10): ogni sessione una volta, nell'ordine del bisogno. */
class SummaryTest {
    private val zone = ZoneId.of("Europe/Rome")
    private fun at(h: Int, m: Int = 0) = LocalDate.of(2026, 10, 3).atTime(h, m).atZone(zone).toEpochSecond()
    private fun s(name: String, st: SessionState = SessionState.IDLE, since: Long = at(9)) =
        Session(id = name, name = name, account = "personale", project = name, state = st, since = since)
    private fun q(id: String, asked: Long) = Question(id, QuestionKind.ASK, "Pubblico?", emptyList(), Tier.LOW, asked)
    private fun done(at: Long) = Outcome("Fatto", "Test verdi.\nProssimi: tagga · apri la PR", at)
    private fun st(vararg ss: Session) = State(v = 1, ts = at(15), host = "pc", sessions = ss.toList())
    private fun build(state: State, sent: List<Sent> = emptyList()) = Summary.build(state, emptyList(), sent, at(15), zone, emptySet())

    @Test fun groupsInOrderOfNeed() {
        val m = build(st(
            s("idle"), s("busy", SessionState.BUSY),
            s("asks", SessionState.WAITING).copy(question = q("1", at(14))),
            s("fin").copy(outcome = done(at(14, 50)), followed = true),
        ))
        assertEquals(listOf(Group.WAITING, Group.FINISHED, Group.WORKING, Group.STILL), m.rows.map { it.group })
        assertEquals(listOf("asks", "fin", "busy", "idle"), m.rows.map { it.session.name })
    }

    // Franz, 03/10 20:31: sulla card la quota delle 5 ore dell'account al posto del «da N m».
    @Test fun eachRowCarriesItsAccountQuota() {
        val work = s("cli", SessionState.BUSY).copy(account = "professionale")
        val quota = mapOf("personale" to QuotaAccount(h5 = 4), "professionale" to QuotaAccount(h5 = 62, stale = true))
        val m = build(st(s("idle"), work).copy(quota = quota))
        assertEquals(listOf(62, 4), m.rows.map { it.quota?.h5 })
        assertEquals(listOf(true, false), m.rows.map { it.quota?.stale })
        assertNull(build(st(s("idle"))).rows.single().quota)
    }

    @Test fun aFinishedSessionIsNotAlsoStill() {
        val m = build(st(s("fin").copy(outcome = done(at(14, 50)), followed = true)))
        assertEquals(listOf(Group.FINISHED), m.rows.map { it.group })
    }

    @Test fun theMasterOnlyAppearsWhenItAsks() {
        assertTrue(build(st(s("master", SessionState.BUSY))).rows.isEmpty())
        assertEquals(listOf("master"), build(st(s("master", SessionState.WAITING).copy(question = q("1", at(14))))).rows.map { it.session.name })
    }

    @Test fun closedAreOneRow() {
        val m = build(st(s("a"), s("x", SessionState.GONE), s("y", SessionState.GONE)))
        assertEquals(listOf("x", "y"), m.closed.map { it.name })
        assertTrue(m.rows.none { it.session.state == SessionState.GONE })
    }

    @Test fun openCountsLiveSessionsWithoutTheMaster() = assertEquals(2, build(st(s("a"), s("b"), s("master"), s("x", SessionState.GONE))).open)

    @Test fun stillMostRecentFirst() =
        assertEquals(listOf("new", "old"), build(st(s("old", since = at(8)), s("new", since = at(12)))).rows.map { it.session.name })

    @Test fun noMasterStillBuilds() {
        val m = build(st(s("a")))
        assertNull(m.master); assertEquals(1, m.rows.size)
    }

    @Test fun serviceRowsAreTheOtherKinds() {
        val state = st(s("a")).copy(night = Night(items = emptyList()))
        val m = Summary.build(state, emptyList(), emptyList(), at(21), zone, emptySet())
        assertEquals(listOf(MasterHome.Kind.NIGHT), m.service.map { it.kind })
    }

    // Revisione finale: uno stato di passaggio del relay non nasconde e non raddoppia una sessione.
    @Test fun waitingWithoutQuestionIsStillListed() =
        assertEquals(listOf("w"), build(st(s("w", SessionState.WAITING))).rows.map { it.session.name })

    @Test fun busyWithAQuestionAppearsOnce() {
        val m = build(st(s("b", SessionState.BUSY).copy(question = q("1", at(14)))))
        assertEquals(listOf(Group.WAITING), m.rows.map { it.group })
    }
}
