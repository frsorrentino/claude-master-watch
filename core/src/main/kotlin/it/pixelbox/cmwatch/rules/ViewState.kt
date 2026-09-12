package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.data.Snapshot

/** Le schermate dell'app. Le rotte di navigazione derivano da qui (wear/ui/Nav.kt). */
sealed class Screen {
    data object Sessions : Screen()
    data class Session(val name: String) : Screen()
    data class Question(val name: String) : Screen()
    data object Settings : Screen()
    data object Pairing : Screen()
    data class Outcome(val name: String) : Screen()
    data class Terminal(val name: String) : Screen()
    data object Timeline : Screen()
    data object Launch : Screen()
    data object Quota : Screen()
    data object Recap : Screen()
    data object Night : Screen()
}

/** Un solo ViewState con priorità: non accoppiato > domanda aperta non ancora vista > schermata scelta (design, sezione 2). */
object ViewState {
    fun reduce(snap: Snapshot, paired: Boolean, chosen: Screen, seen: Set<String>): Screen {
        if (!paired) return Screen.Pairing
        val sessions = snap.state?.sessions.orEmpty()
        val open = sessions.firstOrNull { it.question != null && it.question.id !in seen }
        if (open != null) return Screen.Question(open.name)
        if (chosen is Screen.Question && sessions.none { it.name == chosen.name && it.question != null }) return Screen.Sessions
        return chosen
    }
}
