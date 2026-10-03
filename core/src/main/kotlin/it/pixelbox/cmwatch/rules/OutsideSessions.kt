package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session

/**
 * «Fuori dalle sessioni» (mockup A, scelto da Franz il 03/10 alle 22:34): sotto le sessioni, le righe di servizio divise in
 * categorie con la loro intestazione, dalla più urgente. Da sistemare (contesto alto), programmati, da riprendere (i
 * prossimi passi del diario), stanotte (resoconto e coda), chiuse. Una categoria vuota non si mostra.
 */
object OutsideSessions {
    enum class Category { FIX, SCHEDULED, RESUME, NIGHT, CLOSED }

    data class Group(val category: Category, val rows: List<MasterHome.Row> = emptyList(), val closed: List<Session> = emptyList()) {
        val count: Int get() = if (category == Category.CLOSED) closed.size else rows.size
    }

    private fun categoryOf(kind: MasterHome.Kind): Category? = when (kind) {
        MasterHome.Kind.CONTEXT -> Category.FIX
        MasterHome.Kind.SCHEDULED -> Category.SCHEDULED
        MasterHome.Kind.NEXT_STEP -> Category.RESUME
        MasterHome.Kind.NIGHT_REPORT, MasterHome.Kind.NIGHT -> Category.NIGHT
        // Domande e turni finiti hanno le loro card fra le sessioni.
        MasterHome.Kind.QUESTION, MasterHome.Kind.FINISHED -> null
    }

    fun groups(service: List<MasterHome.Row>, closed: List<Session>): List<Group> {
        val byCategory = service.groupBy { categoryOf(it.kind) }
        return Category.entries.mapNotNull { c ->
            when (c) {
                Category.CLOSED -> closed.takeIf { it.isNotEmpty() }?.let { Group(c, closed = it) }
                // La notte: prima il resoconto (la mattina), poi la coda.
                Category.NIGHT -> byCategory[c]?.sortedBy { if (it.kind == MasterHome.Kind.NIGHT_REPORT) 0 else 1 }?.let { Group(c, it) }
                else -> byCategory[c]?.let { Group(c, it) }
            }
        }
    }
}
