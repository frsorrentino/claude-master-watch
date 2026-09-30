package it.pixelbox.cmwatch.mobile

import android.app.LocaleManager
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import it.pixelbox.cmwatch.rules.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * La voce di sistema per il tasto ▶ (niente microfono in-app, design 12/09). Parla nella lingua scelta per l'app.
 * `speaking` = il testo che sta leggendo: il suo tasto diventa Stop (Franz, 30/09 23:12: «i tasti play non hanno stop»).
 */
class Speech(ctx: Context) {
    @Volatile private var ready = false
    private val locales = ctx.getSystemService(LocaleManager::class.java)
    private val _speaking = MutableStateFlow<String?>(null)
    val speaking: StateFlow<String?> = _speaking
    private var current: String? = null
    private val tts = TextToSpeech(ctx.applicationContext) { ready = it == TextToSpeech.SUCCESS }.apply {
        setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _speaking.value = current }
            override fun onDone(utteranceId: String?) { _speaking.value = null }
            @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { _speaking.value = null }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { _speaking.value = null }
        })
    }

    fun speak(text: String) {
        if (!ready) return
        // Letta a ogni lettura: la lingua si cambia nelle impostazioni mentre l'app è aperta.
        tts.language = AppLanguage.voiceLocale(AppLanguage.fromTags(locales.applicationLocales.toLanguageTags()), Locale.getDefault())
        current = text
        _speaking.value = text
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cm-" + text.hashCode())
    }

    /** Lo stesso tasto: legge, o ferma se sta già leggendo quel testo. */
    fun toggle(text: String) { if (_speaking.value == text) stop() else speak(text) }
    fun stop() { tts.stop(); _speaking.value = null }
    fun shutdown() { tts.shutdown() }
}
