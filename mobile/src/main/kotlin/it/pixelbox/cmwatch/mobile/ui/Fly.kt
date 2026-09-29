package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/** Le scene del «volo» card → scheda (design 24/09, «Movimento»); assenti negli snapshot, dove il modificatore non fa nulla. */
@OptIn(ExperimentalSharedTransitionApi::class)
class Fly(val shared: SharedTransitionScope, val anim: AnimatedVisibilityScope)

val LocalFly = compositionLocalOf<Fly?> { null }

/** La card si allarga fino a diventare la scheda, senza dissolvenza finale; con le animazioni spente, subito. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.fly(key: String): Modifier {
    val f = LocalFly.current ?: return this
    val off = animationsOff()
    return with(f.shared) {
        this@fly.sharedBounds(
            rememberSharedContentState(key), f.anim,
            enter = EnterTransition.None, exit = ExitTransition.None,
            boundsTransform = { _, _ -> if (off) snap() else tween(320, easing = CmMotion.easing) },
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    }
}
