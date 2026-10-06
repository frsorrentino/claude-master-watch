package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.contract.Model
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Modello ed effort scelti dal telefono (segnalazioni 01/10 20:22 e 20:25): il selettore non segnava il modello in uso
 * e dopo un cambio di effort restava il valore vecchio, perché il PC lo riporta solo al turno successivo.
 */
class TuneTest {
    private val opus = Model("claude-opus-5-5", "Opus 5.5")
    private val opus1m = Model("claude-opus-5-5[1m]", "Opus 5.5")
    private val sonnet = Model("claude-sonnet-5", "Sonnet 5")
    private fun s(model: Model? = opus, effort: String? = "medium") =
        Session(id = "m", name = "master", account = "personal", project = "p", state = SessionState.IDLE, since = 0, model = model, effort = effort)

    @Test fun kindForThePanelLine() {
        assertEquals(listOf("opus", "fable", "sonnet", "haiku", null), listOf("claude-opus-5-5[1m]", "claude-fable-5-1", "claude-sonnet-5", "claude-haiku-4-5", "claude-x").map(Tune::kind))
    }

    @Test fun theOneMillionWindowIsTheSameModel() {
        assertTrue(Tune.sameModel("claude-opus-5-5[1m]", "claude-opus-5-5"))
        assertFalse(Tune.sameModel("claude-sonnet-5", "claude-opus-5-5"))
        assertFalse(Tune.sameModel(null, "claude-opus-5-5"))
    }

    @Test fun aPickShowsUntilThePcReportsIt() {
        val pick = Tune.Pick(cmd = "c1", at = 100, effort = "max", was = "medium")
        assertEquals("max", Tune.effort(s(), pick, null, now = 160))
        // Il PC lo riporta: da lì vale lo stato.
        assertEquals("max", Tune.effort(s(effort = "max"), pick, null, now = 200))
        // Cambiato altrove dopo la scelta: vince lo stato, non la scelta vecchia.
        assertEquals("low", Tune.effort(s(effort = "low"), pick, null, now = 200))
    }

    @Test fun aRefusedOrOldPickIsDropped() {
        val pick = Tune.Pick(cmd = "c1", at = 100, effort = "max", was = "medium")
        assertEquals("medium", Tune.effort(s(), pick, CmdResult("c1", ok = false, text = "no", at = 110), now = 120))
        assertEquals("medium", Tune.effort(s(), pick, null, now = 100 + Tune.HOLD_S + 1))
    }

    @Test fun modelPickComparesWithoutTheWindowSuffix() {
        val pick = Tune.Pick(cmd = "c2", at = 100, model = sonnet, was = opus.id)
        assertEquals(sonnet, Tune.model(s(), pick, null, now = 130))
        assertEquals(opus1m, Tune.model(s(model = opus1m), Tune.Pick(cmd = "c3", at = 100, model = opus1m, was = opus.id), null, now = 130))
        assertEquals(sonnet, Tune.model(s(model = sonnet), pick, null, now = 130))
    }
}
