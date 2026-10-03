package it.pixelbox.cmwatch.rules

import kotlin.math.abs

/**
 * La velocità della voce del tasto ▶ (Franz, 02/10 15:49: «la lettura con la voce è un po' troppo rapida»): le scelte
 * delle impostazioni, in multipli della velocità del motore (1 = la sua).
 */
object SpeechRate {
    const val NORMAL = 1.0f
    val choices = listOf(0.7f, 0.8f, 0.9f, NORMAL, 1.25f, 1.5f, 2.0f)

    /** La pillola accanto a ■ durante la lettura (Franz, 03/10 21:16): a ogni tocco la più veloce dopo, da 2× di nuovo 1×. */
    val pill = listOf(NORMAL, 1.25f, 1.5f, 2.0f)
    fun next(current: Float): Float = pill.firstOrNull { it > current + 0.01f }?.takeIf { current >= NORMAL } ?: NORMAL

    /** Il valore salvato, portato alla scelta più vicina; senza scelta o con un valore rovinato, la velocità del motore. */
    fun of(saved: Float?): Float = saved?.takeIf { it.isFinite() && it > 0f }?.let { s -> choices.minBy { abs(it - s) } } ?: NORMAL
}
