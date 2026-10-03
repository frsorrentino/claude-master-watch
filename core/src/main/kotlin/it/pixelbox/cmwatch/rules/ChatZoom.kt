package it.pixelbox.cmwatch.rules

/**
 * Lo zoom del testo della conversazione con due dita (Franz, 03/10 21:16): un fattore sulla grandezza dei caratteri, che
 * vanno a capo da soli; ricordato fra un'apertura e l'altra.
 */
object ChatZoom {
    const val MIN = 0.85f
    const val MAX = 1.6f

    fun clamp(z: Float): Float = if (z.isFinite()) z.coerceIn(MIN, MAX) else 1f

    /** Il valore salvato; senza valore o con uno rovinato, la grandezza normale. */
    fun of(saved: Float?): Float = saved?.takeIf { it.isFinite() && it > 0f }?.let(::clamp) ?: 1f
}
