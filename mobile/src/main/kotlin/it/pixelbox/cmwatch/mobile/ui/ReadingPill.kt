package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.ReadingBar
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Il mini-controller in sovraimpressione (Franz, 04/10 00:18): uno solo sopra tutte le schermate, che resta fermo mentre
 * le pagine cambiano sotto. `bar` lo mette `MainActivity` (null quando la voce tace); i posti sopra il campo (`ReadingSlot`)
 * dicono dove posarlo, senza posto sta in fondo.
 */
class ReadingOverlay {
    var bar by mutableStateOf<(@Composable () -> Unit)?>(null)
    internal var heightPx by mutableIntStateOf(0)
    internal val slots = mutableStateMapOf<Any, Rect>()
}

val LocalReadingOverlay = staticCompositionLocalOf<ReadingOverlay?> { null }

/** Lo spazio sopra il campo (`SessionSheet`) dove il mini-controller si posa; vuoto, il controller è disegnato dall'host. */
@Composable
fun ReadingSlot(modifier: Modifier = Modifier) {
    val o = LocalReadingOverlay.current ?: return
    if (o.bar == null) return
    val key = remember { Any() }
    DisposableEffect(key) { onDispose { o.slots.remove(key) } }
    val h = with(LocalDensity.current) { if (o.heightPx > 0) o.heightPx.toDp() else 60.dp }
    Spacer(modifier.fillMaxWidth().height(h).onGloballyPositioned { c ->
        val r = c.boundsInWindow()
        if (o.slots[key] != r) o.slots[key] = r
    })
}

/**
 * Il controller sopra tutto. Fra più posti (le pagine vicine dello scorrimento) vale quello della pagina in vista; il salto
 * fra un posto e l'altro, o verso il fondo, scivola invece di riapparire. Con la tastiera aperta si fa da parte.
 */
@Composable
fun ReadingOverlayHost(o: ReadingOverlay, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val imeOpen = WindowInsets.ime.getBottom(density) > 0
    val bottomPx = WindowInsets.navigationBars.getBottom(density) + with(density) { 12.dp.roundToPx() }
    var rootBottom by remember { mutableFloatStateOf(0f) }
    var last by remember { mutableStateOf(o.bar) }
    o.bar?.let { last = it }
    Box(modifier.fillMaxSize().onGloballyPositioned { rootBottom = it.boundsInWindow().bottom }) {
        val slot = o.slots.values.filter { abs(it.left) < it.width / 2 }.minByOrNull { abs(it.left) }
        val lift = slot?.let { (rootBottom - it.bottom).roundToInt() } ?: bottomPx
        val shown by animateIntAsState(lift, tween(220, easing = FastOutSlowInEasing), label = "readingLift")
        AnimatedVisibility(
            o.bar != null && !imeOpen, Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            Box(Modifier.offset { IntOffset(0, -shown) }.padding(horizontal = 12.dp).onSizeChanged { o.heightPx = it.height }) { last?.invoke() }
        }
    }
}

/**
 * Il mini-controller della lettura (Franz, 03/10 23:00): su ogni schermata finché la voce legge. Le barrette che si
 * muovono, da dove arriva il testo e la sua prima riga (il tocco riporta lì), la velocità e ■ per fermare.
 */
@Composable
fun ReadingPill(source: String?, text: String, onOpen: (() -> Unit)?, onStop: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = CmColors.surfaceHigh, shadowElevation = 6.dp) {
        Row(
            Modifier.heightIn(min = 60.dp).padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Bars()
            val openLabel = source?.let { stringResource(R.string.reading_open, it) }
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .then(if (onOpen != null) Modifier.clickable(onClickLabel = openLabel, onClick = onOpen) else Modifier)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(source ?: stringResource(R.string.reading_now), style = MonoSmall, maxLines = 1, overflow = TextOverflow.Clip)
                Text(ReadingBar.excerpt(text), style = MaterialTheme.typography.bodyMedium, color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip)
            }
            RatePill()
            FilledTonalIconButton(onClick = onStop, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Rounded.Stop, stringResource(R.string.stop_reading), tint = CmColors.actionIcon)
            }
        }
    }
}

/** Tre barrette che salgono e scendono mentre legge; ferme con le animazioni spente. */
@Composable
private fun Bars() {
    val off = animationsOff()
    val flow = if (off) null else rememberInfiniteTransition(label = "bars")
    Row(Modifier.height(20.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(0, 180, 360).forEach { delay ->
            val h = flow?.let {
                val v by it.animateFloat(0.35f, 1f, infiniteRepeatable(tween(520, delayMillis = delay, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bar$delay")
                v
            } ?: 0.6f
            Box(Modifier.width(4.dp).fillMaxHeight(h).clip(RoundedCornerShape(2.dp)).background(CmColors.actionIcon))
        }
    }
}
