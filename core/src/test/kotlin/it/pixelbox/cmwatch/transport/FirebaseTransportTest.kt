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

    @Before fun up() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val path = request.requestUrl!!.encodedPath.removePrefix("/").removeSuffix(".json")
                assertEquals("t0k", request.requestUrl!!.queryParameter("auth"))
                if (request.getHeader("Accept") == "text/event-stream") {
                    return MockResponse().setHeader("Content-Type", "text/event-stream").setBody(streamBody)
                }
                return when (request.method) {
                    "GET" -> MockResponse().setBody(store[path] ?: "null")
                    "PUT" -> { store[path] = request.body.readUtf8(); MockResponse().setBody(store[path]!!) }
                    else -> MockResponse().setResponseCode(405)
                }
            }
        }
        server.start()
    }

    @After fun down() = server.shutdown()

    private fun blobOf(json: String) = Blob.seal(json, key)
    private fun transport(timeout: Long = 20_000, key: ByteArray? = this.key) = FirebaseTransport(
        rtdb = Rtdb(server.url("/").toString().removeSuffix("/"), token = { "t0k" }),
        key = { key }, uid = { "u1" }, deviceKeyPair = { Pairing.newKeyPair() }, now = { clock },
        resultTimeoutMs = timeout, pollMs = 10, backoffMs = listOf(10),
    )

    @Test fun stateArrivesFromTheStreamDecrypted() = runBlocking {
        streamBody = "event: put\ndata: {\"path\":\"/\",\"data\":${blobOf(Fixtures.stateQuestion)}}\n\nevent: keep-alive\ndata: null\n\n"
        val s = transport().state.first()
        assertEquals(4, s.sessions.size); assertEquals("ledger-api", s.sessions[0].name)
    }

    @Test fun fetchStateIsOneGet() = runBlocking {
        store["state"] = blobOf(Fixtures.stateIdle)
        assertEquals(1, transport().fetchState().sessions.size)
        assertEquals("/state.json", requests.last().requestUrl!!.encodedPath)
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

    @Test fun eventsAreListedNewestFirst() = runBlocking {
        val ev = ContractJson.decodeEvents(Fixtures.events)
        val obj = JsonObject(ev.associate { it.key to Json.parseToJsonElement(blobOf(ContractJson.json.encodeToString(Event.serializer(), it))) })
        store["events"] = obj.toString()
        streamBody = ""
        val got = transport().events.first()
        assertEquals(6, got.size); assertTrue(got[0].ts >= got[1].ts)
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

    @Test fun pairingWithWrongPcCheckFails() {
        val pc = Pairing.newKeyPair()
        store["pair/123456"] = JsonObject(mapOf("pc_pub" to JsonPrimitive(Pairing.publicB64(pc)), "host" to JsonPrimitive("h"), "exp" to JsonPrimitive(clock + 300))).toString()
        store["pair/123456/ok"] = JsonObject(mapOf("host" to JsonPrimitive("h"), "check" to JsonPrimitive("0000000000000000"))).toString()
        assertThrows(TransportException.Network::class.java) { runBlocking { transport(key = null).pair("123456", "w") } }
    }

    @Test fun pairingWithUnknownCodeFails() {
        assertThrows(TransportException.Network::class.java) { runBlocking { transport(key = null).pair("000000", "w") } }
    }
}
