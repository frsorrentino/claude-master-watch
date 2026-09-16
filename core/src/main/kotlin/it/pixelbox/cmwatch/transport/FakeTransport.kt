package it.pixelbox.cmwatch.transport

import it.pixelbox.cmwatch.contract.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

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
                            replace(ses.copy(question = null, state = SessionState.BUSY, since = now(), turnStarted = now()))
                            ok("answered ${q.options.size + 1}. $t")
                        }
                    }
                    cmd.arg == "chat" -> {
                        replace(ses.copy(question = null, state = SessionState.IDLE, since = now()))
                        ok("answered ${q.options.size + 2}. Chat about this")
                    }
                    cmd.arg?.toIntOrNull() == null -> ko("answer ${cmd.arg}: expected a number, text:<text> or chat")
                    else -> {
                        replace(ses.copy(question = null, state = SessionState.BUSY, since = now(), turnStarted = now()))
                        ok("answered ${cmd.arg}. ${opt?.label ?: cmd.arg}")
                    }
                }
            }
            CmdOp.PROMPT -> if (ses == null) ko("no session ${cmd.session}") else {
                replace(ses.copy(state = SessionState.BUSY, question = null, turnStarted = now())); ok("delivered")
            }
            CmdOp.LAUNCH -> s.projects.firstOrNull { it.path == cmd.arg }?.let { ok("launched ${it.name} (${it.account})") } ?: ko("unknown project")
            CmdOp.FOLLOW -> if (ses == null) ko("no session ${cmd.session}") else {
                current.value = s.copy(sessions = s.sessions.map { it.copy(followed = it.name == ses.name) }); ok("following ${ses.name}")
            }
            CmdOp.UNFOLLOW -> { current.value = s.copy(sessions = s.sessions.map { it.copy(followed = false) }); ok("unfollowed") }
            CmdOp.RESUME -> if (ses?.state == SessionState.GONE) ko("${ses.name} is gone: use launch") else ok("resumed")
            CmdOp.SCREEN -> if (ses == null) ko("no session ${cmd.session}") else ok("$ pytest -q tests\n42 passed in 3.1s\nEdit app/admin.py\nRead app/seed.py")
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
