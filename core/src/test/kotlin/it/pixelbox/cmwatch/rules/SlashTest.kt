package it.pixelbox.cmwatch.rules

import org.junit.Assert.*
import org.junit.Test

/** I comandi slash dal telefono (Franz, 02/10 11:12): suggeriti scrivendo «/», solo quelli che il relay consente. */
class SlashTest {
    private val allowed = listOf("compact", "clear", "exit", "context", "cost")

    @Test fun suggestsWhileTypingTheName() {
        assertEquals(allowed, Slash.suggest("/", allowed))
        assertEquals(listOf("compact", "context", "cost"), Slash.suggest("/c", allowed).filter { it != "clear" })
        assertEquals(listOf("clear"), Slash.suggest("/cl", allowed))
        assertTrue(Slash.suggest("/compact tieni le decisioni", allowed).isEmpty())   // nome finito: niente lista
        assertTrue(Slash.suggest("ciao", allowed).isEmpty())
        assertTrue(Slash.suggest("/", null).isEmpty())                                  // relay senza `slash`
    }

    @Test fun parsesAnAllowedCommandWithItsArguments() {
        assertEquals(Slash.Parsed("compact", "tieni le decisioni"), Slash.parse("/compact tieni le decisioni", allowed))
        assertEquals(Slash.Parsed("exit", null), Slash.parse("  /exit  ", allowed))
    }

    @Test fun anythingElseStaysAPrompt() {
        assertNull(Slash.parse("/deploy subito", allowed))      // non consentito: resta testo
        assertNull(Slash.parse("usa /tmp per i file", allowed))
        assertNull(Slash.parse("/compact", null))
    }

    @Test fun clearAndExitAskFirst() {
        assertTrue(Slash.confirm("clear")); assertTrue(Slash.confirm("exit"))
        assertFalse(Slash.confirm("compact"))
    }
}
