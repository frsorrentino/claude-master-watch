package it.pixelbox.cmwatch.mobile.pair

import it.pixelbox.cmwatch.crypto.Pairing
import it.pixelbox.cmwatch.pairing.*
import it.pixelbox.cmwatch.settings.SettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.KeyPair

enum class Step { PHONE, WATCH, PC }
enum class StepState { WAIT, WORKING, DONE, PENDING, SKIPPED, FAILED }
enum class PairFail { EXPIRED, INVALID, NO_WATCH, WATCH_APP_MISSING, NETWORK, PC_NO_CONFIRM, WATCH_FAILED, WATCH_UID_CHANGED, FAILED }
enum class Phase { IDLE, RUNNING, DONE, FAILED, RESTART }

data class PairUi(
    val phase: Phase = Phase.IDLE,
    val steps: Map<Step, StepState> = Step.entries.associateWith { StepState.WAIT },
    val fail: PairFail? = null,
    /** L'orologio si riavvia per un progetto nuovo: non è un errore, si aspetta. */
    val restarting: Boolean = false,
    val host: String? = null,
    val watchName: String? = null,
)

/** Il flusso del telefono (design 24/09, «Flusso»): telefono, orologio, PC, poi K all'orologio. */
class PairingController(
    private val store: SettingsStore,
    private val firebase: PhoneFirebase,
    private val link: WatchLink,
    private val keys: KeyWrap,
    private val pairer: (PhoneFirebase) -> PcPairer,
    private val phoneName: String,
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
    private val restartWaitMs: Long = 3_000,
    private val newEph: () -> KeyPair = { Pairing.newKeyPair() },
) {
    private val _ui = MutableStateFlow(PairUi())
    val ui: StateFlow<PairUi> = _ui
    private val mutex = Mutex()
    private var lastQr: String? = null

    suspend fun run(text: String, withoutWatch: Boolean = false) = mutex.withLock {
        // Revisione finale (24/09): un'eccezione fuori da PairError (Keystore, base64, JSON) è un errore mostrato, non un crash.
        try { runLocked(text, withoutWatch) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { fail(PairFail.FAILED, currentStep()) }
    }

    /** Il QR salvato prima del riavvio per un altro progetto: si consuma alla lettura, così un fallimento non lo ripete a ogni avvio. */
    suspend fun resume(): Boolean {
        val qr = store.current().resumeQr ?: return false
        store.update { it.copy(resumeQr = null) }
        run(qr)
        return true
    }

    suspend fun retry(withoutWatch: Boolean = false) { lastQr?.let { run(it, withoutWatch) } }

    fun reset() { _ui.value = PairUi() }

    suspend fun installOnWatch(): Boolean = link.anyConnected()?.let { link.openPlayOnWatch(it) } ?: false

    private suspend fun runLocked(text: String, withoutWatch: Boolean) {
        lastQr = text
        _ui.value = PairUi(phase = Phase.RUNNING)
        val qr = PairQr.parse(text) ?: return fail(PairFail.INVALID, Step.PHONE)
        if (qr.expired(now())) return fail(PairFail.EXPIRED, Step.PHONE)
        val cfg = qr.f.config()

        step(Step.PHONE, StepState.WORKING)
        val phoneUid = when (val e = firebase.ensure(cfg)) {
            is Ensure.Ready -> e.uid
            Ensure.Restart -> {
                // Un altro progetto: l'accoppiamento vecchio non può vivere sul database nuovo, si butta (revisione finale, 1).
                store.update { it.copy(firebaseJson = cfg.toJson(), resumeQr = text, paired = false, wrappedKey = null, pairingJson = null) }
                _ui.value = _ui.value.copy(phase = Phase.RESTART)
                return
            }
            Ensure.Failed -> return fail(PairFail.NETWORK, Step.PHONE)
        }
        store.update { it.copy(firebaseJson = cfg.toJson(), resumeQr = null) }
        step(Step.PHONE, StepState.DONE)

        step(Step.WATCH, StepState.WORKING)
        val node = link.find()
        var hello: HelloResponse? = null
        if (node == null) {
            // Senza orologio, o con un orologio senza la nostra app, si può accoppiare il solo telefono (Franz, 25/09 06:58).
            if (withoutWatch) step(Step.WATCH, StepState.SKIPPED)
            else return fail(if (link.anyConnected() != null) PairFail.WATCH_APP_MISSING else PairFail.NO_WATCH, Step.WATCH)
        } else {
            hello = hello(node, cfg) ?: return fail(PairFail.WATCH_FAILED, Step.WATCH)
            _ui.value = _ui.value.copy(watchName = hello.name ?: node.name)
            step(Step.WATCH, StepState.DONE)
        }
        val peer = hello?.let { WatchPeer(it.uid!!, it.name ?: node!!.name) }

        step(Step.PC, StepState.WORKING)
        val result = try {
            pairer(firebase).pair(qr, phoneUid, phoneName, peer)
        } catch (e: PairError) {
            return fail(when (e) {
                is PairError.Expired -> PairFail.EXPIRED
                is PairError.Unknown -> PairFail.INVALID
                is PairError.Network -> PairFail.NETWORK
                is PairError.NoConfirm, is PairError.BadConfirm -> PairFail.PC_NO_CONFIRM
            }, Step.PC)
        }
        val record = PairingRecord(
            uids = result.uids,
            names = buildMap { put(phoneUid, phoneName); peer?.let { put(it.uid, it.name) } },
            watchUid = peer?.uid, watchName = peer?.name, watchPending = peer != null,
        )
        store.update { it.copy(paired = true, uid = phoneUid, host = result.host, wrappedKey = keys.wrap(result.key), pairingJson = record.toJson()) }
        step(Step.PC, StepState.DONE)
        _ui.value = _ui.value.copy(host = result.host)

        if (node != null && hello != null) {
            if (deliver(node, hello, result.key, result.host)) store.update { it.copy(pairingJson = record.copy(watchPending = false).toJson()) }
            else step(Step.WATCH, StepState.PENDING)
        }
        _ui.value = _ui.value.copy(phase = Phase.DONE)
    }

    /** L'orologio si è ricollegato dopo l'accoppiamento: riceve K (design 24/09, «Casi particolari»). */
    suspend fun completePending(): Boolean = mutex.withLock {
        try { completeLocked() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { false }
    }

    private suspend fun completeLocked(): Boolean {
        val s = store.current()
        val record = PairingRecord.fromJson(s.pairingJson)?.takeIf { it.watchPending } ?: return false
        val cfg = FirebaseConfig.fromJson(s.firebaseJson) ?: return false
        val key = s.wrappedKey?.let { runCatching { keys.unwrap(it) }.getOrNull() } ?: return false
        val node = link.find() ?: return false
        val hello = hello(node, cfg) ?: return false
        if (hello.uid != record.watchUid) {
            // Dati cancellati sull'orologio: l'uid nuovo non è in /allowed, K non gli serve e non gli si dà. L'orologio esce
            // dal record, così l'avviso compare una volta e non a ogni ritorno in primo piano; il telefono resta accoppiato.
            store.update { it.copy(pairingJson = record.copy(watchUid = null, watchName = null, watchPending = false).toJson()) }
            fail(PairFail.WATCH_UID_CHANGED, Step.WATCH)
            return false
        }
        val ok = deliver(node, hello, key, s.host.orEmpty())
        if (ok) store.update { it.copy(pairingJson = record.copy(watchPending = false).toJson()) }
        return ok
    }

    /** `hello`, con fino a tre attese se l'orologio si riavvia per un progetto nuovo. */
    private suspend fun hello(node: WatchNode, cfg: FirebaseConfig): HelloResponse? {
        repeat(4) {
            val body = runCatching {
                link.request(node, HandoffMessages.HELLO, HandoffMessages.encode(HelloRequest.serializer(), HelloRequest(f = cfg.compact())))
            }.getOrNull() ?: return null
            val r = HandoffMessages.decode(HelloResponse.serializer(), body) ?: return null
            val eph = r.eph
            when {
                r.restart -> { _ui.value = _ui.value.copy(restarting = true); delay(restartWaitMs) }
                r.error != null || r.uid == null || eph == null || !validKey(eph) -> return null
                else -> { _ui.value = _ui.value.copy(restarting = false); return r }
            }
        }
        return null
    }

    /** Passo 6: K cifrata per l'orologio. Un `no_session` (orologio riavviato nel frattempo) rifà `hello` una volta. */
    private suspend fun deliver(node: WatchNode, hello: HelloResponse, key: ByteArray, host: String, retry: Boolean = true): Boolean {
        val eph = newEph()
        val req = KeyRequest(host = host, eph = Pairing.publicB64(eph), box = Handoff.seal(key, eph.private, hello.eph!!, hello.uid!!))
        val body = runCatching { link.request(node, HandoffMessages.KEY, HandoffMessages.encode(KeyRequest.serializer(), req)) }.getOrNull() ?: return false
        val r = HandoffMessages.decode(KeyResponse.serializer(), body) ?: return false
        if (r.ok) return true
        if (retry && r.error == HandoffMessages.ERR_NO_SESSION) {
            val cfg = FirebaseConfig.fromJson(store.current().firebaseJson) ?: return false
            val again = hello(node, cfg) ?: return false
            if (again.uid != hello.uid) return false
            return deliver(node, again, key, host, retry = false)
        }
        return false
    }

    /** La chiave temporanea dell'orologio deve essere 32 byte in base64: altrimenti `Handoff.seal` esploderebbe dopo l'accoppiamento. */
    private fun validKey(b64: String): Boolean = runCatching { Pairing.rawFromB64(b64).size == 32 }.getOrDefault(false)

    private fun currentStep(): Step = Step.entries.lastOrNull { _ui.value.steps[it] == StepState.WORKING } ?: Step.PHONE

    private fun step(s: Step, st: StepState) { _ui.value = _ui.value.copy(steps = _ui.value.steps + (s to st)) }

    private fun fail(f: PairFail, at: Step) {
        _ui.value = _ui.value.copy(phase = Phase.FAILED, fail = f, steps = _ui.value.steps + (at to StepState.FAILED))
    }
}
