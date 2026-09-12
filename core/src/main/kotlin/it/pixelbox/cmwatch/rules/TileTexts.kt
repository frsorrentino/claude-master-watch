package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/** Testi della tile (design, sezione 2): conteggi, la sessione ferma su una riga intera, bottoni, freschezza adattiva. */
object TileTexts {
    enum class Button { OPEN, SESSIONS, QUOTA }

    fun header(state: State, sessionsLabel: String): String {
        val q = state.sessions.count { it.question != null }
        val g = state.sessions.count { it.state == SessionState.GONE }
        val parts = mutableListOf("${state.sessions.size} $sessionsLabel")
        if (q > 0) parts += "$q?"
        if (g > 0) parts += "$g✗"
        return parts.joinToString(" · ")
    }

    private fun icon(s: SessionState) = when (s) {
        SessionState.WAITING -> "❓"; SessionState.BUSY, SessionState.AWAITING -> "▶"; SessionState.IDLE -> "✓"; SessionState.GONE -> "✗"
    }

    /** La sessione ferma con la domanda intera; senza domande la seguita, altrimenti la più recente. */
    fun line(state: State, now: Long): String? {
        state.sessions.firstOrNull { it.question != null }?.let { return "❓ ${it.name} · ${it.question!!.text}" }
        val s = state.sessions.firstOrNull { it.followed && it.state != SessionState.GONE }
            ?: state.sessions.filter { it.state != SessionState.GONE }.maxByOrNull { maxOf(it.since, it.turnStarted ?: 0, it.outcome?.at ?: 0) }
            ?: return null
        return "${icon(s.state)} ${SessionsText.row(s, now)}"
    }

    fun buttons(state: State): List<Button> =
        if (state.sessions.any { it.question != null }) listOf(Button.OPEN, Button.SESSIONS) else listOf(Button.SESSIONS, Button.QUOTA)

    /** 30 s con domande aperte, 15 min altrimenti. */
    fun freshnessMs(state: State): Long = if (state.sessions.any { it.question != null }) 30_000L else 15 * 60_000L

    fun staleLine(f: Freshness.Stale, pattern: String): String = pattern.format(f.minutes)
}
