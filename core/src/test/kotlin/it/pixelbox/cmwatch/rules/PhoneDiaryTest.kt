package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import org.junit.Assert.*
import org.junit.Test

class PhoneDiaryTest {
    private fun e(key: String, kind: EventKind, ts: Long, ref: String?, body: String = key) =
        Event(key = key, kind = kind, ts = ts, title = key, body = body, ref = ref)

    private val ev = listOf(
        e("r1", EventKind.RECAP, 100, "2026-09-27"), e("r2", EventKind.RECAP, 200, "2026-09-28"),
        e("r2b", EventKind.RECAP, 250, "2026-09-28", body = "resent"), e("n1", EventKind.NIGHT_REPORT, 150, "2026-09-28"),
        e("n2", EventKind.NIGHT_REPORT, 300, "2026-09-29"), e("q", EventKind.QUOTA, 400, null),
    )

    @Test fun oneRecapPerDayLatestSendWinsNewestDayFirst() {
        val r = PhoneDiary.recaps(ev)
        assertEquals(listOf("2026-09-28", "2026-09-27"), r.map { it.ref })
        assertEquals("resent", r[0].body)
    }

    @Test fun lastNightReport() = assertEquals("n2", PhoneDiary.lastNightReport(ev)!!.key)

    @Test fun noEventsNoDiary() {
        assertTrue(PhoneDiary.recaps(emptyList()).isEmpty()); assertNull(PhoneDiary.lastNightReport(emptyList()))
    }
}
