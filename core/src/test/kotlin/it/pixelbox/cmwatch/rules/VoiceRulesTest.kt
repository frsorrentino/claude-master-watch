package it.pixelbox.cmwatch.rules

import org.junit.Assert.*
import org.junit.Test

/** Voce della lettura scelta a orecchio fra quelle italiane del motore (Franz, 14/09 13:28: «una voce maschile»). */
class VoiceRulesTest {
    private val v = listOf("it-it-x-itb-local", "it-it-x-itc-local", "it-it-x-itd-local")

    @Test fun dallaPredefinitaSiPassaAllaPrima() = assertEquals("it-it-x-itb-local", VoiceRules.next(v, null))
    @Test fun siScorreInOrdine() = assertEquals("it-it-x-itd-local", VoiceRules.next(v, "it-it-x-itc-local"))
    @Test fun dopoLUltimaSiTornaAllaPredefinita() = assertNull(VoiceRules.next(v, "it-it-x-itd-local"))
    @Test fun unaVoceSparitaRipartedallaPrima() = assertEquals("it-it-x-itb-local", VoiceRules.next(v, "vecchia"))
    @Test fun senzaVociRestaLaPredefinita() = assertNull(VoiceRules.next(emptyList(), "x"))
    @Test fun laPosizioneContaDaUno() {
        assertEquals(0, VoiceRules.position(v, null)); assertEquals(2, VoiceRules.position(v, "it-it-x-itc-local"))
    }
}
