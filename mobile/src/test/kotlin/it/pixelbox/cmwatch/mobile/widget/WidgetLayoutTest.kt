package it.pixelbox.cmwatch.mobile.widget

import org.junit.Assert.*
import org.junit.Test

/**
 * Le misure del widget copiate da ads-widget (`computeLayoutMetrics`, Franz 01/10 13:22: «accorda meglio a ads
 * widget»). La taglia di riferimento è quella sul telefono di Franz: 4×1, 401×103 dp.
 */
class WidgetLayoutTest {
    @Test fun tickerOnFranzPhone() {
        val m = WidgetLayout.of(401f, 103f)
        assertTrue(m.ticker)
        assertEquals(1, m.padV); assertEquals(4, m.padH)
        assertEquals(27, m.headerHeight); assertEquals(5, m.headerInset)
        assertEquals(7, m.arrow); assertEquals(13, m.title); assertEquals(12, m.time)
        assertEquals(23, m.chip); assertEquals(9, m.chipRadius); assertEquals(15, m.refreshGlyph); assertEquals(50, m.refreshBox)
        assertEquals(74, m.ring); assertEquals(6, m.ringStroke); assertEquals(18, m.ringValue); assertEquals(8, m.ringLabel)
        assertTrue(m.gauge)
        assertEquals(21, m.icon); assertEquals(12, m.label)
        // Misurato sul telefono (screenshot 01/10 13:22, scala caratteri 1,3): le cifre dei KPI di ads-widget sono alte
        // 21 dp, cioè 24sp, non i 29 della formula.
        assertEquals(24, m.value)
    }

    @Test fun tallWidgetHasTheFullHeader() {
        val m = WidgetLayout.of(401f, 220f)
        assertFalse(m.ticker); assertFalse(m.gauge)
        assertEquals(14, m.padV); assertEquals(14, m.padH)
        assertEquals(100, m.ring); assertEquals(16, m.ringValue)
        assertEquals(12, m.title); assertEquals(22, m.value); assertEquals(10, m.label); assertEquals(12, m.icon)
    }

    @Test fun smallSizesClampTheScale() {
        val m = WidgetLayout.of(110f, 40f)
        assertEquals(0.5f, m.sf, 0.001f)
        assertTrue(m.ring >= 42)
    }
}
