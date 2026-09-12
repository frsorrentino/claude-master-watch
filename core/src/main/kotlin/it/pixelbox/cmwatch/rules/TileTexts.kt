package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/** Testi della tile (design, sezione 2): conteggi, la sessione ferma su una riga intera, bottoni, freschezza adattiva. */
object TileTexts {
    enum class Button { OPEN, SESSIONS, QUOTA, REPLY }
    enum class Accent { QUESTION, BUSY, IDLE, STALE }
    data class Labels(val none: String, val stale: String, val works: String)

    /** Cosa mostra la tile a colpo d'occhio: etichetta dei conteggi, sessione in evidenza, corpo, un solo bottone di bordo. */
    data class Glance(val counts: String, val name: String?, val body: String, val button: Button, val accent: Accent, val target: String)

    fun counts(state: State): String {
        val q = state.sessions.count { it.question != null }
        val busy = state.sessions.count { it.question == null && (it.state == SessionState.BUSY || it.state == SessionState.AWAITING) }
        val idle = state.sessions.count { it.state == SessionState.IDLE }
        val gone = state.sessions.count { it.state == SessionState.GONE }
        return listOf(q to "❓", busy to "▶", idle to "✓", gone to "✗").filter { it.first > 0 }.joinToString(" · ") { "${it.first} ${it.second}" }
    }

    fun glance(state: State?, freshness: Freshness, now: Long, l: Labels, seen: Set<String> = emptySet()): Glance {
        if (state == null) return Glance("", null, l.none, Button.SESSIONS, Accent.IDLE, "cmwatch://sessions")
        if (freshness is Freshness.Stale) return Glance("", null, staleLine(freshness, l.stale), Button.SESSIONS, Accent.STALE, "cmwatch://sessions")
        val counts = counts(state)
        state.sessions.firstOrNull { it.question != null && it.question.id !in seen }?.let {
            return Glance(counts, it.name, it.question!!.text, Button.REPLY, Accent.QUESTION, "cmwatch://question/${it.name}")
        }
        val s = state.sessions.firstOrNull { it.followed && it.state != SessionState.GONE }
            ?: state.sessions.filter { it.state != SessionState.GONE }.maxByOrNull { maxOf(it.since, it.turnStarted ?: 0, it.outcome?.at ?: 0) }
            ?: return Glance(counts, null, l.none, Button.SESSIONS, Accent.IDLE, "cmwatch://sessions")
        val busy = s.state == SessionState.BUSY || s.state == SessionState.AWAITING
        val age = Durations.since(if (busy) s.turnStarted ?: s.since else s.since, now)
        val body = if (busy) "▶ ${s.tool ?: l.works} · $age" else s.outcome?.short ?: "✓ · $age"
        return Glance(counts, s.name, body, Button.SESSIONS, if (busy) Accent.BUSY else Accent.IDLE, "cmwatch://session/${s.name}")
    }

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
