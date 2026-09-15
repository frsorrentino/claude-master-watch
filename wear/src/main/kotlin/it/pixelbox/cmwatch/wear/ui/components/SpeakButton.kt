package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.wear.ui.theme.CmColors

/** ▶ / ■ da 48 dp accanto a un testo da leggere a voce. */
@Composable
fun SpeakButton(speaking: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onToggle, modifier = modifier.size(48.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = CmColors.surface, contentColor = CmColors.actionIcon)) {
        Icon(
            imageVector = if (speaking) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
            contentDescription = stringResource(if (speaking) R.string.tts_stop else R.string.tts_play),
        )
    }
}
