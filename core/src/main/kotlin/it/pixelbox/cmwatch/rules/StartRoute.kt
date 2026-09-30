package it.pixelbox.cmwatch.rules

/**
 * Dove si apre l'app del telefono (restyling 30/09, dal vivo): un nuovo avvio parte sempre dalla Panoramica e senza
 * schede aperte; solo una rotazione (stato salvato) tiene la scheda e la sessione aperta.
 */
object StartRoute {
    enum class Tab { OVERVIEW, SESSIONS, DIARY }

    fun tab(restored: Tab?): Tab = restored ?: Tab.OVERVIEW

    fun openSheet(freshLaunch: Boolean, restored: String?): String? = if (freshLaunch) null else restored
}
