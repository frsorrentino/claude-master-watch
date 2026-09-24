package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Azione secondaria con l'icona a sinistra, come nelle schede delle app di sistema (review UX, 13/09): quattro
 * bottoni larghi identici davano lo stesso peso a tutto, e l'icona dice in un colpo d'occhio di che azione si tratta.
 */
@Composable
fun IconAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    transformation: SurfaceTransformation? = null,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(21.dp),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        transformation = transformation,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = CmColors.surface, contentColor = CmColors.text),
        icon = { Icon(imageVector = icon, contentDescription = null, tint = CmColors.actionIcon, modifier = Modifier.size(22.dp)) },
        label = { Text(label, maxLines = 1) },
    )
}
