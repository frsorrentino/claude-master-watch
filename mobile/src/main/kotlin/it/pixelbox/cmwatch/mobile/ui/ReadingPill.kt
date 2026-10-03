package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.ReadingBar
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** Il mini-controller mentre la voce legge (`ReadingPill`), da `MainActivity`; null quando non legge. */
val LocalReadingBar = staticCompositionLocalOf<(@Composable () -> Unit)?> { null }

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
