package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/** Testi della complication (tre tipi, stessa sorgente; design, sezione 2). */
object ComplicationTexts {
    data class Ranged(val value: Float, val max: Float, val text: String)

    /** «1?» / «▶3» / «✓»; «PC» con il PC fermo; «—» senza stato. */
    fun short(state: State?, fresh: Boolean, seen: Set<String> = emptySet()): String {
        state ?: return "—"
        if (!fresh) return "PC"
        val q = state.sessions.count { it.question != null && it.question.id !in seen }
        if (q > 0) return "$q?"
        val busy = state.sessions.count { it.state == SessionState.BUSY || it.state == SessionState.AWAITING }
        return if (busy > 0) "▶$busy" else "✓"
    }

    /** «❓ ledger-api · Deploy now?» oppure «▶ 2 · ✓ 3». */
    fun long(state: State?, fresh: Boolean, staleLabel: String, maxGist: Int = 30, seen: Set<String> = emptySet()): String {
        state ?: return "—"
        if (!fresh) return staleLabel
        state.sessions.firstOrNull { it.question != null && it.question.id !in seen }?.let { return "❓ ${it.name} · ${gist(it.question!!.text, maxGist)}" }
        val busy = state.sessions.count { it.state == SessionState.BUSY || it.state == SessionState.AWAITING }
        val idle = state.sessions.count { it.state == SessionState.IDLE }
        return "▶ $busy · ✓ $idle"
    }

    /** Il testo intero se entra; altrimenti l'ultima frase interrogativa che entra; altrimenti taglio a parola intera, senza «…». */
    fun gist(text: String, max: Int): String {
        val t = text.trim()
        if (t.length <= max) return t
        val sentences = Regex("[^.?!]+[.?!]").findAll(t).map { it.value.trim() }.toList()
        sentences.lastOrNull { it.endsWith("?") && it.length <= max }?.let { return it }
        val words = t.split(Regex("\\s+"))
        val sb = StringBuilder()
        for (w in words) { if (sb.length + w.length + (if (sb.isEmpty()) 0 else 1) > max) break; if (sb.isNotEmpty()) sb.append(' '); sb.append(w) }
        return sb.toString().ifEmpty { t.take(max) }
    }

    /** Anello della quota 5 h dell'account scelto. */
    fun ranged(state: State?, account: String): Ranged {
        val h5 = state?.quota?.get(account)?.h5 ?: return Ranged(0f, 100f, "—")
        return Ranged(h5.toFloat(), 100f, "$h5 %")
    }

    fun tapTarget(state: State?): String =
        state?.sessions?.firstOrNull { it.question != null }?.let { "cmwatch://question/${it.name}" } ?: "cmwatch://sessions"
}
