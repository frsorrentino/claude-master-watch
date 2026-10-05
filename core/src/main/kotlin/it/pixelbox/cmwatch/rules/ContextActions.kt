package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Choices
import it.pixelbox.cmwatch.contract.Model
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/**
 * Il foglio che si apre toccando la percentuale del contesto, e la master a cui chiedere un controllo (proposte
 * approvate da Franz, 01/10 21:19). I testi dei prompt stanno nell'app (strings.xml); qui le regole.
 */
object ContextActions {
    /** Contesto quasi pieno, per la lista della master (Franz, 01/10 20:22: «quando sopra 80»). */
    const val URGENT = 80

    /**
     * Contratto 1.37: le fasce in cui la proposta sopra il campo torna dopo essere stata chiusa. Dalla prima, «Handoff,
     * poi /clear» è anche il tasto pieno del foglio del contesto (mockup approvato il 05/10 21:07, pieno al 64 %).
     */
    val BANDS = listOf(60, 70, 80)

    fun band(pct: Int?): Int? = pct?.let { p -> BANDS.lastOrNull { p >= it } }

    /** La proposta «handoff, poi /clear» sopra il campo: sessione ferma, contesto oltre il 60 %, non chiusa in questa fascia. */
    fun nudge(s: Session, dismissed: Int?): Int? {
        if (s.state != SessionState.IDLE) return null
        val b = band(s.context) ?: return null
        return b.takeIf { dismissed == null || dismissed < it }
    }

    /** Alla master la sua ricorrente `master-handoff`, se c'è; alle altre il prompt di sempre. */
    fun handoffPrompt(st: State, s: Session, fallback: String): String =
        (if (s.name == MASTER) st.recurring?.firstOrNull { it.id == "master-handoff" }?.prompt else null) ?: fallback

    fun canClear(st: State): Boolean = st.slash?.contains("clear") == true

    /** /clear a turno finito: la sessione è di nuovo ferma con un esito più nuovo dell'handoff. */
    fun clearDue(s: Session?, sentAt: Long): Boolean = s != null && s.state == SessionState.IDLE && (s.outcome?.at ?: 0) >= sentAt

    fun urgent(pct: Int?): Boolean = pct != null && pct >= URGENT

    /** La finestra da 1M dello stesso modello, se c'è fra le scelte e la sessione non la usa già. */
    fun wider(s: Session, choices: Choices?): Model? {
        val id = s.model?.id ?: return null
        if (id.endsWith("[1m]")) return null
        return choices?.models?.firstOrNull { it.id.endsWith("[1m]") && Tune.sameModel(it.id, id) }
    }

    /** La master è la sessione che si chiama «master», se è aperta. */
    fun master(state: State?): Session? = state?.sessions?.firstOrNull { it.name == MASTER && it.state != SessionState.GONE }

    const val MASTER = "master"
}
