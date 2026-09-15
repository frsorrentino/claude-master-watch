package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.rules.ReopenText.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Franz, 15/09 19:14: «deve apparire un feedback appena si preme riprendi».
class ReopenTextTest {
    private fun r(ok: Boolean, text: String) = CmdResult(id = "id", ok = ok, text = text, at = 0)

    @Test fun inviatoSenzaRispostaEInAvvio() = assertEquals(Status.Starting, ReopenText.status(null, gone = true, notDelivered = false))

    @Test fun ilPcHaFinitoMaLoStatoNonLaMostraAncora() =
        assertEquals(Status.Starting, ReopenText.status(r(true, "reopened orbit-docs (work): same conversation"), gone = true, notDelivered = false))

    @Test fun tornataVivaIlTastoTornaNormale() =
        assertNull(ReopenText.status(r(true, "reopened orbit-docs (work): same conversation"), gone = false, notDelivered = false))

    @Test fun unRifiutoDelPcSiLegge() =
        assertEquals(Status.Failed("orbit-docs is already running"), ReopenText.status(r(false, "orbit-docs is already running"), gone = true, notDelivered = false))

    @Test fun nonConsegnato() = assertEquals(Status.Failed(null), ReopenText.status(null, gone = true, notDelivered = true))
}

// Contratto 1.10: il testo libero a una domanda passa da «Type something.».
class QuestionTextArgTest {
    @Test fun gliACapoDiventanoSpazi() = assertEquals("text:ship it tonight", QuestionRules.textArg("ship it\n tonight"))

    @Test fun chat() = assertEquals("chat", QuestionRules.CHAT_ARG)
}
