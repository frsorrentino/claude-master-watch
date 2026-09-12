package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
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
import it.pixelbox.cmwatch.contract.Night
import it.pixelbox.cmwatch.contract.Recap
import it.pixelbox.cmwatch.rules.RecapText
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.roundListPadding

/** Recap del giorno: «progetto · fatto» e «→ prossimo». */
@Composable
fun RecapScreen(recap: Recap) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val rows = RecapText.rows(recap)
    ScreenScaffold(scrollState = listState, contentPadding = roundListPadding()) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) { Text(stringResource(R.string.recap_title, recap.date)) } }
            if (rows.isEmpty()) item { Text(stringResource(R.string.recap_empty), color = CmColors.text2, modifier = Modifier) }
            for (r in rows) {
                item { Text(r.done, style = MaterialTheme.typography.bodyMedium, color = CmColors.text, modifier = Modifier.fillMaxWidth()) }
                r.next?.let { n -> item { Text(n, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, modifier = Modifier.fillMaxWidth()) } }
            }
        }
    }
}

/** Notte: coda e sessione in corso. */
@Composable
fun NightScreen(night: Night) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = listState, contentPadding = roundListPadding()) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) { Text(stringResource(R.string.night_title)) } }
            item { Text(stringResource(R.string.night_queued, night.queued), style = MaterialTheme.typography.bodyMedium, color = CmColors.text, modifier = Modifier.fillMaxWidth()) }
            item { Text(stringResource(R.string.night_running, night.running ?: stringResource(R.string.quota_none)), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, modifier = Modifier.fillMaxWidth()) }
        }
    }
}
