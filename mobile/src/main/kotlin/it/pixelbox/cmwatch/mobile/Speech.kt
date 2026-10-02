package it.pixelbox.cmwatch.mobile

import android.app.LocaleManager
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import it.pixelbox.cmwatch.rules.AnswerText
import it.pixelbox.cmwatch.rules.AppLanguage
import it.pixelbox.cmwatch.rules.NextSteps
import it.pixelbox.cmwatch.rules.OutcomeLine
import it.pixelbox.cmwatch.rules.SpeechText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * La voce di sistema per il tasto ▶ (niente microfono in-app, design 12/09). Parla nella lingua scelta per l'app, con la
 * voce scelta nelle impostazioni (Franz, 02/10 00:01). Legge a paragrafi come l'orologio (`AnswerText.blocks`), ognuno
 * pulito dal markdown e a pezzi sotto il limite del motore; `block` = il paragrafo che sta leggendo, e si può ripartire da
 * un paragrafo toccato. `speaking` = il testo che sta leggendo: il suo tasto diventa Stop.
 */
class Speech(ctx: Context) {
    private val app = ctx.applicationContext
    private val prefs = app.getSharedPreferences("speech", Context.MODE_PRIVATE)
    @Volatile private var ready = false
    private val locales = ctx.getSystemService(LocaleManager::class.java)
    private val _speaking = MutableStateFlow<String?>(null)
    val speaking: StateFlow<String?> = _speaking
    private val _block = MutableStateFlow<Int?>(null)
    val block: StateFlow<Int?> = _block
    private val _voices = MutableStateFlow<List<String>>(emptyList())
    /** Le voci italiane del motore, prima quelle installate e poi quelle di rete (come l'orologio). */
    val voices: StateFlow<List<String>> = _voices
    private val _voice = MutableStateFlow(prefs.getString("voice", null))
    /** La voce scelta; null = la predefinita del telefono. */
    val voice: StateFlow<String?> = _voice
    private var current: String? = null
    @Volatile private var prefix = "-"
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(app) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) _voices.value = italian()
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _speaking.value = current
                _block.value = BLOCK.find(utteranceId.orEmpty())?.groupValues?.get(1)?.toInt()
            }
            // Solo l'ultimo pezzo chiude la lettura: il tasto resta ■ per tutto il testo.
            override fun onDone(utteranceId: String?) { if (utteranceId?.endsWith("-end") == true) finished() }
            @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { finished() }
            // Un testo nuovo interrompe il vecchio: si spegne solo se è stato fermato il testo che sta leggendo adesso.
            override fun onStop(utteranceId: String?, interrupted: Boolean) { if (utteranceId?.startsWith(prefix) == true) finished() }
        })
    }

    fun speak(text: String) = speakBlocks(text, 0)

    /**
     * Legge `text` dal paragrafo `from` in poi: senza le righe di servizio («Prossimi:», «Watch:» letta come esito), ogni
     * paragrafo pulito dal markdown (Franz, 02/10 00:01: leggeva «asterisco asterisco») e a pezzi.
     */
    fun speakBlocks(text: String, from: Int) {
        if (!ready) return
        // Letta a ogni lettura: la lingua si cambia nelle impostazioni mentre l'app è aperta.
        tts.language = AppLanguage.voiceLocale(AppLanguage.fromTags(locales.applicationLocales.toLanguageTags()), Locale.getDefault())
        applyVoice()
        current = text
        _speaking.value = text
        prefix = "cm-" + text.hashCode() + "-"
        val code = app.getString(R.string.tts_code)
        val items = blocksOf(text).withIndex().drop(from).flatMap { (i, b) -> SpeechText.chunks(SpeechText.forBlock(b.kind, b.text, code)).map { i to it } }
        if (items.isEmpty()) { finished(); return }
        items.forEachIndexed { k, (i, p) ->
            tts.speak(p, if (k == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, prefix + "b" + i + "-" + k + if (k == items.lastIndex) "-end" else "")
        }
    }

    /** I paragrafi di un testo, gli stessi che la chat mostra durante la lettura. */
    fun blocksOf(text: String): List<AnswerText.Block> =
        AnswerText.blocks(OutcomeLine.forPhone(NextSteps.parse(text).text, app.getString(R.string.outcome_label)))

    /** Lo stesso tasto: legge, o ferma se sta già leggendo quel testo. */
    fun toggle(text: String) { if (_speaking.value == text) stop() else speak(text) }
    fun stop() { tts.stop(); finished() }
    fun shutdown() { tts.shutdown() }

    /** La voce scelta nelle impostazioni, ricordata; null = la predefinita. */
    fun setVoice(name: String?) {
        _voice.value = name
        prefs.edit().putString("voice", name).apply()
        if (ready) applyVoice()
    }

    private fun finished() { _speaking.value = null; _block.value = null }

    private fun applyVoice() {
        val v = _voice.value?.let { n -> runCatching { tts.voices }.getOrNull()?.firstOrNull { it.name == n } }
        if (v != null) tts.voice = v
    }

    private fun italian(): List<String> = runCatching {
        tts.voices.orEmpty()
            .filter { it.locale.language == "it" && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features }
            .sortedWith(compareBy({ it.isNetworkConnectionRequired }, { it.name }))
            .map { it.name }
    }.getOrDefault(emptyList())

    private companion object { val BLOCK = Regex("-b(\\d+)-") }
}
