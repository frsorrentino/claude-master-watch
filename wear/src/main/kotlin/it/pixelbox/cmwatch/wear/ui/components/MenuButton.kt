package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.wear.ui.theme.CmColors

/** Tasto Menu contornato con icona: non si confonde con le righe piene delle sessioni. */
@Composable
fun MenuButton(onClick: () -> Unit, transformation: SurfaceTransformation? = null, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick, modifier = modifier, transformation = transformation,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CmColors.text2),
    ) {
        Icon(Icons.Rounded.Menu, contentDescription = null, tint = CmColors.text2)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.menu_title))
    }
}
