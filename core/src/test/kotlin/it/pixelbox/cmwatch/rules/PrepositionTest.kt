package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** «Scrivi ad atlas-shop», «Scrivi a ledger-api» (Franz, 04/10 09:14): «ad» solo davanti alla stessa vocale. */
class PrepositionTest {
    @Test fun adBeforeA() {
        assertTrue(Preposition.ad("atlas-shop"))
        assertTrue(Preposition.ad("Atlas"))
        assertTrue(Preposition.ad("àncora"))
    }

    @Test fun aBeforeEverythingElse() {
        assertFalse(Preposition.ad("ledger-api"))
        assertFalse(Preposition.ad("orbit-docs"))
        assertFalse(Preposition.ad("Euro"))
        assertFalse(Preposition.ad(""))
    }
}
