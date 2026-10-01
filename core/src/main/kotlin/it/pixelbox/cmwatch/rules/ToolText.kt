package it.pixelbox.cmwatch.rules

/**
 * Da nome di strumento a frase compiuta. Il contratto porta il campo grezzo («SendMessage», «Bash cd /home/demo/…»,
 * «Read core/src/main/kotlin/.../TileTexts.kt») e sulla tile «SendMessage» non dice niente (Franz, 13/09 20:36).
 * Qui diventa «scrive a un'altra sessione», «esegue cd /home/demo/…», «legge TileTexts.kt»: i percorsi si riducono
 * al nome del file, perché al polso la cartella è rumore.
 */
object ToolText {
    /** Il tipo di strumento, per l'icona della riga del passaggio (Franz, 01/10 15:59). */
    enum class Kind { RUN, READ, EDIT, WRITE, SEARCH, WEB, MESSAGE, DELEGATE, PLAN, OTHER }

    fun kind(tool: String?): Kind = when (tool?.trim()?.substringBefore(' ')?.lowercase()) {
        "bash", "shell", "run" -> Kind.RUN
        "read", "notebookread", "view" -> Kind.READ
        "edit", "multiedit", "notebookedit", "update" -> Kind.EDIT
        "write", "create" -> Kind.WRITE
        "grep", "glob", "search", "find" -> Kind.SEARCH
        "webfetch", "websearch", "fetch" -> Kind.WEB
        "sendmessage", "listagents", "senduserfile" -> Kind.MESSAGE
        "task", "agent", "workflow" -> Kind.DELEGATE
        "todowrite", "exitplanmode", "enterplanmode" -> Kind.PLAN
        else -> Kind.OTHER
    }

    /**
     * Le due righe di un passaggio: in chiaro sopra, il dettaglio sotto (in monospazio nell'app). Un comando con la sua
     * descrizione: descrizione, poi comando. Un file: il nome, poi la cartella. Altrimenti il testo, senza seconda riga.
     */
    fun row(tool: String?, text: String?, note: String?): Pair<String, String?> {
        val t = text?.trim().orEmpty()
        val n = note?.trim()?.takeIf { it.isNotEmpty() }
        return when (kind(tool)) {
            Kind.READ, Kind.EDIT, Kind.WRITE -> {
                val name = t.substringAfterLast('/')
                val dir = t.substringBeforeLast('/', "").takeIf { it.isNotEmpty() }
                (n ?: name) to (if (n != null) t else dir)
            }
            else -> if (n != null) n to t.takeIf { it.isNotEmpty() } else t to null
        }
    }

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

    /**
     * Cosa sta facendo, detto per esteso: la description che Claude scrive accanto al comando (contratto 1.5) dice
     * «Run the plugin test suite» dove il comando grezzo dice «cd ~/Desktop/…» (Franz, 14/09 13:00); senza, la frase
     * dallo strumento. Senza uno strumento in corso la nota è vecchia e non si mostra.
     */
    fun describe(note: String?, tool: String?, l: Labels): String? =
        if (tool.isNullOrBlank()) null else note?.trim()?.takeIf { it.isNotEmpty() } ?: phrase(tool, l)

    /** Un percorso si riduce al nome del file: «core/src/main/.../TileTexts.kt» → «TileTexts.kt». */
    private fun breve(arg: String): String {
        if (arg.isEmpty()) return ""
        val primo = arg.substringBefore(' ')
        return if (primo.contains('/') && !arg.contains(' ')) primo.substringAfterLast('/') else arg
    }
}
