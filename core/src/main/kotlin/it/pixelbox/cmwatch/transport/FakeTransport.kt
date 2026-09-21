package it.pixelbox.cmwatch.transport

import it.pixelbox.cmwatch.contract.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Gli stati della storia dei video promozionali (piano 16/09): uno per scena, raggiungibili via adb. */
enum class DemoStep { CALM, QUESTION, DEPLOYED, FOLLOWUP, TICK, BLOG, NEW, SHOPASK }

/**
 * Legge le fixture del contratto e simula il PC. Gli scarti temporali delle fixture 1 e 2 vengono riportati
 * a «adesso»; la fixture 3 (stale) tiene il suo ts vecchio perché deve mostrare «PC fermo».
 */
class FakeTransport(
    private val load: (String) -> String,
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
) : Transport {
    private val current = MutableStateFlow(rebase(ContractJson.decodeState(load("state-1-question"))))
    private val eventList = demoEvents(ContractJson.decodeEvents(load("events-sample")), ContractJson.decodeState(load("state-1-question")))
    private val results = HashMap<String, CmdResult>()

    override val state: Flow<State> get() = current
    override val events: Flow<List<Event>> = MutableStateFlow(eventList)

    // Ruoli della storia, per id della fixture `state-1-question`: la sessione della domanda è quella del deploy, la ferma
    // scrive il post. Per id e non per posizione: ogni risposta riordina la lista.
    private val base = ContractJson.decodeState(load("state-1-question"))
    private val deployId = base.sessions[0].id
    private val shopId = base.sessions[1].id
    private val blogId = base.sessions[2].id
    private val newId = base.sessions[3].id
    private val originalQuestion = base.sessions[0].question
    private var screenGrowth = 0
    /** Acceso da FOLLOWUP: il terminale della sessione del deploy cresce a ogni cattura e TICK fa avanzare il lavoro. */
    private var growing = false
    private val ticks = listOf("Edit CHANGELOG.md" to "Edit", "Tag v2.8.0" to "Bash", "Push the tag" to "Bash")
    private var tick = 0

    /** Porta la demo allo stato di una scena. I testi sono in inglese come la demo. */
    fun demoStep(step: DemoStep) {
        val s = current.value
        val t = now()
        fun at(id: String, f: (Session) -> Session) = s.copy(ts = t, sessions = Order.sessions(s.sessions.map { if (it.id == id) f(it) else it }))
        if (step != DemoStep.FOLLOWUP && step != DemoStep.TICK) growing = false
        current.value = when (step) {
            // Nessuna sessione seguita all'inizio: la pressione lunga della scena 3 deve accendere la campanella, non trovarla accesa.
            // La sessione del rilascio non è mai vuota: al polso una card «Idle» senza testo è un buco (Franz, 18/09 07:39).
            DemoStep.CALM -> s.copy(ts = t, sessions = Order.sessions(s.sessions.map {
                if (it.id == deployId) it.copy(state = SessionState.BUSY, question = null, since = t - 900, turnStarted = t - 900, followed = false, tool = "Bash", toolNote = "Running the staging checks for 2.8.0") else it.copy(followed = false)
            }))
            // Id nuovo a ogni scena: con quello della fixture, già tra le domande viste, la notifica non partiva.
            DemoStep.QUESTION -> at(deployId) { it.copy(state = SessionState.WAITING, since = t, question = originalQuestion?.copy(id = "${originalQuestion.id}-$t", askedAt = t)) }
            DemoStep.DEPLOYED -> at(deployId) {
                it.copy(
                    state = SessionState.IDLE, question = null, since = t, followed = true,
                    outcome = Outcome(
                        "Deployed 2.8.0, smoke tests green",
                        "Deployed 2.8.0 to production. Smoke tests are green on checkout, refunds and webhooks; error rate unchanged after ten minutes.", t,
                    ),
                )
            }
            DemoStep.FOLLOWUP -> {
                screenGrowth = 0; growing = true; tick = 0
                at(deployId) { it.copy(state = SessionState.BUSY, turnStarted = t, since = t, tool = "Bash", toolNote = "Update the changelog and tag the release") }
            }
            // Il Terminale chiede una cattura nuova solo quando la sessione cambia: ogni tick le dà il passo successivo.
            // Senza lavoro in corso il tick rinfresca solo l'ora dello stato: dopo tre minuti fermi la demo risultava
            // vecchia e l'app spegneva i tasti (prova 17/09 07:41).
            DemoStep.TICK -> if (!growing) s.copy(ts = t) else {
                val (note, tool) = ticks[tick % ticks.size]; tick++
                at(deployId) { it.copy(since = t, tool = tool, toolNote = note) }
            }
            // La Panoramica del film arriva DOPO che la domanda del deploy è stata risposta: la domanda aperta che mostra
            // deve essere di un'altra sessione, se no si vede come aperta una cosa già chiusa (Franz, 21/09 08:44).
            DemoStep.SHOPASK -> at(shopId) {
                it.copy(
                    state = SessionState.WAITING, since = t,
                    question = originalQuestion?.copy(
                        id = "shop-$t", askedAt = t,
                        text = "The checkout tests pass. Ship the Stripe webhook change?",
                    ),
                )
            }
            DemoStep.BLOG -> at(blogId) { it.copy(state = SessionState.BUSY, turnStarted = t, since = t, toolNote = "Draft a post about the 2.8.0 release") }
            // L'ultima scena del film («Start the next one»): una sessione appena nata, su un lavoro NUOVO — non una
            // correzione (Franz, 19/09 12:06). Il testo sta in due righe e lascia una barra di quota sulla tile.
            DemoStep.NEW -> s.copy(ts = t, sessions = Order.sessions(s.sessions.map {
                if (it.id == newId) it.copy(
                    state = SessionState.BUSY, question = null, since = t - 120, turnStarted = t - 120, followed = false,
                    tool = "Edit", toolNote = "Sketch the new pricing page for the App Store listing",
                ) else it.copy(state = SessionState.IDLE, question = null, followed = false)
            }))
        }
    }

    fun useFixture(name: String) {
        val s = ContractJson.decodeState(load(name))
        current.value = if (name.endsWith("stale")) s else rebase(s)
    }

    private fun rebase(s: State): State {
        val d = now() - s.ts
        fun sh(t: Long?) = t?.plus(d)
        return s.copy(ts = s.ts + d, sessions = s.sessions.map { ses ->
            ses.copy(since = ses.since + d, turnStarted = sh(ses.turnStarted),
                question = ses.question?.let { it.copy(askedAt = it.askedAt + d) },
                outcome = ses.outcome?.let { it.copy(at = it.at + d) })
        },
            // Demo per i video (16/09 16:05): la finestra delle 5 ore è in corso da tre ore, così il ritmo ha una linea e una
            // proiezione; la settimana e l'ultimo uso dei progetti seguono lo stesso spostamento delle sessioni.
            quota = s.quota.mapValues { (_, q) ->
                q.copy(
                    resetH5 = if (q.h5 != null && q.resetH5 != null) now() + DEMO_WINDOW_LEFT_S else sh(q.resetH5),
                    resetW7 = sh(q.resetW7),
                )
            },
            projects = s.projects.map { it.copy(lastUsed = sh(it.lastUsed)) },
        )
    }

    /**
     * I campioni della quota delle 5 ore che l'orologio avrebbe registrato nelle ultime tre ore: salgono fino alla quota di
     * adesso. Servono solo alla demo, dove lo stato non cambia mai e il ritmo resterebbe senza linea.
     */
    fun demoQuotaSamples(): Map<String, List<it.pixelbox.cmwatch.rules.QuotaHistory.Sample>> {
        val s = current.value
        return s.quota.filter { (_, q) -> q.h5 != null && q.resetH5 != null }.mapValues { (_, q) ->
            val fine = now()
            val inizio = maxOf(q.resetH5!! - it.pixelbox.cmwatch.rules.QuotaHistory.WINDOW_S, fine - 3 * 3600) + 600
            val passi = 9
            (0..passi).map { i ->
                val ts = inizio + (fine - inizio) * i / passi
                it.pixelbox.cmwatch.rules.QuotaHistory.Sample(ts, (q.h5!! * (i + 1) / (passi + 1)))
            }
        }
    }

    /** Gli eventi della fixture portati ad adesso, più un giro di eventi sparsi lungo la giornata per le colonne di «Oggi». */
    private fun demoEvents(fixture: List<Event>, base: State): List<Event> {
        // Il più recente della fixture cade adesso: gli eventi restano tutti, con le stesse distanze, e nessuno nel futuro.
        val d = now() - (fixture.maxOfOrNull { it.ts } ?: base.ts)
        val nomi = base.sessions.map { it.name }
        val tipi = listOf(EventKind.OUTCOME, EventKind.LAUNCHED, EventKind.ANSWERED, EventKind.QUESTION)
        val sparsi = (1..14).map { k ->
            val nome = nomi[k % nomi.size]
            Event(key = "demo-$k", kind = tipi[k % tipi.size], session = nome, ts = now() - k * 47L * 60, title = nome)
        }
        return (fixture.map { it.copy(ts = it.ts + d) } + sparsi).sortedByDescending { it.ts }
    }

    override suspend fun fetchState(): State = current.value

    override suspend fun send(cmd: Cmd): CmdResult {
        results[cmd.id]?.let { return it }
        val s = current.value
        val ses = s.sessions.firstOrNull { it.name == cmd.session }
        fun ok(text: String) = CmdResult(cmd.id, true, text, now())
        fun ko(text: String) = CmdResult(cmd.id, false, text, now())
        val r = when (cmd.op) {
            CmdOp.ANSWER -> {
                val q = ses?.question
                val opt = q?.options?.firstOrNull { it.n.toString() == cmd.arg }
                when {
                    ses == null -> ko("no session ${cmd.session}")
                    q == null -> ko("${ses.name} has no question")
                    // Contratto 1.10: «Type something.» e «Chat about this», con i testi del relay.
                    cmd.arg?.startsWith("text:") == true -> {
                        val t = cmd.arg.removePrefix("text:")
                        if (t.isBlank()) ko("empty text") else {
                            replace(ses.copy(question = null, state = SessionState.BUSY, since = now(), turnStarted = now(), tool = "Bash", toolNote = t))
                            ok("answered ${q.options.size + 1}. $t")
                        }
                    }
                    cmd.arg == "chat" -> {
                        replace(ses.copy(question = null, state = SessionState.IDLE, since = now()))
                        ok("answered ${q.options.size + 2}. Chat about this")
                    }
                    cmd.arg?.toIntOrNull() == null -> ko("answer ${cmd.arg}: expected a number, text:<text> or chat")
                    else -> {
                        // Dopo la risposta la sessione dice cosa sta facendo, non «turno in corso» (Franz, 18/09 07:39).
                        replace(ses.copy(question = null, state = SessionState.BUSY, since = now(), turnStarted = now(), tool = "Bash", toolNote = "Deploying 2.8.0 to production"))
                        ok("answered ${cmd.arg}. ${opt?.label ?: cmd.arg}")
                    }
                }
            }
            CmdOp.PROMPT -> if (ses == null) ko("no session ${cmd.session}") else {
                replace(ses.copy(state = SessionState.BUSY, question = null, turnStarted = now(), tool = "Bash", toolNote = cmd.text ?: ses.toolNote)); ok("delivered")
            }
            CmdOp.LAUNCH -> s.projects.firstOrNull { it.path == cmd.arg }?.let { p ->
                // Demo (piano 16/09): la sessione del progetto si mette al lavoro, così la storia arriva sulla sua Scheda.
                current.value = s.copy(ts = now(), sessions = Order.sessions(s.sessions.map {
                    if (it.name == p.name) it.copy(state = SessionState.BUSY, question = null, turnStarted = now(), since = now(), tool = "Bash", toolNote = cmd.text ?: it.toolNote) else it
                }))
                CmdResult(cmd.id, true, "launched ${p.name} (${p.account})", now(), session = p.name)
            } ?: ko("unknown project")
            CmdOp.FOLLOW -> if (ses == null) ko("no session ${cmd.session}") else {
                current.value = s.copy(sessions = s.sessions.map { it.copy(followed = it.name == ses.name) }); ok("following ${ses.name}")
            }
            CmdOp.UNFOLLOW -> { current.value = s.copy(sessions = s.sessions.map { it.copy(followed = false) }); ok("unfollowed") }
            CmdOp.RESUME -> if (ses?.state == SessionState.GONE) ko("${ses.name} is gone: use launch") else ok("resumed")
            CmdOp.SCREEN -> if (ses == null) ko("no session ${cmd.session}") else {
                // Dopo il passo FOLLOWUP il terminale cresce a ogni cattura: nel video le righe arrivano dal vivo.
                // Nel video il terminale deve mostrare quello che Claude SCRIVE mentre lavora — le sue frasi e le chiamate
                // agli strumenti — non un log di shell né la risposta finale (Franz, 20/09 17:23). Righe sotto i 28
                // caratteri, la larghezza del polso.
                val extra = listOf("⏺ Updating the changelog", "Edit(CHANGELOG.md)", "⏺ Tagging the release", "Bash(git tag v2.8.0)", "⏺ Pushing the tag", "Bash(git push --tags)")
                val righe = if (ses.id == deployId && growing) extra.take(screenGrowth++.coerceAtMost(extra.size)) else listOf("Edit app/admin.py", "Read app/seed.py")
                ok((listOf("⏺ Reading the checklist", "Read(RELEASE.md)") + righe).joinToString("\n"))
            }
            CmdOp.ALLOW_ALL -> ko("no «don't ask again» option on this question")
            CmdOp.LAST -> ses?.outcome?.full?.takeIf { it.isNotBlank() }?.let { ok(it) } ?: ko("${cmd.session}: nessun messaggio da leggere")
            // Contratto 1.9: i testi del relay, di successo e di rifiuto.
            CmdOp.REOPEN -> when {
                ses == null -> ko("${cmd.session} is not a closed session")
                ses.state != SessionState.GONE -> ko("${ses.name} is already running")
                else -> ok("reopened ${ses.name} (${ses.account}): last conversation in the folder")
            }
            // Contratto 1.12: la demo non cambia modello né effort, lo dice come farebbe il relay con una sessione occupata.
            CmdOp.MODEL, CmdOp.EFFORT -> ko("${cmd.session}: not available in demo")
        }
        results[cmd.id] = r
        return r
    }

    private fun replace(ses: Session) {
        val s = current.value
        current.value = s.copy(ts = now(), sessions = Order.sessions(s.sessions.map { if (it.id == ses.id) ses else it }))
    }

    override suspend fun pair(code: String, deviceName: String): PairingInfo {
        if (!code.matches(Regex("\\d{6}"))) throw TransportException.Network("bad code")
        return PairingInfo(uid = "fake-$deviceName", host = current.value.host)
    }
}

private const val DEMO_WINDOW_LEFT_S = 2L * 3600
