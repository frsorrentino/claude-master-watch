package it.pixelbox.cmwatch.rules

/**
 * Dove si apre l'app del telefono (restyling 30/09, dal vivo): un nuovo avvio parte sempre dalla Panoramica e senza
 * schede aperte; solo una rotazione (stato salvato) tiene la scheda e la sessione aperta.
 */
object StartRoute {
    /** OVERVIEW è il riepilogo unico (design 03/10): il nome resta per lo stato salvato. */
    enum class Tab { OVERVIEW, DIARY }

    fun tab(restored: Tab?): Tab = restored ?: Tab.OVERVIEW

    /**
     * La master non è una pagina come le altre sessioni (Franz, 03/10 16:30: «l'apertura della master dovrebbe rimanere su
     * home, con una specie di toggle»): chiunque la apra porta la home sulla sua chat.
     */
    fun masterAtHome(name: String?): Boolean = name == ContextActions.MASTER

    /**
     * La pagina aperta resta finché la sua sessione c'è (dal vivo 03/10 19:57: chiusa, la pagina restava nera). Chiusa dopo
     * un «Chiudi la sessione» dato dal telefono ([leaving]) si torna alla home; chiusa per conto suo resta, con «Riapri».
     */
    fun stillOpen(open: String, sessions: List<it.pixelbox.cmwatch.contract.Session>, leaving: String?): Boolean {
        val s = sessions.firstOrNull { it.name == open } ?: return false
        return !(s.state == it.pixelbox.cmwatch.contract.SessionState.GONE && leaving == open)
    }

    fun openSheet(freshLaunch: Boolean, restored: String?): String? = if (freshLaunch) null else restored

    /**
     * L'app ancora viva e riaperta dopo un'assenza lunga vale un avvio nuovo (revisione 30/09): si torna sulla
     * Panoramica. Un giro breve, come «Apri in Claude» e ritorno, lascia la scheda dov'era.
     */
    const val AWAY_RESET_MS = 10 * 60_000L

    fun resetOnReturn(awayMs: Long): Boolean = awayMs >= AWAY_RESET_MS
}
