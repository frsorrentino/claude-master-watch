package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Option
import it.pixelbox.cmwatch.contract.Question
import it.pixelbox.cmwatch.contract.QuestionKind
import it.pixelbox.cmwatch.contract.Tier

/** Regole della Domanda (design, sezioni 2 e 5): tier high con pressione lunga, «consenti sempre» mai per high. */
object QuestionRules {
    fun needsLongPress(tier: Tier) = tier == Tier.HIGH
    fun allowAllVisible(q: Question) = q.kind == QuestionKind.PERMISSION && q.tier != Tier.HIGH
    fun optionLabel(o: Option) = "${o.n} · ${o.label}"
    /** Un solo bottone pieno per schermata: la prima opzione. */
    fun isPrimary(index: Int) = index == 0
}
