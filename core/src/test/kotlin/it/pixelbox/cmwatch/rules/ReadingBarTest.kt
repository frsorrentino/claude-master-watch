package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** Il mini-controller della lettura (Franz, 03/10 23:00): cosa sta leggendo, in una riga. */
class ReadingBarTest {
    @Test fun theFirstRealLineWithoutMarkdownNorTheOutcomeLabel() {
        assertEquals("home A installata alle 22:56", ReadingBar.excerpt("\n\n**Esito:** home A installata alle 22:56\nWatch: Home A installata"))
        assertEquals("Consiglio KMP", ReadingBar.excerpt("# Consiglio KMP\n\naltro"))
    }

    // La riga per l'orologio non si legge e non fa da titolo.
    @Test fun skipsTheWatchLine() = assertEquals("Fatto", ReadingBar.excerpt("Watch: breve\nFatto"))

    @Test fun emptyTextGivesEmpty() = assertEquals("", ReadingBar.excerpt("  \n "))
}
