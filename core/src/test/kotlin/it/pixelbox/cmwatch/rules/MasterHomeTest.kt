package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.rules.MasterHome.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    // Franz, 02/10 20:49: un prossimo passo non si propone a una sessione già aperta («Avvia» sembrava riaprirla); resta
    // solo per i progetti senza sessione, da lanciare.
    @Test fun nextStepOnlyForProjectsWithoutASession() {
        val recap = Recap("2026-10-02", listOf(RecapItem("kb", "nota scritta", "distillare la nota"), RecapItem("atlas", "test", "pubblicare")))
        val projects = listOf(Project("/w/kb", "kb", "personale"), Project("/w/atlas", "atlas", "personale"))
        assertTrue(MasterHome.forYou(st(s("kb"), s("atlas", SessionState.BUSY), recap = recap, projects = projects), emptyList(), emptyList(), at(15), zone).rows.isEmpty())
        val closed = MasterHome.forYou(st(recap = recap, projects = projects), emptyList(), emptyList(), at(15), zone)
        assertEquals(listOf(Kind.NEXT_STEP, Kind.NEXT_STEP), kinds(closed))
        assertEquals("kb", closed.rows[0].title); assertEquals("distillare la nota", closed.rows[0].detail)
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
        val key = MasterHome.nextKey("kb", "distillare la nota")
        assertTrue(MasterHome.forYou(st(recap = recap), emptyList(), emptyList(), at(15), zone, read = setOf(key)).rows.isEmpty())
    }

    // Franz, 02/10 20:49 (variante 3 dei mockup): dopo le domande, chi ha finito il turno, con la logica dell'avviso nella chat.
    private fun done(name: String, at: Long, followed: Boolean = true) =
        s(name).copy(outcome = Outcome("Fatto", "Test verdi.\nProssimi: tagga · apri la PR", at), followed = followed)

    @Test fun finishedTurnsAfterQuestionsNewestFirst() {
        val f = MasterHome.forYou(st(s("a", SessionState.WAITING, q = q("1", at(14))), done("b", at(14, 30)), done("c", at(14, 50))), emptyList(), emptyList(), at(15), zone)
        assertEquals(listOf(Kind.QUESTION, Kind.FINISHED, Kind.FINISHED), kinds(f))
        assertEquals(at(14), f.rows[0].at)
        assertEquals(listOf("c", "b"), f.rows.drop(1).map { it.session })
        assertEquals(at(14, 50), f.rows[1].at); assertEquals("Test verdi.\nProssimi: tagga · apri la PR", f.rows[1].detail)
    }

    @Test fun finishedOnlyFromFollowedOrWrittenSessions() {
        val other = done("b", at(14, 30), followed = false)
        assertTrue(MasterHome.forYou(st(other), emptyList(), emptyList(), at(15), zone).rows.isEmpty())
        val sent = listOf(Sent("1", "b", "fai i test", sentAt = at(14)))
        assertEquals(listOf(Kind.FINISHED), kinds(MasterHome.forYou(st(other), emptyList(), sent, at(15), zone)))
    }

    @Test fun finishedLeavesOnceAnsweredReadOrOld() {
        val b = done("b", at(14, 30))
        val replied = listOf(Sent("1", "b", "tagga", sentAt = at(14, 40)))
        assertTrue(MasterHome.forYou(st(b), emptyList(), replied, at(15), zone).rows.isEmpty())
        assertTrue(MasterHome.forYou(st(b), emptyList(), emptyList(), at(15), zone, read = setOf(MasterHome.finishedKey("b", at(14, 30)))).rows.isEmpty())
        assertTrue(MasterHome.forYou(st(b), emptyList(), emptyList(), at(14, 30) + MasterHome.FINISHED_S + 1, zone).rows.isEmpty())
    }

    // Franz, 02/10 21:29: «In corso» entra in «Per te» come gruppo «al lavoro», solo chi lavora (gli altri sono già sopra).
    @Test fun workingOnlyBusySessionsWithoutTheMaster() {
        val state = st(s("a", SessionState.BUSY), s("b", SessionState.AWAITING), s("c", SessionState.WAITING, q = q("1", at(14))), s("d"), s("master", SessionState.BUSY))
        assertEquals(setOf("a", "b"), MasterHome.working(state).map { it.session.name }.toSet())
    }

    // Franz, 03/10 09:15: in «al lavoro» l'ultimo esito e le risposte rapide pronte all'invio, non il comando che gira.
    @Test fun workingCarriesTheLastOutcomeNotTheTool() {
        val busy = s("a", SessionState.BUSY).copy(tool = "Bash", toolNote = "cat /tmp/x", outcome = Outcome("Fatto", "Test verdi.\nProssimi: tagga · apri la PR", at(14)))
        assertEquals("Test verdi.\nProssimi: tagga · apri la PR", MasterHome.working(st(busy)).single().detail)
        assertNull(MasterHome.working(st(s("a", SessionState.BUSY).copy(tool = "Bash", toolNote = "cat /tmp/x"))).single().detail)
    }

    // Consulenza del 02/10 (Codex e Antigravity, approvata da Franz il 03/10): con una domanda aperta «Per te» va prima
    // dell'ultimo esito; senza, l'esito resta in testa.
    @Test fun aQuestionPutsForYouFirst() {
        assertTrue(MasterHome.forYouFirst(MasterHome.forYou(st(s("a", SessionState.WAITING, q = q("1", at(14)))), emptyList(), emptyList(), at(15), zone)))
        assertFalse(MasterHome.forYouFirst(MasterHome.forYou(st(s("a", ctx = 85)), emptyList(), emptyList(), at(15), zone)))
    }

    @Test fun theMasterIsNeverInItsOwnList() =
        assertTrue(MasterHome.forYou(st(done("master", at(14, 30))), emptyList(), emptyList(), at(15), zone).rows.isEmpty())

    // Casa A (mockup approvato da Franz, 02/10 07:38): l'esito della master in grande, i consigli, le sessioni in corso.
    private fun claude(id: String, text: String, at: Long) = TranscriptEntry(id = id, role = "assistant", text = text, at = at)

    @Test fun heroTakesTheOutcomeLineAsHeadline() {
        val h = MasterHome.hero(listOf(claude("a", "vecchia", at(5)), claude("b", "Lanciata la sessione.\n\nEsito: Fase 2.2 avviata\nProssimi: distilla nella kb · prova la casa\nWatch: avviata", at(6))), s("master"))!!
        assertEquals("Fase 2.2 avviata", h.headline)
        assertEquals("Lanciata la sessione.", h.body)
        assertEquals(listOf("distilla nella kb", "prova la casa"), h.steps)
        assertEquals(at(6), h.at)
    }

    @Test fun heroWithoutOutcomeLineUsesTheFirstLine() {
        val h = MasterHome.hero(listOf(claude("a", "Fatto il push.\nCI verde.", at(6))), s("master"))!!
        assertEquals("Fatto il push.", h.headline)
        assertEquals("CI verde.", h.body)
    }

    @Test fun heroFallsBackToTheRelayOutcome() {
        val m = s("master").copy(outcome = Outcome("corto", "Esito: dal relay", at(7)))
        assertEquals("dal relay", MasterHome.hero(emptyList(), m)!!.headline)
        assertEquals(null, MasterHome.hero(emptyList(), s("master")))
    }

    @Test fun runningLeavesOutMasterAndClosedWithOneLineEach() {
        val busy = s("kb", SessionState.BUSY).copy(tool = "Bash", toolNote = "Lancia la suite")
        val idle = s("docs").copy(outcome = Outcome("Resa fatta", "Resa fatta e controllata", at(6)))
        val waiting = s("ledger", SessionState.WAITING, q = q("1", at(5)))
        val rows = MasterHome.running(st(s("master"), busy, idle, waiting, s("old", SessionState.GONE)))
        assertEquals(listOf("ledger", "kb", "docs"), rows.map { it.session.name })
        assertEquals(listOf("Pubblico?", "Bash · Lancia la suite", "Resa fatta"), rows.map { it.detail })
    }
}
