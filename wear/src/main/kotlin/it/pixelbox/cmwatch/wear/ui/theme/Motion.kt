package it.pixelbox.cmwatch.wear.ui.theme

import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import kotlin.math.abs

/**
 * Testo che scorre quando non entra (Franz, 13/09 09:40): parte dopo 2 s, tre giri, poi resta fermo e troncato.
 * Scorre solo la riga al centro della lista; mai in ambient né con le animazioni di sistema spente.
 */
fun Modifier.cmMarquee(enabled: Boolean): Modifier =
    if (!enabled) this else this.basicMarquee(iterations = 3, initialDelayMillis = 2000, repeatDelayMillis = 2000, spacing = MarqueeSpacing(24.dp))

/** Indice dell'elemento che la trasformazione tiene al centro (scala piena). */
@Composable
fun rememberCenterIndex(state: TransformingLazyColumnState): Int? {
    val center by remember(state) {
        derivedStateOf {
            val info = state.layoutInfo
            val viewportCenter = (info.viewportSize.height) / 2
            info.visibleItems.minByOrNull { abs((it.offset + it.transformedHeight / 2) - viewportCenter) }?.index
        }
    }
    return center
}
