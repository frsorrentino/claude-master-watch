package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Scorrimento guidato per i video promozionali (Franz, 16/09 18:08: «movimento unico continuo e sinuoso fino agli
 * elementi»). La corona simulata via adb arriva a scatti e i trascinamenti si fermano a ogni stacco del dito: qui è la
 * lista stessa a scorrere di una distanza data, in un solo movimento con partenza e arrivo morbidi. Le richieste arrivano
 * solo dall'extra `scroll_px` con la demo accesa: nell'uso normale nessuno ne manda.
 */
object DemoScrollBus {
    /** Pixel da scorrere (negativo = verso l'alto) e durata in millisecondi. */
    val requests = MutableSharedFlow<Pair<Int, Int>>(extraBufferCapacity = 4)
}

/** Curva sinusoidale in entrata e in uscita: il movimento accelera piano, scorre e rallenta fino a fermarsi. */
private val Sinuosa = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

@Composable
fun DemoScroll(state: ScrollableState) {
    LaunchedEffect(state) {
        DemoScrollBus.requests.collect { (px, ms) -> state.animateScrollBy(px.toFloat(), tween(ms, easing = Sinuosa)) }
    }
}
