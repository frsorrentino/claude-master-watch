package it.pixelbox.cmwatch.rules

/**
 * Il mini-controller della lettura (Franz, 03/10 23:00): una pillola su ogni schermata mentre la voce legge, con da dove
 * arriva il testo e la sua prima riga. Qui la prima riga: testo vero, senza markdown, senza l'etichetta «Esito:» e senza la
 * riga «Watch:» per l'orologio.
 */
object ReadingBar {
    private const val OUTCOME = "Esito:"
    private const val WATCH = "Watch:"

    fun excerpt(text: String): String = Markdown.parse(text).text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith(WATCH) }
        .firstOrNull()?.removePrefix(OUTCOME)?.trim().orEmpty()
}
