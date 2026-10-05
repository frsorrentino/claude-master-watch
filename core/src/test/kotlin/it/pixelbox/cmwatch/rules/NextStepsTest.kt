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

    // Franz, 04/10 20:21: il suggerito del terminale ripeteva uno dei Prossimi.
    @Test fun theFirstStepGoesInTheFieldAndTheTerminalCopyDisappears() {
        val b = NextSteps.box(listOf("Prova dal vivo", "Apri la CI", "Scrivi il piano"), "prova dal vivo.", "")
        assertEquals("Prova dal vivo", b.field)
        assertEquals(listOf("Apri la CI", "Scrivi il piano"), b.rows)
    }

    @Test fun aDifferentTerminalSuggestionIsTheLastRow() {
        val b = NextSteps.box(listOf("Prova dal vivo", "Apri la CI"), "Committa", "")
        assertEquals(listOf("Apri la CI", "Committa"), b.rows)
    }

    @Test fun withoutStepsTheTerminalSuggestionStaysInTheField() {
        assertEquals(NextSteps.Box("Committa", emptyList()), NextSteps.box(emptyList(), "Committa", ""))
        assertEquals(NextSteps.Box(null, emptyList()), NextSteps.box(emptyList(), "Committa", "scrivo altro"))
    }

    @Test fun aStepInTheFieldLeavesTheBoxAndTheFieldOneComesBack() {
        val b = NextSteps.box(listOf("Prova dal vivo", "Apri la CI", "Scrivi il piano"), null, "Fai il merge e poi apri la CI")
        assertEquals(null, b.field)
        assertEquals(listOf("Prova dal vivo", "Scrivi il piano"), b.rows)
    }

    @Test fun aTappedStepIsQueuedWithThen() {
        assertEquals("Prova dal vivo", NextSteps.append("", "Prova dal vivo", "e poi"))
        assertEquals("Fai il merge e poi prova dal vivo", NextSteps.append("Fai il merge.", "Prova dal vivo", "e poi"))
        assertEquals("Fai il merge e poi CI verde", NextSteps.append("Fai il merge", "CI verde", "e poi"))
    }

    // Contratto 1.38: «!» davanti a una voce = sblocca un lavoro fermo; si toglie dal testo e non conta nei 40 caratteri.
    @Test fun bangMarksTheUnblockingSteps() {
        val p = NextSteps.parse("Fatto.\n\nProssimi: !ok release claude-master 0.6.9 · aggiorna il changelog · !${"x".repeat(40)}")
        assertEquals(listOf("ok release claude-master 0.6.9", "aggiorna il changelog", "x".repeat(40)), p.steps)
        assertEquals(setOf("ok release claude-master 0.6.9", "x".repeat(40)), p.blocking)
    }
}
