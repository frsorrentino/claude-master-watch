package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.crypto.Blob
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
    /** Contratto 1.30: il PC ha già quattro dispositivi, l'aggiunta è rifiutata. */
    class Full : PairError("the PC already has four devices")
    class Network(msg: String) : PairError(msg)
}

/** Passi 4 e 5 del flusso (design 24/09): la risposta in `/pair/<id>/watch` con gli uid di telefono e orologio, poi la conferma del PC. */
class PhonePairer(
    private val rtdb: Rtdb,
    private val deviceKeyPair: () -> KeyPair = { Pairing.newKeyPair() },
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
    private val pollMs: Long = 1000,
    private val timeoutMs: Long = 30_000,
    /** Contratto 1.32: phone, tablet o chromebook; l'orologio portato con sé è sempre «watch». */
    private val kind: String = "phone",
) {
    suspend fun pair(qr: PairQr, phoneUid: String, phoneName: String, watch: WatchPeer?): PhonePairResult {
        if (qr.expired(now())) throw PairError.Expired()
        try {
            rtdb.get("pair/${qr.i}") ?: throw PairError.Unknown()
            val kp = deviceKeyPair()
            val key = Pairing.sharedKey(kp.private, qr.c)
            rtdb.put("pair/${qr.i}/watch", response(Pairing.publicB64(kp), phoneUid, phoneName, Pairing.checkCode(key, qr.i), watch, kind).toString())
            val ok = withTimeoutOrNull(timeoutMs) {
                while (true) {
                    rtdb.get("pair/${qr.i}/ok")?.let { doc ->
                        // Un `/ok` che non è un oggetto è una conferma sbagliata, non un'eccezione (revisione finale, 2).
                        return@withTimeoutOrNull runCatching { Json.parseToJsonElement(doc).jsonObject }.getOrElse { throw PairError.BadConfirm() }
                    }
                    // Contratto 1.30: con quattro dispositivi il PC risponde {"error": "full"} al posto della conferma.
                    if (qr.add) rtdb.get("pair/${qr.i}/error")?.let { e ->
                        if (runCatching { Json.parseToJsonElement(e).jsonPrimitive.content }.getOrNull() == "full") throw PairError.Full()
                    }
                    delay(pollMs)
                }
                @Suppress("UNREACHABLE_CODE") null
            } ?: throw PairError.NoConfirm()
            if (ok["check"]?.jsonPrimitive?.content != Pairing.checkCode(key, "${qr.i}:pc")) throw PairError.BadConfirm()
            // Contratto 1.30: in un'aggiunta la chiave è quella che il relay ha già, cifrata con la chiave del giro.
            val relayKey = if (qr.add) relayKey(ok, key) else key
            return PhonePairResult(relayKey, ok["host"]?.jsonPrimitive?.content ?: qr.h, listOfNotNull(phoneUid, watch?.uid))
        } catch (e: TransportException) {
            throw PairError.Network(e.message ?: "network")
        }
    }

    /** `ok.key` = la busta {v, enc} di /state con dentro {"key": "<64 cifre hex>"}; qualunque altra cosa non accoppia. */
    private fun relayKey(ok: JsonObject, pairKey: ByteArray): ByteArray {
        val env = (ok["key"] as? JsonObject) ?: throw PairError.BadConfirm()
        val hex = runCatching { Json.parseToJsonElement(Blob.open(env.toString(), pairKey)).jsonObject["key"]!!.jsonPrimitive.content }
            .getOrElse { throw PairError.BadConfirm() }
        if (!hex.matches(Regex("[0-9a-fA-F]{64}"))) throw PairError.BadConfirm()
        return ByteArray(32) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    companion object {
        /** Il nodo si chiama ancora `watch`, per compatibilità con il relay: lo scrive il telefono per tutti e due. */
        fun response(pub: String, phoneUid: String, phoneName: String, check: String, watch: WatchPeer?, kind: String = "phone"): JsonObject = buildJsonObject {
            put("watch_pub", pub)
            put("uid", phoneUid)
            put("name", phoneName)
            put("check", check)
            putJsonArray("uids") { add(phoneUid); watch?.let { add(it.uid) } }
            putJsonObject("names") { put(phoneUid, phoneName); watch?.let { put(it.uid, it.name) } }
            // Contratto 1.32: il tipo, per lo schema dei collegamenti di tutti i dispositivi.
            put("kind", kind)
            putJsonObject("kinds") { put(phoneUid, kind); watch?.let { put(it.uid, "watch") } }
        }
    }
}
