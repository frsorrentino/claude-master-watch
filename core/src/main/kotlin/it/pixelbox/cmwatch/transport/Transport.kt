package it.pixelbox.cmwatch.transport

import it.pixelbox.cmwatch.contract.Cmd
import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.State
import kotlinx.coroutines.flow.Flow

/** `key` = chiave di sessione derivata (null con il Transport finto: il chiamante ne genera una). */
data class PairingInfo(val uid: String, val host: String, val key: ByteArray? = null)

sealed class TransportException(msg: String) : Exception(msg) {
    class Timeout(id: String) : TransportException("no result for $id")
    class NotPaired : TransportException("not paired")
    class Network(msg: String) : TransportException(msg)
    /** Contratto 1.19: l'immagine cifrata supera `share.max_bytes`. */
    class TooLarge(size: Int, max: Int) : TransportException("share $size > $max")
}

/** Contratto 1.24: un file della conversazione, già decifrato. */
class FileBlob(val mime: String, val bytes: ByteArray)

/** Il bus con il PC. Due implementazioni: FakeTransport (fixture del contratto) e FirebaseTransport (RTDB). */
interface Transport {
    /** Ogni cambiamento di /state, già decifrato. */
    val state: Flow<State>
    /** /events ordinati per ts decrescente. */
    val events: Flow<List<Event>>
    /** Un GET (sveglia FCM). */
    suspend fun fetchState(): State
    /**
     * Scrive /cmd/<id> e attende /result/<id>; TransportException.Timeout dopo RESULT_TIMEOUT_MS. `onWritten` scatta
     * appena il comando è sul canale, prima del risultato: la chat lo mostra come «inviato al PC» (30/09 22:13).
     */
    suspend fun send(cmd: Cmd, onWritten: () -> Unit = {}): CmdResult
    suspend fun pair(code: String, deviceName: String): PairingInfo
    /** Contratto 1.19: scrive l'immagine cifrata in /share/<id> prima del comando `report`. */
    suspend fun share(id: String, mime: String, data: ByteArray, maxBytes: Int)
    /** Contratto 1.24: legge e cancella /file/<id> dopo il comando `file` riuscito; null se non c'è (la Demo non ha file). */
    suspend fun fetchFile(id: String): FileBlob? = null

    companion object { const val RESULT_TIMEOUT_MS = 20_000L }
}
