package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/** Testi della complication (tre tipi, stessa sorgente; design, sezione 2). */
object ComplicationTexts {
    data class Ranged(val value: Float, val max: Float, val text: String, val week: Boolean? = null, val title: String? = null)

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

    /**
     * Anello della quota dell'account scelto: il numero come testo, la sigla della finestra («5h», «7d») come titolo,
     * che il quadrante disegna sotto e più piccolo (Franz, 15/09 17:00: «su 2 righe con gerarchia diversa»). Con le
     * 5 ore a zero o senza lettura (di notte, nel weekend) l'anello passa alla settimana, l'unica cosa che conta allora
     * (Franz, 15/09 16:44). `week` e `title` sono null senza nessuna lettura.
     */
    fun ranged(state: State?, account: String, h5Tag: String = "5h", weekTag: String = "7d"): Ranged {
        val q = state?.quota?.get(account) ?: return Ranged(0f, 100f, "—")
        val week = (q.h5 == null || q.h5 == 0) && q.w7 != null
        val v = (if (week) q.w7 else q.h5) ?: return Ranged(0f, 100f, "—")
        return Ranged(v.toFloat(), 100f, "$v%", week, if (week) weekTag else h5Tag)
    }

    fun tapTarget(state: State?): String =
        state?.sessions?.firstOrNull { it.question != null }?.let { "cmwatch://question/${it.name}" } ?: "cmwatch://sessions"
}
