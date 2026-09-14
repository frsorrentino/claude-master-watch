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
    private var pending: List<String>? = null
    @Volatile private var lastId: String? = null
    private var batch = 0
    private var voiceName: String? = null
    private val _voices = MutableStateFlow<List<String>>(emptyList())
    /** Voci italiane del motore, prima quelle installate sul polso e poi quelle di rete: si provano dalle impostazioni. */
    val voices: StateFlow<List<String>> = _voices
    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(ctx.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                if (tts.isLanguageAvailable(Locale.ITALIAN) >= TextToSpeech.LANG_AVAILABLE) tts.language = Locale.ITALIAN
                _voices.value = italian()
                Log.i("cmwatch-tts", "voci italiane: ${_voices.value}")
                applyVoice()
                pending?.let { speakAll(it) }; pending = null
            }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _speaking.value = true }
            override fun onDone(utteranceId: String?) { if (utteranceId == lastId) _speaking.value = false }
            @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { _speaking.value = false }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { if (utteranceId == lastId) _speaking.value = false }
        })
    }

    fun speak(text: String) = speakAll(listOf(text))

    /** Primo pezzo in `QUEUE_FLUSH` (interrompe quello che stava leggendo), gli altri in coda. */
    fun speakAll(parts: List<String>) {
        if (parts.isEmpty()) return
        _speaking.value = true
        if (!ready) { pending = parts; return }
        val b = ++batch
        parts.forEachIndexed { i, p ->
            val id = "cm-$b-$i"
            if (i == parts.lastIndex) lastId = id
            tts.speak(p, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, id)
        }
    }

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

    fun stop() { pending = null; tts.stop(); _speaking.value = false }
    fun release() { tts.shutdown() }
}
