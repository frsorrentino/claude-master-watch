package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Il rifiuto «too large» del PC letto come misura (Franz, 04/10 21:55: l'app mostrava «too large: 3630371»). */
class FileRefusalTest {
    @Test fun theSizeOfTodaysRefusal() {
        assertEquals(FileRefusal.TooLarge(3630371, null), FileRefusal.tooLarge("too large: 3630371"))
    }

    @Test fun theLimitWhenThePcSaysIt() {
        assertEquals(FileRefusal.TooLarge(31457280, 26214400), FileRefusal.tooLarge("too large: 31457280 max 26214400"))
    }

    @Test fun otherRefusalsStayAsTheyAre() {
        assertNull(FileRefusal.tooLarge("missing or unreadable"))
        assertNull(FileRefusal.tooLarge("not in the transcript"))
    }
}
