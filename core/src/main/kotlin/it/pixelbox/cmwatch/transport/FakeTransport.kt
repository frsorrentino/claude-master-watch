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
    private val eventList = ContractJson.decodeEvents(load("events-sample")).sortedByDescending { it.ts }
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
        })
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
