package it.pixelbox.cmwatch.pairing

import org.junit.Assert.*
import org.junit.Test

class FirebaseBootTest {
    private val a = FirebaseConfig("k", "p", "1:1:android:a", "https://a")
    private val b = a.copy(projectId = "q")

    /** Quella arrivata dal telefono vince su quella dentro la build; senza tutte e due, Firebase resta spento. */
    @Test fun storedWinsOverBundled() {
        assertEquals(a, FirebaseBoot.pick(a, b))
        assertEquals(b, FirebaseBoot.pick(null, b))
        assertNull(FirebaseBoot.pick(null, null))
    }
}
