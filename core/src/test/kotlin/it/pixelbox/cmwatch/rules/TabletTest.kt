package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Il tablet (piano 04/10, approvato da Franz): plancia e colonne sugli schermi larghi. */
class TabletTest {
    private val zone = ZoneId.of("Europe/Rome")
    private fun at(h: Int, m: Int = 0) = LocalDate.of(2026, 10, 3).atTime(h, m).atZone(zone).toEpochSecond()
    private fun s(name: String, st: SessionState = SessionState.IDLE, since: Long = at(9)) =
        Session(id = name, name = name, account = "personale", project = "personali/$name", state = st, since = since)
    private fun q(id: String, asked: Long) = Question(id, QuestionKind.ASK, "Pubblico?", emptyList(), Tier.LOW, asked)
    private fun st(vararg ss: Session, quota: Map<String, QuotaAccount> = emptyMap(), night: Night = Night()) =
        State(v = 1, ts = at(15), host = "penguin", sessions = ss.toList(), quota = quota, night = night)
    private fun summary(state: State) = Summary.build(state, emptyList(), emptyList(), at(15), zone, emptySet())

    // Pezzo 1: la plancia solo da 840 dp (tablet orizzontale, finestra larga del Chromebook); sotto, l'app del telefono.
    @Test fun wideFrom840dp() {
        assertFalse(Tablet.wide(839))
        assertTrue(Tablet.wide(840))
        assertTrue(Tablet.wide(1280))
    }

    // L'ispettore a destra solo quando c'è posto per la conversazione in mezzo.
    @Test fun inspectorFrom1200dp() {
        assertFalse(Tablet.inspector(1199))
        assertTrue(Tablet.inspector(1200))
    }

    // La riga di stato: le sessioni per stato (la master compresa), la notte, le quote per account, personale prima.
    @Test fun statusCountsSessionsByNeed() {
        val state = st(
            s("master"), s("busy", SessionState.BUSY), s("asks", SessionState.WAITING).copy(question = q("1", at(14))),
            s("gone", SessionState.GONE),
            quota = mapOf(
                "work" to QuotaAccount(h5 = 40, w7 = 70, kind = "work", resetH5 = at(17)),
                "personale" to QuotaAccount(h5 = 9, w7 = 24, kind = "personal", resetH5 = at(16, 40)),
            ),
            night = Night(queued = 2),
        )
        val s = Tablet.status(state, summary(state), at(15), stale = false)
        assertEquals(3, s.open)
        assertEquals(1, s.working)
        assertEquals(1, s.waiting)
        assertEquals(0, s.finished)
        assertEquals(1, s.closed)
        assertEquals(2, s.night)
        assertEquals(listOf("personale", "work"), s.accounts.map { it.account })
        assertEquals(at(16, 40), s.accounts[0].resetAt)
        assertEquals("penguin", s.host)
    }

    // La colonna delle sessioni: i gruppi del riepilogo, con la master al suo posto come le altre.
    @Test fun columnPutsTheMasterInItsGroup() {
        val state = st(s("master"), s("busy", SessionState.BUSY), s("idle", since = at(10)))
        val g = Tablet.groups(summary(state))
        assertEquals(listOf(Summary.Group.WORKING, Summary.Group.STILL), g.map { it.first })
        assertEquals(listOf("idle", "master"), g[1].second.map { it.session.name })
    }

    @Test fun masterAtWorkGoesWithTheWorking() {
        val state = st(s("master", SessionState.BUSY).copy(turnStarted = at(14, 50)), s("idle"))
        val g = Tablet.groups(summary(state))
        assertEquals(listOf("master"), g.first { it.first == Summary.Group.WORKING }.second.map { it.session.name })
    }

    // L'ispettore: aperta da, turno in corso, e la cronologia di oggi della sessione viva (non quella chiusa della cartella).
    @Test fun inspectorTodayFromTheTimeline() {
        val ses = s("phone", SessionState.BUSY, since = at(10, 2)).copy(turnStarted = at(14, 54), tool = "Bash", context = 61)
        val page = TimelinePage(
            since = at(9), sessions = listOf(
                TimelineSession("phone", live = false, events = listOf(TimelineEvent(at(11), "commit", "old", ref = "aaa"))),
                TimelineSession("phone", live = true, events = listOf(
                    TimelineEvent(LocalDate.of(2026, 10, 2).atTime(23, 0).atZone(zone).toEpochSecond(), "commit", "ieri", ref = "b"),
                    TimelineEvent(at(12), "commit", "menus close again", ref = "7fe65be"),
                    TimelineEvent(at(13), "prompt", "ok A", ref = "phone"),
                    TimelineEvent(at(13, 30), "test", "CI verify", ok = true),
                    TimelineEvent(at(14), "commit", "outside the sessions", ref = "2703acf"),
                )),
            ),
        )
        val i = Tablet.inspect(ses, page, at(15), zone)
        assertEquals(at(10, 2), i.openedAt)
        assertEquals(4 * 3600L + 58 * 60, i.openFor)
        assertEquals(6 * 60L, i.turn)
        assertEquals(listOf("menus close again", "ok A", "CI verify", "outside the sessions"), i.today.map { it.text })
        assertEquals(2, i.commits)
        assertEquals(1, i.prompts)
    }

    @Test fun inspectorWithoutTimelineHasNoDay() {
        val i = Tablet.inspect(s("x"), null, at(15), zone)
        assertNull(i.turn)
        assertTrue(i.today.isEmpty())
        assertNull(i.commits)
    }

    // La richiesta della cronologia: da mezzanotte, come epoch (contratto 1.29).
    @Test fun timelineArgIsLocalMidnight() = assertEquals(LocalDate.of(2026, 10, 3).atStartOfDay(zone).toEpochSecond().toString(), Tablet.timelineArg(at(15), zone))

    // Il grafico della quota 5h: la finestra va da 5 ore prima della ripartenza alla ripartenza; i campioni ci stanno dentro.
    @Test fun forecastPlotsTheWindow() {
        val reset = at(16)
        val pace = QuotaHistory.Pace(listOf(QuotaHistory.Sample(at(12), 2), QuotaHistory.Sample(at(14), 6)), 10, reset)
        val f = Tablet.forecast(pace, reset, at(14))
        assertEquals(0.2f, f.points[0].first, 0.001f)
        assertEquals(0.6f, f.points[1].first, 0.001f)
        assertEquals(0.6f, f.nowX, 0.001f)
        assertEquals(10, f.projected)
        assertEquals(at(11), f.start)
    }

    // La settimana senza storico per giorno: il ritmo medio da quando è ripartita, esteso al rinnovo, fino a 100.
    @Test fun weekPaceFromTheAverage() {
        val reset = at(15) + 4 * 86400
        assertEquals(56, Tablet.weekProjected(24, reset, at(15)))
        assertEquals(100, Tablet.weekProjected(80, reset, at(15)))
        assertNull(Tablet.weekProjected(null, reset, at(15)))
        // Appena ripartita (meno di un'ora): il ritmo non si può dire.
        assertNull(Tablet.weekProjected(1, at(15) + 7 * 86400 - 600, at(15)))
    }

    // Pezzo 5, le colonne: al massimo 4, solo sessioni vive, nell'ordine in cui le hai messe.
    @Test fun columnsKeepTheLiveOnesInOrder() {
        val live = listOf("a", "b", "c", "d", "e")
        assertEquals(listOf("c", "a"), Tablet.columns(listOf("c", "gone", "a"), live))
        assertEquals(listOf("a", "b", "c", "d"), Tablet.columns(listOf("a", "b", "c", "d", "e"), live))
    }

    // Senza scelta salvata: le prime tre della colonna delle sessioni. Tolte tutte a mano, restano nessuna.
    @Test fun columnsDefaultToTheFirstThree() {
        assertEquals(listOf("x", "y", "z"), Tablet.columns(null, listOf("x", "y", "z", "w")))
        assertEquals(emptyList<String>(), Tablet.columns(emptyList(), listOf("x", "y")))
    }

    // «+ colonna» aggiunge in fondo; «in colonna» (o ×) toglie; con 4 colonne la nuova prende il posto dell'ultima.
    @Test fun toggleAddsRemovesAndReplacesTheLast() {
        assertEquals(listOf("a", "b"), Tablet.toggle(listOf("a"), "b"))
        assertEquals(listOf("b"), Tablet.toggle(listOf("a", "b"), "a"))
        assertEquals(listOf("a", "b", "c", "e"), Tablet.toggle(listOf("a", "b", "c", "d"), "e"))
    }

    // Le preferenze salvano la lista su una riga; un nome vuoto non diventa una colonna.
    @Test fun columnsPrefRoundTrip() {
        assertEquals("a\nb", Tablet.columnsPref(listOf("a", "b")))
        assertEquals(listOf("a", "b"), Tablet.columnsFromPref("a\nb\n"))
        assertNull(Tablet.columnsFromPref(null))
        assertEquals(emptyList<String>(), Tablet.columnsFromPref(""))
    }
}
