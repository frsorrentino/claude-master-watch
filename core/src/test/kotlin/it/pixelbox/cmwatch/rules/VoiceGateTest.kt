package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

// Dal vivo 09/10 22:25 («la live è attiva da diversi minuti ma non dice nulla»): l'avviso di partenza e il primo recap
// arrivavano prima che la voce di sistema fosse pronta e si perdevano. Ora aspettano e partono appena la voce c'è.
class VoiceGateTest {
    @Test fun beforeTheVoiceTheTextsWaitAndLeaveInOrder() {
        val g = VoiceGate()
        assertEquals(VoiceGate.Say.LATER, g.say("Live accesa"))
        assertEquals(VoiceGate.Say.LATER, g.say("Il recap"))
        assertEquals(listOf("Live accesa", "Il recap"), g.ready())
        assertEquals(VoiceGate.Say.NOW, g.say("Una notizia"))
        assertEquals(emptyList<String>(), g.ready())
    }

    @Test fun aVoiceThatNeverComesLetsTheLiveGoOn() {
        val g = VoiceGate()
        g.say("Live accesa")
        assertEquals(1, g.giveUp())
        assertEquals(VoiceGate.Say.SKIP, g.say("Il recap"))
        // Arriva tardi: da lì in poi si parla.
        assertEquals(emptyList<String>(), g.ready())
        assertEquals(VoiceGate.Say.NOW, g.say("Una notizia"))
        assertEquals(0, g.giveUp())
    }

    @Test fun hushDropsTheWaitingTexts() {
        val g = VoiceGate()
        g.say("Live accesa")
        g.clear()
        assertEquals(emptyList<String>(), g.ready())
    }
}
