package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/** Le scene del «volo» card → scheda (design 24/09, «Movimento»); assenti negli snapshot, dove il modificatore non fa nulla. */
@OptIn(ExperimentalSharedTransitionApi::class)
class Fly(
    val shared: SharedTransitionScope, val anim: AnimatedVisibilityScope,
    /** Il volo c'è solo se la card toccata era sullo schermo (transizione C, 09/10); altrimenti la pagina entra di lato. */
    val enabled: Boolean = true,
)

/**
 * Le card delle sessioni sullo schermo della home, per nome (transizione C, Franz 09/10 20:15): una sessione aperta da qui vola
 * dalla sua card; aperta da menu, avviso o ricerca, senza card in vista, entra di lato come una pagina.
 */
val LocalCardsOnScreen = compositionLocalOf<androidx.compose.runtime.snapshots.SnapshotStateMap<String, Int>?> { null }

val LocalFly = compositionLocalOf<Fly?> { null }

/** La durata del volo card ↔ scheda. */
const val FLY_MS = 320

/** La card si allarga fino a diventare la scheda, senza dissolvenza finale; con le animazioni spente, subito. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.fly(key: String): Modifier {
    val f = LocalFly.current?.takeIf { it.enabled } ?: return this
    val off = animationsOff()
    return with(f.shared) {
        this@fly.sharedBounds(
            rememberSharedContentState(key), f.anim,
            enter = EnterTransition.None, exit = ExitTransition.None,
            boundsTransform = { _, _ -> if (off) snap() else tween(FLY_MS, easing = CmMotion.easing) },
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    }
}
