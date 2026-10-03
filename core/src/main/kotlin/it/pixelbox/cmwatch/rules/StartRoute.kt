package it.pixelbox.cmwatch.rules

/**
 * Dove si apre l'app del telefono (restyling 30/09, dal vivo): un nuovo avvio parte sempre dalla Panoramica e senza
 * schede aperte; solo una rotazione (stato salvato) tiene la scheda e la sessione aperta.
 */
object StartRoute {
    /** OVERVIEW è il riepilogo unico (design 03/10): il nome resta per lo stato salvato. */
    enum class Tab { OVERVIEW, DIARY }

    fun tab(restored: Tab?): Tab = restored ?: Tab.OVERVIEW

    fun openSheet(freshLaunch: Boolean, restored: String?): String? = if (freshLaunch) null else restored

    /**
     * L'app ancora viva e riaperta dopo un'assenza lunga vale un avvio nuovo (revisione 30/09): si torna sulla
     * Panoramica. Un giro breve, come «Apri in Claude» e ritorno, lascia la scheda dov'era.
     */
    const val AWAY_RESET_MS = 10 * 60_000L

    fun resetOnReturn(awayMs: Long): Boolean = awayMs >= AWAY_RESET_MS
}
