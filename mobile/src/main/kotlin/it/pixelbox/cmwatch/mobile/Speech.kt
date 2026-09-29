package it.pixelbox.cmwatch.mobile

import android.content.Context
import android.speech.tts.TextToSpeech

/** La voce di sistema per il tasto ▶ (niente microfono in-app, design 12/09). */
class Speech(ctx: Context) {
    @Volatile private var ready = false
    private val tts = TextToSpeech(ctx.applicationContext) { ready = it == TextToSpeech.SUCCESS }

    fun speak(text: String) { if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cm-" + text.hashCode()) }
    fun stop() { tts.stop() }
}
