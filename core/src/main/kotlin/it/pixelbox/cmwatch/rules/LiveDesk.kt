package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.LiveFeed.Feed
import it.pixelbox.cmwatch.rules.LiveFeed.Kind
import it.pixelbox.cmwatch.rules.LiveFeed.News
import it.pixelbox.cmwatch.rules.LiveRoute.Route
import java.time.ZoneId

/**
 * La regia della modalità live (specifica live, §3-§8): tiene insieme notiziario (`LiveFeed`), smistamento del parlato
 * (`LiveRoute`) e doppia conferma (`DoubleConfirm`). Funzioni pure: ogni ingresso (stato nuovo, tocco sul watch, fine
 * frase, passare del tempo, risposta del relay, collegamento, pausa) dà la regia nuova e gli effetti che il servizio esegue
 * (voce, comandi del contratto, scheda per il watch). L'ora entra da fuori, in millisecondi.
 *
 * La voce non interrompe una notizia per un'altra; interrompe solo per rispondere a Franz. Una notizia interrotta resta in
 * coda e si rilegge. Durante un'interazione aperta (istruzione in partenza, scelta, testo atteso, doppia conferma) la
 * coda aspetta e la scheda resta quella dell'interazione.
 */
object LiveDesk {
    /** Il recap della live (Franz, 08/10 20:15): ogni 4 minuti di partenza; il tocco nelle impostazioni passa al valore dopo, 0 = spento. */
    const val RECAP_DEFAULT_MIN = 4
    val RECAP_CHOICES = listOf(3, 4, 5, 10, 15, 0)
    fun nextRecap(min: Int): Int = RECAP_CHOICES.getOrElse(RECAP_CHOICES.indexOf(min) + 1) { RECAP_CHOICES.first() }

    /** «Mando a nome: testo» e Annulla per 5 secondi, poi parte. */
    const val TELL_MS = 5_000L

    /** Dopo la lettura, una scheda con tasti (domanda, ok, esito con Prossimi) resta per 20 secondi; le altre per 4. */
    const val HOLD_ACTION_MS = 20_000L
    const val HOLD_MS = 4_000L

    /** La domanda alla master: un suono ogni 10 secondi, la frase della master lenta a 60, decade a 10 minuti. */
    const val TONE_MS = 10_000L
    const val SLOW_MS = 60_000L
    const val ASK_EXPIRE_MS = 600_000L

    const val MASTER = ContextActions.MASTER

    /** Il tasto del primo tocco («Approva») e quello da tenere premuto: diversi, come vuole la doppia conferma. */
    private const val FIRST_KEY = "approve"
    private const val HOLD_KEY = "hold"
    private const val MASTER_ANSWER = "master:answer"

    /** Le parole della regia, da strings.xml. */
    data class Words(
        val alreadyAnswered: String, val notUnderstood: String, val notFound: String, val whatToSay: String,
        val sending: String, val cancelled: String, val approved: String, val pickItem: String, val confirm: String,
        val masterAsked: String, val masterSlow: String, val linkLost: String, val linkBack: String,
        val nobodyWaiting: String, val nothingNew: String, val failed: String, val outcomeLabel: String,
    )

    data class Lang(val feed: LiveFeed.Labels, val words: Words, val zone: ZoneId)

    /** A cosa serve un comando mandato: il servizio rimanda il risultato con la sua etichetta. */
    enum class Tag { ANSWER, STEP, TELL, ASK, LAST, APPROVE }

    sealed class Effect {
        /** Da leggere in coda alle frasi già date; il servizio chiama `spoken` quando la voce ha finito tutto. */
        data class Say(val text: String) : Effect()

        /** Zitta subito, coda della voce svuotata. */
        data object Hush : Effect()
        data class Send(
            val op: CmdOp, val session: String?, val arg: String?, val tag: Tag, val text: String? = null,
            val voice: Boolean? = null, val via: String? = null, val confirmations: Int? = null,
        ) : Effect()
        data class Show(val card: LiveCard) : Effect()

        /** Il suono breve: attesa della master, ripresa dopo una pausa. */
        data object Tone : Effect()

        /** «Stop» dal watch: il servizio si spegne. */
        data object Stop : Effect()
    }

    data class Tell(val session: String, val text: String, val until: Long)

    /** La domanda alla master in sospeso: `baseline` = l'`outcome.at` della master all'invio; `asked` = `last` partito. */
    data class Ask(val sentAt: Long, val baseline: Long, val lastTone: Long, val slowSaid: Boolean = false, val asked: Boolean = false)

    /**
     * `prev` = l'ultimo stato pianificato (fermo mentre il collegamento è perso); `current` = la notizia sulla scheda;
     * `reading` = la chiave della notizia che la voce sta leggendo; `holdUntil` = fin quando la scheda resta dopo la voce.
     */
    data class Desk(
        val feed: Feed = Feed(), val prev: State? = null, val current: News? = null, val reading: String? = null,
        val speaking: Boolean = false, val holdUntil: Long = 0, val lastSaid: String? = null,
        val confirm: DoubleConfirm.Machine = DoubleConfirm.Machine(), val tell: Tell? = null, val pick: Route.Pick? = null,
        val askingFor: String? = null, val ask: Ask? = null, val onlyBlocking: Boolean = false, val linked: Boolean = true,
        val paused: Boolean = false, val card: LiveCard? = null, val seq: Long = 0,
        /** Ogni quanto il recap (Franz, 08/10 20:15), 0 = spento; `lastRecap` l'ultima volta, 0 = mai: il primo all'accensione. */
        val recapEveryMs: Long = 0, val lastRecap: Long = 0,
        /** Le azioni offerte dal tasto Azioni del watch (Franz, 08/10 21:17), nell'ordine dei tasti della scelta. */
        val actions: List<RecapActions.Action> = emptyList(),
    )

    data class Out(val desk: Desk, val effects: List<Effect>)

    /** Uno stato nuovo dal relay, con gli eventi arrivati dall'ultima chiamata. */
    fun state(desk: Desk, cur: State, events: List<Event>, nowMs: Long, lang: Lang): Out = Run(desk, nowMs, lang).apply {
        if (!d.linked) return@apply
        d = d.copy(feed = LiveFeed.plan(d.feed, d.prev, cur, events, nowMs / 1000, lang.feed, lang.zone), prev = cur)
        // L'esito della master che risponde a una domanda a voce arriva come risposta letta, non come notizia d'esito.
        if (d.ask != null) d = d.copy(feed = LiveFeed.skip(d.feed, "s:$MASTER"))
        val c = d.current
        if (c != null && c.kind == Kind.QUESTION && superseded(c, cur, events)) {
            if (d.speaking && d.reading == c.key) { hush(); say(lang.words.alreadyAnswered) }
            d = d.copy(current = null, holdUntil = 0)
        }
        if (c != null && c.kind == Kind.APPROVAL && d.confirm.phase == DoubleConfirm.Phase.IDLE && cur.approvals.none { "ok:${it.task}" == c.key }) {
            d = d.copy(current = null, holdUntil = 0)
        }
        confirmTick(cur)
        askTick(cur)
        advance(cur)
    }.out()

    /** Un tocco sul watch (o un tasto degli auricolari, tradotto in tocco dal servizio). */
    fun tap(desk: Desk, tap: LiveTap, state: State, nowMs: Long, lang: Lang): Out = Run(desk, nowMs, lang).apply {
        val c = d.current
        when (tap.action) {
            LiveTap.Action.OPTION -> if (c?.kind == Kind.QUESTION && c.session != null && tap.index >= 1) {
                send(Effect.Send(CmdOp.ANSWER, c.session, tap.index.toString(), Tag.ANSWER))
                done(c, state)
            }
            LiveTap.Action.STEP -> {
                val step = d.card?.takeIf { it.kind == LiveCard.Kind.OUTCOME }?.options?.getOrNull(tap.index - 1)
                if (c?.kind == Kind.OUTCOME && c.session != null && step != null) {
                    send(Effect.Send(CmdOp.PROMPT, c.session, step, Tag.STEP))
                    done(c, state)
                }
            }
            LiveTap.Action.PICK -> d.pick?.let { p ->
                val name = p.options.getOrNull(tap.index - 1) ?: return@let
                d = d.copy(pick = null)
                route(
                    when (p.intent) {
                        LiveRoute.Intent.TELL -> if (p.text.isEmpty()) Route.AskText(name) else Route.Tell(name, p.text)
                        LiveRoute.Intent.STATUS -> Route.Status(name)
                        LiveRoute.Intent.APPROVE -> Route.Approve(name)
                        // Il tasto Azioni: l'azione scelta parte come un «Mando a …», con Annulla per 5 secondi.
                        LiveRoute.Intent.ACTION -> d.actions.getOrNull(tap.index - 1)?.let { a -> Route.Tell(a.to, a.send) } ?: return@let
                    },
                    state,
                )
            }
            LiveTap.Action.REPEAT -> d.lastSaid?.let { t ->
                val again = d.current?.takeIf { n -> n.text == t && d.feed.items.any { it.key == n.key } }?.key
                say(t, reading = again)
            }
            LiveTap.Action.LATER -> if (c != null) {
                val inQueue = d.feed.items.any { it.key == c.key }
                val feed = if (inQueue) LiveFeed.later(d.feed, c.key, nowMs / 1000)
                else d.feed.copy(items = d.feed.items + c.copy(notBefore = nowMs / 1000 + LiveFeed.LATER_S))
                d = d.copy(feed = feed)
                moveOn(state)
            }
            LiveTap.Action.SKIP -> if (c != null) done(c, state)
            LiveTap.Action.SAY -> said(tap.text.orEmpty(), state)
            LiveTap.Action.APPROVE -> if (c?.kind == Kind.APPROVAL) arm(c.key.removePrefix("ok:"), state)
            LiveTap.Action.PRESS -> d = d.copy(confirm = DoubleConfirm.press(d.confirm, HOLD_KEY, nowMs))
            LiveTap.Action.RELEASE -> confirmed(d.confirm, DoubleConfirm.release(d.confirm, HOLD_KEY, nowMs), state)
            LiveTap.Action.CANCEL -> cancel(state)
            LiveTap.Action.ROUND -> route(Route.Round, state)
            LiveTap.Action.ACTIONS -> {
                val acts = RecapActions.of(state)
                if (acts.isEmpty()) reply(lang.words.nothingNew)
                else { d = d.copy(actions = acts); route(Route.Pick(LiveRoute.Intent.ACTION, acts.map { it.text }), state) }
            }
            LiveTap.Action.ONLY_BLOCKING -> {
                d = d.copy(onlyBlocking = !d.onlyBlocking)
                d.card?.let { show(it.copy(buzz = LiveCard.Buzz.NONE)) }
                advance(state)
            }
            LiveTap.Action.STOP -> { hush(); show(LiveCard(0, LiveCard.Kind.OFF)); fx += Effect.Stop }
        }
    }.out()

    /** La voce ha finito tutto quello che aveva in coda. */
    fun spoken(desk: Desk, state: State, nowMs: Long, lang: Lang): Out = Run(desk, nowMs, lang).apply {
        if (!d.speaking) return@apply
        val r = d.reading
        d = d.copy(speaking = false, reading = null)
        d = if (r != null) {
            val actionable = d.current?.let { n -> n.kind in ACTIONABLE || (n.kind == Kind.OUTCOME && d.card?.options?.isNotEmpty() == true) } == true
            d.copy(feed = LiveFeed.spoken(d.feed, r, nowMs / 1000), holdUntil = nowMs + if (actionable) HOLD_ACTION_MS else HOLD_MS)
        } else d.copy(holdUntil = nowMs + HOLD_MS)
        advance(state)
    }.out()

    /** Il passare del tempo: istruzione in partenza, doppia conferma, attesa della master, «Dopo» che scade. */
    fun tick(desk: Desk, state: State, nowMs: Long, lang: Lang): Out = Run(desk, nowMs, lang).apply {
        d.tell?.let { t ->
            if (nowMs >= t.until) {
                d = d.copy(tell = null)
                send(Effect.Send(CmdOp.PROMPT, t.session, t.text, Tag.TELL))
                restore(state)
            }
        }
        confirmTick(state)
        askTick(state)
        advance(state)
        recapTick(state)
    }.out()

    /** La risposta del relay a un comando mandato con `tag`. */
    fun result(desk: Desk, tag: Tag, ok: Boolean, text: String, state: State, nowMs: Long, lang: Lang): Out = Run(desk, nowMs, lang).apply {
        val w = lang.words
        when (tag) {
            Tag.ANSWER -> if (!ok) reply(w.alreadyAnswered)
            Tag.STEP, Tag.TELL -> if (!ok) reply(w.failed.format(text))
            Tag.ASK -> if (!ok) { d = d.copy(ask = null); reply(w.failed.format(text)) }
            Tag.LAST -> {
                d = d.copy(ask = null)
                if (ok) {
                    // La risposta passa davanti alla coda: livello 1 e `at` 0, la più vecchia di tutte.
                    val n = News(MASTER_ANSWER, 1, Kind.MASTER, MASTER, SpeechText.forPhone(text, lang.feed.code, w.outcomeLabel), at = 0)
                    d = d.copy(feed = LiveFeed.skip(LiveFeed.skip(d.feed, MASTER_ANSWER), "s:$MASTER").let { it.copy(items = it.items + n) })
                } else reply(w.failed.format(text))
            }
            Tag.APPROVE -> {
                val m = DoubleConfirm.result(d.confirm, ok)
                if (m.phase != d.confirm.phase) {
                    d = d.copy(confirm = m)
                    say(if (ok) w.approved else w.cancelled)
                    restore(state)
                }
            }
        }
        advance(state)
    }.out()

    /** Il collegamento col relay: perso, la coda si ferma; tornato, lo stato seguente dice cosa è cambiato. */
    fun link(desk: Desk, up: Boolean, nowMs: Long, lang: Lang): Out = Run(desk, nowMs, lang).apply {
        if (!up && d.linked) {
            d = d.copy(linked = false)
            reply(lang.words.linkLost)
        } else if (up && !d.linked) {
            d = d.copy(linked = true)
            reply(lang.words.linkBack)
        }
    }.out()

    /** Un avviso del servizio (batteria, live accesa): detto subito, come una risposta a Franz. */
    fun notice(desk: Desk, text: String, nowMs: Long, lang: Lang): Out = Run(desk, nowMs, lang).apply { reply(text) }.out()

    /** Pausa (auricolari scollegati, audio di una chiamata) e ripresa, con un suono e la notizia interrotta da capo. */
    fun pause(desk: Desk, on: Boolean, state: State, nowMs: Long, lang: Lang): Out = Run(desk, nowMs, lang).apply {
        if (on && !d.paused) {
            hush()
            d = d.copy(paused = true)
        } else if (!on && d.paused) {
            d = d.copy(paused = false)
            fx += Effect.Tone
            val c = d.current
            if (c != null && d.feed.items.any { it.key == c.key }) say(c.text, reading = c.key) else advance(state)
        }
    }.out()

    private val ACTIONABLE = setOf(Kind.QUESTION, Kind.APPROVAL)

    private fun superseded(n: News, cur: State, events: List<Event>): Boolean =
        events.any { it.kind == EventKind.ANSWERED && it.ref == n.questionId } ||
            cur.sessions.firstOrNull { it.name == n.session }?.question?.id != n.questionId

    /** Una regia in corso di calcolo: la regia nuova e gli effetti, in ordine. */
    private class Run(var d: Desk, val now: Long, val lang: Lang) {
        val fx = mutableListOf<Effect>()

        fun out() = Out(d, fx.toList())

        fun hush() {
            if (d.speaking) fx += Effect.Hush
            d = d.copy(speaking = false, reading = null)
        }

        /** Parla; se stava parlando, prima zitta (la notizia interrotta resta in coda). In pausa non dice nulla. */
        fun say(text: String, reading: String? = null) {
            if (d.paused) return
            hush()
            fx += Effect.Say(text)
            d = d.copy(speaking = true, reading = reading, lastSaid = text)
        }

        fun show(card: LiveCard) {
            val c = card.copy(seq = d.seq + 1, onlyBlocking = d.onlyBlocking)
            d = d.copy(seq = c.seq, card = c)
            fx += Effect.Show(c)
        }

        fun send(s: Effect.Send) { fx += s }

        /** Una risposta a Franz: voce e scheda, la notizia in corso lascia il posto. */
        fun reply(text: String) {
            say(text)
            d = d.copy(current = null, holdUntil = 0)
            show(LiveCard(0, LiveCard.Kind.NEWS, text = text))
        }

        /** Il recap a intervalli: a voce zitta, senza notizia in corso né interazioni; se non c'è niente da dire, tace. */
        fun recapTick(state: State) {
            if (d.recapEveryMs <= 0) return
            // Il primo appena la live parte e la voce tace (Franz, 09/10 16:20), poi uno ogni intervallo.
            if (d.lastRecap != 0L && now - d.lastRecap < d.recapEveryMs) return
            if (d.paused || !d.linked || d.speaking || busy() || d.current != null || d.ask != null) return
            val all = LiveFeed.round(state, lang.feed, d.feed, now / 1000)
            d = d.copy(feed = LiveFeed.heard(d.feed), lastRecap = now)
            if (all.isNotEmpty()) reply(all.joinToString(" "))
        }

        fun busy() = d.tell != null || d.pick != null || d.askingFor != null ||
            d.confirm.phase == DoubleConfirm.Phase.ARMED || d.confirm.phase == DoubleConfirm.Phase.CONFIRMING

        /** La prossima notizia, se la voce tace, nessuna interazione è aperta e la scheda di prima ha avuto il suo tempo. */
        fun advance(state: State) {
            if (d.paused || !d.linked || d.speaking || busy()) return
            val n = LiveFeed.next(d.feed, now / 1000, d.onlyBlocking)
            val c = d.current
            if (now < d.holdUntil && !(n != null && n.level == 1 && (c?.level ?: Int.MAX_VALUE) > 1)) return
            if (n == null) {
                if (c != null || d.card == null || d.card?.kind != LiveCard.Kind.IDLE) {
                    d = d.copy(current = null)
                    show(LiveCard(0, LiveCard.Kind.IDLE))
                }
                return
            }
            d = d.copy(current = n, holdUntil = 0)
            say(n.text, reading = n.key)
            show(cardOf(n, state))
        }

        /** La notizia toccata (risposta, Prossimo, Salta) esce dalla coda e si passa alla seguente. */
        fun done(c: News, state: State) {
            d = d.copy(feed = LiveFeed.skip(d.feed, c.key))
            moveOn(state)
        }

        fun moveOn(state: State) {
            hush()
            d = d.copy(current = null, holdUntil = 0)
            advance(state)
        }

        /** Finita un'interazione, torna la scheda della notizia in corso o quella vuota. */
        fun restore(state: State) {
            val c = d.current
            if (c != null) show(cardOf(c, state).copy(buzz = LiveCard.Buzz.NONE)) else show(LiveCard(0, LiveCard.Kind.IDLE))
        }

        fun cancel(state: State) {
            val w = lang.words
            when {
                d.tell != null -> d = d.copy(tell = null)
                d.confirm.phase == DoubleConfirm.Phase.ARMED -> d = d.copy(confirm = DoubleConfirm.cancel(d.confirm))
                d.pick != null || d.askingFor != null -> d = d.copy(pick = null, askingFor = null)
                else -> return
            }
            say(w.cancelled)
            restore(state)
        }

        fun said(text: String, state: State) {
            val s = d.askingFor
            if (s != null) {
                d = d.copy(askingFor = null)
                if (text.isBlank()) reply(lang.words.notUnderstood) else route(Route.Tell(s, text.trim()), state)
                return
            }
            route(LiveRoute.parse(text, state), state)
        }

        fun route(r: Route, state: State) {
            val w = lang.words
            val l = lang.feed
            when (r) {
                is Route.Tell -> {
                    val name = SpeakableName.of(r.session)
                    say(w.sending.format(name, r.text))
                    d = d.copy(tell = Tell(r.session, r.text, now + TELL_MS))
                    show(LiveCard(0, LiveCard.Kind.TELL, title = name, text = r.text, until = now + TELL_MS))
                }
                is Route.AskText -> {
                    val text = w.whatToSay.format(SpeakableName.of(r.session))
                    say(text)
                    d = d.copy(askingFor = r.session)
                    show(LiveCard(0, LiveCard.Kind.NEWS, title = SpeakableName.of(r.session), text = text))
                }
                is Route.Pick -> {
                    val labels = r.options.map { o ->
                        when (r.intent) {
                            LiveRoute.Intent.APPROVE -> state.approvals.firstOrNull { it.task == o }?.title ?: o
                            LiveRoute.Intent.ACTION -> o
                            else -> SpeakableName.of(o)
                        }
                    }
                    val text = labels.mapIndexed { i, n -> w.pickItem.format(l.numbers.getOrNull(i) ?: (i + 1).toString(), n) }.joinToString(", ")
                    say(text)
                    d = d.copy(pick = r)
                    show(LiveCard(0, LiveCard.Kind.PICK, text = text, options = labels))
                }
                is Route.NotFound -> reply(w.notFound.format(r.name))
                is Route.Status -> state.sessions.firstOrNull { it.name == r.session }?.let { reply(LiveFeed.status(it, l)) }
                Route.Waiting -> reply(LiveFeed.waiting(state, l).joinToString(" ").ifEmpty { w.nobodyWaiting })
                Route.Quota -> reply(LiveFeed.quotaAll(state, l, lang.zone).joinToString(" ").ifEmpty { w.nothingNew })
                Route.Round -> {
                    val all = LiveFeed.round(state, l, d.feed, now / 1000)
                    d = d.copy(feed = LiveFeed.heard(d.feed), lastRecap = now)
                    reply(all.joinToString(" ").ifEmpty { w.nothingNew })
                }
                is Route.Approve -> arm(r.task, state)
                is Route.Master -> {
                    val baseline = state.sessions.firstOrNull { it.name == MASTER }?.outcome?.at ?: 0
                    d = d.copy(ask = Ask(now, baseline, now))
                    send(Effect.Send(CmdOp.PROMPT, MASTER, r.text, Tag.ASK, voice = true))
                    reply(w.masterAsked)
                }
                Route.Empty -> reply(w.notUnderstood)
            }
        }

        /** Primo tocco della doppia conferma: la voce rilegge cosa e dove; la scheda mostra il tasto da tenere premuto. */
        fun arm(task: String, state: State) {
            val a = state.approvals.firstOrNull { it.task == task } ?: return reply(lang.words.notFound.format(task))
            d = d.copy(confirm = DoubleConfirm.arm(d.confirm, task, FIRST_KEY, now))
            val what = listOfNotNull(a.what, a.where?.let { lang.feed.where.format(it) }).joinToString(", ")
            say(lang.words.confirm.format(what))
            show(LiveCard(0, LiveCard.Kind.CONFIRM, title = a.title, text = what, until = d.confirm.armedAt + DoubleConfirm.TIMEOUT_MS))
        }

        fun confirmTick(state: State) {
            val m = d.confirm
            confirmed(m, DoubleConfirm.tick(m, now, present = state.approvals.any { it.task == m.target }), state)
        }

        /** Da armata: a 2 secondi parte l'ok live; annullata, la voce lo dice. */
        fun confirmed(old: DoubleConfirm.Machine, m: DoubleConfirm.Machine, state: State) {
            d = d.copy(confirm = m)
            if (old.phase != DoubleConfirm.Phase.ARMED || m.phase == old.phase) return
            if (m.phase == DoubleConfirm.Phase.CONFIRMING) {
                send(Effect.Send(CmdOp.APPROVE, null, m.target, Tag.APPROVE, text = "ok", via = "live", confirmations = 2))
                d.card?.let { show(it.copy(buzz = LiveCard.Buzz.DONE)) }
            } else if (m.phase == DoubleConfirm.Phase.CANCELLED) {
                say(lang.words.cancelled)
                restore(state)
            }
        }

        /** La domanda alla master: esito nuovo → `last`; altrimenti il suono d'attesa, la frase della master lenta, la scadenza. */
        fun askTick(state: State) {
            val a = d.ask ?: return
            if (now - a.sentAt >= ASK_EXPIRE_MS) { d = d.copy(ask = null); return }
            if (a.asked) return
            val at = state.sessions.firstOrNull { it.name == MASTER }?.outcome?.at
            when {
                at != null && at > a.baseline -> {
                    d = d.copy(ask = a.copy(asked = true))
                    send(Effect.Send(CmdOp.LAST, MASTER, null, Tag.LAST))
                }
                d.speaking -> Unit
                !a.slowSaid && now - a.sentAt >= SLOW_MS -> {
                    d = d.copy(ask = a.copy(slowSaid = true, lastTone = now))
                    say(lang.words.masterSlow)
                }
                now - a.lastTone >= TONE_MS -> {
                    d = d.copy(ask = a.copy(lastTone = now))
                    if (!d.paused) fx += Effect.Tone
                }
            }
        }

        fun cardOf(n: News, state: State): LiveCard {
            val s = n.session?.let { name -> state.sessions.firstOrNull { it.name == name } }
            val title = n.session?.let(SpeakableName::of).orEmpty()
            val buzz = when (n.level) { 1 -> LiveCard.Buzz.LONG; 2 -> LiveCard.Buzz.SHORT; else -> LiveCard.Buzz.NONE }
            return when {
                n.kind == Kind.QUESTION && s?.question != null -> LiveCard(
                    0, LiveCard.Kind.QUESTION, title, s.question.text, s.question.options.map { QuestionRules.optionText(it.label) }, buzz = buzz,
                )
                n.kind == Kind.OUTCOME && s?.outcome != null -> {
                    val steps = s.nextSteps.orEmpty().sortedBy { !it.blocking }
                    LiveCard(0, LiveCard.Kind.OUTCOME, title, s.outcome.short, steps.map { it.text }, steps.map { it.blocking }, buzz)
                }
                n.kind == Kind.APPROVAL -> {
                    val a = state.approvals.firstOrNull { "ok:${it.task}" == n.key }
                    val what = listOfNotNull(a?.what, a?.where?.let { lang.feed.where.format(it) }).joinToString(", ")
                    LiveCard(0, LiveCard.Kind.APPROVAL, a?.title.orEmpty(), what, buzz = buzz)
                }
                else -> LiveCard(0, LiveCard.Kind.NEWS, title, n.text, buzz = buzz)
            }
        }
    }
}
