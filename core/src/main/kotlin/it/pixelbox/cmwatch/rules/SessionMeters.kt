package it.pixelbox.cmwatch.rules

/**
 * Le misure della sessione come le legge la sua card (Franz, 16/09 08:29: «ogni misura dovrebbe avere la sua
 * grafica»). Il contesto diventa una barra e un colore, l'effort tre tacche: qui stanno i numeri, il disegno sta
 * nell'app. Quello che il PC non manda resta null e non si disegna, come sul resto della schermata.
 */
object SessionMeters {
    /** Le tacche accese: basso 1, medio 2, alto 3. Un effort che non conosciamo non ne accende nessuna. */
    fun effortStep(effort: String?): Int? = when (effort?.trim()?.lowercase()) {
        "low" -> 1
        "medium" -> 2
        "high" -> 3
        else -> null
    }

    /** Quante tacche esistono in tutto: la grafica ne disegna sempre tre, spente quelle oltre il livello. */
    const val EFFORT_STEPS = 3

    /** Oltre tre quarti il contesto è da guardare, dal 90 % in su è il momento di chiudere il turno. */
    fun contextTone(pct: Int?): BriefCards.Tone = when {
        pct == null -> BriefCards.Tone.NEUTRAL
        pct >= 90 -> BriefCards.Tone.ALERT
        pct >= 75 -> BriefCards.Tone.WARN
        else -> BriefCards.Tone.NEUTRAL
    }

    /** La barra va da 0 a 1; oltre il 100 % resta piena invece di uscire dal binario. */
    fun contextFraction(pct: Int?): Float? = pct?.let { (it / 100f).coerceIn(0f, 1f) }
}
