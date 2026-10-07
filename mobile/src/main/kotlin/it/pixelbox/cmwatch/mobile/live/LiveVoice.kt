package it.pixelbox.cmwatch.mobile.live

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import it.pixelbox.cmwatch.rules.AppLanguage
import it.pixelbox.cmwatch.rules.SpeechRate
import it.pixelbox.cmwatch.rules.SpeechText
import java.util.Locale

/**
 * La voce della modalità live: frasi in coda, una sola cuffia (spec live, §2: mai l'altoparlante). Prende l'audio focus
 * transitorio per la durata delle frasi e lo lascia quando la coda è vuota; `onIdle` arriva quando la voce ha detto tutto.
 * `onFocus(false)` = una chiamata o un altro audio con priorità: la live va in pausa.
 */
class LiveVoice(ctx: Context, private val onIdle: () -> Unit, private val onFocus: (Boolean) -> Unit) {
    private val app = ctx.applicationContext
    private val audio = app.getSystemService(AudioManager::class.java)
    private val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attrs)
        .setOnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> onFocus(false)
                AudioManager.AUDIOFOCUS_GAIN -> onFocus(true)
            }
        }
        .build()
    @Volatile private var ready = false
    /** Ogni Hush cambia generazione: i callback dei pezzi interrotti non contano. */
    @Volatile private var generation = 0
    @Volatile private var pending = 0
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 60) }.getOrNull()
    private val tts = TextToSpeech(app) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) setup()
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = done(utteranceId)
            @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) = done(utteranceId)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = Unit
        })
    }

    private fun setup() {
        tts.setAudioAttributes(attrs)
        // La lingua dell'app, come il tasto ▶ (`Speech`).
        val tags = app.getSystemService(android.app.LocaleManager::class.java).applicationLocales.toLanguageTags()
        tts.language = AppLanguage.voiceLocale(AppLanguage.fromTags(tags), Locale.getDefault())
        val prefs = app.getSharedPreferences("speech", Context.MODE_PRIVATE)
        // La voce e la velocità scelte nelle impostazioni del tasto ▶ valgono anche qui.
        prefs.getString("voice", null)?.let { n -> runCatching { tts.voices }.getOrNull()?.firstOrNull { it.name == n }?.let { tts.voice = it } }
        tts.setSpeechRate(SpeechRate.of(if (prefs.contains("rate")) prefs.getFloat("rate", SpeechRate.NORMAL) else null))
    }

    /** C'è una cuffia: bluetooth, BLE, cablata, USB o apparecchio acustico. */
    fun headset(): Boolean = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in HEADSETS }

    @Synchronized fun say(text: String) {
        if (!ready || !headset()) { onIdle(); return }
        if (pending == 0 && audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { onFocus(false); return }
        val g = generation
        val parts = SpeechText.chunks(text)
        pending += parts.size
        parts.forEachIndexed { i, p -> tts.speak(p, TextToSpeech.QUEUE_ADD, null, "live-$g-${System.nanoTime()}-$i") }
    }

    @Synchronized fun hush() {
        generation++
        pending = 0
        tts.stop()
        audio.abandonAudioFocusRequest(focus)
    }

    fun tone() { if (headset()) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 150) }

    fun shutdown() { hush(); tts.shutdown(); tone?.release() }

    private fun done(id: String?) {
        val idle = synchronized(this) {
            if (id?.startsWith("live-$generation-") != true) return
            pending = (pending - 1).coerceAtLeast(0)
            if (pending == 0) audio.abandonAudioFocusRequest(focus)
            pending == 0
        }
        if (idle) onIdle()
    }

    companion object {
        val HEADSETS = setOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_HEARING_AID,
        )
    }
}
