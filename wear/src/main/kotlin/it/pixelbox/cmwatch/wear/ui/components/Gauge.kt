package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.wear.compose.material3.MaterialTheme
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import it.pixelbox.cmwatch.rules.BriefCards
import it.pixelbox.cmwatch.wear.ui.theme.CmColors

/**
 * Gauge come quelli del brief mattutino: arco aperto in basso con l'icona del dato al centro (Franz, 13/09 17:01).
 * L'apertura è di 31° centrata sulle ore 6 e il tratto è di 8 dp: misurati sui fotogrammi della sua schermata.
 * Quando il gauge arriva al pieno pulsa tre volte e si ferma: al 100 % la finestra di quota è finita o il PC è
 * fermo, e va notato senza restare ad animare per sempre addosso alla batteria.
 */
@Composable
fun Gauge(
    progress: Float,
    tone: BriefCards.Tone,
    glyph: BriefCards.Glyph,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    animate: Boolean = true,
    /** L'arco si riempie quando la card entra nell'inquadratura, non quando viene composta (Franz, 16/09 01:45). */
    visible: Boolean = true,
) {
    val target = progress.coerceIn(0f, 1f)
    val shown = remember { Animatable(if (animate) 0f else target) }
    // L'arco si riempie con la molla lenta del motion scheme di M3 Expressive, non con una curva fissa (B9, 15/09 23:40).
    val fill = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(target, animate, visible) {
        // G9: il riempimento parte quando la card entra nell'inquadratura (Franz, 16/09 01:45), non quando viene
        // composta. Prima finiva mentre la schermata stava ancora entrando e al polso non si vedeva.
        when {
            !animate -> shown.snapTo(target)
            visible -> shown.animateTo(target, fill)
            else -> shown.snapTo(0f)
        }
    }
    // Il dato vecchio si spegne piano: l'arco e l'icona scendono a metà luce invece di cambiare colore di colpo.
    val light by animateFloatAsState(if (tone == BriefCards.Tone.STALE) 0.5f else 1f, MaterialTheme.motionScheme.slowEffectsSpec(), label = "luce")
    val full = target >= 1f
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(full, animate) {
        if (full && animate) {
            // Tre respiri: l'arco si smorza e torna pieno. Poi resta fermo.
            repeat(3) {
                pulse.animateTo(0.4f, tween(durationMillis = 450, easing = LinearEasing))
                pulse.animateTo(1f, tween(durationMillis = 450, easing = LinearEasing))
            }
        } else {
            pulse.snapTo(1f)
        }
    }
    val ink = colour(tone)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { shown.value },
            modifier = Modifier.fillMaxSize(),
            startAngle = START,
            endAngle = END,
            strokeWidth = 8.dp,
            colors = ProgressIndicatorDefaults.colors(
                indicatorColor = ink.copy(alpha = pulse.value * light),
                trackColor = CmColors.briefTrack,
            ),
        )
        Icon(
            imageVector = vector(glyph),
            contentDescription = null,
            tint = ink.copy(alpha = light),
            modifier = Modifier.size(size * 0.38f).graphicsLayer {
                val s = 1f + (1f - pulse.value) * 0.18f
                scaleX = s; scaleY = s
            },
        )
    }
}

/** Apertura di 31° centrata sulle ore 6 (90°), misurata sul gauge del brief. */
private const val START = 105.5f
private const val END = 74.5f

private fun colour(tone: BriefCards.Tone): Color = when (tone) {
    BriefCards.Tone.GOOD -> CmColors.briefGood
    BriefCards.Tone.WARN -> CmColors.briefWarn
    BriefCards.Tone.ALERT -> CmColors.briefAlertRing
    BriefCards.Tone.STALE -> CmColors.stale
    BriefCards.Tone.NEUTRAL -> CmColors.briefRing
}

private fun vector(glyph: BriefCards.Glyph): ImageVector = when (glyph) {
    BriefCards.Glyph.TIME -> Icons.Rounded.Schedule
    BriefCards.Glyph.SESSIONS -> Icons.Rounded.Terminal
    BriefCards.Glyph.QUESTION -> Icons.Rounded.QuestionMark
    BriefCards.Glyph.NIGHT -> Icons.Rounded.Bedtime
    BriefCards.Glyph.SYNC -> Icons.Rounded.Sync
}
