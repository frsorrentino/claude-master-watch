package it.pixelbox.cmwatch.wear.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/** TTS di sistema con il tasto ▶ (design, «Decisioni fisse»): niente microfono, niente waveform finta. */
class Speaker(ctx: Context) {
    private var ready = false
    private var pendingText: String? = null
    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(ctx.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                if (tts.isLanguageAvailable(Locale.ITALIAN) >= TextToSpeech.LANG_AVAILABLE) tts.language = Locale.ITALIAN
                pendingText?.let { speak(it) }; pendingText = null
            }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _speaking.value = true }
            override fun onDone(utteranceId: String?) { _speaking.value = false }
            @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { _speaking.value = false }
        })
    }

    fun speak(text: String) {
        if (!ready) { pendingText = text; return }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cm-${text.hashCode()}")
    }

    fun stop() { tts.stop(); _speaking.value = false }
    fun release() { tts.shutdown() }
}
