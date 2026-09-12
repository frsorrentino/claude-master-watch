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
}

/** Il bus con il PC. Due implementazioni: FakeTransport (fixture del contratto) e FirebaseTransport (RTDB). */
interface Transport {
    /** Ogni cambiamento di /state, già decifrato. */
    val state: Flow<State>
    /** /events ordinati per ts decrescente. */
    val events: Flow<List<Event>>
    /** Un GET (sveglia FCM). */
    suspend fun fetchState(): State
    /** Scrive /cmd/<id> e attende /result/<id>; TransportException.Timeout dopo RESULT_TIMEOUT_MS. */
    suspend fun send(cmd: Cmd): CmdResult
    suspend fun pair(code: String, deviceName: String): PairingInfo

    companion object { const val RESULT_TIMEOUT_MS = 20_000L }
}
