package it.pixelbox.cmwatch.rules

/**
 * I consigli che la sessione scrive in fondo alla risposta quando si ferma (design 01/10, variante C): l'ultima riga
 * `Prossimi: a · b · c`. Si tolgono dal testo e diventano al massimo [MAX] righe sotto la risposta; un consiglio più lungo
 * di [MAX_CHARS] caratteri si scarta (sotto la risposta va su una riga). Conta solo l'ultima riga non vuota, saltando la
 * riga per l'orologio («Watch:»), che la master chiede dopo tutto il resto.
 */
object NextSteps {
    const val MAX = 3
    const val MAX_CHARS = 40
    private const val PREFIX = "Prossimi:"
    private const val WATCH = "Watch:"

    data class Parsed(val text: String, val steps: List<String>)

    fun parse(text: String): Parsed {
        val lines = text.split('\n')
        val idx = lines.indices.reversed().firstOrNull { i -> lines[i].isNotBlank() && !lines[i].trimStart().startsWith(WATCH) }
            ?: return Parsed(text, emptyList())
        val line = lines[idx].trim()
        if (!line.startsWith(PREFIX)) return Parsed(text, emptyList())
        val steps = line.removePrefix(PREFIX).split('·').map { it.trim() }
            .filter { it.isNotEmpty() && it.length <= MAX_CHARS }.take(MAX)
        val rest = (lines.take(idx) + lines.drop(idx + 1)).joinToString("\n").trimEnd()
        return Parsed(rest, steps)
    }
}
