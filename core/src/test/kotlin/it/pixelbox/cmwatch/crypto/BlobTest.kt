package it.pixelbox.cmwatch.crypto

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom

class BlobTest {
    private val key = ByteArray(32).also { SecureRandom().nextBytes(it) }

    @Test fun sealThenOpen() {
        val doc = Blob.seal("""{"a":1,"è":"sì"}""", key)
        val o = Json.parseToJsonElement(doc).jsonObject
        assertEquals(1, o.getValue("v").jsonPrimitive.content.toInt())
        assertTrue(o.getValue("enc").jsonPrimitive.content.matches(Regex("[A-Za-z0-9+/=]+")))
        assertEquals("""{"a":1,"è":"sì"}""", Blob.open(doc, key))
    }

    @Test fun twoSealsDiffer() { assertNotEquals(Blob.seal("x", key), Blob.seal("x", key)) }

    @Test(expected = BlobException::class) fun wrongKeyFails() {
        val other = ByteArray(32).also { SecureRandom().nextBytes(it) }
        Blob.open(Blob.seal("x", key), other)
    }

    @Test(expected = BlobException::class) fun wrongVersionFails() {
        Blob.open("""{"v":2,"enc":"AAAA"}""", key)
    }

    @Test fun pythonLayoutIsNonceThenCiphertext() {
        // Stesso layout di cm-relay-crypto.py: base64(nonce12 ‖ ct‖tag), AAD claude-master-relay-v1.
        val doc = Blob.seal("hi", key)
        val enc = java.util.Base64.getDecoder().decode(Json.parseToJsonElement(doc).jsonObject.getValue("enc").jsonPrimitive.content)
        assertEquals(12 + 2 + 16, enc.size)
    }
}
