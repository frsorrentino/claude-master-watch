package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Il testo che il telefono legge a voce (Franz, 02/10 00:01: leggeva «asterisco asterisco parola asterisco asterisco»):
 * senza markdown, senza la riga dei consigli, con «Watch:» letto come esito.
 */
class SpeechTextPhoneTest {
    @Test fun noMarkdownNoStepsLine() {
        val t = "Fatto il **push** di `3d353f4`.\n\nProssimi: installa · prova"
        assertEquals("Fatto il push di 3d353f4.", SpeechText.forPhone(t, "Codice", "Esito"))
    }

    @Test fun watchLineReadAsOutcome() {
        assertEquals("Fatto.\nEsito: release avviata.", SpeechText.forPhone("Fatto.\nWatch: release avviata", "Codice", "Esito"))
    }

    // Revisione finale 02/10 (#1): un paragrafo di codice si annuncia, non si legge parola per parola.
    @Test fun codeBlockIsAnnounced() {
        assertEquals("Codice.", SpeechText.forBlock(AnswerText.Kind.CODE, "val x = foo(bar) // calcola", "Codice"))
        assertEquals("Fatto il push.", SpeechText.forBlock(AnswerText.Kind.PARA, "Fatto il **push**", "Codice"))
    }
}
