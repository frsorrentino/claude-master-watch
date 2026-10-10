package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.contract.Outcome
import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.LiveFeed.Feed
import it.pixelbox.cmwatch.rules.LiveFeed.Kind
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class LiveFeedTest {
    private val q = ContractJson.decodeState(Fixtures.stateQuestion)
    private val idle = ContractJson.decodeState(Fixtures.stateIdle)
    private val events = ContractJson.decodeEvents(Fixtures.events)
    private val zone = ZoneId.of("Europe/Rome")
    private val t0 = 1_789_220_000L

    private fun plan(feed: Feed, prev: State?, cur: State, now: Long = t0, ev: List<it.pixelbox.cmwatch.contract.Event> = emptyList()) =
        LiveFeed.plan(feed, prev, cur, ev, now, IT, zone)

    private fun Feed.byKey(key: String) = items.single { it.key == key }

    /** state-2 con l'esito di atlas-shop cambiato: un esito nuovo di livello 2. */
    private fun withOutcome(short: String, at: Long, s: State = idle) =
        s.copy(sessions = s.sessions.map { if (it.name == "atlas-shop") it.copy(outcome = Outcome(short, "Esito: $short", at)) else it })

    @Test fun aNewQuestionIsLevelOneWithItsOptions() {
        val n = plan(Feed(), idle, q).byKey("s:ledger-api")
        assertEquals(1, n.level)
        assertEquals(Kind.QUESTION, n.kind)
        assertEquals("q-1789210500-1", n.questionId)
        assertEquals("ledger api chiede: Deploy ready, waiting for the client's ok. Deploy now? Uno: yes. Due: no.", n.text)
    }

    @Test fun anApprovalIsLevelOneAndAsksForTheDoubleConfirm() {
        val n = plan(Feed(), idle, q).byKey("ok:atlas-release-2-4")
        assertEquals(1, n.level)
        assertEquals(Kind.APPROVAL, n.kind)
        assertEquals(
            "Richiesta di ok: Release 2.4 of atlas-shop, tag v2.4 and push to origin main, su production (shop.example.com). Serve la doppia conferma.",
            n.text,
        )
    }

    // atlas-shop in state-1 ha «!ok to deploy on staging»: serve Franz, e i Prossimi con «!» si dicono per primi.
    @Test fun anOutcomeWithABlockingNextStepIsLevelOne() {
        val prev = q.copy(sessions = q.sessions.map { if (it.name == "atlas-shop") it.copy(outcome = null, nextSteps = null) else it })
        val n = plan(Feed(), prev, q).byKey("s:atlas-shop")
        assertEquals(1, n.level)
        assertEquals(Kind.OUTCOME, n.kind)
        assertEquals(
            "atlas shop ha finito: Migrations 008-011 applied, tests green. Azioni: uno. ok to deploy on staging. due. review the test seeds.",
            n.text,
        )
    }

    @Test fun blockingNextStepsGoFirst() {
        val steps = listOf(it.pixelbox.cmwatch.contract.NextStep("review the seeds"), it.pixelbox.cmwatch.contract.NextStep("ok to deploy", blocking = true))
        val cur = withOutcome("Done", 100).let { s -> s.copy(sessions = s.sessions.map { it.copy(nextSteps = steps) }) }
        assertTrue(plan(Feed(), withOutcome("Old", 50), cur).byKey("s:atlas-shop").text.endsWith("Azioni: uno. ok to deploy. due. review the seeds."))
    }

    // Franz, 10/10 08:38: «quando rispondo in una sessione il recap live dovrebbe aggiornarsi, adesso continua a ripetere la
    // domanda». Una sessione che lavora alla tua risposta non ti aspetta più, anche se l'esito di prima aveva Prossimi con «!».
    @Test fun aSessionWorkingOnYourAnswerNoLongerWaits() {
        val steps = listOf(it.pixelbox.cmwatch.contract.NextStep("ok to deploy", blocking = true))
        val waiting = withOutcome("Deploy ready", 100).let { s -> s.copy(sessions = s.sessions.map { x -> if (x.name == "atlas-shop") x.copy(nextSteps = steps, state = SessionState.IDLE) else x }) }
        assertTrue(LiveFeed.round(waiting, IT).any { r -> r.startsWith("atlas shop ha finito: Deploy ready.") })
        val working = waiting.copy(sessions = waiting.sessions.map { x -> if (x.name == "atlas-shop") x.copy(state = SessionState.BUSY) else x })
        val r = LiveFeed.round(working, IT)
        assertTrue(r.none { t -> t.contains("Deploy ready") })
        assertTrue(r.contains("atlas shop è al lavoro."))
        assertEquals("atlas shop è al lavoro.", LiveFeed.status(working.sessions.first { x -> x.name == "atlas-shop" }, IT))
        assertTrue(LiveFeed.waiting(working, IT).none { t -> t.contains("Deploy ready") })
    }

    // Franz, 10/10 08:35: «quando ci sono delle scelte deve leggerle in forma completa, non basta dire scegli 1, 2 o 3». Le righe
    // numerate della risposta, anche dentro un punto, dopo l'esito.
    @Test fun theChoicesInTheReplyAreReadInFull() {
        val full = "Tre punti da decidere.\n\n1. **Il formato:**\n   1. sedici noni;\n   2. nove sedicesimi.\n2. **La musica:** quella del primo montaggio.\n\nEsito: animatic pronto; aspetta il tuo ok."
        val s = idle.copy(sessions = idle.sessions.map { x -> if (x.name == "atlas-shop") x.copy(outcome = Outcome("animatic pronto; aspetta il tuo ok", full, 100)) else x })
        assertEquals(
            "atlas shop ha finito: animatic pronto; aspetta il tuo ok. Le scelte: 1. Il formato: 1. sedici noni; 2. nove sedicesimi. 2. La musica: quella del primo montaggio.",
            LiveFeed.status(s.sessions.first { x -> x.name == "atlas-shop" }, IT),
        )
        // Una riga numerata sola non fa delle scelte.
        assertEquals(emptyList<String>(), LiveFeed.choices("Fatto.\n1. una cosa sola"))
    }

    @Test fun anOutcomeWithoutBlockingStepsIsLevelTwo() {
        val n = plan(Feed(), q, idle).byKey("s:atlas-shop")
        assertEquals(2, n.level)
        assertEquals("atlas shop ha finito: Seeds and admin page reviewed.", n.text)
    }

    @Test fun aClosedSessionIsLevelTwo() {
        val prev = q.copy(sessions = q.sessions.map { if (it.name == "orbit-docs") it.copy(state = SessionState.IDLE) else it })
        val n = plan(Feed(), prev, q).byKey("s:orbit-docs")
        assertEquals(2, n.level)
        assertEquals(Kind.GONE, n.kind)
        assertEquals("orbit docs si è chiusa.", n.text)
        // sparita anche dalla lista: state-2 non ha più ledger-api e field-notes; orbit-docs era già chiusa
        val vanished = plan(Feed(), q, idle)
        assertEquals("ledger api si è chiusa.", vanished.byKey("s:ledger-api").text)
        assertEquals("field notes si è chiusa.", vanished.byKey("s:field-notes").text)
        assertTrue(vanished.items.none { it.key == "s:orbit-docs" })
    }

    @Test fun aFailedRestartIsLevelOne() {
        val n = plan(Feed(), q, q, ev = events).byKey("restart:field-notes")
        assertEquals(1, n.level)
        assertEquals("Il riavvio di field notes non è riuscito.", n.text)
    }

    // Le altre voci di events-sample: domanda, esito e chiusura arrivano dallo stato; launched e recap sono livello 3 e
    // si sentono solo nel giro completo; le quote si leggono dallo stato.
    @Test fun otherEventsQueueNothingByThemselves() {
        assertEquals(listOf("restart:field-notes"), plan(Feed(), q, q, ev = events).items.map { it.key })
    }

    @Test fun quotaOverTheThresholdIsLevelOneWithTheResetTime() {
        val hot = idle.copy(quota = idle.quota + ("personal" to QuotaAccount(h5 = 96, w7 = 38, resetH5 = 1_789_228_800L, resetW7 = 1_789_610_400L)))
        val n = plan(Feed(), idle, hot).byKey("quota:personal")
        assertEquals(1, n.level)
        assertEquals("Quota personal al 96 per cento, si azzera alle 18:00.", n.text)
        // una volta sola, e via dalla coda quando la quota torna sotto
        val again = plan(Feed(), hot, hot)
        assertTrue(again.items.none { it.key == "quota:personal" })
        assertTrue(plan(plan(Feed(), idle, hot), hot, idle).items.none { it.key == "quota:personal" })
    }

    // Avvio della live: si annuncia quello che aspetta Franz, non gli esiti vecchi.
    @Test fun theFirstStateQueuesOnlyLevelOne() {
        val f = plan(Feed(), null, q)
        assertEquals(setOf("s:ledger-api", "s:atlas-shop", "ok:atlas-release-2-4"), f.items.map { it.key }.toSet())
        assertTrue(f.items.all { it.level == 1 })
    }

    @Test fun anUnchangedSessionStaysSilent() {
        assertTrue(plan(Feed(), q, q).items.isEmpty())
    }

    @Test fun eventsOfTheSameSessionBecomeOneNewsWithTheLatestState() {
        val s0 = withOutcome("First", 100).let { s -> s.copy(sessions = s.sessions.map { it.copy(outcome = null) }) }
        val f1 = plan(Feed(), s0, withOutcome("First", 100))
        val f2 = plan(f1, withOutcome("First", 100), withOutcome("Second", 200), now = t0 + 5)
        assertEquals(1, f2.items.size)
        assertEquals("atlas shop ha finito: Second.", f2.items.single().text)
    }

    @Test fun answeredOrVanishedQuestionsLeaveTheQueue() {
        val f = plan(Feed(), idle, q)
        assertTrue(f.items.any { it.key == "s:ledger-api" })
        val answered = events.filter { it.kind == EventKind.ANSWERED }
        // l'evento arriva prima dello stato nuovo: la domanda c'è ancora nello stato, la notizia se ne va lo stesso
        assertTrue(plan(f, q, q, ev = answered).items.none { it.key == "s:ledger-api" })
        val gone = q.copy(sessions = q.sessions.map { if (it.name == "ledger-api") it.copy(state = SessionState.BUSY, question = null) else it })
        assertTrue(plan(f, q, gone).items.none { it.key == "s:ledger-api" })
    }

    @Test fun anApprovalThatVanishesLeavesTheQueue() {
        val f = plan(Feed(), idle, q)
        assertTrue(f.items.any { it.key.startsWith("ok:") })
        assertTrue(plan(f, q, q.copy(approvals = emptyList())).items.none { it.key.startsWith("ok:") })
    }

    @Test fun levelOneComesFirst() {
        // l'esito di atlas-shop (livello 2) è più vecchio della domanda di ledger-api, ma la domanda passa davanti
        val done = withOutcome("New", 100)
        val f = plan(plan(Feed(), withOutcome("Old", 50), done), done, done.copy(sessions = done.sessions + q.sessions.first()), now = t0 + 10)
        assertEquals(2, f.items.size)
        assertEquals("s:ledger-api", LiveFeed.next(f, t0 + 10)?.key)
    }

    @Test fun atMostOneLevelTwoNewsPerSessionEveryThreeMinutes() {
        var f = plan(Feed(), withOutcome("A", 50), withOutcome("B", 100))
        val first = LiveFeed.next(f, t0)!!
        f = LiveFeed.spoken(f, first.key, t0)
        f = plan(f, withOutcome("B", 100), withOutcome("C", 200), now = t0 + 30)
        assertNull(LiveFeed.next(f, t0 + 179))
        assertEquals("atlas shop ha finito: C.", LiveFeed.next(f, t0 + 180)?.text)
        // il livello 1 non ha limiti: la stessa sessione chiede subito dopo
        val asking = withOutcome("C", 200).let { s -> s.copy(sessions = s.sessions.map { it.copy(question = q.sessions.first().question) }) }
        val f3 = plan(f, withOutcome("C", 200), asking, now = t0 + 40)
        assertEquals("s:atlas-shop", LiveFeed.next(f3, t0 + 41)?.key)
        assertEquals(Kind.QUESTION, LiveFeed.next(f3, t0 + 41)?.kind)
    }

    @Test fun laterPutsTheNewsBackInFiveMinutes() {
        var f = plan(Feed(), withOutcome("A", 50), withOutcome("B", 100))
        f = LiveFeed.later(f, "s:atlas-shop", t0)
        assertNull(LiveFeed.next(f, t0 + 299))
        assertEquals("s:atlas-shop", LiveFeed.next(f, t0 + 300)?.key)
    }

    // «Dopo» vale per quella notizia: un aggiornamento della stessa sessione è una notizia nuova.
    @Test fun aNewerUpdateOverridesLater() {
        var f = plan(Feed(), withOutcome("A", 50), withOutcome("B", 100))
        f = LiveFeed.later(f, "s:atlas-shop", t0)
        f = plan(f, withOutcome("B", 100), withOutcome("C", 200), now = t0 + 10)
        assertEquals("atlas shop ha finito: C.", LiveFeed.next(f, t0 + 11)?.text)
    }

    @Test fun skipRemovesTheNews() {
        val f = plan(Feed(), withOutcome("A", 50), withOutcome("B", 100))
        assertEquals(1, f.items.size)
        assertTrue(LiveFeed.skip(f, "s:atlas-shop").items.isEmpty())
        assertNull(LiveFeed.next(LiveFeed.skip(f, "s:atlas-shop"), t0))
    }

    @Test fun onlyBlockingReadsLevelOne() {
        val done = withOutcome("B", 100)
        val f = plan(plan(Feed(), withOutcome("A", 50), done), done, done.copy(sessions = done.sessions + q.sessions.first()))
        val one = LiveFeed.next(f, t0, onlyBlocking = true)!!
        assertEquals("s:ledger-api", one.key)
        val rest = LiveFeed.spoken(f, one.key, t0)
        assertNull(LiveFeed.next(rest, t0, onlyBlocking = true))
        assertEquals("s:atlas-shop", LiveFeed.next(rest, t0)?.key)
    }

    @Test fun aLongQuestionReadsTheStartThenPointsToTheWatch() {
        val long = (1..12).joinToString(" ") { "Sentence number $it explains one more detail of the change." }
        val cur = q.copy(sessions = q.sessions.map { s -> if (s.name == "ledger-api") s.copy(question = s.question!!.copy(text = long)) else s })
        val text = plan(Feed(), idle, cur).byKey("s:ledger-api").text
        assertTrue(text, text.startsWith("ledger api chiede: Sentence number 1 "))
        assertTrue(text, text.endsWith(". Continua sul watch."))
        assertTrue(text, text.length <= LiveFeed.QUESTION_MAX + " Continua sul watch.".length)
        assertFalse(text.contains("…"))
    }

    // Bassa priorità attiva: le sue notizie di routine vanno solo nel giro completo; quello che serve a Franz no.
    @Test fun lowPriorityOutcomesStayOutOfTheQueue() {
        fun low(s: State) = s.copy(sessions = s.sessions.map { it.copy(lowPriority = "active") })
        assertEquals(1, plan(Feed(), withOutcome("A", 50), withOutcome("B", 100)).items.size)
        assertTrue(plan(Feed(), low(withOutcome("A", 50)), low(withOutcome("B", 100))).items.isEmpty())
        // anche la chiusura, sia con lo stato «gone» sia sparendo dalla lista
        val lowQ = low(q.copy(sessions = q.sessions.map { if (it.name == "orbit-docs") it.copy(state = SessionState.IDLE) else it }))
        assertTrue(plan(Feed(), lowQ, low(q)).items.none { it.kind == Kind.GONE })
        assertTrue(plan(Feed(), lowQ, low(idle)).items.none { it.kind == Kind.GONE })
    }

    @Test fun theFullRoundGoesWaitingThenWorkingThenFollowedIdle() {
        // state-1: ledger-api chiede, poi la richiesta di ok; atlas-shop ha un «!» nell'esito di prima ma è al lavoro, quindi
        // non ti aspetta più (Franz, 10/10 08:38), ed è a bassa priorità, quindi non è nemmeno fra quelle al lavoro;
        // field-notes è ferma ma non seguita, orbit-docs è chiusa
        assertEquals(
            listOf(
                "ledger api chiede: Deploy ready, waiting for the client's ok. Deploy now? Uno: yes. Due: no.",
                "Richiesta di ok: Release 2.4 of atlas-shop, tag v2.4 and push to origin main, su production (shop.example.com). Serve la doppia conferma.",
            ),
            LiveFeed.round(q, IT),
        )
        val atlas = idle.sessions.single().copy(followed = true)
        val working = atlas.copy(name = "pix-news-it", state = SessionState.BUSY, outcome = null)
        val lazy = atlas.copy(name = "slow-one", state = SessionState.BUSY, lowPriority = "active")
        val quiet = atlas.copy(name = "quiet", outcome = null)
        val state = idle.copy(sessions = listOf(atlas, lazy, quiet, working))
        assertEquals(
            listOf("news è al lavoro.", "atlas shop ha finito: Seeds and admin page reviewed.", "quiet è ferma."),
            LiveFeed.round(state, IT),
        )
        assertTrue(LiveFeed.round(idle, IT).isEmpty())
    }

    // Il recap (Franz, 08/10 20:11-20:15): per una sessione al lavoro cosa deve fare e cosa sta facendo, da quanto.
    @Test fun theRecapSaysWhatABusySessionMustDoAndIsDoing() {
        val atlas = q.sessions.single { it.name == "atlas-shop" }.copy(lowPriority = null, outcome = null)
        val state = q.copy(sessions = listOf(atlas), approvals = emptyList())
        assertEquals(
            listOf("atlas shop lavora da 12 minuti, ora: Run the test suite. Obiettivo: All checkout tests green and the release tagged."),
            LiveFeed.round(state, IT, now = 1_789_210_700L + 12 * 60),
        )
        // Senza nota dello strumento, senza obiettivo: il prossimo passo detto, e «pensa».
        val bare = atlas.copy(toolNote = null, tool = null, goal = null, turnStarted = 1_789_210_700L)
        assertEquals(
            listOf("atlas shop lavora da 1 minuto, ora: sta pensando. Obiettivo: Review the seeds and the admin page."),
            LiveFeed.round(state.copy(sessions = listOf(bare)), IT, now = 1_789_210_700L + 70),
        )
    }

    // Livello 3 (Franz, 07/10 mattina): «sessione avviata» e i riepiloghi non entrano in coda, ma il giro completo li legge.
    @Test fun launchedAndRecapAreKeptForTheFullRound() {
        val now = 1_789_237_000L
        val feed = plan(Feed(), idle, idle, now, events)
        assertTrue(feed.items.none { it.level == 3 })
        // launched (1789198000) è più vecchio di 2 ore: resta fuori; il recap (1789236000) entra.
        assertEquals(listOf("Riepilogo: Recap 12/09/2026, 3 projects, 1 waiting on a question."), feed.info.map { it.text })
        val fresh = events.single { it.kind == EventKind.LAUNCHED }.copy(ts = now - 60)
        val both = plan(feed, idle, idle, now, listOf(fresh))
        assertEquals(listOf(Kind.RECAP, Kind.LAUNCHED), both.info.map { it.kind })
        assertEquals("atlas shop avviata.", both.info[1].text)
        // Lo stesso evento due volte resta uno.
        assertEquals(2, plan(both, idle, idle, now, listOf(fresh)).info.size)
        assertEquals(listOf("Riepilogo: Recap 12/09/2026, 3 projects, 1 waiting on a question.", "atlas shop avviata."), LiveFeed.round(idle, IT, both))
        assertTrue(LiveFeed.heard(both).info.isEmpty())
    }

    @Test fun atMostFiveInfoNewsStay() {
        val now = 1_789_237_000L
        val launched = events.single { it.kind == EventKind.LAUNCHED }
        val many = (1..7).map { launched.copy(key = "k$it", session = "s$it", ts = now - 100 + it) }
        val feed = plan(Feed(), idle, idle, now, many)
        assertEquals(listOf("s3 avviata.", "s4 avviata.", "s5 avviata.", "s6 avviata.", "s7 avviata."), feed.info.map { it.text })
    }

    @Test fun statusWaitingAndQuotaPhrases() {
        val ledger = q.sessions.single { it.name == "ledger-api" }
        assertEquals("ledger api chiede: Deploy ready, waiting for the client's ok. Deploy now? Uno: yes. Due: no.", LiveFeed.status(ledger, IT))
        assertEquals("field notes ha finito: README rewritten.", LiveFeed.status(q.sessions.single { it.name == "field-notes" }, IT))
        assertEquals("orbit docs si è chiusa.", LiveFeed.status(q.sessions.single { it.name == "orbit-docs" }, IT))
        val busy = idle.sessions.single().copy(state = SessionState.BUSY)
        assertEquals("atlas shop è al lavoro.", LiveFeed.status(busy, IT))
        assertEquals(LiveFeed.round(q, IT), LiveFeed.waiting(q, IT))
        assertTrue(LiveFeed.waiting(idle, IT).isEmpty())
        assertEquals(
            listOf("Quota personal al 36 per cento, si azzera alle 04:00.", "Quota work al 75 per cento, si azzera alle 06:00."),
            LiveFeed.quotaAll(q, IT, zone),
        )
    }

    companion object {
        /** Le frasi italiane della specifica (§5), come arriveranno da strings.xml. */
        val IT = LiveFeed.Labels(
            code = "Codice",
            numbers = listOf("uno", "due", "tre", "quattro", "cinque", "sei", "sette", "otto", "nove", "dieci"),
            question = "%1\$s chiede: %2\$s",
            option = "%1\$s: %2\$s.",
            more = "Continua sul watch.",
            approval = "Richiesta di ok: %1\$s. Serve la doppia conferma.",
            where = "su %1\$s",
            outcome = "%1\$s ha finito: %2\$s.",
            next = "Azioni: %1\$s.",
            step = "%1\$s. %2\$s",
            gone = "%1\$s si è chiusa.",
            restartFailed = "Il riavvio di %1\$s non è riuscito.",
            quota = "Quota %1\$s al %2\$d per cento, si azzera alle %3\$s.",
            quotaNoReset = "Quota %1\$s al %2\$d per cento.",
            busy = "%1\$s è al lavoro.",
            idle = "%1\$s è ferma.",
            launched = "%1\$s avviata.",
            recap = "Riepilogo: %1\$s.",
        )
    }
}
