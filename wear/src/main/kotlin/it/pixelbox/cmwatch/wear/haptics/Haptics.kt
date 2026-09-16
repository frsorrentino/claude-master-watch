package it.pixelbox.cmwatch.wear.haptics

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Aptica per tipo (design, sezione 3): domanda 2×60 ms; esito 40; sparita 200; inviato tick; confermato due tick;
 * errore tre colpi. Dal 16/09 gli interruttori hanno un segno proprio: acceso sale, spento scende, così «segui» e
 * «non seguire» si distinguono al buio senza guardare. Dove il motore le sostiene si usano le primitive composte
 * (più corte e più definite dei millisecondi grezzi), altrimenti si ripiega su un'onda equivalente.
 */
object Haptics {
    enum class Kind { QUESTION, OUTCOME, GONE, SENT, CONFIRMED, ERROR, TOGGLE_ON, TOGGLE_OFF }

    fun play(ctx: Context, kind: Kind) {
        val v = ctx.getSystemService(Vibrator::class.java) ?: return
        if (!v.hasVibrator()) return
        if ((kind == Kind.TOGGLE_ON || kind == Kind.TOGGLE_OFF) && composed(v, kind)) return
        val eff = when (kind) {
            Kind.QUESTION -> VibrationEffect.createWaveform(longArrayOf(0, 60, 80, 60), -1)
            Kind.OUTCOME -> VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            Kind.GONE -> VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            Kind.SENT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            Kind.CONFIRMED -> VibrationEffect.createWaveform(longArrayOf(0, 20, 60, 20), -1)
            Kind.ERROR -> VibrationEffect.createWaveform(longArrayOf(0, 50, 60, 50, 60, 50), -1)
            // Ripiego senza primitive: acceso due colpi crescenti, spento due calanti (le ampiezze dicono il verso).
            Kind.TOGGLE_ON -> VibrationEffect.createWaveform(longArrayOf(0, 25, 40, 35), intArrayOf(0, 90, 0, 200), -1)
            Kind.TOGGLE_OFF -> VibrationEffect.createWaveform(longArrayOf(0, 35, 40, 25), intArrayOf(0, 200, 0, 90), -1)
        }
        v.vibrate(eff)
    }

    /** Primitive composte: salita per l'acceso, discesa per lo spento. Falso se il motore non le sostiene. */
    private fun composed(v: Vibrator, kind: Kind): Boolean {
        val rise = VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
        val fall = VibrationEffect.Composition.PRIMITIVE_QUICK_FALL
        val tick = VibrationEffect.Composition.PRIMITIVE_TICK
        val wanted = if (kind == Kind.TOGGLE_ON) intArrayOf(rise, tick) else intArrayOf(fall, tick)
        if (!v.areAllPrimitivesSupported(*wanted)) return false
        val c = VibrationEffect.startComposition()
        if (kind == Kind.TOGGLE_ON) c.addPrimitive(rise, 0.5f).addPrimitive(tick, 0.7f, 30)
        else c.addPrimitive(fall, 0.5f).addPrimitive(tick, 0.4f, 30)
        v.vibrate(c.compose())
        return true
    }
}
