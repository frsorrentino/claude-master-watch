package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.crypto.Blob
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Franz, 07/10 19:49: un file da 1 MB passava il controllo del telefono («limite 1,1 MB») e poi il caricamento diceva
 * «troppo grande». La busta di /share codifica il file due volte in base64: pesa circa 16/9 del file, non 4/3.
 */
class ShareLimitsTest {
    private val key = ByteArray(32) { it.toByte() }

    private fun envelope(size: Int, name: String): Int {
        val plain = buildJsonObject {
            put("mime", JsonPrimitive("application/octet-stream"))
            put("data", JsonPrimitive(java.util.Base64.getEncoder().encodeToString(ByteArray(size))))
            put("name", JsonPrimitive(name))
        }.toString()
        return Json.parseToJsonElement(Blob.seal(plain, key)).jsonObject.getValue("enc").jsonPrimitive.content.length
    }

    @Test fun theLimitOfTodaysRelayIsAbout843Kb() {
        assertEquals(842_961L, ShareLimits.maxFileBytes(1_500_000))
    }

    @Test fun aFileAtTheLimitFitsTheEnvelopeEvenWithALongName() {
        val n = ShareLimits.maxFileBytes(1_500_000).toInt()
        assertTrue(envelope(n, "x".repeat(120)) <= 1_500_000)
        assertTrue(envelope(n, "è".repeat(120)) <= 1_500_000)
    }

    @Test fun oneMegabyteDoesNotFitToday() {
        assertTrue(envelope(1_000_000, "a.pdf") > 1_500_000)
        assertTrue(1_000_000 > ShareLimits.maxFileBytes(1_500_000))
    }

    @Test fun noRelayLimitNoFile() {
        assertEquals(0L, ShareLimits.maxFileBytes(0))
    }
}
