package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.wear.ui.ambient.animationsOff

/**
 * Numeri che rotolano (proposta 19, fase 1): quando il valore cambia, quello nuovo scorre dentro dall'alto se è
 * cresciuto e dal basso se è calato, come nelle app Google. Percentuali della quota, età delle sessioni, contatori.
 * Con «riduci animazioni» attive, o in ambient, il testo cambia e basta: niente movimento addosso alla batteria.
 */
@Composable
fun RollingText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    animate: Boolean = true,
) {
    if (!animate || animationsOff()) {
        Text(text, style = style, color = color, maxLines = maxLines, modifier = modifier)
        return
    }
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.unit.IntOffset>()
    val fade = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            // Il verso lo dice il numero: cresciuto scende dall'alto, calato sale dal basso. Senza numeri, dall'alto.
            val su = numero(targetState) >= numero(initialState)
            val giu = if (su) -1 else 1
            (slideInVertically(spec) { h -> giu * h } + fadeIn(fade)) togetherWith
                (slideOutVertically(spec) { h -> -giu * h } + fadeOut(fade))
        },
        label = "numero",
    ) { t -> Text(t, style = style, color = color, maxLines = maxLines) }
}

/** Il primo numero del testo («76 %» → 76, «1 h 34» → 1); serve solo a scegliere il verso. */
private fun numero(t: String): Long = Regex("\\d+").find(t)?.value?.toLongOrNull() ?: 0L
