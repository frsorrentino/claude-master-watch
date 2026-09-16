package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Model
import org.junit.Assert.*
import org.junit.Test

/**
 * Le misure della Scheda (Franz, 16/09 08:29: «ogni misura dovrebbe avere la sua grafica»). Il contesto ha una barra,
 * l'effort tre tacche, il modello una pillola col pallino di famiglia: qui stanno i numeri che quelle grafiche leggono.
 */
class SessionMetersTest {
    @Test fun leTreTaccheDellEffort() {
        assertEquals(1, SessionMeters.effortStep("low"))
        assertEquals(2, SessionMeters.effortStep("medium"))
        assertEquals(3, SessionMeters.effortStep("high"))
    }

    @Test fun lEffortSiLeggeComunqueSiaScritto() {
        assertEquals(3, SessionMeters.effortStep("HIGH"))
        assertEquals(2, SessionMeters.effortStep(" Medium "))
    }

    @Test fun unEffortSconosciutoNonAccendeTacche() {
        assertNull(SessionMeters.effortStep("fortissimo"))
        assertNull(SessionMeters.effortStep(null))
    }

    /** Il contesto si guarda oltre tre quarti e si chiude il turno oltre il 90 % (la soglia era già nel riquadro). */
    @Test fun ilColoreDelContestoSegueLeSoglie() {
        assertEquals(BriefCards.Tone.NEUTRAL, SessionMeters.contextTone(43))
        assertEquals(BriefCards.Tone.NEUTRAL, SessionMeters.contextTone(74))
        assertEquals(BriefCards.Tone.WARN, SessionMeters.contextTone(75))
        assertEquals(BriefCards.Tone.ALERT, SessionMeters.contextTone(90))
        assertEquals(BriefCards.Tone.NEUTRAL, SessionMeters.contextTone(null))
    }

    @Test fun laBarraDelContestoEUnaFrazione() {
        assertEquals(0.43f, SessionMeters.contextFraction(43)!!, 0.001f)
        assertEquals(1f, SessionMeters.contextFraction(120)!!, 0.001f)
        assertNull(SessionMeters.contextFraction(null))
    }

    /** Il pallino della pillola dice la famiglia: si riconosce il modello prima di leggerne il nome. */
    @Test fun laFamigliaDelModelloSiLeggeDallId() {
        assertEquals(ModelText.Family.OPUS, ModelText.family(Model("claude-opus-5[1m]", "Opus 5")))
        assertEquals(ModelText.Family.SONNET, ModelText.family(Model("claude-sonnet-5", null)))
        assertEquals(ModelText.Family.HAIKU, ModelText.family(Model("claude-haiku-4-5-20251001", null)))
        assertEquals(ModelText.Family.OTHER, ModelText.family(Model("claude-qualcosa-nuovo", null)))
        assertNull(ModelText.family(null))
    }
}
