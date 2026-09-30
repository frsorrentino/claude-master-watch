package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.StartRoute.Tab
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Tre schede in basso, Panoramica, Sessioni e Diario (restyling 30/09), ⚙ in alto, la fascia «Demo» fissa quando la Demo
 * è accesa (design 29/09). `fab`: il bottone mobile con il menu di «Lancia», solo dove serve.
 */
@Composable
fun AppShell(
    tab: Tab, demo: Boolean, onTab: (Tab) -> Unit, onSettings: () -> Unit, fab: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = CmColors.bg,
        floatingActionButton = fab,
        topBar = {
            Column(Modifier.background(CmColors.bg).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.app_title), style = MaterialTheme.typography.titleLarge, color = CmColors.text, modifier = Modifier.weight(1f))
                    IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = CmColors.actionIcon) }
                }
                if (demo) {
                    Text(
                        stringResource(R.string.demo_banner), color = CmColors.briefWarnInk, style = MaterialTheme.typography.labelLarge,
                        maxLines = 1, overflow = TextOverflow.Clip,
                        modifier = Modifier.fillMaxWidth().background(CmColors.briefWarn).padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = CmColors.surfaceLow) {
                NavigationBarItem(
                    selected = tab == Tab.OVERVIEW, onClick = { onTab(Tab.OVERVIEW) },
                    icon = { Icon(Icons.Rounded.Dashboard, null) }, label = { Text(stringResource(R.string.tab_overview)) },
                )
                NavigationBarItem(
                    selected = tab == Tab.SESSIONS, onClick = { onTab(Tab.SESSIONS) },
                    icon = { Icon(Icons.AutoMirrored.Rounded.List, null) }, label = { Text(stringResource(R.string.tab_sessions)) },
                )
                NavigationBarItem(
                    selected = tab == Tab.DIARY, onClick = { onTab(Tab.DIARY) },
                    icon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, null) }, label = { Text(stringResource(R.string.tab_diary)) },
                )
            }
        },
    ) { pad -> Box(Modifier.padding(pad).fillMaxSize()) { content() } }
}

/**
 * Il bottone mobile di Panoramica e Sessioni (restyling 30/09): si apre in un menu con «Lancia» e «Aggiungi alla notte»,
 * al posto del bottone pieno in fondo alla regia. `startOpen` per gli snapshot.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LaunchFab(onLaunch: () -> Unit, onNight: (() -> Unit)?, startOpen: Boolean = false) {
    var open by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(startOpen) }
    androidx.activity.compose.BackHandler(enabled = open) { open = false }
    FloatingActionButtonMenu(
        expanded = open,
        button = {
            ToggleFloatingActionButton(
                checked = open, onCheckedChange = { open = it },
                containerColor = ToggleFloatingActionButtonDefaults.containerColor(CmColors.primary, CmColors.primary),
            ) {
                val icon = if (checkedProgress > 0.5f) Icons.Rounded.Close else Icons.Rounded.Add
                Icon(icon, stringResource(R.string.launch), tint = CmColors.onPrimary)
            }
        },
    ) {
        FloatingActionButtonMenuItem(
            onClick = { open = false; onLaunch() }, text = { Text(stringResource(R.string.launch)) },
            icon = { Icon(Icons.Rounded.RocketLaunch, null) },
            containerColor = CmColors.primary, contentColor = CmColors.onPrimary,
        )
        if (onNight != null) FloatingActionButtonMenuItem(
            onClick = { open = false; onNight() }, text = { Text(stringResource(R.string.night_add)) },
            icon = { Icon(Icons.Rounded.Bedtime, null) },
            containerColor = CmColors.primary, contentColor = CmColors.onPrimary,
        )
    }
}
