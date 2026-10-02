package it.pixelbox.cmwatch.rules

/**
 * I comandi slash dal telefono (Franz, 02/10 11:12). Il relay dice quali consente (`allowed`, senza «/»; null = relay
 * che non li supporta). Scrivendo «/» e il nome si suggeriscono; un testo che comincia con un comando consentito parte
 * come comando, tutto il resto resta un prompt. Quelli che svuotano o chiudono la sessione chiedono conferma.
 */
object Slash {
    data class Parsed(val cmd: String, val args: String?)

    private val CONFIRM = setOf("clear", "exit")

    fun suggest(draft: String, allowed: List<String>?): List<String> {
        if (allowed == null || !draft.startsWith("/") || draft.contains(' ')) return emptyList()
        val typed = draft.removePrefix("/")
        return allowed.filter { it.startsWith(typed) && it != typed }.ifEmpty { if (typed.isEmpty()) allowed else emptyList() }
    }

    fun parse(draft: String, allowed: List<String>?): Parsed? {
        val t = draft.trim()
        if (allowed == null || !t.startsWith("/")) return null
        val name = t.removePrefix("/").substringBefore(' ')
        if (name !in allowed) return null
        return Parsed(name, t.substringAfter(' ', "").trim().ifEmpty { null })
    }

    fun confirm(cmd: String): Boolean = cmd in CONFIRM
}
