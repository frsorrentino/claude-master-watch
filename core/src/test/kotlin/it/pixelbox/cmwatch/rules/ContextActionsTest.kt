package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Choices
import it.pixelbox.cmwatch.contract.Model
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Il foglio del contesto e «Fai controllare alla master» (proposte approvate da Franz, 01/10 21:19). */
class ContextActionsTest {
    private val choices = Choices(
        models = listOf(Model("claude-opus-5-5[1m]", "Opus 5.5"), Model("claude-sonnet-5", "Sonnet 5")),
        efforts = listOf("low", "max"),
    )
    private fun s(name: String = "kb", model: Model? = Model("claude-opus-5-5", "Opus 5.5"), context: Int? = 83, state: SessionState = SessionState.IDLE) =
        Session(id = name, name = name, account = "personal", project = "p", state = state, since = 0, model = model, context = context)

    @Test fun handoffStandsOutFromEightyPercent() {
        assertTrue(ContextActions.urgent(80))
        assertFalse(ContextActions.urgent(79))
        assertFalse(ContextActions.urgent(null))
    }

    @Test fun theOneMillionWindowIsOfferedOnlyWhenNotInUse() {
        assertEquals("claude-opus-5-5[1m]", ContextActions.wider(s(), choices)?.id)
        assertNull(ContextActions.wider(s(model = Model("claude-opus-5-5[1m]")), choices))
        assertNull(ContextActions.wider(s(model = Model("claude-sonnet-5")), choices))
        assertNull(ContextActions.wider(s(), null))
    }

    @Test fun theMasterIsTheSessionCalledMaster() {
        val st = State(v = 1, ts = 0, host = "pc", sessions = listOf(s("kb"), s("master")))
        assertEquals("master", ContextActions.master(st)?.name)
        assertNull(ContextActions.master(st.copy(sessions = listOf(s("kb"), s("master", state = SessionState.GONE)))))
    }
}
