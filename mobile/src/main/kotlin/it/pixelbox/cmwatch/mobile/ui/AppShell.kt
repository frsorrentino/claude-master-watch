package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.StartRoute.Tab
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Tre schede in basso, Panoramica, Sessioni e Diario (restyling 30/09), la fascia «Demo» fissa quando la Demo è accesa
 * (design 29/09). In alto, al posto del titolo, il menu delle sessioni (Franz, 30/09 20:38: più spazio): mostra la
 * sessione aperta o «Tutte le sessioni», e sceglierne una apre la sua scheda; ⚙ a destra. Con una scheda aperta le
 * schede in basso spariscono, così la barra di scrittura sta sopra la tastiera. `fab`: il bottone mobile di «Lancia».
 */
@Composable
fun AppShell(
    tab: Tab, demo: Boolean, onTab: (Tab) -> Unit, onSettings: () -> Unit, fab: @Composable () -> Unit = {},
    sessions: List<Session> = emptyList(), current: String? = null, onPick: (String?) -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = CmColors.bg,
        floatingActionButton = fab,
        topBar = {
            Column(Modifier.background(CmColors.bg).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SessionMenu(sessions, current, onPick, Modifier.weight(1f))
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
            if (current == null) NavigationBar(containerColor = CmColors.surfaceLow) {
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
    ) { pad -> Box(Modifier.padding(pad).consumeWindowInsets(pad).fillMaxSize()) { content() } }
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

/** Il menu delle sessioni in alto: la sessione aperta o «Tutte le sessioni»; le voci con il badge, nell'ordine della regia. */
@Composable
private fun SessionMenu(
    sessions: List<Session>, current: String?, onPick: (String?) -> Unit, modifier: Modifier = Modifier,
) {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val cur = sessions.firstOrNull { it.name == current }
    Box(modifier) {
        Row(
            Modifier.clip(CircleShape).clickable { open = true }.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            cur?.let { SessionBadge(it, size = 18.dp) }
            Text(
                cur?.name ?: stringResource(R.string.all_sessions), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = CmColors.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Icons.Rounded.ArrowDropDown, null, tint = CmColors.text2)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = CmColors.surface) {
            if (current != null) DropdownMenuItem(
                text = { Text(stringResource(R.string.all_sessions)) }, onClick = { open = false; onPick(null) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.List, null, tint = CmColors.text2) },
            )
            sessions.filter { it.state != SessionState.GONE }.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.name, fontWeight = if (s.name == current) FontWeight.SemiBold else FontWeight.Normal) },
                    leadingIcon = { SessionBadge(s, size = 20.dp) }, onClick = { open = false; onPick(s.name) },
                )
            }
        }
    }
}
