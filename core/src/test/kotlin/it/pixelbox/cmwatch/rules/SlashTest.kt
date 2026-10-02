package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.CmdResult
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

    // Dal vivo 02/10 14:51: /cost dal telefono non mostrava niente. Il relay 0.5.6 rimanda il pannello dopo «sent /cost to …»;
    // le colonne del terminale diventano un solo spazio, una riga per riga.
    private val cost = Sent("c1", "kb", "/cost", sentAt = 1)
    private fun ok(text: String) = CmdResult("c1", ok = true, text = text, at = 2)

    @Test fun panelTextFollowsTheSentLine() {
        assertEquals("Session\nTotal cost: \$0.42", Slash.panel(cost, ok("sent /cost to kb\n\n   Session\n   Total cost:            \$0.42")))
        // Pannello rimasto aperto: l'avviso del relay resta in fondo.
        assertEquals("Session\n(the panel is still open on the PC)",
            Slash.panel(cost, ok("sent /cost to kb\n\n   Session\n\n(the panel is still open on the PC)")))
    }

    // Dal vivo 02/10 16:31 («il testo ricevuto è più lungo del necessario»): via le schede in testa, l'ingombro delle skill
    // dei plugin e la freccia di scorrimento; restano costo, durate, modelli, cache e quota.
    @Test fun panelDropsWhatThePhoneDoesNotNeed() {
        val raw = "sent /cost to kb\n\nSettings  Status   Config   Usage   Stats\n   Session\n   Total cost:            \$0.42\n" +
            "   Prompt cache (main):   12 requests · 98% of input tokens from cache\n" +
            "   Plugin skill-listing footprint\n   What each plugin's skill descriptions add to the system prompt (cached input after the first turn).\n" +
            "   alpha                       7 skills · ~300 tok/turn\n   beta                        1 skill · ~52 tok/turn\n" +
            "   Total                       ~352 tok/turn\n   Current session\n                                          0% used\n" +
            "   Resets 9pm (Europe/Rome)\n                                                        ↓"
        assertEquals(
            "Session\nTotal cost: \$0.42\nPrompt cache (main): 12 requests · 98% of input tokens from cache\nCurrent session\n0% used\nResets 9pm (Europe/Rome)",
            Slash.panel(cost, ok(raw)),
        )
    }

    @Test fun noPanelWithoutOne() {
        assertNull(Slash.panel(cost, ok("sent /compact to kb")))
        assertNull(Slash.panel(cost, CmdResult("c1", ok = false, text = "kb is busy\n\nlater", at = 2)))
        assertNull(Slash.panel(cost, null))
        assertNull(Slash.panel(cost.copy(text = "ciao"), ok("delivered\n\nsomething")))   // un prompt non ha pannello
    }
}
