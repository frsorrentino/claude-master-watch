package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.Fixtures
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class PairQrTest {
    private val text = Fixtures.read("pair-qr.json")

    @Test fun readsTheFixture() {
        val q = PairQr.parse(text)!!
        assertEquals("q3Vb2mXz0rT8yKp1LwN4sA", q.i)
        assertEquals("penguin", q.h)
        assertEquals(1789211100L, q.e)
        val cfg = q.f.config()
        assertEquals("cmwatch-demo", cfg.projectId)
        assertEquals("123456789012", cfg.senderId)
        assertEquals("watch", cfg.topic)
    }

    /** `relay pair --text` stampa lo stesso JSON su una riga. */
    @Test fun oneLineFormAsPrinted() = assertNotNull(PairQr.parse(Json.parseToJsonElement(text).toString()))

    @Test fun rejectsWhatIsNotOurs() {
        assertNull(PairQr.parse("https://example.com"))
        assertNull(PairQr.parse(""))
        assertNull(PairQr.parse(text.replace("\"v\": 1", "\"v\": 2")))
        assertNull(PairQr.parse(text.replace("q3Vb2mXz0rT8yKp1LwN4sA", "123456")))
        assertNull(PairQr.parse(text.replace("https://", "http://")))
        assertNull(PairQr.parse(text.replace("j0DFrbaPJWJK5bIU6nZ6bslNgp09e14a0bpvPiE4KF8=", "abc=")))
        assertNull(PairQr.parse(text.dropLast(20)))
        assertNull(PairQr.parse(text.replace("\"penguin\"", "\"\"")))
    }

    @Test fun expiry() {
        val q = PairQr.parse(text)!!
        assertFalse(q.expired(1789211100L))
        assertTrue(q.expired(1789211101L))
    }
}
