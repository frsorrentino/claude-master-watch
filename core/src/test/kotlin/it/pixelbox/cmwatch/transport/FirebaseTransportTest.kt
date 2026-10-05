package it.pixelbox.cmwatch.transport

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.crypto.Blob
import it.pixelbox.cmwatch.crypto.Pairing
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom
import java.util.Base64

class FirebaseTransportTest {
    private val server = MockWebServer()
    private val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
    private val store = HashMap<String, String>()          // path → JSON (RTDB finto)
    private val requests = ArrayList<RecordedRequest>()
    private var streamBody = ""
    private var clock = 1789210800L
    private var refuseSeen = false

    @Before fun up() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val path = request.requestUrl!!.encodedPath.removePrefix("/").removeSuffix(".json")
                assertEquals("t0k", request.requestUrl!!.queryParameter("auth"))
                if (request.getHeader("Accept") == "text/event-stream") {
                    return MockResponse().setHeader("Content-Type", "text/event-stream").setBody(streamBody)
                }
                if (refuseSeen && path.startsWith("seen/")) return MockResponse().setResponseCode(401)
                return when (request.method) {
                    "GET" -> MockResponse().setBody(store[path] ?: "null")
                    "PUT" -> { store[path] = request.body.readUtf8(); MockResponse().setBody(store[path]!!) }
                    "DELETE" -> { store.keys.removeIf { k -> k == path || k.startsWith("$path/") }; MockResponse().setBody("null") }
                    else -> MockResponse().setResponseCode(405)
                }
            }
        }
        server.start()
    }

    @After fun down() = server.shutdown()

    private fun blobOf(json: String) = Blob.seal(json, key)
    private fun transport(timeout: Long = 20_000, key: ByteArray? = this.key, slashTimeout: Long = 60_000) = FirebaseTransport(
        rtdb = Rtdb(server.url("/").toString().removeSuffix("/"), token = { "t0k" }),
        key = { key }, uid = { "u1" }, deviceKeyPair = { Pairing.newKeyPair() }, now = { clock },
        resultTimeoutMs = timeout, slashTimeoutMs = slashTimeout, pollMs = 10, backoffMs = listOf(10),
    )

    @Test fun stateArrivesFromTheStreamDecrypted() = runBlocking {
        streamBody = "event: put\ndata: {\"path\":\"/\",\"data\":${blobOf(Fixtures.stateQuestion)}}\n\nevent: keep-alive\ndata: null\n\n"
        val s = transport().state.first()
        assertEquals(4, s.sessions.size); assertEquals("ledger-api", s.sessions[0].name)
    }

    @Test fun fetchStateIsOneGet() = runBlocking {
        store["state"] = blobOf(Fixtures.stateIdle)
        assertEquals(1, transport().fetchState().sessions.size)
        assertEquals(listOf("/state.json"), requests.filter { it.method == "GET" }.map { it.requestUrl!!.encodedPath })
    }

    @Test fun wrongKeyIsANetworkError() {
        store["state"] = blobOf(Fixtures.stateIdle)
        val other = ByteArray(32).also { SecureRandom().nextBytes(it) }
        assertThrows(TransportException.Network::class.java) { runBlocking { transport(key = other).fetchState() } }
    }

    @Test fun notPairedWithoutKey() {
        assertThrows(TransportException.NotPaired::class.java) { runBlocking { transport(key = null).fetchState() } }
    }

    @Test fun sendWritesTheCommandAndWaitsForTheResult() = runBlocking {
        val cmd = Cmd("id-1", CmdOp.ANSWER, "ledger-api", "1", clock, "watch")
        val t = transport()
        // il PC risponde al terzo poll
        var polls = 0
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl!!.encodedPath.removePrefix("/").removeSuffix(".json")
                if (request.method == "PUT") { store[path] = request.body.readUtf8(); return MockResponse().setBody(store[path]!!) }
                if (path == "result/id-1") { polls++; return MockResponse().setBody(if (polls >= 3) blobOf("""{"id":"id-1","ok":true,"text":"answered 1. yes","at":1}""") else "null") }
                return MockResponse().setBody("null")
            }
        }
        val r = t.send(cmd)
        assertTrue(r.ok); assertEquals("answered 1. yes", r.text)
        val written = Json.parseToJsonElement(store.getValue("cmd/id-1")).jsonObject
        assertEquals(1, written.getValue("v").jsonPrimitive.content.toInt())
        assertEquals(cmd, ContractJson.json.decodeFromString(Cmd.serializer(), Blob.open(store.getValue("cmd/id-1"), key)))
    }

    @Test fun noResultIsATimeout() {
        assertThrows(TransportException.Timeout::class.java) {
            runBlocking { transport(timeout = 200).send(Cmd("id-2", CmdOp.SCREEN, "x", null, clock, "watch")) }
        }
    }

    // Dal vivo 02/10 16:30: il relay risponde a /cost dopo aver letto il pannello; lo slash aspetta più degli altri comandi.
    @Test fun aSlashCommandWaitsLongerThanTheOthers() {
        val late = blobOf("""{"id":"id-3","ok":true,"text":"sent /cost to x","at":1}""")
        val start = System.currentTimeMillis()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl!!.encodedPath.removePrefix("/").removeSuffix(".json")
                if (request.method == "PUT") { store[path] = request.body.readUtf8(); return MockResponse().setBody(store[path]!!) }
                if (path == "result/id-3" && System.currentTimeMillis() - start > 600) return MockResponse().setBody(late)
                return MockResponse().setBody("null")
            }
        }
        val t = transport(timeout = 200, slashTimeout = 5_000)
        assertEquals("sent /cost to x", runBlocking { t.send(Cmd("id-3", CmdOp.SLASH, "x", "cost", clock, "phone")) }.text)
        assertThrows(TransportException.Timeout::class.java) {
            runBlocking { t.send(Cmd("id-4", CmdOp.SCREEN, "x", null, clock, "phone")) }
        }
    }

    @Test fun eventsAreListedNewestFirst() = runBlocking {
        val ev = ContractJson.decodeEvents(Fixtures.events)
        val obj = JsonObject(ev.associate { it.key to Json.parseToJsonElement(blobOf(ContractJson.json.encodeToString(Event.serializer(), it))) })
        store["events"] = obj.toString()
        streamBody = ""
        val got = transport().events.first()
        assertEquals(9, got.size); assertTrue(got[0].ts >= got[1].ts)
        assertTrue(requests.any { it.requestUrl!!.encodedPath == "/events.json" && it.requestUrl!!.queryParameter("orderBy") == "\"\$key\"" })
    }

    @Test fun pairingDerivesTheSameKeyAsThePc() = runBlocking {
        val pc = Pairing.newKeyPair()
        store["pair/123456"] = JsonObject(mapOf("pc_pub" to JsonPrimitive(Pairing.publicB64(pc)), "host" to JsonPrimitive("crostini-demo"), "exp" to JsonPrimitive(clock + 300))).toString()
        val watch = Pairing.newKeyPair()
        val expected = Pairing.sharedKey(pc.private, Pairing.publicB64(watch))
        store["pair/123456/ok"] = JsonObject(mapOf("host" to JsonPrimitive("crostini-demo"), "check" to JsonPrimitive(Pairing.checkCode(expected, "123456:pc")))).toString()
        val t = FirebaseTransport(Rtdb(server.url("/").toString().removeSuffix("/"), { "t0k" }), { null }, { "u1" }, { watch }, { clock }, pollMs = 10, backoffMs = listOf(10))
        val info = t.pair("123456", "watch-pixel5")
        assertEquals("crostini-demo", info.host); assertEquals("u1", info.uid)
        assertArrayEquals(expected, info.key)
        val written = Json.parseToJsonElement(store.getValue("pair/123456/watch")).jsonObject
        assertEquals("watch-pixel5", written.getValue("name").jsonPrimitive.content)
        assertEquals(Pairing.checkCode(expected, "123456"), written.getValue("check").jsonPrimitive.content)
        assertEquals(32, Base64.getDecoder().decode(written.getValue("watch_pub").jsonPrimitive.content).size)
    }

    // Contratto 1.30 col codice a 6 cifre (Franz, 05/10 10:21): con `relay pair --add` la chiave non si deriva, arriva
    // cifrata in /ok; salvare quella del giro rompeva l'orologio.
    @Test fun pairingAnAddSavesTheRelayKey() = runBlocking {
        val pc = Pairing.newKeyPair()
        store["pair/123456"] = JsonObject(mapOf("pc_pub" to JsonPrimitive(Pairing.publicB64(pc)), "host" to JsonPrimitive("crostini-demo"), "exp" to JsonPrimitive(clock + 300), "mode" to JsonPrimitive("add"))).toString()
        val watch = Pairing.newKeyPair()
        val shared = Pairing.sharedKey(pc.private, Pairing.publicB64(watch))
        val relayKey = ByteArray(32) { (96 + it).toByte() }
        val env = Json.parseToJsonElement(Blob.seal("""{"key":"${relayKey.joinToString("") { "%02x".format(it) }}"}""", shared))
        store["pair/123456/ok"] = JsonObject(mapOf("host" to JsonPrimitive("crostini-demo"), "check" to JsonPrimitive(Pairing.checkCode(shared, "123456:pc")), "key" to env)).toString()
        val t = FirebaseTransport(Rtdb(server.url("/").toString().removeSuffix("/"), { "t0k" }), { null }, { "u1" }, { watch }, { clock }, pollMs = 10, backoffMs = listOf(10))
        assertArrayEquals(relayKey, t.pair("123456", "watch-pixel5").key)
        assertEquals("watch", Json.parseToJsonElement(store.getValue("pair/123456/watch")).jsonObject.getValue("kind").jsonPrimitive.content)
    }

    @Test fun pairingWithWrongPcCheckFails() {
        val pc = Pairing.newKeyPair()
        store["pair/123456"] = JsonObject(mapOf("pc_pub" to JsonPrimitive(Pairing.publicB64(pc)), "host" to JsonPrimitive("h"), "exp" to JsonPrimitive(clock + 300))).toString()
        store["pair/123456/ok"] = JsonObject(mapOf("host" to JsonPrimitive("h"), "check" to JsonPrimitive("0000000000000000"))).toString()
        assertThrows(TransportException.Network::class.java) { runBlocking { transport(key = null).pair("123456", "w") } }
    }

    @Test fun pairingWithUnknownCodeFails() {
        assertThrows(TransportException.Network::class.java) { runBlocking { transport(key = null).pair("000000", "w") } }
    }

    // Contratto 1.19: l'immagine di «Condividi» va cifrata in /share/<id> prima del comando `report`.
    @Test fun shareWritesTheSealedImage() = runBlocking {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        transport().share("s1", "image/jpeg", bytes, maxBytes = 1_500_000)
        val plain = Json.parseToJsonElement(Blob.open(store.getValue("share/s1"), key)).jsonObject
        assertEquals("image/jpeg", plain.getValue("mime").jsonPrimitive.content)
        assertArrayEquals(bytes, java.util.Base64.getDecoder().decode(plain.getValue("data").jsonPrimitive.content))
    }

    // Contratto 1.28: un file di qualunque formato porta il suo nome nel blob; senza nome la chiave non c'è.
    @Test fun shareCarriesTheFileName() = runBlocking {
        transport().share("s3", "application/pdf", byteArrayOf(9), maxBytes = 1_500_000, name = "Preventivo.pdf")
        val plain = Json.parseToJsonElement(Blob.open(store.getValue("share/s3"), key)).jsonObject
        assertEquals("Preventivo.pdf", plain.getValue("name").jsonPrimitive.content)
        transport().share("s4", "image/jpeg", byteArrayOf(9), maxBytes = 1_500_000)
        assertFalse(Json.parseToJsonElement(Blob.open(store.getValue("share/s4"), key)).jsonObject.containsKey("name"))
    }

    // Contratto 1.24: il file chiesto con `file` si legge da /file/<id del comando>, si decifra e si cancella.
    @Test fun fetchFileReadsOpensAndDeletes() = runBlocking {
        store["file/c1"] = blobOf("""{"mime":"image/png","data":"AQID"}""")
        val f = transport().fetchFile("c1")!!
        assertEquals("image/png", f.mime)
        assertArrayEquals(byteArrayOf(1, 2, 3), f.bytes)
        assertFalse(store.containsKey("file/c1"))
        assertNull(transport().fetchFile("c2"))
    }

    // Contratto 1.34: i pezzi in ordine, la misura e lo sha256 controllati, poi via tutto /file/<id>.
    @Test fun fetchFileInPartsJoinsChecksAndDeletes() = runBlocking {
        val file = "hello, parts!\n".toByteArray()
        val sha = java.security.MessageDigest.getInstance("SHA-256").digest(file).joinToString("") { "%02x".format(it) }
        store["file/c3/parts/0"] = sealBytes(file.copyOfRange(0, 8))
        store["file/c3/parts/1"] = sealBytes(file.copyOfRange(8, file.size))
        store["file/c3/meta"] = blobOf("""{"n":2,"size":${file.size},"sha256":"$sha","mime":"text/plain","name":"hello.txt"}""")
        val f = transport().fetchFile("c3")!!
        assertEquals("text/plain", f.mime)
        assertArrayEquals(file, f.bytes)
        assertTrue(store.keys.none { it.startsWith("file/c3") })
    }

    @Test fun aPartThatDoesNotMatchTheHashIsRefused() = runBlocking {
        store["file/c4/parts/0"] = sealBytes("altro".toByteArray())
        store["file/c4/meta"] = blobOf("""{"n":1,"size":5,"sha256":"00","mime":"text/plain"}""")
        try { transport().fetchFile("c4"); fail("expected a mismatch") } catch (e: TransportException.Network) { }
        assertTrue(store.keys.none { it.startsWith("file/c4") })
    }

    /** Un pezzo come lo scrive il relay: la busta dei byte grezzi (nonce, AES-GCM con lo stesso AAD). */
    private fun sealBytes(b: ByteArray): String {
        val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val c = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        c.init(javax.crypto.Cipher.ENCRYPT_MODE, javax.crypto.spec.SecretKeySpec(key, "AES"), javax.crypto.spec.GCMParameterSpec(128, nonce))
        c.updateAAD(Blob.AAD)
        val enc = java.util.Base64.getEncoder().encodeToString(nonce + c.doFinal(b))
        return """{"v":1,"enc":"$enc"}"""
    }

    @Test fun shareTooLargeIsRefusedBeforeWriting() = runBlocking {
        try { transport().share("s2", "image/jpeg", ByteArray(2_000), maxBytes = 1_000); fail("expected TooLarge") }
        catch (e: TransportException.TooLarge) { }
        assertFalse(store.containsKey("share/s2"))
    }

    // Contratto 1.20 (R6): chi riceve lo dice in /seen/<uid> con l'ora del server; il relay ripiega su Telegram solo se nessuno legge.
    @Test fun fetchStateMarksSeen() = runBlocking {
        store["state"] = blobOf(Fixtures.stateIdle)
        transport().fetchState()
        assertEquals("{\".sv\":\"timestamp\"}", store["seen/u1"]!!.replace(" ", ""))
    }

    @Test fun streamMarksSeenWhenItOpens() = runBlocking {
        streamBody = "event: put\ndata: {\"path\":\"/\",\"data\":${blobOf(Fixtures.stateQuestion)}}\n\nevent: keep-alive\ndata: null\n\n"
        transport().state.first()
        assertNotNull(store["seen/u1"])
    }

    /** Regole RTDB precedenti: la scrittura di /seen viene rifiutata, e la lettura dello stato non ne soffre. */
    @Test fun refusedSeenDoesNotBreakTheRead() = runBlocking {
        refuseSeen = true
        store["state"] = blobOf(Fixtures.stateIdle)
        assertEquals(1, transport().fetchState().sessions.size)
    }
}

