package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.contract.Model
import it.pixelbox.cmwatch.contract.Session

/**
 * Modello ed effort scelti dal telefono (segnalazioni 01/10 20:22 e 20:25). Il PC legge i due valori dalla trascrizione,
 * che li scrive solo al turno successivo: fino ad allora la pillola mostra la scelta fatta. La scelta vale finché lo
 * stato resta quello di prima (`was`), il PC non la rifiuta e non passano [HOLD_S] secondi; poi vince lo stato.
 */
object Tune {
    const val HOLD_S = 15 * 60L

    /** `was`: l'id del modello o l'effort al momento della scelta. */
    data class Pick(val cmd: String, val at: Long, val model: Model? = null, val effort: String? = null, val was: String? = null)

    /** «claude-opus-5-5[1m]» e «claude-opus-5-5» sono lo stesso modello: la lista delle scelte porta la finestra, la sessione no. */
    fun sameModel(a: String?, b: String?): Boolean = a != null && b != null && a.substringBefore('[') == b.substringBefore('[')

    fun model(s: Session, pick: Pick?, result: CmdResult?, now: Long): Model? {
        val p = pick?.takeIf { held(it, result, now) && it.model != null } ?: return s.model
        val unchanged = s.model?.id?.substringBefore('[') == p.was?.substringBefore('[')
        return if (unchanged) p.model else s.model
    }

    fun effort(s: Session, pick: Pick?, result: CmdResult?, now: Long): String? {
        val p = pick?.takeIf { held(it, result, now) && it.effort != null } ?: return s.effort
        return if (s.effort == p.was) p.effort else s.effort
    }

    private fun held(p: Pick, result: CmdResult?, now: Long) = result?.ok != false && now - p.at < HOLD_S
}
