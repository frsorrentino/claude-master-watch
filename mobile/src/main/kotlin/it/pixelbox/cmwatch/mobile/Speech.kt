package it.pixelbox.cmwatch.mobile

import android.app.LocaleManager
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import it.pixelbox.cmwatch.rules.AppLanguage
import it.pixelbox.cmwatch.rules.SpeechText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * La voce di sistema per il tasto ▶ (niente microfono in-app, design 12/09). Parla nella lingua scelta per l'app.
 * `speaking` = il testo che sta leggendo: il suo tasto diventa Stop (Franz, 30/09 23:12: «i tasti play non hanno stop»).
 */
class Speech(ctx: Context) {
    private val app = ctx.applicationContext
    @Volatile private var ready = false
    private val locales = ctx.getSystemService(LocaleManager::class.java)
    private val _speaking = MutableStateFlow<String?>(null)
    val speaking: StateFlow<String?> = _speaking
    private var current: String? = null
    @Volatile private var prefix = "-"
    private val tts = TextToSpeech(ctx.applicationContext) { ready = it == TextToSpeech.SUCCESS }.apply {
        setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _speaking.value = current }
            // Solo l'ultimo pezzo chiude la lettura: il tasto resta ■ per tutto il testo.
            override fun onDone(utteranceId: String?) { if (utteranceId?.endsWith("-end") == true) _speaking.value = null }
            @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { _speaking.value = null }
            // Un testo nuovo interrompe il vecchio: si spegne solo se è stato fermato il testo che sta leggendo adesso.
            override fun onStop(utteranceId: String?, interrupted: Boolean) { if (utteranceId?.startsWith(prefix) == true) _speaking.value = null }
        })
    }

    fun speak(text: String) {
        if (!ready) return
        // Letta a ogni lettura: la lingua si cambia nelle impostazioni mentre l'app è aperta.
        tts.language = AppLanguage.voiceLocale(AppLanguage.fromTags(locales.applicationLocales.toLanguageTags()), Locale.getDefault())
        current = text
        _speaking.value = text
        // Senza markdown né righe di servizio (Franz, 02/10 00:01: leggeva «asterisco asterisco»), a pezzi sotto il limite
        // del motore: il primo interrompe quello che stava leggendo, gli altri in coda; `speaking` resta il testo intero.
        prefix = "cm-" + text.hashCode() + "-"
        val pieces = SpeechText.chunks(SpeechText.forPhone(text, app.getString(R.string.tts_code), app.getString(R.string.outcome_label)))
        pieces.forEachIndexed { i, p ->
            tts.speak(p, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, prefix + i + if (i == pieces.lastIndex) "-end" else "")
        }
    }

    /** Lo stesso tasto: legge, o ferma se sta già leggendo quel testo. */
    fun toggle(text: String) { if (_speaking.value == text) stop() else speak(text) }
    fun stop() { tts.stop(); _speaking.value = null }
    fun shutdown() { tts.shutdown() }
}
