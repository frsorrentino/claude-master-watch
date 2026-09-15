package it.pixelbox.cmwatch.wear.ui.theme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import it.pixelbox.cmwatch.wear.ui.ambient.animationsOff

/**
 * Bagliore al bordo inferiore (proposta 44, fase 1; idea presa da Gemini su Wear OS, che accende una luce in fondo al
 * quadrante quando ascolta). Qui dice che la sessione sta lavorando: respira piano, si spegne da solo quando la sessione
 * si ferma. Niente in ambient e niente con «riduci animazioni»: resta una luce ferma e fioca.
 */
@Composable
fun Modifier.bottomGlow(color: Color, visible: Boolean, ambient: Boolean = false): Modifier {
    if (!visible) return this
    val ferme = ambient || animationsOff()
    val respiro by if (ferme) {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    } else {
        rememberInfiniteTransition(label = "bagliore").animateFloat(
            initialValue = 0.25f, targetValue = 0.75f,
            animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "luce",
        )
    }
    return drawWithContent {
        drawContent()
        // Una fascia alta un quinto dello schermo, dal bordo in su, che sfuma nel nero: non copre il testo.
        val h = size.height * 0.2f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, color.copy(alpha = 0.10f + 0.18f * respiro)),
                startY = size.height - h, endY = size.height,
            ),
            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - h),
            size = androidx.compose.ui.geometry.Size(size.width, h),
        )
    }
}
