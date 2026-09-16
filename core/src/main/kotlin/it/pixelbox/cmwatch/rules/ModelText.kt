package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Model

/**
 * Nome breve del modello (Franz, 16/09 08:09: nel riquadro il modello non compariva). Il contratto 1.11 prevede
 * `label`, ma dal vivo il relay la manda null su tutte le sessioni: allora il nome si ricava dall'id, invece di non
 * mostrare niente. L'etichetta del PC, quando c'è, vince sempre: è lui che sa come si chiama il modello.
 */
object ModelText {
    /** «claude-opus-5[1m]» → «Opus 5»; «claude-haiku-4-5-20251001» → «Haiku 4.5». Null se non c'è il modello. */
    fun short(model: Model?): String? {
        val m = model ?: return null
        m.label?.takeIf { it.isNotBlank() }?.let { return it }
        val id = m.id.substringBefore('[').removePrefix("claude-").trim('-')
        if (id.isEmpty()) return null
        val pezzi = id.split('-')
        // La data di rilascio in coda («20251001») non è parte del nome.
        val senzaData = pezzi.filterNot { it.length == 8 && it.all(Char::isDigit) }
        val famiglia = senzaData.firstOrNull() ?: return null
        val numeri = senzaData.drop(1).filter { p -> p.all(Char::isDigit) }
        val nome = famiglia.replaceFirstChar { it.uppercase() }
        // Un id conosciuto diventa «Opus 5» o «Haiku 4.5»; uno che non riconosciamo si mostra com'è, senza inventare.
        return if (numeri.isEmpty()) senzaData.joinToString("-") else "$nome ${numeri.joinToString(".")}"
    }

    /** La finestra da 1M la dice il suffisso dell'id, come nel contratto 1.11. */
    fun millionWindow(model: Model?): Boolean = model?.id?.endsWith("[1m]") == true

    /** Le famiglie che l'app sa colorare: il pallino della pillola si riconosce prima di leggere il nome. */
    enum class Family { OPUS, SONNET, HAIKU, OTHER }

    /** La famiglia si legge dall'id, non dall'etichetta: l'etichetta la scrive il PC e può cambiare forma. */
    fun family(model: Model?): Family? {
        val id = model?.id?.lowercase() ?: return null
        return when {
            id.contains("opus") -> Family.OPUS
            id.contains("sonnet") -> Family.SONNET
            id.contains("haiku") -> Family.HAIKU
            else -> Family.OTHER
        }
    }
}
