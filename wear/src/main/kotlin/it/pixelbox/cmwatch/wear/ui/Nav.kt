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
    const val OUTCOME = "outcome/{name}"
    const val TERMINAL = "terminal/{name}"
    const val TIMELINE = "timeline"
    const val LAUNCH = "launch"
    const val QUOTA = "quota"
    const val RECAP = "recap"
    const val NIGHT = "night"

    fun of(screen: Screen): String = when (screen) {
        Screen.Sessions -> SESSIONS
        is Screen.Session -> "session/${Uri.encode(screen.name)}"
        is Screen.Question -> "question/${Uri.encode(screen.name)}"
        Screen.Settings -> SETTINGS
        Screen.Pairing -> PAIRING
        is Screen.Outcome -> "outcome/${Uri.encode(screen.name)}"
        is Screen.Terminal -> "terminal/${Uri.encode(screen.name)}"
        Screen.Timeline -> TIMELINE
        Screen.Launch -> LAUNCH
        Screen.Quota -> QUOTA
        Screen.Recap -> RECAP
        Screen.Night -> NIGHT
    }

    /** Dalla rotta corrente (con argomenti risolti) alla Screen. */
    fun parse(route: String?, name: String?): Screen = when (route) {
        SESSION -> Screen.Session(name.orEmpty())
        QUESTION -> Screen.Question(name.orEmpty())
        SETTINGS -> Screen.Settings
        PAIRING -> Screen.Pairing
        OUTCOME -> Screen.Outcome(name.orEmpty())
        TERMINAL -> Screen.Terminal(name.orEmpty())
        TIMELINE -> Screen.Timeline
        LAUNCH -> Screen.Launch
        QUOTA -> Screen.Quota
        RECAP -> Screen.Recap
        NIGHT -> Screen.Night
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
            "outcome" -> name?.let { Screen.Outcome(it) }
            "terminal" -> name?.let { Screen.Terminal(it) }
            "timeline" -> Screen.Timeline
            "launch" -> Screen.Launch
            "recap" -> Screen.Recap
            "night" -> Screen.Night
            else -> null
        }
    }
}
