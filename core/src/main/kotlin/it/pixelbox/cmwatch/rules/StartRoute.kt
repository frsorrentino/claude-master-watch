package it.pixelbox.cmwatch.rules

/**
 * Dove si apre l'app del telefono (restyling 30/09, dal vivo): un nuovo avvio parte sempre dalla Panoramica e senza
 * schede aperte; solo una rotazione (stato salvato) tiene la scheda e la sessione aperta.
 */
object StartRoute {
    enum class Tab { OVERVIEW, SESSIONS, DIARY }

    fun tab(restored: Tab?): Tab = restored ?: Tab.OVERVIEW

    /** La scheda dopo uno scorrimento laterale (segnalazione 01/10 20:12), nell'ordine della barra; ai bordi si resta. */
    fun swipe(from: Tab, toNext: Boolean): Tab =
        Tab.entries.getOrNull(from.ordinal + if (toNext) 1 else -1) ?: from

    fun openSheet(freshLaunch: Boolean, restored: String?): String? = if (freshLaunch) null else restored

    /**
     * L'app ancora viva e riaperta dopo un'assenza lunga vale un avvio nuovo (revisione 30/09): si torna sulla
     * Panoramica. Un giro breve, come «Apri in Claude» e ritorno, lascia la scheda dov'era.
     */
    const val AWAY_RESET_MS = 10 * 60_000L

    fun resetOnReturn(awayMs: Long): Boolean = awayMs >= AWAY_RESET_MS
}
