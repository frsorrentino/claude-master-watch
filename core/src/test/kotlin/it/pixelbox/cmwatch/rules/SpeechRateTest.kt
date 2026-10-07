package it.pixelbox.cmwatch.rules

import org.junit.Assert.*
import org.junit.Test

/**
 * La velocità della voce (Franz, 02/10 15:49: «un po' troppo rapida»), ricordata. Dal 07/10 (approvata alle 22:06) è
 * continua: uno slider da 0,5× a 2× a passi di 0,05, con uno scatto su 1×.
 */
class SpeechRateTest {
    @Test fun withoutAChoiceTheEngineSpeed() = assertEquals(1.0f, SpeechRate.of(null))

    @Test fun aSavedChoiceStays() {
        assertEquals(0.9f, SpeechRate.of(0.9f))
        assertEquals(1.35f, SpeechRate.of(1.35f))
        assertEquals(1.25f, SpeechRate.of(1.25f))   // le scelte di prima restano quelle
    }

    @Test fun theSliderMovesInStepsOfFiveHundredths() {
        assertEquals(0.85f, SpeechRate.snap(0.86f))
        assertEquals(1.4f, SpeechRate.snap(1.41f))
        assertEquals(1.2f, SpeechRate.of(1.2f))
    }

    @Test fun nearOneItSnapsToOne() {
        assertEquals(1.0f, SpeechRate.snap(1.03f))
        assertEquals(1.0f, SpeechRate.snap(0.97f))
        assertEquals(1.05f, SpeechRate.snap(1.05f))   // 1,05× resta raggiungibile
    }

    @Test fun outsideTheRangeOrBrokenValues() {
        assertEquals(SpeechRate.MAX, SpeechRate.of(5f))
        assertEquals(SpeechRate.MIN, SpeechRate.of(0.1f))
        assertEquals(1.0f, SpeechRate.of(Float.NaN))
        assertEquals(1.0f, SpeechRate.of(-1f))
    }

    @Test fun theSliderSpansHalfToDouble() {
        assertEquals(0.5f, SpeechRate.MIN); assertEquals(2.0f, SpeechRate.MAX)
        assertEquals(1f / 3f, SpeechRate.fraction(1.0f), 1e-6f)
    }
}
