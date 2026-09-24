package it.pixelbox.cmwatch.mobile.pair

import it.pixelbox.cmwatch.pairing.FirebaseConfig
import it.pixelbox.cmwatch.pairing.PairQr
import it.pixelbox.cmwatch.pairing.PhonePairResult
import it.pixelbox.cmwatch.pairing.WatchPeer
import it.pixelbox.cmwatch.transport.Rtdb

data class WatchNode(val id: String, val name: String)

/** Il canale di Wear OS verso l'orologio. */
interface WatchLink {
    /** Un orologio raggiungibile con la nostra app (capacità `cmwatch_wear`). */
    suspend fun find(): WatchNode?
    /** Un orologio collegato qualsiasi: distingue «nessun orologio» da «manca l'app». */
    suspend fun anyConnected(): WatchNode?
    suspend fun request(node: WatchNode, path: String, body: ByteArray): ByteArray
    suspend fun openPlayOnWatch(node: WatchNode): Boolean
}

sealed class Ensure {
    data class Ready(val uid: String) : Ensure()
    /** Gira già un altro progetto Firebase: si riparte dal QR salvato. */
    data object Restart : Ensure()
    data object Failed : Ensure()
}

/** Firebase sul telefono: avviato con la configurazione del QR e con l'accesso anonimo fatto. */
interface PhoneFirebase {
    suspend fun ensure(cfg: FirebaseConfig): Ensure
    fun rtdb(): Rtdb
}

/** K sotto la chiave del Keystore del telefono. */
interface KeyWrap {
    fun wrap(key: ByteArray): String
    fun unwrap(wrapped: String): ByteArray
}

/** I passi 4 e 5 (PhonePairer), sostituibili nei test. */
fun interface PcPairer {
    suspend fun pair(qr: PairQr, phoneUid: String, phoneName: String, watch: WatchPeer?): PhonePairResult
}
