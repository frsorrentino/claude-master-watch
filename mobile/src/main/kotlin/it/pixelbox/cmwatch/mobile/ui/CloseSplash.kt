package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.CloseSplash
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SPLASH_HM = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Il pannello di chiusura sopra la pagina della sessione (Franz, 08/10 18:29, variante A): la pagina resta sotto, velata,
 * e il pannello dice che cosa succede. «Si sta chiudendo» con i passi veri; «è chiusa» con l'ora, «Riapri» e «Torna alla
 * home», e dopo 3 s il ritorno da solo, con la barra che si consuma.
 */
@Composable
fun CloseSplashOverlay(
    phase: CloseSplash.Phase, onHome: () -> Unit, onReopen: (() -> Unit)?,
    /** In una colonna del tablet il pannello toglie la colonna invece di tornare alla home. */
    column: Boolean = false,
) {
    val home by rememberUpdatedState(onHome)
    val hm = { at: Long -> SPLASH_HM.format(Instant.ofEpochSecond(at).atZone(ZoneId.systemDefault())) }
    // Il velo prende i tocchi: la pagina sotto non si usa più.
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f))
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp).widthIn(max = 420.dp).fillMaxWidth()
                .background(CmColors.surface, RoundedCornerShape(28.dp)).padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val closed = phase is CloseSplash.Phase.Closed
            Icon(Icons.Rounded.PowerSettingsNew, null, tint = if (closed) CmColors.text2 else CmColors.actionIcon, modifier = Modifier.size(44.dp))
            when (phase) {
                is CloseSplash.Phase.Closing -> {
                    Text(stringResource(R.string.splash_closing, phase.name), style = MaterialTheme.typography.titleLarge, color = CmColors.text, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.splash_sent_at, hm(phase.sentAt)), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
                    Column(Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SplashStep(phase.delivered, stringResource(R.string.splash_step_delivered))
                        SplashStep(false, stringResource(R.string.splash_step_confirmed))
                    }
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp), color = CmColors.actionIcon, trackColor = CmColors.surfaceHigh)
                    if (phase.slow) {
                        Text(stringResource(R.string.splash_slow), style = MaterialTheme.typography.labelMedium, color = CmColors.waiting)
                        TextButton(onClick = onHome) { Text(stringResource(if (column) R.string.splash_drop_column else R.string.splash_home), color = CmColors.actionIcon) }
                    } else Text(stringResource(R.string.splash_usually), style = MaterialTheme.typography.labelMedium, color = CmColors.text2)
                }
                is CloseSplash.Phase.Closed -> {
                    Text(stringResource(R.string.splash_closed, phase.name), style = MaterialTheme.typography.titleLarge, color = CmColors.text, textAlign = TextAlign.Center)
                    Text(
                        stringResource(if (phase.byMe) R.string.splash_closed_line else R.string.splash_closed_line_other, hm(phase.at)),
                        style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, textAlign = TextAlign.Center,
                    )
                    // La barra si consuma nei 3 s prima del ritorno alla home.
                    val left = remember(phase) { androidx.compose.animation.core.Animatable(1f) }
                    LaunchedEffect(phase) {
                        left.animateTo(0f, androidx.compose.animation.core.tween(CloseSplash.CLOSED_SHOW_MS.toInt(), easing = androidx.compose.animation.core.LinearEasing))
                        home()
                    }
                    LinearProgressIndicator({ left.value }, Modifier.fillMaxWidth().padding(top = 8.dp), color = CmColors.actionIcon, trackColor = CmColors.surfaceHigh, drawStopIndicator = {})
                    Text(stringResource(if (column) R.string.splash_drop_soon else R.string.splash_back_soon), style = MaterialTheme.typography.labelMedium, color = CmColors.text2)
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        onReopen?.let { TextButton(onClick = it) { Text(stringResource(R.string.splash_reopen), color = CmColors.actionIcon) } }
                        Button(onClick = onHome, colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary)) {
                            Text(stringResource(if (column) R.string.splash_drop_column else R.string.splash_home))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SplashStep(done: Boolean, text: String) = Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    Icon(if (done) Icons.Rounded.Check else Icons.Rounded.RadioButtonUnchecked, null, tint = if (done) CmColors.text else CmColors.text2, modifier = Modifier.size(18.dp))
    Text(text, style = MaterialTheme.typography.bodyMedium, color = if (done) CmColors.text else CmColors.text2)
}
