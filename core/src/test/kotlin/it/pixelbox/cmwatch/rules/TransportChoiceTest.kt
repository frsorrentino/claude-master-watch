package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class TransportChoiceTest {
    @Test fun firebaseOnlyWhenEverythingIsThere() {
        assertEquals(TransportChoice.Kind.FIREBASE, TransportChoice.pick(paired = true, hasKey = true, firebase = true))
        assertEquals(TransportChoice.Kind.FAKE, TransportChoice.pick(true, true, false))
        assertEquals(TransportChoice.Kind.FAKE, TransportChoice.pick(true, false, true))
        assertEquals(TransportChoice.Kind.FAKE, TransportChoice.pick(false, true, true))
    }
}
