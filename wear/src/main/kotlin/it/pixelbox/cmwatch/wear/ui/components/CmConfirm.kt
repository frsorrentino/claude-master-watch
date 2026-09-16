package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.wear.ui.ambient.animationsOff
import it.pixelbox.cmwatch.wear.ui.theme.CmColors

/** Cosa mostrare: il segno, il suo colore e una frase breve. La costruisce chi fa l'azione. */
data class CmConfirmState(val icon: ImageVector, val tint: Color, val text: String)

/**
 * La nostra conferma (Franz, 16/09 03:08: «quelle di sistema sono brutte»): velo scuro, un cerchio con il segno di
 * quello che è appena successo, la frase su una riga sotto. Niente testo curvo, niente forma ruotata: le stesse forme
 * tonde del resto dell'app. Entra con una molla corta, resta un attimo e si chiude da sola; un tocco la chiude prima.
 * Con «riduci animazioni» compare e sparisce senza movimento.
 */
@Composable
fun CmConfirm(state: CmConfirmState?, onDone: () -> Unit) {
    if (state == null) return
    val ferme = animationsOff()
    val enter = remember0()
    // La molla si legge qui: il motion scheme è @Composable, il blocco dell'effetto no.
    val molla = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    LaunchedEffect(state) {
        if (!ferme) enter.animateTo(1f, molla) else enter.snapTo(1f)
        kotlinx.coroutines.delay(if (ferme) 900 else 1200)
        onDone()
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f))
            .clickable(interactionSource = remember1(), indication = null, onClick = onDone),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 24.dp).scale(0.86f + 0.14f * enter.value),
        ) {
            Box(Modifier.size(76.dp).clip(CircleShape).background(CmColors.surfaceHigh), contentAlignment = Alignment.Center) {
                Icon(state.icon, contentDescription = null, tint = state.tint, modifier = Modifier.size(38.dp))
            }
            Text(
                state.text, style = MaterialTheme.typography.titleSmall, color = CmColors.text,
                textAlign = TextAlign.Center, maxLines = 2,
            )
        }
    }
}

@Composable private fun remember0() = androidx.compose.runtime.remember { Animatable(0f) }

@Composable private fun remember1() = androidx.compose.runtime.remember { MutableInteractionSource() }

/** Durata dell'uscita, se un giorno servisse dissolvere invece di chiudere di colpo. */
@Suppress("unused") private val FADE = tween<Float>(durationMillis = 150)
