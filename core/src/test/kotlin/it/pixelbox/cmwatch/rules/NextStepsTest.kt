package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** I consigli scritti dalla sessione in fondo alla risposta (design 01/10, variante C). */
class NextStepsTest {
    @Test fun lastLineBecomesSteps() {
        val r = NextSteps.parse("Ho pushato.\n\nEsito: push fatto.\n\nProssimi: apri l'app · scrivi il piano · correggi Lancia")
        assertEquals("Ho pushato.\n\nEsito: push fatto.", r.text)
        assertEquals(listOf("apri l'app", "scrivi il piano", "correggi Lancia"), r.steps)
    }

    @Test fun atMostThreeAndNotTooLong() {
        val long = "x".repeat(NextSteps.MAX_CHARS + 1)
        val r = NextSteps.parse("Fatto.\nProssimi: uno · $long · due · tre · quattro")
        assertEquals(listOf("uno", "due", "tre"), r.steps)
    }

    @Test fun noLineNoSteps() {
        val r = NextSteps.parse("Fatto.\n\nEsito: tutto ok.")
        assertEquals("Fatto.\n\nEsito: tutto ok.", r.text)
        assertEquals(emptyList<String>(), r.steps)
    }

    /** Conta solo l'ultima riga non vuota: una riga «Prossimi:» in mezzo al testo resta testo. */
    @Test fun onlyTheLastLineCounts() {
        val t = "Prossimi: questo no\n\nAltro testo."
        assertEquals(t, NextSteps.parse(t).text)
        assertEquals(emptyList<String>(), NextSteps.parse(t).steps)
    }

    /** La riga per l'orologio («Watch:») può venire dopo: i consigli si trovano lo stesso. */
    @Test fun watchLineAfterIsSkipped() {
        val r = NextSteps.parse("Fatto.\nProssimi: apri l'app · prova\nWatch: fatto")
        assertEquals("Fatto.\nWatch: fatto", r.text)
        assertEquals(listOf("apri l'app", "prova"), r.steps)
    }
}
