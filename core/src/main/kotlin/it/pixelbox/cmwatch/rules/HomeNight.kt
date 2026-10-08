package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.NightReportRef

/**
 * Il riquadro Notte in testa alla home (mockup approvato da Franz l'08/10 alle 12:57): c'è quando il PC ha un rapporto
 * della notte e quella notte non è ancora stata aperta. `opened` = il giorno dell'ultimo rapporto aperto.
 */
object HomeNight {
    fun show(ref: NightReportRef?, opened: String?): Boolean = ref != null && ref.date != opened
}
