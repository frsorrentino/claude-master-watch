package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.Fixtures
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

/**
 * L'accoppiamento a zero tocchi sul Chromebook (Franz, 04/10 15:48, «ok c»): il relay apre `cmwatch://pair?q=<riga in
 * base64url>` nell'Android della stessa macchina. L'app ne legge la riga e chiede sempre conferma prima di accoppiare.
 */
class PairLinkTest {
    private val line = Json.parseToJsonElement(Fixtures.read("pair-add.json")).jsonObject["qr"].toString()
    private fun b64(s: String, pad: Boolean = false) = Base64.getUrlEncoder().let { if (pad) it else it.withoutPadding() }.encodeToString(s.toByteArray())

    @Test fun readsTheLineFromTheLink() {
        val got = PairLink.line("cmwatch://pair?q=${b64(line)}")
        assertEquals(line, got)
        assertTrue(PairQr.parse(got!!)!!.add)
        assertEquals(line, PairLink.line("cmwatch://pair?q=${b64(line, pad = true)}"))
    }

    // Contratto 1.32: lo stesso vettore del relay (pair-link.json), la riga e il link che il relay apre con adb.
    @Test fun theRelayLinkGivesTheRelayLine() {
        val fx = Json.parseToJsonElement(Fixtures.read("pair-link.json")).jsonObject
        assertEquals(fx["line"]!!.jsonPrimitive.content, PairLink.line(fx["uri"]!!.jsonPrimitive.content))
    }

    // «Apri sul telefono» dall'orologio apre cmwatch://pair senza invito: non è un accoppiamento.
    @Test fun withoutQIsNotAnInvitation() = assertNull(PairLink.line("cmwatch://pair"))

    @Test fun anythingElseIsNotAnInvitation() {
        assertNull(PairLink.line("https://pair?q=${b64(line)}"))
        assertNull(PairLink.line("cmwatch://open?q=${b64(line)}"))
        assertNull(PairLink.line("cmwatch://pair?q=%%%"))
        assertNull(PairLink.line("cmwatch://pair?q=${b64("{\"v\":1}")}"))
        assertNull(PairLink.line("cmwatch://pair?q=${b64("482913")}"))
    }
}
