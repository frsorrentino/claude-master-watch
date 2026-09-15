package it.pixelbox.cmwatch.wear.tts

import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.rules.SpeechText
import it.pixelbox.cmwatch.wear.CmApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Lettura a voce del testo intero (Franz, 14/09 12:17, «applica tutte»). Per una sessione chiede al PC l'ultima
 * risposta (contratto 1.4, `last`), aspetta fino a 5 s (il relay risponde di norma in uno o due) e la legge a pezzi;
 * se il PC non risponde, o non ha messaggi, legge il ripiego che l'orologio ha già. Un secondo tocco ferma, anche
 * mentre aspetta.
 */
class Reader(private val app: CmApp) {
    private var job: Job? = null
    private val _preparing = MutableStateFlow(false)
    /** Vero mentre aspetta il testo dal PC: il ▶ mostra già ■, così un tocco annulla. */
    val preparing: StateFlow<Boolean> = _preparing

    val busy: Boolean get() = app.speaker.speaking.value || _preparing.value

    fun toggleLast(session: String, fallback: String?) {
        if (busy) { stop(); return }
        _preparing.value = true
        job = app.scope.launch {
            try {
                val id = app.repo.command(CmdOp.LAST, session, null)
                val r = withTimeoutOrNull(WAIT_MS) { app.repo.resultsById.map { it[id] }.filterNotNull().first() }
                SpeechText.pick(r, fallback)?.let { say(it) }
            } finally { _preparing.value = false }
        }
    }

    fun toggleText(text: String) { if (busy) stop() else say(text) }

    /**
     * La Risposta a paragrafi (Franz, 15/09 17:19): `all` è il ▶ in testata, che parte dal primo o ferma; toccare un
     * paragrafo legge da lì in avanti, ritoccare quello che sta leggendo ferma.
     */
    fun toggleBlocks(texts: List<String>, from: Int, all: Boolean) {
        if (busy && (all || app.speaker.block.value == from)) { stop(); return }
        val code = app.getString(R.string.tts_code)
        app.speaker.speakBlocks(texts.map { SpeechText.chunks(SpeechText.clean(it, code)) }, from)
    }

    fun stop() { job?.cancel(); job = null; _preparing.value = false; app.speaker.stop() }

    private fun say(text: String) =
        app.speaker.speakAll(SpeechText.chunks(SpeechText.clean(text, app.getString(R.string.tts_code))))

    companion object { const val WAIT_MS = 5_000L }
}
