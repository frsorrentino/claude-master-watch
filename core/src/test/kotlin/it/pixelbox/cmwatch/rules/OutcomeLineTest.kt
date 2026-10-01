package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * La riga «Watch: …» che la master chiede in fondo alle risposte è per l'orologio; nell'app si chiama «Esito» come nel
 * terminale (segnalazione 01/10 22:07), e con una riga «Esito:» già presente non si ripete.
 */
class OutcomeLineTest {
    @Test fun watchLineBecomesOutcome() {
        assertEquals("Fatto il push.\n\nEsito: release avviata", OutcomeLine.forPhone("Fatto il push.\n\nWatch: release avviata", "Esito"))
    }

    @Test fun watchLineGoesWhenTheOutcomeIsAlreadyThere() {
        val t = "Fatto.\n\nEsito: push di 3d353f4 fatto, CI in corso.\n\nWatch: push fatto, CI in corso"
        assertEquals("Fatto.\n\nEsito: push di 3d353f4 fatto, CI in corso.", OutcomeLine.forPhone(t, "Esito"))
    }

    @Test fun otherTextStaysAsItIs() {
        assertEquals("Il watch: segna l'ora", OutcomeLine.forPhone("Il watch: segna l'ora", "Esito"))
        assertEquals("niente", OutcomeLine.forPhone("niente", "Esito"))
    }
}
