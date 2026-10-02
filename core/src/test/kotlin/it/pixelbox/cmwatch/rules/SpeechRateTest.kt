package it.pixelbox.cmwatch.rules

import org.junit.Assert.*
import org.junit.Test

/** La velocità della voce (Franz, 02/10 15:49: «un po' troppo rapida»): una scelta delle impostazioni, ricordata. */
class SpeechRateTest {
    @Test fun withoutAChoiceTheEngineSpeed() = assertEquals(1.0f, SpeechRate.of(null))

    @Test fun aSavedChoiceStays() = assertEquals(0.9f, SpeechRate.of(0.9f))

    // Un valore salvato fuori dalle scelte va alla più vicina; uno rovinato torna alla velocità del motore.
    @Test fun anythingElseGoesToTheNearestChoice() {
        assertEquals(0.9f, SpeechRate.of(0.86f))
        assertEquals(SpeechRate.choices.last(), SpeechRate.of(5f))
        assertEquals(1.0f, SpeechRate.of(Float.NaN))
        assertEquals(1.0f, SpeechRate.of(-1f))
    }

    @Test fun slowerChoicesThanTheEngine() = assertTrue(SpeechRate.choices.count { it < 1.0f } >= 2 && 1.0f in SpeechRate.choices)
}
