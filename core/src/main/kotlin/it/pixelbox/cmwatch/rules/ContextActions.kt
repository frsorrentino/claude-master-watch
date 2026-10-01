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
    /** Da qui «Handoff e riavvio» è il tasto pieno del foglio (Franz, 01/10 20:22: «quando sopra 80»). */
    const val URGENT = 80

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
