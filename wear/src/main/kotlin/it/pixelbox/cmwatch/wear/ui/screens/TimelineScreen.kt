package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.rules.TimelineText
import it.pixelbox.cmwatch.wear.ui.components.WideButton
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.roundListPadding
import java.time.ZoneId

/** Timeline: /events per giorno, filtro sessione a scelta ciclica (tutte → una → …). */
@Composable
fun TimelineScreen(events: List<Event>) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val sessions = events.mapNotNull { it.session }.distinct()
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val groups = TimelineText.groups(events, ZoneId.systemDefault(), filter)
    ScreenScaffold(scrollState = listState, contentPadding = roundListPadding()) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) { Text(stringResource(R.string.timeline_title)) } }
            if (sessions.isNotEmpty()) item {
                WideButton(
                    filter ?: stringResource(R.string.timeline_all),
                    onClick = { filter = if (filter == null) sessions.first() else sessions.getOrNull(sessions.indexOf(filter) + 1) },
                    transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                )
            }
            if (groups.isEmpty()) item { Text(stringResource(R.string.timeline_empty), color = CmColors.text2, modifier = Modifier) }
            for (g in groups) {
                item { ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) { Text(g.day) } }
                for (row in g.rows) item { Text(row, style = MaterialTheme.typography.bodyMedium, color = CmColors.text, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
}
