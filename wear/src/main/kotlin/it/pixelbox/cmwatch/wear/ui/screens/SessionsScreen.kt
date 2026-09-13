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
import it.pixelbox.cmwatch.rules.ToolText
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.data.Snapshot
import it.pixelbox.cmwatch.rules.Screen
import it.pixelbox.cmwatch.wear.ui.components.SessionRow
import it.pixelbox.cmwatch.wear.ui.components.StaleChip
import it.pixelbox.cmwatch.wear.ui.components.MenuButton
import it.pixelbox.cmwatch.wear.ui.components.WideButton
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.rememberCenterIndex
import it.pixelbox.cmwatch.wear.ui.ambient.animationsOff
import it.pixelbox.cmwatch.wear.ui.theme.roundListPadding

/** Lista Sessioni: ordine ❓ ▶ ✓ ✗ (già nel Repo), chip «PC fermo» solo se serve, Impostazioni in fondo. */
@Composable
fun SessionsScreen(snapshot: Snapshot, now: Long, onOpen: (String) -> Unit, onSettings: () -> Unit, onMenu: (Screen) -> Unit = {}, ambient: Boolean = false) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val sessions = snapshot.state?.sessions.orEmpty()
    val fresh = snapshot.freshness is Freshness.Fresh
    // Scorre solo la riga al centro, e solo se il sistema permette le animazioni.
    val center = rememberCenterIndex(listState)
    val canScroll = !ambient && !animationsOff()
    val stale = snapshot.freshness as? Freshness.Stale
    val firstRow = 1 + (if (stale != null) 1 else 0)
    val tools = ToolText.Labels(
        run = stringResource(R.string.tool_run), read = stringResource(R.string.tool_read),
        edit = stringResource(R.string.tool_edit), write = stringResource(R.string.tool_write),
        search = stringResource(R.string.tool_search), web = stringResource(R.string.tool_web),
        message = stringResource(R.string.tool_message), delegate = stringResource(R.string.tool_delegate),
        plan = stringResource(R.string.tool_plan), other = stringResource(R.string.tool_other),
    )
    ScreenScaffold(scrollState = listState, contentPadding = roundListPadding(sides = 0.052f)) { padding ->
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
            val names = sessions.map { it.name }
            items(count = sessions.size, key = { sessions[it].id }) { i ->
                val s = sessions[i]
                SessionRow(
                    s, now, fresh, onClick = { onOpen(s.name) }, transformation = SurfaceTransformation(spec),
                    modifier = Modifier.transformedHeight(this, spec), siblings = names, ambient = ambient,
                    marquee = canScroll && center == firstRow + i, tools = tools,
                )
            }
            // Un solo tasto, diverso dalle righe delle sessioni: apre il Menu (Franz, 12/09 15:35). In ambient sparisce.
            if (!ambient) item { MenuButton(onClick = { onMenu(Screen.Menu) }, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) }
        }
    }
}
