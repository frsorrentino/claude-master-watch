package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.rules.MasterHome.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** «Per te» nella casa della master (design 01/10, approvato 23:02): le righe, l'ordine, il taglio a tre. */
class MasterHomeTest {
    private val zone = ZoneId.of("Europe/Rome")
    private fun at(h: Int, m: Int = 0) = LocalDate.of(2026, 10, 2).atTime(h, m).atZone(zone).toEpochSecond()
    private fun s(name: String, state: SessionState = SessionState.IDLE, ctx: Int? = 10, q: Question? = null, project: String = name) =
        Session(id = name, name = name, account = "personale", project = project, state = state, since = 0, context = ctx, question = q)
    private fun q(id: String, asked: Long) = Question(id, QuestionKind.ASK, "Pubblico?", emptyList(), Tier.LOW, asked)
    private fun st(vararg sessions: Session, recap: Recap = Recap(), night: Night = Night(), projects: List<Project> = emptyList()) =
        State(v = 1, ts = 0, host = "pc", sessions = sessions.toList(), recap = recap, night = night, projects = projects)
    private fun kinds(f: MasterHome.ForYou) = f.rows.map { it.kind }

    @Test fun nothingToDoMeansNoRows() {
        val f = MasterHome.forYou(st(s("kb")), emptyList(), emptyList(), at(15), zone)
        assertTrue(f.rows.isEmpty()); assertEquals(0, f.more)
    }

    @Test fun questionsOldestFirstThenContext() {
        val f = MasterHome.forYou(st(s("a", SessionState.WAITING, q = q("2", at(14))), s("b", SessionState.WAITING, q = q("1", at(13))), s("master", ctx = 83)),
            emptyList(), emptyList(), at(15), zone)
        assertEquals(listOf(Kind.QUESTION, Kind.QUESTION, Kind.CONTEXT), kinds(f))
        assertEquals(listOf("b", "a", "master"), f.rows.map { it.session })
        assertEquals(83, f.rows[2].number)
    }

    @Test fun cutAtThreeWithMore() {
        val f = MasterHome.forYou(st(s("a", ctx = 81), s("b", ctx = 82), s("c", ctx = 83), s("d", ctx = 84)), emptyList(), emptyList(), at(15), zone)
        assertEquals(3, f.rows.size); assertEquals(1, f.more)
        assertEquals(listOf("d", "c", "b"), f.rows.map { it.session })
        // Aprendo «+N» si vedono tutte.
        assertEquals(4, MasterHome.forYou(st(s("a", ctx = 81), s("b", ctx = 82), s("c", ctx = 83), s("d", ctx = 84)), emptyList(), emptyList(), at(15), zone, limit = Int.MAX_VALUE).rows.size)
    }

    private val report = Event("nr-1", EventKind.NIGHT_REPORT, ts = at(6, 10), title = "Stanotte", body = "3 lavori fatti")

    @Test fun nightReportOnlyInTheMorningUntilRead() {
        assertEquals(listOf(Kind.NIGHT_REPORT), kinds(MasterHome.forYou(st(s("kb")), listOf(report), emptyList(), at(7, 40), zone)))
        assertTrue(MasterHome.forYou(st(s("kb")), listOf(report), emptyList(), at(12, 30), zone).rows.isEmpty())
        assertTrue(MasterHome.forYou(st(s("kb")), listOf(report), emptyList(), at(7, 40), zone, read = setOf("nr-1")).rows.isEmpty())
    }

    @Test fun nightOnlyFromEightPmWithTheNightOn() {
        val on = Night(queued = 0, items = emptyList())
        assertTrue(MasterHome.forYou(st(s("kb"), night = on), emptyList(), emptyList(), at(19, 59), zone).rows.isEmpty())
        val f = MasterHome.forYou(st(s("kb"), night = on), emptyList(), emptyList(), at(20, 0), zone)
        assertEquals(listOf(Kind.NIGHT), kinds(f)); assertEquals(0, f.rows[0].number)
        assertTrue(MasterHome.forYou(st(s("kb"), night = Night(items = null)), emptyList(), emptyList(), at(21), zone).rows.isEmpty())
    }

    @Test fun nextStepOnlyForIdleOrClosedProjects() {
        val recap = Recap("2026-10-02", listOf(RecapItem("kb", "nota scritta", "distillare la nota"), RecapItem("atlas", "test", "pubblicare")))
        val projects = listOf(Project("/w/kb", "kb", "personale"), Project("/w/atlas", "atlas", "personale"))
        val f = MasterHome.forYou(st(s("kb"), s("atlas", SessionState.BUSY), recap = recap, projects = projects), emptyList(), emptyList(), at(15), zone)
        assertEquals(listOf(Kind.NEXT_STEP), kinds(f))
        assertEquals("kb", f.rows[0].title); assertEquals("distillare la nota", f.rows[0].detail); assertEquals("kb", f.rows[0].session)
        val closed = MasterHome.forYou(st(recap = recap, projects = projects), emptyList(), emptyList(), at(15), zone)
        assertEquals(listOf(null, null), closed.rows.map { it.session }); assertEquals("/w/kb", closed.rows[0].project)
    }

    @Test fun oldRecapIsIgnored() {
        val recap = Recap("2026-09-29", listOf(RecapItem("kb", "x", "y")))
        assertTrue(MasterHome.forYou(st(recap = recap), emptyList(), emptyList(), at(15), zone).rows.isEmpty())
    }

    @Test fun scheduledSendsCountAndFirstTime() {
        val sent = listOf(Sent("1", "kb", "a", sentAt = at(14), scheduledFor = at(16)), Sent("2", "kb", "b", sentAt = at(14), scheduledFor = at(15, 30)))
        val f = MasterHome.forYou(st(s("kb")), emptyList(), sent, at(15), zone)
        assertEquals(listOf(Kind.SCHEDULED), kinds(f)); assertEquals(2, f.rows[0].number); assertEquals(at(15, 30), f.rows[0].at)
    }

    // Revisione finale 02/10 (#4): un prossimo passo già avviato non torna, né con la chiave ricordata né se è già stato
    // mandato a quella sessione.
    @Test fun startedNextStepDoesNotComeBack() {
        val recap = Recap("2026-10-02", listOf(RecapItem("kb", "nota scritta", "distillare la nota")))
        val state = st(s("kb"), recap = recap)
        val key = MasterHome.nextKey("kb", "distillare la nota")
        assertTrue(MasterHome.forYou(state, emptyList(), emptyList(), at(15), zone, read = setOf(key)).rows.isEmpty())
        val sent = listOf(Sent("1", "kb", "distillare la nota", sentAt = at(14)))
        assertTrue(MasterHome.forYou(state, emptyList(), sent, at(15), zone).rows.isEmpty())
    }
}
