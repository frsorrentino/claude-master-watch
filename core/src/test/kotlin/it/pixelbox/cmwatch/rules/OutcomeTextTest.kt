package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Il titolo dell'Esito è la riga «Esito:» intera, non i suoi primi 60 caratteri (Franz, 14/09 13:20). */
class OutcomeTextTest {
    private val corto = "lavoro del relay chiuso e attivo, in attesa della prova dal"

    @Test fun ilTitoloEDellaRigaDiEsitoIntera() {
        val o = Outcome(corto, "Ho chiuso il relay.\nEsito: lavoro del relay chiuso e attivo, in attesa della prova dal vivo di Franz.", 0)
        assertEquals("lavoro del relay chiuso e attivo, in attesa della prova dal vivo di Franz.", OutcomeText.headline(o))
    }

    @Test fun valeAncheLaRigaWatchEIlGrassetto() =
        assertEquals("tile rifatta, quota su una riga.", OutcomeText.headline(Outcome("tile rifatta", "testo\n**Watch:** tile rifatta, quota su una riga.", 0)))

    @Test fun conPiuRigheDiEsitoValeLUltima() =
        assertEquals("seconda.", OutcomeText.headline(Outcome("x", "Esito: prima.\naltro\nEsito: seconda.", 0)))

    // Sotto il titolo non si ripete la stessa frase (Franz, 14/09 14:09: «descrizione diversa dal titolo»).
    @Test fun laDescrizioneNonRipeteIlTitolo() {
        val o = Outcome(corto, "Ho chiuso il relay e i test sono verdi.\nEsito: lavoro del relay chiuso e attivo, in attesa della prova dal vivo di Franz.", 0)
        assertEquals("Ho chiuso il relay e i test sono verdi.", OutcomeText.body(o))
    }

    @Test fun senzaRigaDiEsitoIlTitoloArrivaAFineFrase() {
        val o = Outcome(corto, "lavoro del relay chiuso e attivo, in attesa della prova dal vivo di Franz. Poi il resto del messaggio.", 0)
        assertEquals("lavoro del relay chiuso e attivo, in attesa della prova dal vivo di Franz.", OutcomeText.headline(o))
        assertEquals("Poi il resto del messaggio.", OutcomeText.body(o))
    }

    @Test fun seRestaSoloIlTitoloNonCeDescrizione() =
        assertNull(OutcomeText.body(Outcome(corto, "lavoro del relay chiuso e attivo, in attesa della prova dal vivo di Franz.", 0)))

    @Test fun senzaRigaDiEsitoRestaIlTestoCorto() =
        assertEquals(corto, OutcomeText.headline(Outcome(corto, "solo testo senza riga finale", 0)))
}
