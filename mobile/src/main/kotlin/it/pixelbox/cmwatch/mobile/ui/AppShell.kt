package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
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
import it.pixelbox.cmwatch.rules.StartRoute
import it.pixelbox.cmwatch.rules.StartRoute.Tab
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Il guscio del riepilogo unico (design 03/10): niente schede in basso. Senza scheda aperta, in alto «Sessioni · N
 * aperte», la lente e il menu ≡, sotto la quota in una riga; con una scheda aperta il menu delle sessioni al posto del
 * titolo. Il Registro (`Tab.DIARY`) si apre dal menu a tutto schermo, con la freccia indietro. La fascia «Demo» resta.
 */
@Composable
fun AppShell(
    tab: Tab, demo: Boolean, onTab: (Tab) -> Unit, onSettings: () -> Unit,
    sessions: List<Session> = emptyList(), current: String? = null, onPick: (String?) -> Unit = {},
    /** La lente accanto al menu; null = niente lente. */
    onSearch: (() -> Unit)? = null,
    /** «Quadro e quota» nel menu: il foglio della Panoramica. */
    onQuadro: () -> Unit = {},
    /** Tirare giù aggiorna lo stato dal PC (segnalazione 01/10 21:08); null = niente gesto, come con una scheda aperta. */
    onRefresh: (() -> Unit)? = null, refreshing: Boolean = false,
    /** «Lancia una sessione» nel menu (il bottone mobile non c'è più). */
    onLaunch: () -> Unit = {},
    /** Il collegamento in testa al menu: il PC, «aggiornato ora» o «… min fa», ambra se fermo. */
    host: String? = null, updated: String = "", stale: Boolean = false,
    /** Le sessioni aperte, per «N aperte» accanto al titolo. */
    openCount: Int = 0,
    /** La quota in una riga sotto la barra, solo sul riepilogo. */
    quota: (@Composable () -> Unit)? = null,
    /** Solo per i provini: il menu già aperto. */
    menuStartOpen: Boolean = false,
    content: @Composable (Tab) -> Unit,
) {
    val summary = current == null && tab == Tab.OVERVIEW
    Scaffold(
        containerColor = CmColors.bg,
        topBar = {
            Column(Modifier.background(CmColors.bg).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    when {
                        current != null -> SessionMenu(sessions, current, onPick, Modifier.weight(1f))
                        tab == Tab.DIARY -> {
                            IconButton(onClick = { onTab(Tab.OVERVIEW) }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = CmColors.text) }
                            Text(stringResource(R.string.menu_register), style = MaterialTheme.typography.titleLarge, color = CmColors.text, modifier = Modifier.weight(1f))
                        }
                        else -> Row(Modifier.weight(1f).padding(start = 12.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.titleLarge, color = CmColors.text)
                            Text(
                                stringResource(R.string.summary_open, openCount), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2,
                                maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.padding(bottom = 2.dp),
                            )
                        }
                    }
                    onSearch?.let { IconButton(onClick = it) { Icon(Icons.Rounded.Search, stringResource(R.string.search), tint = CmColors.text2) } }
                    AppMenu(host, updated, stale, onLaunch, onRegister = { onTab(Tab.DIARY) }, onQuadro, onSearch ?: {}, onSettings, menuStartOpen)
                }
                if (summary) quota?.let { Box(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)) { it() } }
                if (demo) {
                    Text(
                        stringResource(R.string.demo_banner), color = CmColors.briefWarnInk, style = MaterialTheme.typography.labelLarge,
                        maxLines = 1, overflow = TextOverflow.Clip,
                        modifier = Modifier.fillMaxWidth().background(CmColors.briefWarn).padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).consumeWindowInsets(pad).fillMaxSize()) {
            // Sempre lo stesso contenitore, spento con una scheda aperta: cambiarlo ricreava il contenuto a metà del gesto
            // indietro e la scheda si riapriva al rilascio (segnalazioni 01/10 21:50 e 23:04).
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                refreshing, onRefresh ?: {}, Modifier.fillMaxSize(), enabled = onRefresh != null,
            ) { content(tab) }
        }
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
            // La master staccata in testa, con l'icona della sua scheda (Franz, 02/10 07:03).
            val master = sessions.firstOrNull { s -> s.name == it.pixelbox.cmwatch.rules.ContextActions.MASTER && s.state != SessionState.GONE }
            master?.let { m ->
                DropdownMenuItem(
                    text = { Text(m.name, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Rounded.Person, null, tint = CmColors.actionIcon) },
                    trailingIcon = { SessionBadge(m, size = 16.dp) }, onClick = { open = false; onPick(m.name) },
                )
                HorizontalDivider(color = CmColors.line)
            }
            if (current != null) DropdownMenuItem(
                text = { Text(stringResource(R.string.all_sessions)) }, onClick = { open = false; onPick(null) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.List, null, tint = CmColors.text2) },
            )
            sessions.filter { it.state != SessionState.GONE && it != master }.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.name, fontWeight = if (s.name == current) FontWeight.SemiBold else FontWeight.Normal) },
                    leadingIcon = { SessionBadge(s, size = 20.dp) }, onClick = { open = false; onPick(s.name) },
                )
            }
        }
    }
}

/** Il menu ≡ dell'app: apre il pannello (`AppMenuPanel`) al posto della vecchia tendina. */
@Composable
private fun AppMenu(
    host: String?, updated: String, stale: Boolean, onLaunch: () -> Unit, onRegister: () -> Unit, onQuadro: () -> Unit,
    onSearch: () -> Unit, onSettings: () -> Unit, startOpen: Boolean,
) {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(startOpen) }
    IconButton(onClick = { open = true }) { Icon(Icons.Rounded.Menu, stringResource(R.string.menu), tint = CmColors.actionIcon) }
    if (open) AppMenuPanel(host, updated, stale, onLaunch, onRegister, onQuadro, onSearch, onSettings, onDismiss = { open = false })
}
