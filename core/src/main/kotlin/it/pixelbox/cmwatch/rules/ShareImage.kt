package it.pixelbox.cmwatch.rules

import kotlin.math.max
import kotlin.math.roundToInt

/** «Condividi» (contratto 1.19): l'immagine va ridotta a 1600 px sul lato lungo prima di cifrarla (circa 300-600 KB). */
object ShareImage {
    const val LONG_SIDE = 1600

    fun scaled(w: Int, h: Int, longSide: Int = LONG_SIDE): Pair<Int, Int> {
        val big = max(w, h)
        if (big <= longSide) return w to h
        val f = longSide.toDouble() / big
        return (w * f).roundToInt() to (h * f).roundToInt()
    }

    /** Il campionamento della decodifica: la potenza di 2 più grande che lascia il lato lungo sopra `longSide`. */
    fun sampleSize(w: Int, h: Int, longSide: Int = LONG_SIDE): Int {
        var s = 1
        while (max(w, h) / (s * 2) >= longSide) s *= 2
        return s
    }
}
