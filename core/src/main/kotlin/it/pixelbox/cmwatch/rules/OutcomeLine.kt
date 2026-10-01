package it.pixelbox.cmwatch.rules

/**
 * La riga «Watch: …» in fondo alle risposte (la chiede la master: è quella che mostra l'orologio). Nell'app del telefono
 * si legge come «Esito», il nome che ha nel terminale (segnalazione 01/10 22:07); se la risposta ha già la sua riga
 * «Esito:», la riga per l'orologio sparisce invece di ripeterla. Solo una riga che comincia con «Watch:».
 */
object OutcomeLine {
    private const val WATCH = "Watch:"
    private const val OUTCOME = "Esito:"

    fun forPhone(text: String, label: String): String {
        val lines = text.split('\n')
        if (lines.none { it.trimStart().startsWith(WATCH) }) return text
        val hasOutcome = lines.any { it.trimStart().startsWith(OUTCOME) }
        val out = lines.mapNotNull { l ->
            val t = l.trimStart()
            when {
                !t.startsWith(WATCH) -> l
                hasOutcome -> null
                else -> "$label:" + t.removePrefix(WATCH)
            }
        }
        return out.joinToString("\n").trimEnd()
    }
}
