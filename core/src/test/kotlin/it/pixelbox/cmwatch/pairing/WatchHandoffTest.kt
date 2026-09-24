package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.crypto.Pairing
import org.junit.Assert.*
import org.junit.Test

class WatchHandoffTest {
    private var now = 0L
    private val cfg = FirebaseConfig("k", "p", "1:1:android:a", "https://a")
    private val key = ByteArray(32) { 7 }
    private fun h() = WatchHandoff(nowMs = { now })
    private fun keyFor(eph: String, uid: String): KeyRequest {
        val phone = Pairing.newKeyPair()
        return KeyRequest(host = "penguin", eph = Pairing.publicB64(phone), box = Handoff.seal(key, phone.private, eph, uid))
    }

    @Test fun restartOnlyForAnotherProject() {
        val w = h()
        assertFalse(w.needsRestart(null, cfg))
        assertFalse(w.needsRestart(cfg, cfg.copy(topic = "t2")))
        assertTrue(w.needsRestart(cfg, cfg.copy(projectId = "other")))
    }

    @Test fun keyOpensWithinFiveMinutesAndOnlyOnce() {
        val w = h()
        val req = keyFor(w.open("wuid"), "wuid")
        now = 5 * 60_000L
        assertArrayEquals(key, w.take(req))
        assertEquals(HandoffMessages.ERR_NO_SESSION, assertThrows(HandoffException::class.java) { w.take(req) }.code)
    }

    @Test fun expiredSessionAsksForHelloAgain() {
        val w = h()
        val req = keyFor(w.open("wuid"), "wuid")
        now = 5 * 60_000L + 1
        assertEquals(HandoffMessages.ERR_NO_SESSION, assertThrows(HandoffException::class.java) { w.take(req) }.code)
    }

    @Test fun noHelloNoKey() {
        val e = assertThrows(HandoffException::class.java) { h().take(KeyRequest(host = "h", eph = "x", box = "y")) }
        assertEquals(HandoffMessages.ERR_NO_SESSION, e.code)
    }
}
