package it.pixelbox.cmwatch.rules

/**
 * Le quattro azioni su un file della chat (Franz, 07/10 15:32): apri, scarica, copia, condividi. «Copia» mette negli appunti
 * il testo dei file di testo, così si incolla ovunque; degli altri il file stesso, che le app che lo sanno leggere incollano.
 */
object FileActions {
    enum class CopyKind { TEXT, FILE }

    private val TEXTUAL = Regex("""/(json|xml|javascript|x-sh|x-yaml|yaml|toml)$|\+(xml|json)$""")

    fun copyKind(mime: String?): CopyKind {
        val m = mime?.substringBefore(';')?.trim()?.lowercase() ?: return CopyKind.FILE
        return if (m.startsWith("text/") || TEXTUAL.containsMatchIn(m)) CopyKind.TEXT else CopyKind.FILE
    }
}
