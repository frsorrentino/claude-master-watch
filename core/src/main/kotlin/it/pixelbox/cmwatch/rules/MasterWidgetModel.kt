package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/**
 * Il widget Master (spec 2026-10-01-telefono-widget-master-design.md): l'ultimo messaggio della master, o la sua domanda
 * se ne ha una, senza le righe di servizio («Prossimi:», «Watch:» letta come esito) e senza righe vuote; poi le sessioni
 * vive come chip, nell'ordine della regia (prima chi aspetta), senza la master, al massimo `maxChips` più il conto delle
 * altre.
 */
object MasterWidgetModel {
    data class Chip(val name: String, val state: SessionState, val context: Int?)
    data class Model(val master: Session?, val asking: Boolean, val text: String?, val at: Long?, val chips: List<Chip>, val more: Int)

    fun build(state: State?, maxChips: Int, outcomeLabel: String): Model {
        if (state == null) return Model(null, false, null, null, emptyList(), 0)
        val master = ContextActions.master(state)
        val q = master?.question
        val text = q?.text ?: master?.outcome?.full?.let { full ->
            // Senza i segni del markdown (revisione finale 02/10): il widget non sa formattare.
            Markdown.parse(OutcomeLine.forPhone(NextSteps.parse(full).text, outcomeLabel)).text.replace(Regex("\n{2,}"), "\n").trim()
        }
        val live = PhoneBoard.sections(state).filter { it.group != PhoneBoard.Group.CLOSED }.flatMap { it.sessions }
            .filter { it.name != master?.name }
        return Model(
            master, q != null, text, q?.askedAt ?: master?.outcome?.at,
            live.take(maxChips).map { Chip(it.name, it.state, it.context) }, (live.size - maxChips).coerceAtLeast(0),
        )
    }
}
