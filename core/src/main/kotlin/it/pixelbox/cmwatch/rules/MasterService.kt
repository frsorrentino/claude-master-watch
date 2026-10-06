package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Choices
import it.pixelbox.cmwatch.contract.Model
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import java.text.NumberFormat
import java.util.Locale

/**
 * Contratto 1.37, la master al servizio dell'app: le regole del consiglio, delle approvazioni, delle decisioni e della
 * pulizia (mockup approvati da Franz il 05/10 alle 21:07). Il contesto pieno sta in [ContextActions]. Le stesse regole
 * in web/src/lib/masterService.ts.
 */
object MasterService {
    /** Il consiglio nel foglio Modello ed effort: `dot` sulla pillola, `cost` (token di cache) solo a metà lavoro. */
    data class AdviceView(val model: String, val effort: String, val reason: String, val dot: Boolean, val cost: Long?)

    /** Oltre 6 ore il consiglio non vale più (il relay lo toglie; qui anche con uno stato vecchio). */
    private const val ADVICE_MAX_AGE_S = 6 * 3600L

    fun advice(s: Session, choices: Choices?, now: Long): AdviceView? {
        val a = s.advice ?: return null
        if (now - a.at >= ADVICE_MAX_AGE_S) return null
        if (choices == null || choices.models.none { Tune.sameModel(it.id, a.model) } || a.effort !in choices.efforts) return null
        return AdviceView(a.model, a.effort, a.reason, a.differs, a.switchCostTokens.takeIf { a.differs && a.whenToSwitch == "next_task" && it > 0 })
    }

    /** «Usa il consiglio»: solo quello che cambia rispetto alla scelta attuale; null = resta com'è. */
    data class AdviceSteps(val model: Model?, val effort: String?)

    /** Il modello con l'id della lista (`[1m]` compreso), quello che il PC sa applicare. */
    fun adviceSteps(a: AdviceView, choices: Choices?, model: String?, effort: String?): AdviceSteps = AdviceSteps(
        if (Tune.sameModel(model, a.model)) null else choices?.models?.firstOrNull { Tune.sameModel(it.id, a.model) },
        a.effort.takeIf { it != effort },
    )

    /** «36.000»: i token del costo del cambio, come si scrivono in italiano. */
    fun tokens(n: Long): String = NumberFormat.getIntegerInstance(Locale.ITALIAN).format(n)

    /** La nota dell'approvazione; vuota vale «ok», come il default del relay. */
    fun approveText(note: String): String = note.trim().ifEmpty { "ok" }

    /** Il testo di una decisione: al massimo 2000 caratteri, come accetta il relay. */
    const val DECISION_MAX = 2000

    /** Il progetto a cui vale la decisione: l'ultimo pezzo del percorso della sessione. */
    fun decisionProject(s: Session): String? = s.project.split('/').lastOrNull { it.isNotBlank() }

    /** La bozza da una risposta di Claude: senza le righe «Prossimi:» e «Watch:», tagliata a fine parola, senza puntini. */
    fun decisionDraft(text: String): String {
        val body = NextSteps.parse(text).text.lines().filterNot { it.trimStart().startsWith("Watch:") }.joinToString("\n").trim()
        if (body.length <= DECISION_MAX) return body
        val cut = body.take(DECISION_MAX)
        val space = Regex("\\s\\S*$").find(cut)?.range?.first ?: -1
        return (if (space > 0) cut.take(space) else cut).trimEnd()
    }

    /** La pulizia: compito chiuso o doppione, solo a sessione ferma; «Chiudi» solo senza finestra e con /exit permesso. */
    data class Cleanup(val kind: Kind, val of: String?, val canClose: Boolean) {
        enum class Kind { FINISHED, DUPLICATE }
    }

    /** Chi ha finito il compito si propone di chiudere solo dopo dieci minuti ferma: prima la si può ancora usare (Franz, 06/10 12:40-12:45). */
    const val CLEANUP_QUIET_S = 10 * 60L

    fun cleanup(s: Session, canExit: Boolean, now: Long): Cleanup? {
        if (s.state != SessionState.IDLE) return null
        val close = canExit && !s.attached
        s.duplicateOf?.let { return Cleanup(Cleanup.Kind.DUPLICATE, it, close) }
        return if (s.finished && now - s.since >= CLEANUP_QUIET_S) Cleanup(Cleanup.Kind.FINISHED, null, close) else null
    }

    /**
     * Prima di chiudere una sessione, cosa sta facendo (Franz, 06/10 10:57 e 12:25): nessuna chiusura senza una domanda col
     * nome, e la domanda dice che la sessione è aperta. `at` è l'ora dell'ultimo esito per chi ha finito il compito.
     */
    data class CloseState(val kind: Kind, val of: String? = null, val at: Long? = null) {
        enum class Kind { WORKING, FINISHED, DUPLICATE, STILL }
    }

    fun closeState(s: Session): CloseState = when {
        s.state == SessionState.BUSY || s.state == SessionState.AWAITING || s.state == SessionState.WAITING -> CloseState(CloseState.Kind.WORKING)
        s.duplicateOf != null -> CloseState(CloseState.Kind.DUPLICATE, of = s.duplicateOf)
        s.finished -> CloseState(CloseState.Kind.FINISHED, at = s.outcome?.at)
        else -> CloseState(CloseState.Kind.STILL)
    }
}
