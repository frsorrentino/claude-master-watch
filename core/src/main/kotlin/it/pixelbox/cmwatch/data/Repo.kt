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
import kotlinx.coroutines.flow.first
import it.pixelbox.cmwatch.rules.ChatRules
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

/** SENDING = si sta scrivendo; SENT = il comando è sul canale, il PC non ha ancora risposto; QUEUED = senza rete. */
enum class PendingStatus { SENDING, SENT, QUEUED, FAILED }
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
    /** Contratto 1.22: "phone" o "watch" su ogni comando. */
    private val device: String? = null,
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
    /** La coda senza rete è piena: il comando non parte e il chiamante lo deve dire. */
    class QueueFull : Exception("offline queue full")

    private val _uploads = MutableStateFlow<Map<String, ChatRules.Upload>>(emptyMap())
    /** Le immagini in caricamento o rifiutate, per id del messaggio: la chat ne mostra il passaggio e il motivo. */
    val uploads: StateFlow<Map<String, ChatRules.Upload>> = _uploads
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
        // Solo se non è già arrivato uno stato più nuovo: una sveglia FCM a freddo può precedere la lettura (revisione 29/09).
        store.loadState()?.let { (s, _) -> _snapshot.update { if (it.state != null) it else it.copy(state = s, freshness = Freshness.of(s.ts, now())) } }
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
    suspend fun command(op: CmdOp, session: String?, arg: String?, text: String? = null, id: String = UUID.randomUUID().toString()): String {
        val cmd = Cmd(id, op, session, arg, now(), by, text = text, device = device)
        if (!online()) {
            // Le letture delle schermate non entrano nella coda dei comandi dell'utente (revisione 30/09).
            if (op in PASSIVE) throw TransportException.Network("offline")
            enqueue(cmd); return cmd.id
        }
        dispatch(cmd)
        return cmd.id
    }

    /**
     * Contratto 1.19, «Condividi»: prima l'immagine cifrata in /share/<id>, poi `report` con quell'id; senza immagine solo il
     * comando. Senza rete non si accoda: l'immagine non avrebbe dove stare, e il chiamante lo dice all'utente.
     */
    suspend fun report(session: String, text: String?, mime: String?, image: ByteArray?, maxBytes: Int, id: String = UUID.randomUUID().toString()): String {
        // Il caricamento dell'immagine è un passaggio visibile, e un rifiuto si dice subito con il motivo (30/09 22:13).
        val fail = { e: Exception -> _uploads.update { it + (id to ChatRules.Upload.Failed(e.message ?: "upload failed")) } }
        if (!online()) { val e = TransportException.Network("offline"); fail(e); throw e }
        val shareId = image?.let { bytes ->
            _uploads.update { it + (id to ChatRules.Upload.Going) }
            val sid = UUID.randomUUID().toString()
            try { transport.share(sid, mime ?: "image/jpeg", bytes, maxBytes) } catch (e: Exception) { fail(e); throw e }
            _uploads.update { it - id }
            sid
        }
        return command(CmdOp.REPORT, session, shareId, text?.takeIf { it.isNotBlank() }, id = id)
    }

    /** L'esito di `deliver`: il risultato del PC, oppure niente (senza rete, o nessuna risposta entro il limite). */
    sealed interface Delivery {
        data class Done(val result: CmdResult) : Delivery
        data object NotSent : Delivery
    }

    /**
     * Un invio che aspetta il suo esito, per il lavoro in background (invio programmato, revisione finale 01/10): senza
     * rete non entra nella coda offline, che scarta dopo 10 minuti, e il chiamante può riprovare con lo stesso id (il PC
     * ignora i duplicati). Aspetta anche l'apertura da Room, così non sovrascrive i comandi in sospeso salvati.
     */
    suspend fun deliver(op: CmdOp, session: String?, arg: String?, id: String): Delivery {
        loaded.await()
        if (!online()) return Delivery.NotSent
        dispatch(Cmd(id, op, session, arg, now(), by, device = device))
        snapshot.first { s -> s.pending.none { it.cmd.id == id && (it.status == PendingStatus.SENDING || it.status == PendingStatus.SENT) } }
        val r = _resultsById.value[id]
        if (r == null) forget(id)
        return r?.let { Delivery.Done(it) } ?: Delivery.NotSent
    }

    private suspend fun enqueue(cmd: Cmd) {
        val queued = _snapshot.value.pending.filter { it.status == PendingStatus.QUEUED }.map { it.cmd }
        if (queued.size >= MAX_QUEUE) { _notices.tryEmit(Notice.QueueFull); throw QueueFull() }
        _snapshot.update { it.copy(pending = it.pending + Pending(cmd, PendingStatus.QUEUED)) }
        store.savePending(queued + cmd)
    }

    private fun dispatch(cmd: Cmd) {
        _snapshot.update { it.copy(pending = it.pending.filter { p -> p.cmd.id != cmd.id } + Pending(cmd, PendingStatus.SENDING)) }
        if (cmd.op in OPTIMISTIC) optimistic(cmd)
        jobs[cmd.id]?.cancel()
        jobs[cmd.id] = scope.launch {
            val r = try {
                withTimeout(Transport.RESULT_TIMEOUT_MS) {
                    transport.send(cmd) {
                        _snapshot.update { it.copy(pending = it.pending.map { p -> if (p.cmd.id == cmd.id && p.status == PendingStatus.SENDING) p.copy(status = PendingStatus.SENT) else p }) }
                    }
                }
            } catch (e: TimeoutCancellationException) { null } catch (e: TransportException) { null }
            if (r == null) {
                _snapshot.update { it.copy(pending = it.pending.map { p -> if (p.cmd.id == cmd.id) p.copy(status = PendingStatus.FAILED) else p }) }
            } else {
                _snapshot.update { it.copy(pending = it.pending.filter { p -> p.cmd.id != cmd.id }) }
                _resultsById.update { m -> (m + (r.id to r)).entries.toList().takeLast(MAX_RESULTS).associate { e -> e.key to e.value } }
                _results.emit(r)
                if (cmd.op !in PASSIVE) _userResults.emit(r)
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
        /** Le letture che le schermate fanno da sole (terminale, chat): i loro risultati non sono azioni dell'utente. */
        private val PASSIVE = setOf(CmdOp.SCREEN, CmdOp.LAST, CmdOp.TRANSCRIPT)
    }
}
