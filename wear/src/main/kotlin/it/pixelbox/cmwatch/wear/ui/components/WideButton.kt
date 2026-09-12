package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.wear.ui.theme.CmColors

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
    fill: Color = CmColors.accent,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    transformation: SurfaceTransformation? = null,
) {
    val m = modifier.fillMaxWidth().heightIn(min = 52.dp)
    if (primary) {
        Button(
            onClick = onClick, onLongClick = onLongClick, enabled = enabled, modifier = m, transformation = transformation,
            colors = ButtonDefaults.buttonColors(containerColor = fill, contentColor = CmColors.text),
        ) { Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    } else {
        FilledTonalButton(
            onClick = onClick, onLongClick = onLongClick, enabled = enabled, modifier = m, transformation = transformation,
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = CmColors.surface, contentColor = CmColors.text),
            border = BorderStroke(1.dp, CmColors.line),
        ) { Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    }
}
