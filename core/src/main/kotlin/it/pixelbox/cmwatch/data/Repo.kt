package it.pixelbox.cmwatch.data

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.transport.Transport
import it.pixelbox.cmwatch.transport.TransportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

enum class PendingStatus { SENDING, QUEUED, FAILED }
data class Pending(val cmd: Cmd, val status: PendingStatus)
data class Snapshot(val state: State?, val freshness: Freshness, val pending: List<Pending> = emptyList())

/**
 * La verità sull'orologio: ultimo /state (subito da Room, poi dal Transport), comandi ottimistici con
 * /result entro 20 s, coda offline (10 comandi, 10 minuti). Un solo punto letto da app, tile, complication.
 */
class Repo(
    private val store: Store,
    private val transport: Transport,
    private val scope: CoroutineScope,
    private val now: () -> Long,
    private val online: () -> Boolean,
    private val by: String,
    private val freshnessTickMs: Long = FRESHNESS_TICK_MS,
) {
    private val _snapshot = MutableStateFlow(Snapshot(null, Freshness.Stale(0)))
    val snapshot: StateFlow<Snapshot> = _snapshot
    private val _quotaSamples = MutableStateFlow<Map<String, List<it.pixelbox.cmwatch.rules.QuotaHistory.Sample>>>(emptyMap())
    /** I campioni della quota delle 5 ore per account, per il ritmo della finestra nella pagina Quota. */
    val quotaSamples: StateFlow<Map<String, List<it.pixelbox.cmwatch.rules.QuotaHistory.Sample>>> = _quotaSamples
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events
    private val _results = MutableSharedFlow<CmdResult>(extraBufferCapacity = 16)
    /** Ogni /result arrivato: per aptica e avvisi. */
    val results: SharedFlow<CmdResult> = _results
    private val _resultsById = MutableStateFlow<Map<String, CmdResult>>(emptyMap())
    /** Gli ultimi risultati per id: per chi si iscrive dopo l'arrivo (es. il Terminale). */
    val resultsById: StateFlow<Map<String, CmdResult>> = _resultsById
    private val _userResults = MutableSharedFlow<CmdResult>(extraBufferCapacity = 16)
    /**
     * Solo i risultati delle azioni dell'utente (risposte, prompt, lanci, riaperture): per vibrazione e conferma. Le
     * catture che il Terminale chiede da solo ogni pochi secondi (`screen`, `last`) restano fuori (15/09 23:45).
     */
    val userResults: SharedFlow<CmdResult> = _userResults
    private val _notices = MutableSharedFlow<Notice>(extraBufferCapacity = 16)
    val notices: SharedFlow<Notice> = _notices
    private val jobs = HashMap<String, Job>()

    /** Apertura da Room: l'ultimo stato è leggibile anche senza rete, prima che il Transport risponda. */
    suspend fun loadFromStore() {
        store.loadState()?.let { (s, _) -> _snapshot.update { it.copy(state = s, freshness = Freshness.of(s.ts, now())) } }
        _events.value = store.loadEvents()
        _quotaSamples.value = store.loadQuotaSamples(now() - SAMPLES_KEEP_S)
        _snapshot.update { it.copy(pending = store.loadPending().map { c -> Pending(c, PendingStatus.QUEUED) }) }
    }

    private val loaded = CompletableDeferred<Unit>()
    private var streams: Job? = null

    /**
     * `live` = stream del transport aperti. L'app li apre solo in primo piano (Franz, 14/09 17:18, batteria): chiusa, lo
     * stato arriva dal GET della sveglia FCM e dalla tile. Il ciclo di freschezza resta: non usa la rete.
     */
    fun start(live: Boolean = true) {
        scope.launch {
            loadFromStore()
            loaded.complete(Unit)
            if (live) live(true)
        }
        if (freshnessTickMs > 0) scope.launch {
            while (isActive) {
                delay(freshnessTickMs)
                _snapshot.update { it.copy(freshness = it.state?.let { s -> Freshness.of(s.ts, now()) } ?: Freshness.Stale(0)) }
            }
        }
    }

    /**
     * Apre o chiude gli stream di /state e /events; idempotente, lo chiama il ciclo di vita del processo. Gli stream
     * partono dopo il caricamento da Room: uno stream aperto prima verrebbe sovrascritto dallo stato vecchio salvato.
     */
    @Synchronized fun live(on: Boolean) {
        if (!on) { streams?.cancel(); streams = null; return }
        if (streams?.isActive == true) return
        streams = scope.launch {
            loaded.await()
            launch { transport.state.collect { s -> accept(s) } }
            launch {
                transport.events.collect { ev ->
                    store.saveEvents(ev); store.pruneEvents(now() - EVENTS_KEEP_S); _events.value = store.loadEvents()
                }
            }
        }
    }

    private suspend fun accept(s: State) {
        val ordered = s.copy(sessions = Order.sessions(s.sessions))
        store.saveState(ordered, now())
        _snapshot.update { it.copy(state = ordered, freshness = Freshness.of(ordered.ts, now())) }
        recordQuota(ordered)
    }

    /**
     * Demo per i video: i campioni che l'orologio avrebbe registrato, perché nella demo lo stato non cambia mai. Sostituiscono
     * quelli dell'account: aggiunti, le rampe di più accensioni si intrecciavano e il grafico diventava un dente di sega
     * (ripresa della Panoramica, 21/09).
     */
    suspend fun seedQuotaSamples(samples: Map<String, List<it.pixelbox.cmwatch.rules.QuotaHistory.Sample>>) {
        for ((account, list) in samples) {
            store.clearQuotaSamples(account)
            for (s in list) store.saveQuotaSample(account, s)
        }
        _quotaSamples.value = store.loadQuotaSamples(now() - SAMPLES_KEEP_S)
    }

    /**
     * Un campione per account a ogni stato con la lettura delle 5 ore, con l'ora del PC. Uno uguale al precedente entro
     * cinque minuti non si salva: la linea non si riempie di punti fermi. Il dato vecchio (`stale`) non è una lettura.
     */
    private suspend fun recordQuota(s: State) {
        for ((account, q) in s.quota) {
            val pct = q.h5 ?: continue
            if (q.stale) continue
            val ultimo = _quotaSamples.value[account]?.lastOrNull()
            if (ultimo != null && (ultimo.ts >= s.ts || (ultimo.pct == pct && s.ts - ultimo.ts < SAMPLE_MIN_GAP_S))) continue
            store.saveQuotaSample(account, it.pixelbox.cmwatch.rules.QuotaHistory.Sample(s.ts, pct))
        }
        store.pruneQuotaSamples(now() - SAMPLES_KEEP_S)
        _quotaSamples.value = store.loadQuotaSamples(now() - SAMPLES_KEEP_S)
    }

    /** Un GET (sveglia FCM). Vero se lo stato è arrivato. */
    suspend fun refresh(): Boolean = runCatching { accept(transport.fetchState()) }.isSuccess

    suspend fun answer(session: String, n: Int) = command(CmdOp.ANSWER, session, n.toString())
    suspend fun prompt(session: String, text: String) = command(CmdOp.PROMPT, session, text)
    /** Contratto 1.10: testo libero a una domanda aperta, dalla voce «Type something.». */
    suspend fun answerText(session: String, text: String) = command(CmdOp.ANSWER, session, it.pixelbox.cmwatch.rules.QuestionRules.textArg(text))
    /** Contratto 1.10: «Chat about this». */
    suspend fun chat(session: String) = command(CmdOp.ANSWER, session, it.pixelbox.cmwatch.rules.QuestionRules.CHAT_ARG)

    /** Ritorna l'id del comando (uuid): stesso id in Riprova, il PC ignora i duplicati. */
    /** `text`: il primo messaggio di un `launch` (contratto 1.13); per gli altri comandi resta null. */
    suspend fun command(op: CmdOp, session: String?, arg: String?, text: String? = null): String {
        val cmd = Cmd(UUID.randomUUID().toString(), op, session, arg, now(), by, text = text)
        if (!online()) { enqueue(cmd); return cmd.id }
        dispatch(cmd)
        return cmd.id
    }

    private suspend fun enqueue(cmd: Cmd) {
        val queued = _snapshot.value.pending.filter { it.status == PendingStatus.QUEUED }.map { it.cmd }
        if (queued.size >= MAX_QUEUE) { _notices.tryEmit(Notice.QueueFull); return }
        _snapshot.update { it.copy(pending = it.pending + Pending(cmd, PendingStatus.QUEUED)) }
        store.savePending(queued + cmd)
    }

    private fun dispatch(cmd: Cmd) {
        _snapshot.update { it.copy(pending = it.pending.filter { p -> p.cmd.id != cmd.id } + Pending(cmd, PendingStatus.SENDING)) }
        if (cmd.op in OPTIMISTIC) optimistic(cmd)
        jobs[cmd.id]?.cancel()
        jobs[cmd.id] = scope.launch {
            val r = try {
                withTimeout(Transport.RESULT_TIMEOUT_MS) { transport.send(cmd) }
            } catch (e: TimeoutCancellationException) { null } catch (e: TransportException) { null }
            if (r == null) {
                _snapshot.update { it.copy(pending = it.pending.map { p -> if (p.cmd.id == cmd.id) p.copy(status = PendingStatus.FAILED) else p }) }
            } else {
                _snapshot.update { it.copy(pending = it.pending.filter { p -> p.cmd.id != cmd.id }) }
                _resultsById.update { m -> (m + (r.id to r)).entries.toList().takeLast(MAX_RESULTS).associate { e -> e.key to e.value } }
                _results.emit(r)
                if (cmd.op != CmdOp.SCREEN && cmd.op != CmdOp.LAST) _userResults.emit(r)
            }
        }
    }

    /** La domanda sparisce subito dallo schermo; la verità torna con /state. */
    private fun optimistic(cmd: Cmd) {
        _snapshot.update { snap ->
            val s = snap.state ?: return@update snap
            snap.copy(state = s.copy(sessions = s.sessions.map {
                if (it.name != cmd.session) it
                else when (cmd.op) {
                    // Segui e non seguire si vedono subito (Franz, 16/09 01:58): campanella e bordo non aspettano il PC,
                    // altrimenti dopo la pressione lunga non cambia niente sullo schermo. Il prossimo stato dal PC comanda.
                    CmdOp.FOLLOW -> it.copy(followed = true)
                    CmdOp.UNFOLLOW -> it.copy(followed = false)
                    else -> it.copy(question = null, state = SessionState.BUSY)
                }
            }))
        }
    }

    /** Riprova un comando «non consegnato» con lo stesso uuid. */
    suspend fun retry(id: String) {
        val p = _snapshot.value.pending.firstOrNull { it.cmd.id == id } ?: return
        dispatch(p.cmd)
    }

    fun forget(id: String) {
        _snapshot.update { it.copy(pending = it.pending.filter { p -> p.cmd.id != id }) }
    }

    /** Al ritorno della rete: i comandi in coda partono; quelli più vecchi di 10 minuti si scartano con avviso. */
    suspend fun flushQueue() {
        if (!online()) return
        val queued = _snapshot.value.pending.filter { it.status == PendingStatus.QUEUED }.map { it.cmd }
        if (queued.isEmpty()) return
        store.savePending(emptyList())
        _snapshot.update { it.copy(pending = it.pending.filter { p -> p.status != PendingStatus.QUEUED }) }
        val (fresh, old) = queued.partition { now() - it.issued <= MAX_QUEUE_AGE_S }
        if (old.isNotEmpty()) _notices.tryEmit(Notice.Dropped(old.size))
        fresh.forEach { dispatch(it) }
    }

    sealed class Notice {
        data object QueueFull : Notice()
        data class Dropped(val n: Int) : Notice()
    }

    companion object {
        const val MAX_QUEUE = 10
        const val MAX_QUEUE_AGE_S = 600L
        const val EVENTS_KEEP_S = 30L * 86400
        const val FRESHNESS_TICK_MS = 30_000L
        /** Un campione uguale al precedente entro questo tempo non si salva. */
        const val SAMPLE_MIN_GAP_S = 300L
        /** La finestra di 5 ore e un'ora in più: basta per disegnarla anche appena ripartita. */
        const val SAMPLES_KEEP_S = it.pixelbox.cmwatch.rules.QuotaHistory.WINDOW_S + 3600
        const val MAX_RESULTS = 32

        /**
         * Le azioni che si vedono prima della risposta del PC: risposta e prompt (la sessione riparte), segui e non
         * seguire (campanella e bordo). Il prossimo stato che arriva dal PC comanda comunque.
         */
        private val OPTIMISTIC = setOf(CmdOp.ANSWER, CmdOp.PROMPT, CmdOp.FOLLOW, CmdOp.UNFOLLOW)
    }
}
