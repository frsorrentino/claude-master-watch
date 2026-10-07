package it.pixelbox.cmwatch.rules

/**
 * Il nome di una sessione come lo dice la voce della modalità live (specifica live, §5): senza il prefisso dell'account
 * e senza il dominio finale, con i trattini come spazi. `pix-atlas-shop-it` diventa «atlas shop». Anche `_` e `.`
 * separano le parole: una cartella `shop.example.it` letta così direbbe «punto». Il nome non resta mai vuoto: `pix-it`
 * si dice «it».
 */
object SpeakableName {
    /** Prefissi d'account che si tolgono davanti al nome. */
    val PREFIXES = setOf("pix")

    /** Domini che si tolgono in fondo al nome. */
    val DOMAINS = setOf("it", "com", "net", "org", "eu", "app")

    fun of(name: String): String {
        var words = name.split('-', '_', '.').map { it.trim() }.filter { it.isNotEmpty() }
        if (words.size > 1 && words.first().lowercase() in PREFIXES) words = words.drop(1)
        if (words.size > 1 && words.last().lowercase() in DOMAINS) words = words.dropLast(1)
        return words.joinToString(" ")
    }
}
