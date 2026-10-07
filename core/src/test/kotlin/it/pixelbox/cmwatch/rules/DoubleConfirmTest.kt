package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.DoubleConfirm.Machine
import it.pixelbox.cmwatch.rules.DoubleConfirm.Phase
import it.pixelbox.cmwatch.rules.DoubleConfirm.Why
import org.junit.Assert.assertEquals
import org.junit.Test

class DoubleConfirmTest {
    private val t = 1_000_000L
    private val armed = DoubleConfirm.arm(Machine(), "atlas-release-2-4", "approve", t)

    @Test fun approveArms() {
        assertEquals(Phase.ARMED, armed.phase)
        assertEquals("atlas-release-2-4", armed.target)
        assertEquals("approve", armed.firstKey)
        assertEquals(t, armed.armedAt)
    }

    @Test fun holdingAnotherKeyForTwoSecondsConfirms() {
        val held = DoubleConfirm.press(armed, "confirm", t + 1_000)
        assertEquals(Phase.ARMED, DoubleConfirm.tick(held, t + 2_999).phase)
        val confirming = DoubleConfirm.tick(held, t + 3_000)
        assertEquals(Phase.CONFIRMING, confirming.phase)
        assertEquals(Phase.SENT, DoubleConfirm.result(confirming, ok = true).phase)
    }

    // Il tick può arrivare in ritardo: conta l'istante in cui la pressione ha raggiunto i 2 secondi.
    @Test fun releasingAfterTwoSecondsConfirmsEvenWithoutATick() {
        val held = DoubleConfirm.press(armed, "confirm", t + 1_000)
        assertEquals(Phase.CONFIRMING, DoubleConfirm.release(held, "confirm", t + 3_200).phase)
        assertEquals(Phase.CONFIRMING, DoubleConfirm.tick(DoubleConfirm.press(armed, "confirm", t + 7_000), t + 12_000).phase)
    }

    @Test fun theSameKeyNeverConfirms() {
        val same = DoubleConfirm.press(armed, "approve", t + 500)
        assertEquals(Phase.ARMED, DoubleConfirm.tick(same, t + 3_000).phase)
        assertEquals(Phase.ARMED, DoubleConfirm.release(same, "approve", t + 3_000).phase)
    }

    @Test fun releasingEarlyStopsTheHoldButStaysArmed() {
        val early = DoubleConfirm.release(DoubleConfirm.press(armed, "confirm", t + 1_000), "confirm", t + 2_500)
        assertEquals(Phase.ARMED, early.phase)
        assertEquals(Phase.ARMED, DoubleConfirm.tick(early, t + 4_000).phase)
        val again = DoubleConfirm.press(early, "confirm", t + 4_000)
        assertEquals(Phase.CONFIRMING, DoubleConfirm.tick(again, t + 6_000).phase)
    }

    @Test fun tenSecondsWithoutConfirmCancel() {
        assertEquals(Phase.ARMED, DoubleConfirm.tick(armed, t + 9_999).phase)
        val late = DoubleConfirm.tick(armed, t + 10_000)
        assertEquals(Phase.CANCELLED, late.phase)
        assertEquals(Why.TIMEOUT, late.why)
    }

    // Una pressione iniziata a 9 secondi arriva a 2 secondi dopo la scadenza: annullata.
    @Test fun aHoldThatEndsAfterTheDeadlineCancels() {
        val held = DoubleConfirm.press(armed, "confirm", t + 9_000)
        assertEquals(Why.TIMEOUT, DoubleConfirm.tick(held, t + 11_000).why)
        assertEquals(Phase.CANCELLED, DoubleConfirm.release(held, "confirm", t + 11_000).phase)
    }

    @Test fun cancelTap() {
        val c = DoubleConfirm.cancel(armed)
        assertEquals(Phase.CANCELLED, c.phase)
        assertEquals(Why.TAP, c.why)
    }

    @Test fun aVanishedRequestCancelsOnlyBeforeSending() {
        val gone = DoubleConfirm.tick(armed, t + 1_000, present = false)
        assertEquals(Phase.CANCELLED, gone.phase)
        assertEquals(Why.VANISHED, gone.why)
        // in confirming la richiesta sparisce proprio perché l'ok è arrivato
        val confirming = DoubleConfirm.tick(DoubleConfirm.press(armed, "confirm", t), t + 2_000)
        assertEquals(Phase.CONFIRMING, DoubleConfirm.tick(confirming, t + 2_500, present = false).phase)
    }

    @Test fun aRefusalFromTheRelayCancels() {
        val confirming = DoubleConfirm.tick(DoubleConfirm.press(armed, "confirm", t), t + 2_000)
        val refused = DoubleConfirm.result(confirming, ok = false)
        assertEquals(Phase.CANCELLED, refused.phase)
        assertEquals(Why.REFUSED, refused.why)
    }

    @Test fun eventsOutsideTheirPhaseChangeNothing() {
        val idle = Machine()
        assertEquals(idle, DoubleConfirm.press(idle, "confirm", t))
        assertEquals(idle, DoubleConfirm.tick(idle, t))
        assertEquals(idle, DoubleConfirm.cancel(idle))
        assertEquals(idle, DoubleConfirm.result(idle, ok = true))
        assertEquals(armed, DoubleConfirm.arm(armed, "other", "approve", t + 1))
        assertEquals(armed, DoubleConfirm.result(armed, ok = true))
        val confirming = DoubleConfirm.tick(DoubleConfirm.press(armed, "confirm", t), t + 2_000)
        assertEquals(confirming, DoubleConfirm.cancel(confirming))
        assertEquals(confirming, DoubleConfirm.tick(confirming, t + 20_000))
    }

    @Test fun aClosedCycleCanStartAgain() {
        val cancelled = DoubleConfirm.cancel(armed)
        assertEquals(Phase.ARMED, DoubleConfirm.arm(cancelled, "atlas-release-2-4", "approve", t + 20_000).phase)
        val sent = DoubleConfirm.result(DoubleConfirm.tick(DoubleConfirm.press(armed, "confirm", t), t + 2_000), ok = true)
        val again = DoubleConfirm.arm(sent, "blog-post", "approve", t + 30_000)
        assertEquals(Phase.ARMED, again.phase)
        assertEquals(null, again.why)
        assertEquals("blog-post", again.target)
    }
}
