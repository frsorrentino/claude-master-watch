package it.pixelbox.cmwatch.rules

/**
 * Il Registro (ex Diario, mockup approvato da Franz il 02/10 alle 09:10): il resoconto della notte e i diari letti come
 * righe, una per lavoro o per progetto, senza percorsi. Il testo viene dagli eventi del contratto 1.18 (`night_report`,
 * `recap`); una riga che non si riconosce si salta, e senza righe l'app mostra il testo com'è.
 */
object Registro {
    data class Job(val ok: Boolean, val project: String, val seconds: Int?, val text: String)
    data class Line(val project: String, val text: String)

    // «✓ atlas-shop (personal, 812 s, rc=0): Fixed the three flaky tests.»
    private val JOB = Regex("""^\s*([✓✗])\s+(\S+)\s+\((?:[^,()]*,\s*)?(\d+)\s*s(?:,[^)]*)?\):\s*(.*)$""")
    // «● atlas-shop · 6 turns · last 13:45», poi il testo rientrato sulla riga sotto.
    private val PROJECT = Regex("""^[^\p{L}\p{N}\s]+\s+([\p{L}\p{N}._-]+)(?:\s+·.*)?$""")

    fun night(body: String): List<Job> = body.lines().mapNotNull { l ->
        JOB.find(l)?.destructured?.let { (mark, name, secs, text) -> Job(mark == "✓", name, secs.toIntOrNull(), text.trim()) }
    }

    fun recap(body: String): List<Line> {
        val lines = body.lines()
        return lines.mapIndexedNotNull { i, l ->
            val name = PROJECT.find(l)?.groupValues?.get(1) ?: return@mapIndexedNotNull null
            val next = lines.getOrNull(i + 1)?.takeIf { it.startsWith(" ") }?.trim().orEmpty()
            Line(name, next)
        }
    }

    /** I minuti di un lavoro, per eccesso: 812 s sono 14 minuti, non 13. */
    fun minutes(seconds: Int): Int = (seconds + 59) / 60
}
