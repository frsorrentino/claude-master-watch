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
        Session(id = name, name = name, account = "personale", project = "personal/$name", state = st, since = since)
    private fun q(id: String, asked: Long) = Question(id, QuestionKind.ASK, "Pubblico?", emptyList(), Tier.LOW, asked)
    private fun st(vararg ss: Session, quota: Map<String, QuotaAccount> = emptyMap(), night: Night = Night()) =
        State(v = 1, ts = at(15), host = "crostini-demo", sessions = ss.toList(), quota = quota, night = night)
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

    // v2 (Franz, 04/10 16:36): il tocco su una scheda della home aggiunge una colonna; già in colonna non cambia niente;
    // con quattro colonne la nuova prende il posto dell'ultima.
    @Test fun addPutsTheSessionInAColumn() {
        assertEquals(listOf("a", "b"), Tablet.add(listOf("a"), "b"))
        assertEquals(listOf("a", "b"), Tablet.add(listOf("a", "b"), "a"))
        assertEquals(listOf("a", "b", "c", "e"), Tablet.add(listOf("a", "b", "c", "d"), "e"))
    }

    // Trascinata sopra un'altra, una colonna scambia il posto con lei; fuori dalle colonne non succede niente.
    @Test fun swapExchangesTwoColumns() {
        assertEquals(listOf("c", "b", "a"), Tablet.swap(listOf("a", "b", "c"), 0, 2))
        assertEquals(listOf("a", "b", "c"), Tablet.swap(listOf("a", "b", "c"), 1, 1))
        assertEquals(listOf("a", "b", "c"), Tablet.swap(listOf("a", "b", "c"), 0, 5))
    }

    // Le larghezze a scatti: 12 parti, all'inizio uguali.
    @Test fun equalShares() {
        assertEquals(listOf(12), Tablet.Shares.equal(1))
        assertEquals(listOf(6, 6), Tablet.Shares.equal(2))
        assertEquals(listOf(4, 4, 4), Tablet.Shares.equal(3))
        assertEquals(listOf(3, 3, 3, 3), Tablet.Shares.equal(4))
    }

    // Il bordo fra due colonne sposta parti intere dall'una all'altra, e nessuna scende sotto le 2 parti (un sesto).
    @Test fun aBorderMovesWholeParts() {
        // Franz, 05/10 12:30: la colonna che cresce prende lo spazio da tutte le altre, che si dividono in parti uguali il resto.
        assertEquals(listOf(6, 3, 3), Tablet.Shares.drag(listOf(4, 4, 4), border = 0, parts = 2))
        assertEquals(listOf(8, 2, 2), Tablet.Shares.drag(listOf(4, 4, 4), border = 0, parts = 5))
        assertEquals(listOf(3, 2, 7), Tablet.Shares.drag(listOf(4, 4, 4), border = 1, parts = -3))
        assertEquals(listOf(4, 4, 2, 2), Tablet.Shares.drag(listOf(3, 3, 3, 3), border = 0, parts = 1))
        assertEquals(listOf(8, 4), Tablet.Shares.drag(listOf(6, 6), border = 0, parts = 2))
        assertEquals(listOf(6, 6), Tablet.Shares.drag(listOf(6, 6), border = 3, parts = 2))
    }

    // Lo scatto: i pixel trascinati diventano parti intere della larghezza delle colonne.
    @Test fun pixelsSnapToParts() {
        assertEquals(2, Tablet.Shares.parts(dragPx = 190f, widthPx = 1200f))
        assertEquals(-1, Tablet.Shares.parts(dragPx = -60f, widthPx = 1200f))
        assertEquals(0, Tablet.Shares.parts(dragPx = 40f, widthPx = 1200f))
    }

    // Nelle preferenze su una riga; una scelta che non torna col numero di colonne riparte uguale.
    @Test fun sharesPrefRoundTrip() {
        assertEquals("6,2,4", Tablet.Shares.pref(listOf(6, 2, 4)))
        assertEquals(listOf(6, 2, 4), Tablet.Shares.fromPref("6,2,4", 3))
        assertEquals(listOf(4, 4, 4), Tablet.Shares.fromPref("6,6", 3))
        assertEquals(listOf(6, 6), Tablet.Shares.fromPref("11,1", 2))
        assertEquals(listOf(3, 3, 3, 3), Tablet.Shares.fromPref(null, 4))
    }
}
