package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.AgendaPage
import it.pixelbox.cmwatch.contract.AgendaRow

/**
 * Le righe dell'agenda nel Recap (mockup approvato l'08/10, contratto 1.46): le aperte per chi deve muoversi — tu, Claude
 * (con «Fallo», che va alla master), altri — e in fondo tutte le altre (fatto, sospeso, chiuso o uno stato che non si
 * conosce, mostrato com'è). Il relay le manda nell'ordine del file: dentro ogni gruppo resta quell'ordine.
 */
object RecapAgenda {
    enum class Mark { PERSONAL, AGENCY, DESK, NONE }

    data class Model(val you: List<AgendaRow>, val claude: List<AgendaRow>, val other: List<AgendaRow>, val rest: List<AgendaRow>) {
        val isEmpty get() = you.isEmpty() && claude.isEmpty() && other.isEmpty() && rest.isEmpty()
    }

    /** Gli ambiti dei filtri, nell'ordine della pagina. */
    val SCOPES = listOf("agenzia", "personale", "postazione")

    private fun norm(s: String) = s.trim().lowercase()

    /** Il segno a sinistra: tondo personale, quadrato agenzia, rombo postazione. */
    fun mark(scope: String): Mark = when (norm(scope)) {
        "personale" -> Mark.PERSONAL
        "agenzia" -> Mark.AGENCY
        "postazione" -> Mark.DESK
        else -> Mark.NONE
    }

    /** `blocks` è testo libero: «franz» (e «owner» delle fixture) sei tu, «claude» è Claude, il resto è fermo su altro. */
    fun isYou(blocks: String) = norm(blocks) in setOf("franz", "owner")
    fun isClaude(blocks: String) = norm(blocks) == "claude"
    fun isOpen(row: AgendaRow) = norm(row.state) == "aperto"

    /** `scope` null = tutti gli ambiti. La riga d'intestazione del TSV (stato, ambito, …), se arriva, non è una riga. */
    fun of(page: AgendaPage?, scope: String? = null): Model {
        val rows = page?.rows.orEmpty()
            .filter { r -> r.title.isNotBlank() && norm(r.state) != "stato" }
            .filter { r -> scope == null || norm(r.scope) == norm(scope) }
        val open = rows.filter(::isOpen)
        return Model(
            you = open.filter { isYou(it.blocks) }, claude = open.filter { isClaude(it.blocks) },
            other = open.filter { !isYou(it.blocks) && !isClaude(it.blocks) }, rest = rows.filterNot(::isOpen),
        )
    }

    /** «Fallo» su un lavoro che può fare Claude: alla master, dal foglio di conferma. `template` = «… %1$s …» col titolo e il rimando. */
    fun doIt(row: AgendaRow, template: String = "Fai questo lavoro dell'agenda: %1\$s%2\$s", refTemplate: String = " (%1\$s)"): RecapActions.Action {
        val ref = row.ref.trim().takeIf { it.isNotEmpty() }?.let { refTemplate.format(it) }.orEmpty()
        return RecapActions.Action(row.title, template.format(row.title, ref), ContextActions.MASTER, "agenda", viaMaster = true, agenda = true)
    }
}
