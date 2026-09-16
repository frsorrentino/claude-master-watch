package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.rules.Screen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import it.pixelbox.cmwatch.wear.ui.components.WideButton

/** Menu (Franz, 12/09 15:35): le altre schermate stanno qui, non in fondo alla lista delle sessioni. */
@Composable
fun MenuScreen(onOpen: (Screen) -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    // Ogni voce con la sua icona accanto alla parola, come le liste di Messages e i moduli di Maps (proposte 39 e 42).
    val entries = listOf(
        Triple(R.string.timeline_title, Screen.Timeline, Icons.Rounded.History),
        Triple(R.string.launch_title, Screen.Launch, Icons.Rounded.PlayArrow),
        Triple(R.string.quota_title, Screen.Quota, Icons.Rounded.Schedule),
        Triple(R.string.recap_short, Screen.Recap, Icons.Rounded.Notes),
        Triple(R.string.night_title, Screen.Night, Icons.Rounded.Bedtime),
        Triple(R.string.settings_title, Screen.Settings, Icons.Rounded.Settings),
    )
    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) { Text(stringResource(R.string.menu_title)) } }
            for ((label, screen, icon) in entries) item {
                WideButton(
                    stringResource(label), onClick = { onOpen(screen) }, transformation = SurfaceTransformation(spec),
                    modifier = Modifier.transformedHeight(this, spec), icon = icon,
                )
            }
        }
    }
}
