package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.crypto.Pairing
import org.junit.Assert.*
import org.junit.Test

class HandoffTest {
    private fun priv(from: Int) = Pairing.privateFromRaw((from until from + 32).map { it.toByte() }.toByteArray())
    private val key = "dd9f775d5fbdd918e727cb41c05452189759ccc0d87798791eff22474e278b5c".chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    private val phonePub = "eaYx7t4b+cmPEgMs3q3Q56B5OY/HhriMyEbsia+FpRo="
    private val watchPub = "Z13VdO13iTELPS52gfN5C0ZsdzsVIf7PNld5WDcepS8="
    private val uid = "watchUid0000000000000000000"
    private val box = "AAECAwQFBgcICQoLtCFqAvmVEVAwdkhHgr49Nf8yUE0cy5x6KEOrUY36BNwwg8O/d+QUEK3eSLm+02i2"

    @Test fun fixedVectorFromPythonCryptography() {
        assertEquals(box, Handoff.seal(key, priv(64), watchPub, uid, nonce = ByteArray(12) { it.toByte() }))
        assertArrayEquals(key, Handoff.open(box, priv(96), phonePub, uid))
    }

    @Test fun roundTripWithFreshKeys() {
        val p = Pairing.newKeyPair(); val w = Pairing.newKeyPair()
        val b = Handoff.seal(key, p.private, Pairing.publicB64(w), uid)
        assertArrayEquals(key, Handoff.open(b, w.private, Pairing.publicB64(p), uid))
    }

    @Test fun theWatchUidIsBoundIn() {
        val e = assertThrows(HandoffException::class.java) { Handoff.open(box, priv(96), phonePub, "anotherUid") }
        assertEquals(HandoffMessages.ERR_DECRYPT, e.code)
    }

    @Test fun garbageIsADecryptError() {
        val e = assertThrows(HandoffException::class.java) { Handoff.open("AAA", priv(96), phonePub, uid) }
        assertEquals(HandoffMessages.ERR_DECRYPT, e.code)
    }
}
