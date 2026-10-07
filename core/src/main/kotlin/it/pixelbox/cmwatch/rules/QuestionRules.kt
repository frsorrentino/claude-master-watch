package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Option
import it.pixelbox.cmwatch.contract.Question
import it.pixelbox.cmwatch.contract.QuestionKind
import it.pixelbox.cmwatch.contract.Tier

/** Regole della Domanda (design, sezioni 2 e 5): tier high con pressione lunga, «consenti sempre» mai per high. */
object QuestionRules {
    fun needsLongPress(tier: Tier) = tier == Tier.HIGH
    /** Senza opzioni lette non c'è «non chiedere più»: il relay rifiuterebbe allow_all (dal vivo, 30/09). */
    fun allowAllVisible(q: Question) = q.kind == QuestionKind.PERMISSION && q.tier != Tier.HIGH && q.options.isNotEmpty()
    /**
     * «n · etichetta» sulla prima riga che dice qualcosa: l'anteprima dell'opzione arriva disegnata a caratteri
     * (┌─│) e sul tasto diventava righe vuote e un nome tagliato (Franz, 15/09 16:13).
     */
    fun optionLabel(o: Option): String = "${o.n} · ${optionText(o.label)}"

    /** Più corta di così un'opzione sta in un tasto diviso in parti uguali (Franz, 03/10 15:18: tasti sproporzionati). */
    const val INLINE_CHARS = 14

    /** Due o tre opzioni brevi su una riga, in parti uguali; altrimenti una sotto l'altra. */
    fun inline(options: List<Option>): Boolean =
        options.size in 2..3 && options.all { optionText(it.label).length <= INLINE_CHARS }

    /** L'etichetta senza il riquadro: la stessa per tasto, notifica e voce. */
    /**
     * Il testo da mostrare: quello della domanda se ha almeno una lettera o una cifra, altrimenti `fallback`. Il relay ha
     * mandato «-: -» per i prompt di permesso della master, e sull'orologio la domanda sembrava vuota (Franz, 07/10 18:01).
     */
    fun shownText(text: String, fallback: String): String = if (text.any { it.isLetterOrDigit() }) text else fallback

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
