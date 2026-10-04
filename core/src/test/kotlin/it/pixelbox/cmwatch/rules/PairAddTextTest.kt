package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.PairAddText.Refusal
import org.junit.Assert.assertEquals
import org.junit.Test

/** Contratto 1.31 (Franz, 04/10 13:47: «pairing dal telefono senza PC»): i rifiuti di `pair_add`, detti in chiaro. */
class PairAddTextTest {
    @Test fun knownRefusals() {
        assertEquals(Refusal.BUSY, PairAddText.refusal("a pairing is already open on the PC"))
        assertEquals(Refusal.NO_KEY, PairAddText.refusal("pair --add: no saved key, nothing to add to — use `relay pair`"))
        assertEquals(Refusal.NO_KEY, PairAddText.refusal("pair --add: nessuna chiave salvata, niente a cui aggiungersi — usa `relay pair`"))
        assertEquals(Refusal.FULL, PairAddText.refusal("already 4 devices: no room for another"))
        assertEquals(Refusal.FAILED, PairAddText.refusal("pairing not started: firebase unreachable"))
    }

    @Test fun anythingElseIsOther() = assertEquals(Refusal.OTHER, PairAddText.refusal("unknown op pair_add"))

    // Il motivo di «pairing not started» si mostra com'è, senza il prefisso inglese.
    @Test fun failedKeepsTheReason() = assertEquals("firebase unreachable", PairAddText.reason("pairing not started: firebase unreachable"))

    // Quanto resta all'invito: minuti e secondi, mai sotto zero.
    @Test fun leftUntilExpiry() {
        assertEquals("4:05", PairAddText.left(exp = 1_000_245, now = 1_000_000))
        assertEquals("0:00", PairAddText.left(exp = 1_000_000, now = 1_000_030))
    }
}
