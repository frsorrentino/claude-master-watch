package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

/**
 * «Oggi» (proposta 34, fase 1): una barra per ora con quanto hanno lavorato le sessioni, contata dagli eventi che
 * l'orologio ha già in casa. Le barre arrivano fino all'ora corrente: le ore future non si disegnano vuote.
 */
class DayBarsTest {
    private val zone = ZoneId.of("Europe/Rome")
    private fun ev(kind: EventKind, ts: Long, n: Int = 0) =
        Event(key = "$kind-$ts-$n", kind = kind, session = "atlas-shop", ts = ts, title = "x")

    // 16/09/2026, 00:00 locali
    private val mezzanotte = java.time.LocalDate.of(2026, 9, 16).atStartOfDay(zone).toEpochSecond()
    private fun ore(h: Int, m: Int = 0) = mezzanotte + h * 3600L + m * 60

    @Test fun unaBarraPerOgniOraFinoAOra() {
        val out = DayBars.today(emptyList(), now = ore(9, 30), zone = zone)
        assertEquals(10, out.size)
        assertEquals(0, out.first().hour)
        assertEquals(9, out.last().hour)
        assertTrue(out.all { it.count == 0 })
    }

    @Test fun contaGliEventiNellOraGiusta() {
        val eventi = listOf(
            ev(EventKind.LAUNCHED, ore(8, 5)),
            ev(EventKind.OUTCOME, ore(8, 40), 1),
            ev(EventKind.QUESTION, ore(9, 10)),
        )
        val out = DayBars.today(eventi, now = ore(9, 30), zone = zone)
        assertEquals(2, out.first { it.hour == 8 }.count)
        assertEquals(1, out.first { it.hour == 9 }.count)
        assertEquals(0, out.first { it.hour == 7 }.count)
    }

    @Test fun ieriNonEntra() {
        val out = DayBars.today(listOf(ev(EventKind.OUTCOME, ore(8) - 24 * 3600)), now = ore(9), zone = zone)
        assertTrue(out.all { it.count == 0 })
    }

    @Test fun gliEventiDiQuotaNonSonoLavoro() {
        val out = DayBars.today(listOf(ev(EventKind.QUOTA, ore(8, 5))), now = ore(9), zone = zone)
        assertEquals(0, out.first { it.hour == 8 }.count)
    }

    @Test fun ilPiuAltoServeAScalareLeBarre() {
        val eventi = listOf(ev(EventKind.OUTCOME, ore(8, 5)), ev(EventKind.OUTCOME, ore(8, 6), 1), ev(EventKind.ANSWERED, ore(9, 1)))
        assertEquals(2, DayBars.peak(DayBars.today(eventi, now = ore(9, 30), zone = zone)))
    }
}
