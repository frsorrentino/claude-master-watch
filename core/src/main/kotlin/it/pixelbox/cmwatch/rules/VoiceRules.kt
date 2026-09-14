package it.pixelbox.cmwatch.rules

/**
 * Scelta della voce della lettura (Franz, 14/09 13:28: «si può avere una voce maschile?»). Android non dice se una voce
 * è maschile o femminile: le voci italiane del motore si provano una alla volta dalle impostazioni, e si tiene quella
 * che suona giusta. `null` è la voce predefinita del motore.
 */
object VoiceRules {
    /** La voce dopo `current`: dalla predefinita alla prima, poi in ordine, dopo l'ultima di nuovo la predefinita. */
    fun next(voices: List<String>, current: String?): String? {
        if (voices.isEmpty()) return null
        val i = voices.indexOf(current)
        return if (current == null || i < 0) voices.first() else voices.getOrNull(i + 1)
    }

    /** Posizione da mostrare: 0 per la predefinita o per una voce che non c'è più, altrimenti da 1 a n. */
    fun position(voices: List<String>, current: String?): Int = voices.indexOf(current) + 1
}
