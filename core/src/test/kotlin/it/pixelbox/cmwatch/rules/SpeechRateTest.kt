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

    // Franz, 03/10 21:16: la pillola accanto a ■ cambia velocità mentre legge, 1× → 1,25× → 1,5× → 2× → 1×; da una
    // velocità lenta delle impostazioni il primo tocco porta a 1×.
    @Test fun theReadingPillGoesFasterThenBackToNormal() {
        assertEquals(listOf(1.25f, 1.5f, 2.0f, 1.0f), listOf(1.0f, 1.25f, 1.5f, 2.0f).map { SpeechRate.next(it) })
        assertEquals(1.0f, SpeechRate.next(0.8f))
        assertTrue(SpeechRate.pill.all { it in SpeechRate.choices })
    }

    // La vecchia scelta 1,2× salvata va alla più vicina delle nuove.
    @Test fun theOldFastChoiceMovesToTheNearest() = assertEquals(1.25f, SpeechRate.of(1.2f))
}
