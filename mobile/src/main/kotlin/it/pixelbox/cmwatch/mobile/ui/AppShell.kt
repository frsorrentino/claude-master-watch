package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.ui.tokens.CmColors

enum class Tab { SESSIONS, DIARY }

/** Due schede in basso, ⚙ in alto, la fascia «Demo» fissa quando la Demo è accesa (design 29/09). */
@Composable
fun AppShell(tab: Tab, demo: Boolean, onTab: (Tab) -> Unit, onSettings: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        containerColor = CmColors.bg,
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
