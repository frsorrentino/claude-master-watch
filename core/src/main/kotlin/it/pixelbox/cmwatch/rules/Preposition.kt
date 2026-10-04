package it.pixelbox.cmwatch.rules

/**
 * La «d» eufonica (Franz, 04/10 09:14): «Scrivi ad atlas-shop», ma «Scrivi a ledger-api» e «a orbit-docs». Solo davanti
 * alla stessa vocale, maiuscola o accentata compresa; il testo con «ad» lo sceglie chi disegna (`write_to_ad`).
 */
object Preposition {
    fun ad(word: String): Boolean = word.trimStart().firstOrNull()?.lowercaseChar() in setOf('a', 'à', 'á')
}
