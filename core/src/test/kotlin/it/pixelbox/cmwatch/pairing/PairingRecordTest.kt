package it.pixelbox.cmwatch.pairing

import org.junit.Assert.*
import org.junit.Test

class PairingRecordTest {
    @Test fun roundTrip() {
        val r = PairingRecord(listOf("p", "w"), mapOf("p" to "Pixel 9", "w" to "Pixel Watch 5"), "w", "Pixel Watch 5", watchPending = true)
        assertEquals(r, PairingRecord.fromJson(r.toJson()))
        assertNull(PairingRecord.fromJson("x"))
        assertNull(PairingRecord.fromJson(null))
    }
}
