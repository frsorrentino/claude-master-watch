package it.pixelbox.cmwatch.mobile

import android.app.LocaleManager
import android.content.Context
import android.speech.tts.TextToSpeech
import it.pixelbox.cmwatch.rules.AppLanguage
import java.util.Locale

/** La voce di sistema per il tasto ▶ (niente microfono in-app, design 12/09). Parla nella lingua scelta per l'app. */
class Speech(ctx: Context) {
    @Volatile private var ready = false
    private val locales = ctx.getSystemService(LocaleManager::class.java)
    private val tts = TextToSpeech(ctx.applicationContext) { ready = it == TextToSpeech.SUCCESS }

    fun speak(text: String) {
        if (!ready) return
        // Letta a ogni lettura: la lingua si cambia nelle impostazioni mentre l'app è aperta.
        tts.language = AppLanguage.voiceLocale(AppLanguage.fromTags(locales.applicationLocales.toLanguageTags()), Locale.getDefault())
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cm-" + text.hashCode())
    }
    fun stop() { tts.stop() }
    fun shutdown() { tts.shutdown() }
}
