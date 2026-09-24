package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Pillola a tutta larghezza (52 dp). `primary` = l'unico bottone pieno della schermata; `fill` colora
 * il pieno (ambra per tier medium, rosso per high), altrimenti cobalto.
 */
@Composable
fun WideButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    fill: Color = CmColors.primary,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    transformation: SurfaceTransformation? = null,
    /** Bordo rosso sulle opzioni delle domande a rischio alto, ora che i tasti sono tutti blu (Franz, 15/09 18:14). */
    border: BorderStroke? = null,
    /** Icona accanto alla parola, come nelle liste di Messages e nei moduli di Maps (proposta 39, fase 1). */
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val m = modifier.fillMaxWidth().heightIn(min = 56.dp)
    // Sul primario il suo blu notte; sugli altri pieni chiari (ambra) nero, sui pieni scuri (rosso) bianco.
    val onFill = when {
        fill == CmColors.primary -> CmColors.onPrimary
        fill.luminance() > 0.4f -> Color.Black
        else -> CmColors.text
    }
    if (primary) {
        Button(
            onClick = onClick, onLongClick = onLongClick, enabled = enabled, modifier = m, transformation = transformation,
            colors = ButtonDefaults.buttonColors(containerColor = fill, contentColor = onFill), border = border,
        ) {
            if (icon == null) Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            else androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                androidx.wear.compose.material3.Icon(icon, contentDescription = null)
                Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    } else {
        FilledTonalButton(
            onClick = onClick, onLongClick = onLongClick, enabled = enabled, modifier = m, transformation = transformation,
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = CmColors.surface, contentColor = CmColors.text), border = border,
        ) {
            if (icon == null) Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            else androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                androidx.wear.compose.material3.Icon(icon, contentDescription = null)
                Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
