package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** Lo zoom del testo della conversazione (Franz, 03/10 21:16). */
class ChatZoomTest {
    @Test fun staysWithinTheBounds() {
        assertEquals(ChatZoom.MAX, ChatZoom.clamp(3f))
        assertEquals(ChatZoom.MIN, ChatZoom.clamp(0.2f))
        assertEquals(1.3f, ChatZoom.clamp(1.3f))
    }

    @Test fun aMissingOrBrokenValueIsTheNormalSize() {
        assertEquals(1f, ChatZoom.of(null))
        assertEquals(1f, ChatZoom.of(Float.NaN))
        assertEquals(1f, ChatZoom.of(-2f))
        assertEquals(1f, ChatZoom.clamp(Float.POSITIVE_INFINITY))
        assertEquals(ChatZoom.MAX, ChatZoom.of(9f))
    }
}
