package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.contract.Outcome
import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.LiveCard.Buzz
import it.pixelbox.cmwatch.rules.LiveDesk.Effect
import it.pixelbox.cmwatch.rules.LiveDesk.Effect.Hush
import it.pixelbox.cmwatch.rules.LiveDesk.Effect.Say
import it.pixelbox.cmwatch.rules.LiveDesk.Effect.Send
import it.pixelbox.cmwatch.rules.LiveDesk.Effect.Show
import it.pixelbox.cmwatch.rules.LiveDesk.Tag
import it.pixelbox.cmwatch.rules.LiveTap.Action
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class LiveDeskTest {
    private val q = ContractJson.decodeState(Fixtures.stateQuestion)
    private val idle = ContractJson.decodeState(Fixtures.stateIdle)
    private val events = ContractJson.decodeEvents(Fixtures.events)
    private val lang = LiveDesk.Lang(LiveFeedTest.IT, WORDS, ZoneId.of("Europe/Rome"))
    private val t0 = 1_789_220_000_000L

    private val ledgerQ = "ledger api chiede: Deploy ready, waiting for the client's ok. Deploy now? Uno: yes. Due: no."
    private val atlasO = "atlas shop ha finito: Migrations 008-011 applied, tests green. Prossimi: uno, ok to deploy on staging; due, review the test seeds."
    private val okText = "Richiesta di ok: Release 2.4 of atlas-shop, tag v2.4 and push to origin main, su production (shop.example.com). Serve la doppia conferma."

    /** La regia con i suoi effetti, passo per passo. */
    private inner class Run(var desk: LiveDesk.Desk = LiveDesk.Desk(), var state: State = q) {
        var fx: List<Effect> = emptyList()
        fun go(o: LiveDesk.Out): Run { desk = o.desk; fx = o.effects; return this }
        fun state(cur: State, now: Long, ev: List<Event> = emptyList()) = go(LiveDesk.state(desk, cur, ev, now, lang)).also { state = cur }
        fun tap(a: Action, now: Long, index: Int = 0, text: String? = null) = go(LiveDesk.tap(desk, LiveTap(a, index, text), state, now, lang))
        fun spoken(now: Long) = go(LiveDesk.spoken(desk, state, now, lang))
        fun tick(now: Long) = go(LiveDesk.tick(desk, state, now, lang))
        fun result(tag: Tag, ok: Boolean, text: String, now: Long) = go(LiveDesk.result(desk, tag, ok, text, state, now, lang))
        fun said() = fx.filterIsInstance<Say>().map { it.text }
        fun sent() = fx.filterIsInstance<Send>()
        fun card() = fx.filterIsInstance<Show>().last().card
    }

    /** La live accesa su state-1 e la prima frase finita. */
    private fun started(): Run = Run().state(q, t0)

    private fun master(at: Long?) = q.sessions.single { it.name == "field-notes" }
        .copy(name = "master", outcome = at?.let { Outcome("done", "Esito: done", it) })

    @Test fun theFirstStateReadsTheOpenQuestionWithALongBuzz() {
        val r = started()
        assertEquals(listOf(ledgerQ), r.said())
        val c = r.card()
        assertEquals(LiveCard.Kind.QUESTION, c.kind); assertEquals("ledger api", c.title)
        assertEquals(listOf("yes", "no"), c.options); assertEquals(Buzz.LONG, c.buzz)
    }

    @Test fun aSentenceIsNotInterruptedAndAQuestionCardStaysForItsButtons() {
        val r = started()
        val hot = q.copy(quota = q.quota + ("personal" to QuotaAccount(h5 = 97, w7 = 40, resetH5 = 1_789_228_800)))
        assertTrue(r.state(hot, t0 + 1_000).said().isEmpty())
        assertTrue(r.spoken(t0 + 6_000).said().isEmpty())
        assertTrue(r.tick(t0 + 6_000 + LiveDesk.HOLD_ACTION_MS - 1).said().isEmpty())
        assertEquals(listOf(atlasO), r.tick(t0 + 6_000 + LiveDesk.HOLD_ACTION_MS).said())
    }

    @Test fun anOptionAnswersAndMovesOn() {
        val r = started().spoken(t0 + 6_000)
        r.tap(Action.OPTION, t0 + 7_000, index = 1)
        assertEquals(Send(CmdOp.ANSWER, "ledger-api", "1", Tag.ANSWER), r.sent().single())
        assertEquals(listOf(atlasO), r.said())
        val c = r.card()
        assertEquals(LiveCard.Kind.OUTCOME, c.kind)
        assertEquals(listOf("ok to deploy on staging", "review the test seeds"), c.options); assertEquals(listOf(true, false), c.blocking)
        // Il Prossimo toccato parte come prompt alla sua sessione.
        r.spoken(t0 + 12_000).tap(Action.STEP, t0 + 13_000, index = 1)
        assertEquals(Send(CmdOp.PROMPT, "atlas-shop", "ok to deploy on staging", Tag.STEP), r.sent().single())
    }

    @Test fun aQuestionAnsweredElsewhereStopsTheVoice() {
        val r = started()
        val gone = q.copy(sessions = q.sessions.map { if (it.name == "ledger-api") it.copy(question = null) else it })
        r.state(gone, t0 + 2_000, events.filter { it.kind == EventKind.ANSWERED })
        assertTrue(r.fx.first() is Hush)
        assertEquals(listOf(WORDS.alreadyAnswered), r.said())
        // Un rifiuto del relay sulla risposta dice lo stesso.
        assertEquals(listOf(WORDS.alreadyAnswered), started().result(Tag.ANSWER, false, "no question open", t0 + 3_000).said())
    }

    @Test fun repeatLaterAndSkip() {
        val r = started()
        assertEquals(listOf(ledgerQ), r.tap(Action.REPEAT, t0 + 1_000).said())
        assertTrue(r.fx.first() is Hush)
        assertTrue(r.desk.feed.items.any { it.key == "s:ledger-api" })
        r.spoken(t0 + 6_000).tap(Action.LATER, t0 + 7_000)
        assertEquals(listOf(atlasO), r.said())
        val back = r.desk.feed.items.single { it.key == "s:ledger-api" }
        assertEquals((t0 + 7_000) / 1000 + LiveFeed.LATER_S, back.notBefore)
        r.tap(Action.SKIP, t0 + 8_000)
        assertTrue(r.desk.feed.items.none { it.key == "s:atlas-shop" })
        assertEquals(listOf(okText), r.said())
    }

    @Test fun anInstructionLeavesAfterFiveSecondsUnlessCancelled() {
        val r = started().spoken(t0 + 6_000)
        r.tap(Action.SAY, t0 + 7_000, text = "di' a ledger: aspetta il cliente")
        assertEquals(listOf("Mando a ledger api: aspetta il cliente"), r.said())
        assertEquals(LiveCard.Kind.TELL, r.card().kind); assertEquals(t0 + 7_000 + LiveDesk.TELL_MS, r.card().until)
        assertTrue(r.tick(t0 + 11_000).sent().isEmpty())
        assertEquals(Send(CmdOp.PROMPT, "ledger-api", "aspetta il cliente", Tag.TELL), r.tick(t0 + 12_000).sent().single())
        val c = started().spoken(t0 + 6_000).tap(Action.SAY, t0 + 7_000, text = "scrivi a ledger: niente")
        c.tap(Action.CANCEL, t0 + 8_000)
        assertEquals(listOf(WORDS.cancelled), c.said())
        assertTrue(c.tick(t0 + 20_000).sent().isEmpty())
    }

    @Test fun anAmbiguousNameAsksToPickAndAMissingOneIsSaid() {
        val two = q.copy(sessions = q.sessions + q.sessions.single { it.name == "atlas-shop" }.copy(name = "atlas-admin"))
        val r = Run(state = two).state(two, t0).spoken(t0 + 6_000)
        r.tap(Action.SAY, t0 + 7_000, text = "di' a atlas: rivedi i semi")
        assertEquals(listOf("uno: atlas shop, due: atlas admin"), r.said())
        assertEquals(LiveCard.Kind.PICK, r.card().kind); assertEquals(listOf("atlas shop", "atlas admin"), r.card().options)
        r.tap(Action.PICK, t0 + 8_000, index = 2)
        assertEquals(listOf("Mando a atlas admin: rivedi i semi"), r.said())
        assertEquals(listOf("Non trovo zebra."), started().tap(Action.SAY, t0 + 1_000, text = "di' a zebra: ciao").said())
        // Senza testo: «Cosa dico a …?», e il dettato seguente diventa l'istruzione.
        val a = started().tap(Action.SAY, t0 + 1_000, text = "di' a ledger")
        assertEquals(listOf("Cosa dico a ledger api?"), a.said())
        assertEquals(listOf("Mando a ledger api: rilancia i test"), a.tap(Action.SAY, t0 + 5_000, text = "rilancia i test").said())
    }

    @Test fun statusQuestionsAreAnsweredByTheApp() {
        val r = started()
        assertEquals(listOf(WORDS.notUnderstood), r.tap(Action.SAY, t0 + 1_000, text = "  ").said())
        assertEquals(listOf("field notes ha finito: README rewritten."), r.tap(Action.SAY, t0 + 2_000, text = "com'è messa field notes").said())
        assertEquals(listOf(LiveFeed.waiting(q, LiveFeedTest.IT).joinToString(" ")), r.tap(Action.SAY, t0 + 3_000, text = "chi mi aspetta").said())
        assertEquals(listOf(WORDS.nobodyWaiting), Run(state = idle).tap(Action.SAY, t0, text = "chi mi aspetta").said())
        assertTrue(r.tap(Action.SAY, t0 + 4_000, text = "quanta quota").said().single().startsWith("Quota personal al 36 per cento"))
        assertTrue(r.sent().isEmpty())
    }

    @Test fun theFullRoundReadsTheInfoOnceAndThenForgetsIt() {
        val now = 1_789_237_000_000L
        val r = Run(state = idle).state(idle, now, events)
        assertTrue(r.desk.feed.info.isNotEmpty())
        val round = r.tap(Action.ROUND, now + 1_000).said().single()
        assertTrue(round.endsWith("Riepilogo: Recap 12/09/2026, 3 projects, 1 waiting on a question."))
        assertTrue(r.desk.feed.info.isEmpty())
        assertEquals(listOf(WORDS.nothingNew), r.tap(Action.SAY, now + 2_000, text = "giro completo").said())
    }

    @Test fun aFreeQuestionGoesToTheMasterAndItsAnswerIsReadFirst() {
        // Su state-2 non c'è altro da leggere: il suono d'attesa si sente solo quando la voce tace.
        val st = idle.copy(sessions = idle.sessions + master(1_789_210_000))
        val r = Run(state = st).state(st, t0)
        r.tap(Action.SAY, t0 + 7_000, text = "quale sessione è più vicina al rilascio?")
        assertEquals(Send(CmdOp.PROMPT, "master", "quale sessione è più vicina al rilascio?", Tag.ASK, voice = true), r.sent().single())
        assertEquals(listOf(WORDS.masterAsked), r.said())
        r.spoken(t0 + 9_000)
        assertTrue(r.tick(t0 + 16_000).fx.none { it is Effect.Tone })
        assertTrue(r.tick(t0 + 17_000).fx.any { it is Effect.Tone })
        assertEquals(listOf(WORDS.masterSlow), r.tick(t0 + 67_000).said())
        // L'esito nuovo della master: si chiede `last`, e la risposta passa davanti alla coda.
        val answered = st.copy(sessions = idle.sessions + master(1_789_220_070))
        r.spoken(t0 + 69_000).state(answered, t0 + 70_000)
        assertEquals(Send(CmdOp.LAST, "master", null, Tag.LAST), r.sent().single())
        r.result(Tag.LAST, true, "**atlas-shop**: manca solo l'ok.", t0 + 71_000)
        assertNull(r.desk.ask)
        val first = r.desk.feed.items.minWith(compareBy<LiveFeed.News> { it.level }.thenBy { it.at })
        assertEquals(LiveFeed.Kind.MASTER, first.kind); assertEquals("atlas-shop: manca solo l'ok.", first.text)
    }

    @Test fun anUnansweredMasterQuestionLapsesAfterTenMinutes() {
        val st = q.copy(sessions = q.sessions + master(null))
        val r = Run(state = st).state(st, t0).spoken(t0 + 6_000)
        r.tap(Action.SAY, t0 + 7_000, text = "che si dice?").spoken(t0 + 8_000)
        r.tick(t0 + 7_000 + LiveDesk.ASK_EXPIRE_MS)
        assertNull(r.desk.ask)
        // Una domanda nuova sostituisce quella in attesa: una sola attesa.
        r.tap(Action.SAY, t0 + 900_000, text = "una").tap(Action.SAY, t0 + 901_000, text = "due")
        assertEquals(t0 + 901_000, r.desk.ask!!.sentAt)
    }

    @Test fun anApprovalNeedsTheDoubleConfirm() {
        val r = started().spoken(t0 + 6_000).tap(Action.SKIP, t0 + 7_000).spoken(t0 + 12_000).tap(Action.SKIP, t0 + 13_000)
        assertEquals(listOf(okText), r.said())
        r.tap(Action.APPROVE, t0 + 14_000)
        assertEquals(listOf("Approvi tag v2.4 and push to origin main, su production (shop.example.com)? Tieni premuto l'altro tasto per due secondi."), r.said())
        assertEquals(LiveCard.Kind.CONFIRM, r.card().kind); assertEquals(t0 + 14_000 + DoubleConfirm.TIMEOUT_MS, r.card().until)
        r.tap(Action.PRESS, t0 + 16_000)
        assertTrue(r.tick(t0 + 17_999).sent().isEmpty())
        r.tick(t0 + 18_000)
        assertEquals(Send(CmdOp.APPROVE, null, "atlas-release-2-4", Tag.APPROVE, text = "ok", via = "live", confirmations = 2), r.sent().single())
        assertEquals(Buzz.DONE, r.card().buzz)
        assertEquals(listOf(WORDS.approved), r.result(Tag.APPROVE, true, "approved atlas-release-2-4", t0 + 19_000).said())
    }

    @Test fun aDoubleConfirmIsCancelledByTimeTapRefusalOrAVanishedRequest() {
        fun armed() = started().spoken(t0 + 6_000).tap(Action.SKIP, t0 + 7_000).spoken(t0 + 12_000).tap(Action.SKIP, t0 + 13_000)
            .tap(Action.APPROVE, t0 + 14_000)
        assertEquals(listOf(WORDS.cancelled), armed().tick(t0 + 24_000).said())
        assertEquals(listOf(WORDS.cancelled), armed().tap(Action.CANCEL, t0 + 15_000).said())
        val gone = armed()
        assertEquals(listOf(WORDS.cancelled), gone.state(q.copy(approvals = emptyList()), t0 + 15_000).said())
        val refused = armed().tap(Action.PRESS, t0 + 15_000).tick(t0 + 17_000)
        assertEquals(listOf(WORDS.cancelled), refused.result(Tag.APPROVE, false, "atlas-release-2-4: live approval needs a double confirmation", t0 + 18_000).said())
        // Il tasto «Approva» premuto due volte non vale come seconda conferma.
        val same = armed().tap(Action.APPROVE, t0 + 15_000)
        assertTrue(same.tick(t0 + 18_000).sent().isEmpty())
    }

    @Test fun approveSaidAloudOnlyOpensTheConfirm() {
        val r = started().tap(Action.SAY, t0 + 1_000, text = "approva la release")
        assertTrue(r.sent().isEmpty())
        assertEquals(LiveCard.Kind.CONFIRM, r.card().kind)
        assertEquals(DoubleConfirm.Phase.ARMED, r.desk.confirm.phase)
    }

    @Test fun onlyBlockingSkipsLevelTwo() {
        val r = Run(state = idle).state(idle, t0)
        r.tap(Action.ONLY_BLOCKING, t0 + 1_000)
        assertTrue(r.card().onlyBlocking)
        val later = idle.copy(sessions = idle.sessions.map { it.copy(outcome = Outcome("done", "Esito: done", 1_789_219_990)) })
        assertTrue(r.state(later, t0 + 2_000).said().isEmpty())
        r.tap(Action.ONLY_BLOCKING, t0 + 3_000)
        assertEquals(listOf("atlas shop ha finito: done."), r.said())
    }

    @Test fun aLostLinkStopsTheQueueAndTheReturnTellsWhatChanged() {
        val r = Run(state = idle).state(idle, t0)
        r.go(LiveDesk.link(r.desk, false, t0 + 1_000, lang))
        assertEquals(listOf(WORDS.linkLost), r.said())
        r.spoken(t0 + 2_000)
        assertTrue(r.state(q, t0 + 3_000).said().isEmpty())
        r.go(LiveDesk.link(r.desk, true, t0 + 4_000, lang))
        assertEquals(listOf(WORDS.linkBack), r.said())
        r.spoken(t0 + 5_000).state(q, t0 + 6_000)
        assertEquals(listOf(ledgerQ), r.said())
    }

    @Test fun whilePausedNothingIsSaidAndTheResumeRereadsTheNews() {
        val r = started()
        r.go(LiveDesk.pause(r.desk, true, r.state, t0 + 1_000, lang))
        assertTrue(r.fx.first() is Hush)
        assertTrue(r.tap(Action.SAY, t0 + 2_000, text = "chi mi aspetta").said().isEmpty())
        r.go(LiveDesk.pause(r.desk, false, r.state, t0 + 3_000, lang))
        assertTrue(r.fx.any { it is Effect.Tone })
        assertEquals(listOf(ledgerQ), r.said())
    }

    @Test fun aNewStateDuringAnInstructionKeepsItsCard() {
        val r = started().spoken(t0 + 6_000).tap(Action.SAY, t0 + 7_000, text = "di' a ledger: aspetta")
        r.spoken(t0 + 8_000).state(q.copy(quota = q.quota + ("personal" to QuotaAccount(h5 = 99, w7 = 40))), t0 + 9_000)
        assertTrue(r.fx.filterIsInstance<Show>().isEmpty())
        assertTrue(r.said().isEmpty())
    }

    companion object {
        val WORDS = LiveDesk.Words(
            alreadyAnswered = "Già risposto.",
            notUnderstood = "Non ho capito.",
            notFound = "Non trovo %1\$s.",
            whatToSay = "Cosa dico a %1\$s?",
            sending = "Mando a %1\$s: %2\$s",
            cancelled = "Annullato.",
            approved = "Approvato.",
            pickItem = "%1\$s: %2\$s",
            confirm = "Approvi %1\$s? Tieni premuto l'altro tasto per due secondi.",
            masterAsked = "Chiedo alla master.",
            masterSlow = "La master ci mette più del solito, ti avviso quando risponde.",
            linkLost = "Collegamento perso.",
            linkBack = "Di nuovo collegato.",
            nobodyWaiting = "Nessuno ti aspetta.",
            nothingNew = "Niente da dire.",
            failed = "Non riuscito: %1\$s",
            outcomeLabel = "Esito",
        )
    }
}
