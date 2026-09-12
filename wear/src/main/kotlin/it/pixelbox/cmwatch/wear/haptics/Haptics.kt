package it.pixelbox.cmwatch.wear.haptics

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

/** Aptica per tipo (design, sezione 3): domanda 2×60 ms; esito 40; sparita 200; inviato tick; confermato due tick; errore tre colpi. */
object Haptics {
    enum class Kind { QUESTION, OUTCOME, GONE, SENT, CONFIRMED, ERROR }

    fun play(ctx: Context, kind: Kind) {
        val v = ctx.getSystemService(Vibrator::class.java) ?: return
        if (!v.hasVibrator()) return
        val eff = when (kind) {
            Kind.QUESTION -> VibrationEffect.createWaveform(longArrayOf(0, 60, 80, 60), -1)
            Kind.OUTCOME -> VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            Kind.GONE -> VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            Kind.SENT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            Kind.CONFIRMED -> VibrationEffect.createWaveform(longArrayOf(0, 20, 60, 20), -1)
            Kind.ERROR -> VibrationEffect.createWaveform(longArrayOf(0, 50, 60, 50, 60, 50), -1)
        }
        v.vibrate(eff)
    }
}
