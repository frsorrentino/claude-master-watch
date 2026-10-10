package it.pixelbox.cmwatch.rules

/**
 * Il mini-controller della lettura (Franz, 03/10 23:00): una pillola su ogni schermata mentre la voce legge, con da dove
 * arriva il testo e la sua prima riga. Qui la prima riga: testo vero, senza markdown, senza l'etichetta «Esito:» e senza la
 * riga «Watch:» per l'orologio.
 */
object ReadingBar {
    private const val OUTCOME = "Esito:"
    private const val WATCH = "Watch:"

    fun excerpt(text: String): String {
        val lines = Markdown.parse(text).text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith(WATCH) }.take(2).toList()
        val first = lines.firstOrNull()?.removePrefix(OUTCOME)?.trim().orEmpty()
        // Una riga che finisce coi due punti annuncia la seguente (Franz, 10/10 16:52: la live mostrava solo «master ha
        // finito:», con l'esito che cominciava da un elenco): stanno insieme.
        return if (first.endsWith(':') && lines.size > 1) "$first ${lines[1]}" else first
    }
}
