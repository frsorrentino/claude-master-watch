package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.crypto.Pairing
import it.pixelbox.cmwatch.transport.Rtdb
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.security.KeyPair

class PhonePairerTest {
    private val server = MockWebServer()
    private val store = HashMap<String, String>()
    private val qr = PairQr.parse(Fixtures.read("pair-qr.json"))!!
    private val exchange = Json.parseToJsonElement(Fixtures.read("pair-response.json")).jsonObject
    private fun priv(from: Int) = Pairing.privateFromRaw((from until from + 32).map { it.toByte() }.toByteArray())
    private val phoneKeys = KeyPair(Pairing.publicFromRaw(Pairing.rawFromB64("NYBy1jZYgNGu6jKa35EhODhR7SGijjt16WXQ0s0WYlQ=")), priv(32))
    private val watch = WatchPeer("watchUid0000000000000000000", "Pixel Watch 5")
    private var pcAnswers = true

    @Before fun up() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl!!.encodedPath.removePrefix("/").removeSuffix(".json")
                return when (request.method) {
                    "GET" -> MockResponse().setBody(store[path] ?: "null")
                    "PUT" -> {
                        val body = request.body.readUtf8()
                        store[path] = body
                        // Il relay finto: verifica il check con la chiave privata del PC (scalare 0..31) e conferma.
                        if (pcAnswers && path.endsWith("/watch")) {
                            val w = Json.parseToJsonElement(body).jsonObject
                            val k = Pairing.sharedKey(priv(0), w["watch_pub"]!!.jsonPrimitive.content)
                            if (Pairing.checkCode(k, qr.i) == w["check"]!!.jsonPrimitive.content)
                                store["pair/${qr.i}/ok"] = buildJsonObject { put("host", "penguin"); put("check", Pairing.checkCode(k, "${qr.i}:pc")) }.toString()
                        }
                        MockResponse().setBody(body)
                    }
                    else -> MockResponse().setResponseCode(405)
                }
            }
        }
        server.start()
        store["pair/${qr.i}"] = """{"pc_pub":"${qr.c}","host":"penguin","exp":${qr.e}}"""
    }

    @After fun down() = server.shutdown()

    private fun pairer(now: Long = qr.e - 60, timeout: Long = 2_000) = PhonePairer(
        Rtdb(server.url("/").toString().removeSuffix("/"), token = { "t0k" }), { phoneKeys }, { now }, pollMs = 10, timeoutMs = timeout,
    )

    @Test fun writesExactlyTheContractAnswerAndGetsTheKey() {
        val r = runBlocking { pairer().pair(qr, "phoneUid00000000000000000000", "Pixel 9", watch) }
        assertEquals(exchange["watch"], Json.parseToJsonElement(store["pair/${qr.i}/watch"]!!))
        assertEquals(exchange["ok"], Json.parseToJsonElement(store["pair/${qr.i}/ok"]!!))
        assertEquals("dd9f775d5fbdd918e727cb41c05452189759ccc0d87798791eff22474e278b5c", r.key.joinToString("") { "%02x".format(it) })
        assertEquals(listOf("phoneUid00000000000000000000", "watchUid0000000000000000000"), r.uids)
        assertEquals("penguin", r.host)
    }

    @Test fun phoneAloneHasOneUid() {
        val r = runBlocking { pairer().pair(qr, "phoneUid00000000000000000000", "Pixel 9", null) }
        assertEquals(listOf("phoneUid00000000000000000000"), r.uids)
        assertEquals(1, Json.parseToJsonElement(store["pair/${qr.i}/watch"]!!).jsonObject["uids"]!!.jsonArray.size)
    }

    @Test fun expiredCodeFailsWithoutNetwork() {
        assertThrows(PairError.Expired::class.java) { runBlocking { pairer(now = qr.e + 1).pair(qr, "p", "Pixel 9", watch) } }
        assertEquals(0, server.requestCount)
    }

    @Test fun usedOrForeignCodeIsUnknownRightAway() {
        store.remove("pair/${qr.i}")
        assertThrows(PairError.Unknown::class.java) { runBlocking { pairer().pair(qr, "p", "Pixel 9", watch) } }
        assertNull(store["pair/${qr.i}/watch"])
    }

    @Test fun silentPcIsNoConfirm() {
        pcAnswers = false
        assertThrows(PairError.NoConfirm::class.java) { runBlocking { pairer(timeout = 100).pair(qr, "p", "Pixel 9", watch) } }
    }

    /** Revisione finale, 2: un `/ok` che non è un oggetto è una conferma sbagliata, non un'eccezione. */
    @Test fun okThatIsNotAnObjectIsBadConfirm() {
        pcAnswers = false
        store["pair/${qr.i}/ok"] = "\"yes\""
        assertThrows(PairError.BadConfirm::class.java) { runBlocking { pairer().pair(qr, "p", "Pixel 9", watch) } }
    }

    @Test fun forgedConfirmIsRejected() {
        pcAnswers = false
        store["pair/${qr.i}/ok"] = """{"host":"penguin","check":"0000000000000000"}"""
        assertThrows(PairError.BadConfirm::class.java) { runBlocking { pairer().pair(qr, "p", "Pixel 9", watch) } }
    }
}
