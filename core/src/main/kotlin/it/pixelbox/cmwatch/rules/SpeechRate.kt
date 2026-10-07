package it.pixelbox.cmwatch.rules

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * La velocità della voce del tasto ▶ (Franz, 02/10 15:49: «la lettura con la voce è un po' troppo rapida»), in multipli
 * della velocità del motore (1 = la sua). Dal 07/10 è continua (Franz, 21:15: «analogica», con uno slider sul comando;
 * approvata alle 22:06): da 0,5× a 2× a passi di 0,05, con uno scatto su 1×.
 */
object SpeechRate {
    const val NORMAL = 1.0f
    const val MIN = 0.5f
    const val MAX = 2.0f
    private const val STEP = 0.05f
    /** I punti dello slider fra gli estremi (30 intervalli da 0,05). */
    const val SLIDER_STEPS = 29

    /** Al passo di 0,05 più vicino, dentro 0,5-2; vicino a 1 si ferma su 1, così la velocità normale si ritrova al tatto. */
    fun snap(v: Float): Float {
        val c = v.coerceIn(MIN, MAX)
        if (abs(c - NORMAL) < 0.035f) return NORMAL
        return ((c / STEP).roundToInt() * 5) / 100f
    }

    /** Il valore salvato, portato sul passo; senza scelta o con un valore rovinato, la velocità del motore. */
    fun of(saved: Float?): Float = saved?.takeIf { it.isFinite() && it > 0f }?.let { snap(it) } ?: NORMAL

    /** Dove sta `v` sulla corsa dello slider, da 0 a 1: per il segno di 1×. */
    fun fraction(v: Float): Float = (v - MIN) / (MAX - MIN)
}
