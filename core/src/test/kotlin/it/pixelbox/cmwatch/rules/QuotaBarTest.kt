package it.pixelbox.cmwatch.rules

import org.junit.Assert.*
import org.junit.Test

class QuotaBarTest {
    @Test fun pesiProporzionaliConStaccoInMezzo() {
        val s = QuotaBar.of(24)
        assertEquals(24, s.fill); assertEquals(76, s.track); assertTrue(s.gap)
    }

    @Test fun percentualePiccolaTieneUnMonconeVisibile() {
        // All'1 % la pillolina piena sarebbe più piccola del suo raggio: si disegna al minimo, non si nasconde.
        val s = QuotaBar.of(1)
        assertEquals(3, s.fill); assertEquals(97, s.track); assertTrue(s.gap)
    }

    @Test fun soloLoZeroMostraLaSolaTraccia() {
        val s = QuotaBar.of(0)
        assertEquals(0, s.fill); assertEquals(100, s.track); assertFalse(s.gap)
        assertEquals(QuotaBar.of(0), QuotaBar.of(null))
    }

    @Test fun quasiPienoMostraSoloIlPieno() {
        val s = QuotaBar.of(99)
        assertEquals(100, s.fill); assertEquals(0, s.track); assertFalse(s.gap)
    }

    @Test fun valoriFuoriScalaVengonoTagliati() {
        assertEquals(QuotaBar.of(100), QuotaBar.of(140))
        assertEquals(QuotaBar.of(0), QuotaBar.of(-5))
    }
}
