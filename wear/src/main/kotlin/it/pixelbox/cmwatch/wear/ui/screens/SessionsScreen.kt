package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.data.Snapshot
import it.pixelbox.cmwatch.rules.Screen
import it.pixelbox.cmwatch.wear.ui.components.SessionRow
import it.pixelbox.cmwatch.wear.ui.components.StaleChip
import it.pixelbox.cmwatch.wear.ui.components.WideButton
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.roundListPadding

/** Lista Sessioni: ordine ❓ ▶ ✓ ✗ (già nel Repo), chip «PC fermo» solo se serve, Impostazioni in fondo. */
@Composable
fun SessionsScreen(snapshot: Snapshot, now: Long, onOpen: (String) -> Unit, onSettings: () -> Unit, onMenu: (Screen) -> Unit = {}) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val sessions = snapshot.state?.sessions.orEmpty()
    val fresh = snapshot.freshness is Freshness.Fresh
    ScreenScaffold(scrollState = listState, contentPadding = roundListPadding()) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item {
                ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) {
                    Text(stringResource(R.string.sessions_title))
                }
            }
            (snapshot.freshness as? Freshness.Stale)?.let { st ->
                item { StaleChip(st.minutes, Modifier) }
            }
            if (sessions.isEmpty()) {
                item { Text(stringResource(R.string.sessions_empty), color = CmColors.text2, modifier = Modifier) }
            }
            items(sessions, key = { it.id }) { s ->
                SessionRow(s, now, fresh, onClick = { onOpen(s.name) }, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec))
            }
            val menu = listOf(
                R.string.timeline_title to Screen.Timeline, R.string.launch_title to Screen.Launch, R.string.quota_title to Screen.Quota,
                R.string.recap_short to Screen.Recap, R.string.night_title to Screen.Night,
            )
            for ((label, screen) in menu) item {
                WideButton(stringResource(label), onClick = { onMenu(screen) }, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec))
            }
            item {
                WideButton(stringResource(R.string.settings_title), onClick = onSettings, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec))
            }
        }
    }
}
