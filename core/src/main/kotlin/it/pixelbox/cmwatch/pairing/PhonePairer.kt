package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.crypto.Pairing
import it.pixelbox.cmwatch.transport.Rtdb
import it.pixelbox.cmwatch.transport.TransportException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.security.KeyPair

data class WatchPeer(val uid: String, val name: String)

class PhonePairResult(val key: ByteArray, val host: String, val uids: List<String>)

sealed class PairError(msg: String) : Exception(msg) {
    class Expired : PairError("code expired")
    /** Il nodo non c'è: QR già usato, scaduto e spazzato, o di un altro relay. */
    class Unknown : PairError("no such pairing")
    class NoConfirm : PairError("the PC did not confirm")
    class BadConfirm : PairError("PC check failed")
    class Network(msg: String) : PairError(msg)
}

/** Passi 4 e 5 del flusso (design 24/09): la risposta in `/pair/<id>/watch` con gli uid di telefono e orologio, poi la conferma del PC. */
class PhonePairer(
    private val rtdb: Rtdb,
    private val deviceKeyPair: () -> KeyPair = { Pairing.newKeyPair() },
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
    private val pollMs: Long = 1000,
    private val timeoutMs: Long = 30_000,
) {
    suspend fun pair(qr: PairQr, phoneUid: String, phoneName: String, watch: WatchPeer?): PhonePairResult {
        if (qr.expired(now())) throw PairError.Expired()
        try {
            rtdb.get("pair/${qr.i}") ?: throw PairError.Unknown()
            val kp = deviceKeyPair()
            val key = Pairing.sharedKey(kp.private, qr.c)
            rtdb.put("pair/${qr.i}/watch", response(Pairing.publicB64(kp), phoneUid, phoneName, Pairing.checkCode(key, qr.i), watch).toString())
            val ok = withTimeoutOrNull(timeoutMs) {
                while (true) {
                    rtdb.get("pair/${qr.i}/ok")?.let { return@withTimeoutOrNull Json.parseToJsonElement(it).jsonObject }
                    delay(pollMs)
                }
                @Suppress("UNREACHABLE_CODE") null
            } ?: throw PairError.NoConfirm()
            if (ok["check"]?.jsonPrimitive?.content != Pairing.checkCode(key, "${qr.i}:pc")) throw PairError.BadConfirm()
            return PhonePairResult(key, ok["host"]?.jsonPrimitive?.content ?: qr.h, listOfNotNull(phoneUid, watch?.uid))
        } catch (e: TransportException) {
            throw PairError.Network(e.message ?: "network")
        }
    }

    companion object {
        /** Il nodo si chiama ancora `watch`, per compatibilità con il relay: lo scrive il telefono per tutti e due. */
        fun response(pub: String, phoneUid: String, phoneName: String, check: String, watch: WatchPeer?): JsonObject = buildJsonObject {
            put("watch_pub", pub)
            put("uid", phoneUid)
            put("name", phoneName)
            put("check", check)
            putJsonArray("uids") { add(phoneUid); watch?.let { add(it.uid) } }
            putJsonObject("names") { put(phoneUid, phoneName); watch?.let { put(it.uid, it.name) } }
        }
    }
}
