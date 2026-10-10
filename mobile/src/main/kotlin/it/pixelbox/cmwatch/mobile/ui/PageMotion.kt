package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset

/**
 * Le pagine che prendevano lo schermo di colpo (Franz, 10/10 15:59: «possiamo introdurre transizioni su ogni passaggio di
 * schermate che avviene di colpo?»): Impostazioni, Terminale, Recap, Notte, Cerca, Coda e Registro entrano da destra con
 * una dissolvenza, come una sessione aperta senza la sua card (transizione C); tornando indietro la home rientra da sinistra.
 * L'entrata si decide al primo disegno: `animate` falso, o le animazioni spente, e il contenuto è subito al suo posto.
 */
@Composable
fun PageIn(fromStart: Boolean = false, animate: Boolean = true, content: @Composable () -> Unit) {
    val off = animationsOff()
    val shown = remember { MutableTransitionState(off || !animate).apply { targetState = true } }
    val ease = tween<IntOffset>(FLY_MS, easing = CmMotion.easing)
    AnimatedVisibility(
        shown,
        enter = slideInHorizontally(ease) { w -> if (fromStart) -w / 10 else w / 3 } + fadeIn(tween(FLY_MS)),
        exit = ExitTransition.None,
    ) { content() }
}
