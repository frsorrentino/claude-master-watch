package it.pixelbox.cmwatch.transport

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.crypto.Blob
import it.pixelbox.cmwatch.crypto.BlobException
import it.pixelbox.cmwatch.crypto.Pairing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.security.KeyPair

/**
 * Il bus reale: RTDB `/state` (stream), `/events` (GET + stream), `/cmd/<uuid>` → `/result/<uuid>` (poll),
 * `/pair/<code>` per il pairing X25519. Ogni documento è un blob cifrato con la chiave del pairing.
 */
class FirebaseTransport(
    private val rtdb: Rtdb,
    private val key: () -> ByteArray?,
    private val uid: () -> String?,
    private val deviceKeyPair: () -> KeyPair,
    private val now: () -> Long,
    private val resultTimeoutMs: Long = Transport.RESULT_TIMEOUT_MS,
    private val slashTimeoutMs: Long = Transport.SLASH_RESULT_TIMEOUT_MS,
    private val pollMs: Long = 1000,
    private val backoffMs: List<Long> = listOf(1000, 2000, 5000, 15000, 30000),
    private val pairTimeoutMs: Long = 30_000,
) : Transport {

    private fun k(): ByteArray = key() ?: throw TransportException.NotPaired()

    private fun open(doc: JsonElement): String = try { Blob.open(doc.toString(), k()) } catch (e: BlobException) {
        throw TransportException.Network("cannot decrypt: ${e.message}")
    }

    private fun seal(plain: String): String = Blob.seal(plain, k())

    /** `{"path":"/","data":{…}}` → path e data. */
    private fun putEvent(data: String): Pair<String, JsonElement>? {
        val o = runCatching { Json.parseToJsonElement(data).jsonObject }.getOrNull() ?: return null
        val path = o["path"]?.jsonPrimitive?.content ?: return null
        val d = o["data"] ?: return null
        return path to d
    }

    private fun isBlob(e: JsonElement) = e is JsonObject && e.containsKey("enc")

    /** Riconnessione con backoff 1-2-5-15-30 s (design, sezione 4). */
    private fun <T> resilient(block: suspend kotlinx.coroutines.flow.FlowCollector<T>.() -> Unit): Flow<T> = flow {
        var attempt = 0
        while (true) {
            try { block(); attempt = 0 } catch (e: CancellationException) { throw e } catch (e: Exception) { /* riprova */ }
            delay(backoffMs[minOf(attempt, backoffMs.size - 1)]); attempt++
        }
    }

    override val state: Flow<State> = resilient {
        var marked = false
        rtdb.stream("state").collect { ev ->
            if (ev.event != "put" && ev.event != "patch") return@collect
            val (path, d) = putEvent(ev.data) ?: return@collect
            if (path == "/" && isBlob(d)) {
                val st = ContractJson.decodeState(open(d))
                if (!marked) { marked = true; markSeen() }   // prima di emit: chi prende solo il primo stato chiude il flusso
                emit(st)
            }
        }
    }

    /**
     * Contratto 1.20 (R6): «ho ricevuto», con l'ora del server, così il relay ripiega su Telegram solo se nessun dispositivo
     * legge. Mai d'ostacolo alla lettura: con le regole RTDB precedenti la scrittura viene rifiutata e si va avanti.
     */
    private suspend fun markSeen() {
        val id = uid() ?: return
        runCatching { rtdb.put("seen/$id", "{\".sv\":\"timestamp\"}") }
    }

    override val events: Flow<List<Event>> = resilient {
        val all = LinkedHashMap<String, Event>()
        fun decode(e: JsonElement): Event? = if (isBlob(e)) runCatching { ContractJson.json.decodeFromString(Event.serializer(), open(e)) }.getOrNull() else null
        fun snapshot() = all.values.sortedByDescending { it.ts }
        rtdb.get("events", mapOf("orderBy" to "\"\$key\"", "limitToLast" to "200"))?.let { body ->
            (Json.parseToJsonElement(body) as? JsonObject)?.forEach { (key, v) -> decode(v)?.let { all[key] = it } }
        }
        emit(snapshot())
        rtdb.stream("events").collect { ev ->
            if (ev.event != "put" && ev.event != "patch") return@collect
            val (path, d) = putEvent(ev.data) ?: return@collect
            when {
                path == "/" && d is JsonObject -> { all.clear(); d.forEach { (key, v) -> decode(v)?.let { all[key] = it } } }
                path.count { it == '/' } == 1 -> decode(d)?.let { all[path.removePrefix("/")] = it }
                else -> return@collect
            }
            emit(snapshot())
        }
    }

    override suspend fun fetchState(): State {
        k()   // non accoppiato: errore subito, senza rete
        val body = rtdb.get("state") ?: throw TransportException.Network("no state on the bus")
        return ContractJson.decodeState(open(Json.parseToJsonElement(body))).also { markSeen() }
    }

    /** Busta come /cmd, in chiaro {mime, data}; il limite vale sulla stringa `enc` (contratto 1.19). Nulla si scrive oltre il limite. */
    override suspend fun share(id: String, mime: String, data: ByteArray, maxBytes: Int, name: String?) {
        val plain = kotlinx.serialization.json.buildJsonObject {
            put("mime", kotlinx.serialization.json.JsonPrimitive(mime))
            put("data", kotlinx.serialization.json.JsonPrimitive(java.util.Base64.getEncoder().encodeToString(data)))
            // Contratto 1.28: il nome di un file, che il relay ripulisce.
            name?.let { put("name", kotlinx.serialization.json.JsonPrimitive(it)) }
        }.toString()
        val doc = seal(plain)
        val enc = Json.parseToJsonElement(doc).jsonObject.getValue("enc").jsonPrimitive.content
        if (enc.length > maxBytes) throw TransportException.TooLarge(enc.length, maxBytes)
        rtdb.put("share/$id", doc)
    }

    /** Busta come /share, in chiaro {mime, data}; letto, si cancella (il relay toglie dopo 10 minuti quelli non letti). */
    override suspend fun fetchFile(id: String): FileBlob? {
        val body = rtdb.get("file/$id") ?: return null
        val plain = Json.parseToJsonElement(open(Json.parseToJsonElement(body))).jsonObject
        runCatching { rtdb.delete("file/$id") }
        return FileBlob(plain.getValue("mime").jsonPrimitive.content, java.util.Base64.getDecoder().decode(plain.getValue("data").jsonPrimitive.content))
    }

    override suspend fun send(cmd: Cmd, onWritten: () -> Unit): CmdResult {
        k()
        rtdb.put("cmd/${cmd.id}", seal(ContractJson.encode(cmd)))
        onWritten()
        val r = withTimeoutOrNull(if (cmd.op == CmdOp.SLASH) slashTimeoutMs else resultTimeoutMs) {
            while (true) {
                rtdb.get("result/${cmd.id}")?.let { return@withTimeoutOrNull ContractJson.decodeResult(open(Json.parseToJsonElement(it))) }
                delay(pollMs)
            }
            @Suppress("UNREACHABLE_CODE") null
        }
        return r ?: throw TransportException.Timeout(cmd.id)
    }

    /**
     * Pairing: legge `/pair/<code>` (pc_pub, host), deriva la chiave, scrive `/pair/<code>/watch` (watch_pub, uid, name, check),
     * attende `/pair/<code>/ok` dal PC e ne verifica il check (`code + ":pc"`). La chiave torna in PairingInfo.key.
     */
    override suspend fun pair(code: String, deviceName: String): PairingInfo {
        android.util.Log.i("cmwatch", "pair: GET /pair/<code>")
        val body = rtdb.get("pair/$code") ?: throw TransportException.Network("no such code")
        android.util.Log.i("cmwatch", "pair: document found, uid=${uid()}")
        val o = Json.parseToJsonElement(body).jsonObject
        val pcPub = o["pc_pub"]?.jsonPrimitive?.content ?: throw TransportException.Network("bad pairing document")
        val host = o["host"]?.jsonPrimitive?.content ?: ""
        o["exp"]?.jsonPrimitive?.longOrNull?.let { if (now() > it) throw TransportException.Network("code expired") }
        val kp = deviceKeyPair()
        val shared = Pairing.sharedKey(kp.private, pcPub)
        val myUid = uid() ?: throw TransportException.Network("no uid")
        val watch = JsonObject(mapOf(
            "watch_pub" to JsonPrimitive(Pairing.publicB64(kp)), "uid" to JsonPrimitive(myUid),
            "name" to JsonPrimitive(deviceName), "check" to JsonPrimitive(Pairing.checkCode(shared, code)),
        ))
        rtdb.put("pair/$code/watch", watch.toString())
        android.util.Log.i("cmwatch", "pair: /watch written, waiting for /ok")
        val ok = withTimeoutOrNull(pairTimeoutMs) {
            while (true) {
                rtdb.get("pair/$code/ok")?.let { return@withTimeoutOrNull Json.parseToJsonElement(it).jsonObject }
                delay(pollMs)
            }
            @Suppress("UNREACHABLE_CODE") null
        } ?: throw TransportException.Network("the PC did not confirm")
        val pcCheck = ok["check"]?.jsonPrimitive?.content
        if (pcCheck != Pairing.checkCode(shared, "$code:pc")) throw TransportException.Network("PC check failed")
        android.util.Log.i("cmwatch", "pair: PC check ok")
        return PairingInfo(uid = myUid, host = ok["host"]?.jsonPrimitive?.content ?: host, key = shared)
    }
}
