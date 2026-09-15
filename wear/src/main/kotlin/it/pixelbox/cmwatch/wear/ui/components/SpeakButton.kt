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
import androidx.wear.compose.material3.IconToggleButton
import androidx.wear.compose.material3.IconToggleButtonDefaults
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.wear.ui.theme.CmColors

/**
 * ▶ / ■ da 48 dp accanto a un testo da leggere a voce. Toggle con le forme animate di M3 Expressive (proposta A1, 15/09):
 * premuto si schiaccia, in lettura il tondo diventa un quadrato arrotondato con ■, come play/pausa nelle app Google.
 * Con «riduci animazioni» la libreria cambia forma senza movimento.
 */
@Composable
fun SpeakButton(speaking: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    IconToggleButton(
        checked = speaking,
        onCheckedChange = { onToggle() },
        modifier = modifier.size(48.dp),
        colors = IconToggleButtonDefaults.colors(
            checkedContainerColor = CmColors.actionIcon, checkedContentColor = CmColors.bg,
            uncheckedContainerColor = CmColors.surface, uncheckedContentColor = CmColors.actionIcon,
        ),
        shapes = IconToggleButtonDefaults.animatedShapes(),
    ) {
        Icon(
            imageVector = if (speaking) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
            contentDescription = stringResource(if (speaking) R.string.tts_stop else R.string.tts_play),
        )
    }
}
