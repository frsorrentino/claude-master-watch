package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind

/** La scheda Diario dagli eventi del contratto 1.18: un diario per giorno (l'ultimo invio vince), il resoconto più recente. */
object PhoneDiary {
    fun recaps(events: List<Event>): List<Event> =
        events.filter { it.kind == EventKind.RECAP && it.ref != null }
            .groupBy { it.ref }.values.map { day -> day.maxBy { it.ts } }
            .sortedByDescending { it.ref }

    fun lastNightReport(events: List<Event>): Event? = events.filter { it.kind == EventKind.NIGHT_REPORT }.maxByOrNull { it.ts }
}
