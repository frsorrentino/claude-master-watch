package it.pixelbox.cmwatch.wear.tts

import android.util.Log
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * TTS di sistema con il tasto ▶ (design, «Decisioni fisse»): niente microfono, niente waveform finta. Un testo lungo
 * arriva a pezzi (`SpeechText.chunks`) e si legge di fila; `speaking` è vero da subito e resta vero fino alla fine
 * dell'ultimo pezzo, così il ▶ mostra ■ per tutta la lettura.
 */
class Speaker(ctx: Context) {
    private var ready = false
    private var pending: (() -> Unit)? = null
    @Volatile private var lastId: String? = null
    private var batch = 0
    private var voiceName: String? = null
    private val _voices = MutableStateFlow<List<String>>(emptyList())
    /** Voci italiane del motore, prima quelle installate sul polso e poi quelle di rete: si provano dalle impostazioni. */
    val voices: StateFlow<List<String>> = _voices
    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking
    private val _block = MutableStateFlow<Int?>(null)
    /** Il paragrafo della Risposta che sta leggendo, per evidenziarlo e seguirlo (Franz, 15/09 17:19); null fuori da lì. */
    val block: StateFlow<Int?> = _block

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(ctx.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                if (tts.isLanguageAvailable(Locale.ITALIAN) >= TextToSpeech.LANG_AVAILABLE) tts.language = Locale.ITALIAN
                _voices.value = italian()
                Log.i("cmwatch-tts", "voci italiane: ${_voices.value}")
                applyVoice()
                val p = pending; pending = null; p?.invoke()
            }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _speaking.value = true
                _block.value = BLOCCO.find(utteranceId.orEmpty())?.groupValues?.get(1)?.toInt()
            }
            override fun onDone(utteranceId: String?) { if (utteranceId == lastId) finished() }
            @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { finished() }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { if (utteranceId == lastId) finished() }
        })
    }

    fun speak(text: String) = speakAll(listOf(text))

    fun speakAll(parts: List<String>) = enqueue(parts.map { null to it })

    /** I paragrafi della Risposta, ognuno già a pezzi, da `from` in avanti: l'id di ogni pezzo porta il suo paragrafo. */
    fun speakBlocks(blocks: List<List<String>>, from: Int) =
        enqueue(blocks.withIndex().drop(from).flatMap { (i, pezzi) -> pezzi.map { i to it } })

    /** Primo pezzo in `QUEUE_FLUSH` (interrompe quello che stava leggendo), gli altri in coda. */
    private fun enqueue(items: List<Pair<Int?, String>>) {
        if (items.isEmpty()) return
        _speaking.value = true
        if (!ready) { pending = { enqueue(items) }; return }
        val b = ++batch
        items.forEachIndexed { i, (blocco, p) ->
            val id = "cm-$b-$i" + (blocco?.let { "-b$it" } ?: "")
            if (i == items.lastIndex) lastId = id
            tts.speak(p, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, id)
        }
    }

    private fun finished() { _speaking.value = false; _block.value = null }

    /** Voce scelta nelle impostazioni (Franz, 14/09 13:28: «una voce maschile»); null = la predefinita. */
    fun setVoice(name: String?) { voiceName = name; if (ready) applyVoice() }

    private fun applyVoice() {
        val v = voiceName?.let { n -> runCatching { tts.voices }.getOrNull()?.firstOrNull { it.name == n } }
        if (v != null) tts.voice = v
        else if (tts.isLanguageAvailable(Locale.ITALIAN) >= TextToSpeech.LANG_AVAILABLE) tts.language = Locale.ITALIAN
    }

    private fun italian(): List<String> = runCatching {
        tts.voices.orEmpty()
            .filter { it.locale.language == "it" && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features }
            .sortedWith(compareBy({ it.isNetworkConnectionRequired }, { it.name }))
            .map { it.name }
    }.getOrDefault(emptyList())

    fun stop() { pending = null; tts.stop(); finished() }

    private companion object { val BLOCCO = Regex("-b(\\d+)$") }
    fun release() { tts.shutdown() }
}
