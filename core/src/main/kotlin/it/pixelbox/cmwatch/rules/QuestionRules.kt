package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Option
import it.pixelbox.cmwatch.contract.Question
import it.pixelbox.cmwatch.contract.QuestionKind
import it.pixelbox.cmwatch.contract.Tier

/** Regole della Domanda (design, sezioni 2 e 5): tier high con pressione lunga, «consenti sempre» mai per high. */
object QuestionRules {
    fun needsLongPress(tier: Tier) = tier == Tier.HIGH
    fun allowAllVisible(q: Question) = q.kind == QuestionKind.PERMISSION && q.tier != Tier.HIGH
    /**
     * «n · etichetta» sulla prima riga che dice qualcosa: l'anteprima dell'opzione arriva disegnata a caratteri
     * (┌─│) e sul tasto diventava righe vuote e un nome tagliato (Franz, 15/09 16:13).
     */
    fun optionLabel(o: Option): String = "${o.n} · ${optionText(o.label)}"

    /** L'etichetta senza il riquadro: la stessa per tasto, notifica e voce. */
    fun optionText(label: String): String = label.lines()
        .map { it.replace(DISEGNO, " ").replace(SPAZI, " ").trim() }
        .firstOrNull { it.isNotEmpty() } ?: label.trim()

    /** Caratteri di riquadro e blocchi (U+2500–U+259F). */
    private val DISEGNO = Regex("[\\u2500-\\u259F]")
    private val SPAZI = Regex("\\s+")
    /** Contratto 1.10: «Type something.» con il testo scritto al polso; gli a capo diventano spazi. */
    fun textArg(text: String): String = "text:" + text.replace(Regex("\\s*\\n\\s*"), " ").trim()

    /** Contratto 1.10: «Chat about this», la domanda rifiutata e la sessione in attesa di un messaggio. */
    const val CHAT_ARG = "chat"

    /** Un solo bottone pieno per schermata: la prima opzione. */
    fun isPrimary(index: Int) = index == 0
}
