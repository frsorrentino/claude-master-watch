package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
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
import it.pixelbox.cmwatch.wear.ui.components.WideButton
import it.pixelbox.cmwatch.wear.ui.components.CmEdgeButton
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.rememberCenterIndex
import it.pixelbox.cmwatch.wear.ui.ambient.animationsOff
import it.pixelbox.cmwatch.wear.ui.theme.morph

/** Lista Sessioni: le aperte in ordine ❓ ▶ ✓ (già nel Repo), le chiuse dietro un tasto; in fondo Panoramica, Impostazioni e Nuova sessione. */
@Composable
fun SessionsScreen(snapshot: Snapshot, now: Long, onOpen: (String) -> Unit, onSettings: () -> Unit, onMenu: (Screen) -> Unit = {}, ambient: Boolean = false, onReopen: (String) -> Unit = {}, onFollow: (String, Boolean) -> Unit = { _, _ -> }, reopenStatus: (String) -> it.pixelbox.cmwatch.rules.ReopenText.Status? = { null }) {
    val listState = rememberTransformingLazyColumnState()
    it.pixelbox.cmwatch.wear.ui.components.DemoScroll(listState)   // solo per i video promozionali, via adb
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
    var mostraChiuse by rememberSaveable { mutableStateOf(false) }
    val riga: @Composable TransformingLazyColumnItemScope.(Session, Int, List<String>) -> Unit = { s, i, names ->
        SessionRow(
            s, now, fresh, onClick = { onOpen(s.name) }, transformation = SurfaceTransformation(spec),
            // Quando una sessione sale per una domanda o scende a fine lavoro, la card scivola al suo posto (A5, 15/09).
            modifier = Modifier.transformedHeight(this, spec).animateItem(), siblings = names, ambient = ambient,
            marquee = canScroll && center == firstRow + i, tools = tools,
            onReopen = if (s.state == SessionState.GONE) ({ onReopen(s.name) }) else null,
            reopen = reopenStatus(s.name),
            onLongClick = if (s.state != SessionState.GONE) ({ onFollow(s.name, !s.followed) }) else null,
        )
    }
    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item {
                ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) {
                    Text(stringResource(R.string.sessions_title))
                }
            }
            (snapshot.freshness as? Freshness.Stale)?.let { st ->
                item { StaleChip(st.minutes, Modifier.morph(this, spec)) }
            }
            // Solo le sessioni aperte (che lavorano, aspettano o sono ferme al prompt); le chiuse dietro un tasto, con il loro
            // numero (Franz, 16/09 14:33). Una ferma resta: è viva e le si può scrivere.
            val aperte = sessions.filter { it.state != SessionState.GONE }
            val chiuse = sessions.filter { it.state == SessionState.GONE }.sortedByDescending { it.since }
            if (sessions.isEmpty()) {
                item { Text(stringResource(R.string.sessions_empty), color = CmColors.text2, modifier = Modifier.morph(this, spec)) }
            } else if (aperte.isEmpty()) {
                item { Text(stringResource(R.string.sessions_none_open), color = CmColors.text2, modifier = Modifier.morph(this, spec)) }
            }
            val names = sessions.map { it.name }
            items(count = aperte.size, key = { aperte[it].id }) { i -> riga(aperte[i], i, names) }
            if (chiuse.isNotEmpty() && !ambient) {
                item {
                    WideButton(
                        if (mostraChiuse) stringResource(R.string.sessions_gone_hide) else stringResource(R.string.sessions_gone_show, chiuse.size),
                        onClick = { mostraChiuse = !mostraChiuse }, icon = if (mostraChiuse) Icons.Rounded.ExpandLess else Icons.Rounded.History,
                        transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                    )
                }
            }
            if (mostraChiuse || ambient) {
                items(count = chiuse.size, key = { chiuse[it].id }) { i -> riga(chiuse[i], aperte.size + i, names) }
            }
            if (!ambient) {
                // Al posto della pagina Menu (Franz, 16/09 14:33): le due voci rimaste stanno qui, e «Nuova sessione» è il
                // solo tasto pieno, curvo sul bordo come ultimo elemento. Lo slot `edgeButton` dello scaffold, in questa
                // versione della libreria, non disegnava niente (provato al polso, 14/09 07:49).
                item {
                    WideButton(
                        stringResource(R.string.overview_title), onClick = { onMenu(Screen.Quota) }, icon = Icons.Rounded.Insights,
                        transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                    )
                }
                item {
                    WideButton(
                        stringResource(R.string.settings_title), onClick = onSettings, icon = Icons.Rounded.Settings,
                        transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                    )
                }
                item { CmEdgeButton(stringResource(R.string.launch_new), onClick = { onMenu(Screen.Launch) }) }
            }
        }
    }
}
