package it.pixelbox.cmwatch.wear.ui

import android.net.Uri
import it.pixelbox.cmwatch.rules.Screen

/** Rotte di navigazione e deep link `cmwatch://…` (tile, notifiche). */
object Routes {
    const val SESSIONS = "sessions"
    const val SESSION = "session/{name}"
    const val QUESTION = "question/{name}"
    const val SETTINGS = "settings"
    const val PAIRING = "pairing"
    const val TERMINAL = "terminal/{name}"
    const val LAUNCH = "launch"
    const val QUOTA = "quota"

    fun of(screen: Screen): String = when (screen) {
        Screen.Sessions -> SESSIONS
        is Screen.Session -> "session/${Uri.encode(screen.name)}"
        is Screen.Question -> "question/${Uri.encode(screen.name)}"
        Screen.Settings -> SETTINGS
        Screen.Pairing -> PAIRING
        is Screen.Terminal -> "terminal/${Uri.encode(screen.name)}"
        Screen.Launch -> LAUNCH
        Screen.Quota -> QUOTA
    }

    /** Dalla rotta corrente (con argomenti risolti) alla Screen. */
    fun parse(route: String?, name: String?): Screen = when (route) {
        SESSION -> Screen.Session(name.orEmpty())
        QUESTION -> Screen.Question(name.orEmpty())
        SETTINGS -> Screen.Settings
        PAIRING -> Screen.Pairing
        TERMINAL -> Screen.Terminal(name.orEmpty())
        LAUNCH -> Screen.Launch
        QUOTA -> Screen.Quota
        else -> Screen.Sessions
    }

    /** `cmwatch://question/ledger-api` → Screen.Question("ledger-api"). */
    fun fromDeepLink(uri: Uri?): Screen? {
        if (uri == null || uri.scheme != "cmwatch") return null
        val name = uri.pathSegments.firstOrNull()
        return when (uri.host) {
            "sessions" -> Screen.Sessions
            "session" -> name?.let { Screen.Session(it) }
            "question" -> name?.let { Screen.Question(it) }
            "quota" -> Screen.Quota
            "settings" -> Screen.Settings
            // L'Esito è dentro la Scheda (15/09 17:02): le notifiche di esito e i link vecchi aprono lei.
            "outcome" -> name?.let { Screen.Session(it) }
            "terminal" -> name?.let { Screen.Terminal(it) }
            "launch" -> Screen.Launch
            // Timeline, Recap, Notte e Menu tolti (Franz, 16/09 14:26): un link vecchio apre la lista delle sessioni.
            "timeline", "recap", "night", "menu" -> Screen.Sessions
            else -> null
        }
    }
}
