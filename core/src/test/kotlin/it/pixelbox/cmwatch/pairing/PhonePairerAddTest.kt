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

/**
 * Contratto 1.30 (`relay pair --add`, chiesto per il tablet il 04/10): un dispositivo in più. La stretta di mano è la
 * stessa, ma la chiave del relay non si deriva: arriva cifrata nella conferma. Vettori di `contract/pair-add.json`
 * (scalare del PC 0..31, del tablet 64..95, chiave del relay = byte 96..127).
 */
class PhonePairerAddTest {
    private val server = MockWebServer()
    private val store = HashMap<String, String>()
    private val fx = Json.parseToJsonElement(Fixtures.read("pair-add.json")).jsonObject
    private val qr = PairQr.parse(fx["qr"].toString())!!
    private val sent = fx["watch"]!!.jsonObject
    private fun priv(from: Int) = Pairing.privateFromRaw((from until from + 32).map { it.toByte() }.toByteArray())
    private val tabletKeys = KeyPair(Pairing.publicFromRaw(Pairing.rawFromB64(sent["watch_pub"]!!.jsonPrimitive.content)), priv(64))
    /** Cosa fa il relay finto quando il check torna: la conferma della fixture, o il rifiuto «full». */
    private var answer: (() -> Pair<String, String>)? = { "pair/${qr.i}/ok" to fx["ok"].toString() }

    @Before fun up() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl!!.encodedPath.removePrefix("/").removeSuffix(".json")
                return when (request.method) {
                    "GET" -> MockResponse().setBody(store[path] ?: "null")
                    "PUT" -> {
                        val body = request.body.readUtf8()
                        store[path] = body
                        if (path.endsWith("/watch")) {
                            val w = Json.parseToJsonElement(body).jsonObject
                            val k = Pairing.sharedKey(priv(0), w["watch_pub"]!!.jsonPrimitive.content)
                            if (Pairing.checkCode(k, qr.i) == w["check"]!!.jsonPrimitive.content) answer?.invoke()?.let { (p, v) -> store[p] = v }
                        }
                        MockResponse().setBody(body)
                    }
                    else -> MockResponse().setResponseCode(405)
                }
            }
        }
        server.start()
        store["pair/${qr.i}"] = """{"pc_pub":"${qr.c}","host":"penguin","exp":${qr.e},"mode":"add"}"""
    }

    @After fun down() = server.shutdown()

    private fun pairer(timeout: Long = 2_000) = PhonePairer(
        Rtdb(server.url("/").toString().removeSuffix("/"), token = { "t0k" }), { tabletKeys }, { qr.e - 60 }, pollMs = 10, timeoutMs = timeout, kind = "tablet",
    )

    @Test fun theQrSaysItIsAnAddition() {
        assertTrue(qr.add)
        assertFalse(PairQr.parse(Fixtures.read("pair-qr.json"))!!.add)
    }

    // La risposta è quella della fixture (stesso check), e la chiave è quella del relay, non quella del giro.
    @Test fun keepsTheRelayKeyFromTheConfirmation() {
        val r = runBlocking { pairer().pair(qr, sent["uid"]!!.jsonPrimitive.content, sent["name"]!!.jsonPrimitive.content, null) }
        val written = Json.parseToJsonElement(store["pair/${qr.i}/watch"]!!).jsonObject
        // Contratto 1.32: anche il tipo del dispositivo, come nella fixture del relay.
        listOf("watch_pub", "uid", "name", "check", "kind").forEach { k -> assertEquals(k, sent[k], written[k]) }
        assertEquals(fx["relay_key"]!!.jsonPrimitive.content, r.key.joinToString("") { "%02x".format(it) })
        assertEquals("penguin", r.host)
        assertEquals(listOf(sent["uid"]!!.jsonPrimitive.content), r.uids)
    }

    // Oltre quattro dispositivi il PC risponde {"error": "full"}: si dice subito, senza aspettare la conferma.
    @Test fun fullIsSaidRightAway() {
        answer = { "pair/${qr.i}/error" to "\"full\"" }
        val t0 = System.currentTimeMillis()
        assertThrows(PairError.Full::class.java) { runBlocking { pairer(timeout = 5_000).pair(qr, "tabletUid", "Pixel Tablet", null) } }
        assertTrue(System.currentTimeMillis() - t0 < 2_000)
    }

    // Una conferma di aggiunta senza la chiave, o con una busta che non si apre, non accoppia.
    @Test fun additionWithoutKeyIsBadConfirm() {
        val ok = fx["ok"]!!.jsonObject
        answer = { "pair/${qr.i}/ok" to JsonObject(ok - "key").toString() }
        assertThrows(PairError.BadConfirm::class.java) { runBlocking { pairer().pair(qr, "tabletUid", "Pixel Tablet", null) } }
    }

    @Test fun keyThatDoesNotOpenIsBadConfirm() {
        val ok = fx["ok"]!!.jsonObject
        val forged = JsonObject(ok + ("key" to buildJsonObject { put("v", 1); put("enc", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") }))
        answer = { "pair/${qr.i}/ok" to forged.toString() }
        assertThrows(PairError.BadConfirm::class.java) { runBlocking { pairer().pair(qr, "tabletUid", "Pixel Tablet", null) } }
    }
}
