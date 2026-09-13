package it.pixelbox.cmwatch.rules

/**
 * Da nome di strumento a frase compiuta. Il contratto porta il campo grezzo («SendMessage», «Bash cd /home/demo/…»,
 * «Read core/src/main/kotlin/.../TileTexts.kt») e sulla tile «SendMessage» non dice niente (Franz, 13/09 20:36).
 * Qui diventa «scrive a un'altra sessione», «esegue cd /home/demo/…», «legge TileTexts.kt»: i percorsi si riducono
 * al nome del file, perché al polso la cartella è rumore.
 */
object ToolText {
    data class Labels(
        val run: String,
        val read: String,
        val edit: String,
        val write: String,
        val search: String,
        val web: String,
        val message: String,
        val delegate: String,
        val plan: String,
        val other: String,
    )

    /** Frase per la tile e per le righe di stato, null se non c'è nulla da dire. */
    fun phrase(tool: String?, l: Labels): String? {
        val grezzo = tool?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val nome = grezzo.substringBefore(' ').substringBefore('(').trim()
        val resto = grezzo.removePrefix(nome).trim().removePrefix("(").removeSuffix(")").trim()
        val arg = breve(resto)
        return when (nome.lowercase()) {
            "bash", "shell", "run" -> if (arg.isEmpty()) l.other.format(nome) else l.run.format(arg)
            "read", "notebookread", "view" -> if (arg.isEmpty()) l.other.format(nome) else l.read.format(arg)
            "edit", "multiedit", "notebookedit", "update" -> if (arg.isEmpty()) l.other.format(nome) else l.edit.format(arg)
            "write", "create" -> if (arg.isEmpty()) l.other.format(nome) else l.write.format(arg)
            "grep", "glob", "search", "find" -> if (arg.isEmpty()) l.other.format(nome) else l.search.format(arg)
            "webfetch", "websearch", "fetch" -> l.web
            "sendmessage", "listagents", "senduserfile" -> l.message
            "task", "agent", "workflow" -> l.delegate
            "todowrite", "exitplanmode", "enterplanmode" -> l.plan
            else -> l.other.format(nome)
        }
    }

    /** Un percorso si riduce al nome del file: «core/src/main/.../TileTexts.kt» → «TileTexts.kt». */
    private fun breve(arg: String): String {
        if (arg.isEmpty()) return ""
        val primo = arg.substringBefore(' ')
        return if (primo.contains('/') && !arg.contains(' ')) primo.substringAfterLast('/') else arg
    }
}
