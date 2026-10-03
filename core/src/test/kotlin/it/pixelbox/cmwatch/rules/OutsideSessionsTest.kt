package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.rules.MasterHome.Kind
import it.pixelbox.cmwatch.rules.OutsideSessions.Category
import org.junit.Assert.*
import org.junit.Test

/** «Fuori dalle sessioni» (mockup A, Franz 03/10 22:34): le righe di servizio in categorie, dalla più urgente. */
class OutsideSessionsTest {
    private fun row(kind: Kind, title: String = "") = MasterHome.Row(kind, title)
    private fun gone(name: String) = Session(id = name, name = name, account = "personale", project = name, state = SessionState.GONE, since = 0)

    @Test fun categoriesInOrderOfUrgency() {
        val g = OutsideSessions.groups(
            listOf(row(Kind.NIGHT), row(Kind.NEXT_STEP, "kb"), row(Kind.SCHEDULED), row(Kind.CONTEXT, "app"), row(Kind.NEXT_STEP, "atlas"), row(Kind.NIGHT_REPORT)),
            listOf(gone("old")),
        )
        assertEquals(listOf(Category.FIX, Category.SCHEDULED, Category.RESUME, Category.NIGHT, Category.CLOSED), g.map { it.category })
        assertEquals(listOf("kb", "atlas"), g.single { it.category == Category.RESUME }.rows.map { it.title })
        // La notte: prima il resoconto (la mattina), poi la coda.
        assertEquals(listOf(Kind.NIGHT_REPORT, Kind.NIGHT), g.single { it.category == Category.NIGHT }.rows.map { it.kind })
        assertEquals(1, g.last().count); assertEquals(listOf("old"), g.last().closed.map { it.name })
    }

    @Test fun emptyCategoriesAreLeftOut() {
        assertEquals(listOf(Category.NIGHT), OutsideSessions.groups(listOf(row(Kind.NIGHT)), emptyList()).map { it.category })
        assertTrue(OutsideSessions.groups(emptyList(), emptyList()).isEmpty())
    }

    @Test fun countsAreTheRowsOrTheClosedSessions() {
        val g = OutsideSessions.groups(listOf(row(Kind.NEXT_STEP, "a"), row(Kind.NEXT_STEP, "b")), listOf(gone("x"), gone("y"), gone("z")))
        assertEquals(listOf(2, 3), g.map { it.count })
    }
}
