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
import it.pixelbox.cmwatch.wear.ui.components.WideButton
import it.pixelbox.cmwatch.wear.ui.theme.roundListPadding

/** Menu (Franz, 12/09 15:35): le altre schermate stanno qui, non in fondo alla lista delle sessioni. */
@Composable
fun MenuScreen(onOpen: (Screen) -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val entries = listOf(
        R.string.timeline_title to Screen.Timeline, R.string.launch_title to Screen.Launch, R.string.quota_title to Screen.Quota,
        R.string.recap_short to Screen.Recap, R.string.night_title to Screen.Night, R.string.settings_title to Screen.Settings,
    )
    ScreenScaffold(scrollState = listState, contentPadding = roundListPadding()) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) { Text(stringResource(R.string.menu_title)) } }
            for ((label, screen) in entries) item {
                WideButton(stringResource(label), onClick = { onOpen(screen) }, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec))
            }
        }
    }
}
