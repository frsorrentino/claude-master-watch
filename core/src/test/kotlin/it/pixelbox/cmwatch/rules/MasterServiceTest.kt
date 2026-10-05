package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Outcome
import it.pixelbox.cmwatch.contract.Recurring
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contratto 1.37: gli stessi casi di web/src/lib/masterService.test.ts. */
class MasterServiceTest {
    private val st = ContractJson.decodeState(Fixtures.stateQuestion)
    private val atlas = st.sessions.first { it.name == "atlas-shop" }
    private val field = st.sessions.first { it.name == "field-notes" }
    private val at = atlas.advice!!.at

    @Test fun adviceWithDotAndCostMidWork() {
        assertEquals(MasterService.AdviceView("claude-fable-5-1", "high", atlas.advice!!.reason, true, 36000L), MasterService.advice(atlas, st.choices, at + 60))
        val fresh = atlas.copy(advice = atlas.advice!!.copy(whenToSwitch = "now", differs = false))
        val v = MasterService.advice(fresh, st.choices, at)!!
        assertFalse(v.dot); assertNull(v.cost)
    }

    @Test fun adviceHiddenWhenOldOrOutsideTheChoices() {
        assertNull(MasterService.advice(atlas, st.choices, at + 6 * 3600))
        assertNull(MasterService.advice(atlas.copy(advice = atlas.advice!!.copy(model = "claude-x")), st.choices, at))
        assertNull(MasterService.advice(atlas.copy(advice = atlas.advice!!.copy(effort = "turbo")), st.choices, at))
        assertNull(MasterService.advice(field, st.choices, at))
        assertEquals("36.000", MasterService.tokens(36000))
    }

    @Test fun approveNoteDefaultsToOk() {
        assertEquals("ok", MasterService.approveText("  "))
        assertEquals("vai pure", MasterService.approveText(" vai pure "))
    }

    @Test fun contextBandsAndNudge() {
        assertEquals(listOf(null, 60, 60, 70, 80, null), listOf(59, 60, 69, 70, 85, null).map { ContextActions.band(it) })
        val s = field.copy(state = SessionState.IDLE, context = 64)
        assertEquals(60, ContextActions.nudge(s, null))
        assertNull(ContextActions.nudge(s, 60))
        assertEquals(70, ContextActions.nudge(s.copy(context = 72), 60))
        assertNull(ContextActions.nudge(s.copy(state = SessionState.BUSY), null))
        assertNull(ContextActions.nudge(s.copy(context = 40), null))
    }

    @Test fun handoffPromptAndClear() {
        val master = field.copy(name = ContextActions.MASTER)
        val withRec = st.copy(recurring = st.recurring.orEmpty() + Recurring("master-handoff", "Handoff", "fai il master handoff"))
        assertEquals("fai il master handoff", ContextActions.handoffPrompt(withRec, master, "altro"))
        assertEquals("altro", ContextActions.handoffPrompt(withRec, field, "altro"))
        assertEquals("altro", ContextActions.handoffPrompt(st, master, "altro"))
        assertTrue(ContextActions.canClear(st))
        assertFalse(ContextActions.canClear(st.copy(slash = listOf("compact"))))
        assertFalse(ContextActions.clearDue(field.copy(state = SessionState.BUSY, outcome = Outcome("x", "x", 900)), 1000))
        assertFalse(ContextActions.clearDue(field.copy(state = SessionState.IDLE, outcome = Outcome("x", "x", 900)), 1000))
        assertTrue(ContextActions.clearDue(field.copy(state = SessionState.IDLE, outcome = Outcome("x", "x", 1100)), 1000))
        assertFalse(ContextActions.clearDue(null, 1000))
    }

    @Test fun decisionProjectAndDraft() {
        assertEquals("atlas-shop", MasterService.decisionProject(atlas))
        assertEquals("I prezzi includono l'IVA.", MasterService.decisionDraft("I prezzi includono l'IVA.\n\nProssimi: a · b\nWatch: ok"))
        val long = MasterService.decisionDraft("parola ".repeat(400))
        assertTrue(long.length <= MasterService.DECISION_MAX)
        assertFalse(long.endsWith("…")); assertFalse(long.endsWith(" "))
    }

    @Test fun cleanupOnlyWhenStoppedAndCloseOnlyWithoutWindow() {
        val stopped = field.copy(state = SessionState.IDLE, attached = false)
        assertEquals(MasterService.Cleanup(MasterService.Cleanup.Kind.FINISHED, null, true), MasterService.cleanup(stopped, true))
        assertEquals(MasterService.Cleanup(MasterService.Cleanup.Kind.FINISHED, null, false), MasterService.cleanup(stopped.copy(attached = true), true))
        assertEquals(MasterService.Cleanup(MasterService.Cleanup.Kind.DUPLICATE, "field-notes", true),
            MasterService.cleanup(stopped.copy(finished = false, duplicateOf = "field-notes", name = "field-notes-2"), true))
        assertNull(MasterService.cleanup(field.copy(state = SessionState.BUSY), true))
        assertEquals(MasterService.Cleanup(MasterService.Cleanup.Kind.FINISHED, null, false), MasterService.cleanup(stopped, false))
        assertNull(MasterService.cleanup(atlas.copy(finished = false), true))
    }
}
