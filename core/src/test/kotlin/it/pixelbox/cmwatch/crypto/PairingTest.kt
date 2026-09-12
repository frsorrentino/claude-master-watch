package it.pixelbox.cmwatch.crypto

import org.junit.Assert.*
import org.junit.Test

class PairingTest {
    @Test fun sharedKeyMatchesBothWays() {
        val a = Pairing.newKeyPair(); val b = Pairing.newKeyPair()
        val ab = Pairing.sharedKey(a.private, Pairing.publicB64(b))
        val ba = Pairing.sharedKey(b.private, Pairing.publicB64(a))
        assertArrayEquals(ab, ba); assertEquals(32, ab.size)
        assertFalse(ab.contentEquals(Pairing.sharedKey(a.private, Pairing.publicB64(a))))
    }

    @Test fun publicKeyIsRaw32Bytes() {
        assertEquals(32, java.util.Base64.getDecoder().decode(Pairing.publicB64(Pairing.newKeyPair())).size)
    }

    @Test fun rfc7748TestVector() {
        // RFC 7748 §6.1: chiave privata di Alice e pubblica di Bob → segreto condiviso noto; qui verifichiamo
        // la conversione u ↔ raw little-endian con la chiave pubblica di Alice derivata dalla sua privata.
        val alicePub = "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a"
        val raw = alicePub.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val b64 = java.util.Base64.getEncoder().encodeToString(raw)
        assertEquals(b64, Pairing.publicB64FromRaw(raw))
        assertArrayEquals(raw, Pairing.rawFromB64(b64))
    }

    @Test fun checkCodeIs16HexAndKeyBound() {
        val k1 = ByteArray(32) { 1 }; val k2 = ByteArray(32) { 2 }
        val c = Pairing.checkCode(k1, "123456")
        assertTrue(c.matches(Regex("[0-9a-f]{16}")))
        assertNotEquals(c, Pairing.checkCode(k2, "123456"))
    }
}
