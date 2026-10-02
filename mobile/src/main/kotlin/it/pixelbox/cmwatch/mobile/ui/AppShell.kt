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
 * Tre schede in basso, Panoramica, Sessioni e Diario (restyling 30/09), la fascia «Demo» fissa quando la Demo è accesa
 * (design 29/09). In alto, al posto del titolo, il menu delle sessioni (Franz, 30/09 20:38: più spazio): mostra la
 * sessione aperta o «Tutte le sessioni», e sceglierne una apre la sua scheda; ⚙ a destra. Con una scheda aperta le
 * schede in basso spariscono, così la barra di scrittura sta sopra la tastiera. `fab`: il bottone mobile di «Lancia».
 */
@Composable
fun AppShell(
    tab: Tab, demo: Boolean, onTab: (Tab) -> Unit, onSettings: () -> Unit, fab: @Composable () -> Unit = {},
    sessions: List<Session> = emptyList(), current: String? = null, onPick: (String?) -> Unit = {},
    /** La ricerca (piano 30/09, Task 5): la lente accanto al menu; null = niente lente. */
    onSearch: (() -> Unit)? = null,
    /** Lo scorrimento laterale cambia scheda; spento con una scheda sessione aperta, dove scorre fra le sessioni. */
    swipeTabs: Boolean = false,
    /** La casa della master (design 01/10): il nome della master nel menu in alto quando nessuna scheda è aperta. */
    home: String? = null,
    /** «Quadro» nel menu ≡: il foglio della Panoramica (la Panoramica non è più una scheda). */
    onQuadro: () -> Unit = {},
    /** Tirare giù aggiorna lo stato dal PC (segnalazione 01/10 21:08); null = niente gesto, come con una scheda aperta. */
    onRefresh: (() -> Unit)? = null, refreshing: Boolean = false,
    /** Il contenuto di una scheda: con lo scorrimento si vedono due schede insieme, ognuna disegnata per la sua. */
    content: @Composable (Tab) -> Unit,
) {
    Scaffold(
        containerColor = CmColors.bg,
        floatingActionButton = fab,
        topBar = {
            Column(Modifier.background(CmColors.bg).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SessionMenu(sessions, current ?: home, onPick, Modifier.weight(1f))
                    // Al posto di ⚙ il menu dell'app (Franz, 30/09 23:04): con una scheda aperta le schede in basso non ci sono.
                    onSearch?.let { IconButton(onClick = it) { Icon(Icons.Rounded.Search, stringResource(R.string.search), tint = CmColors.text2) } }
                    AppMenu(onTab, onSettings, onQuadro)
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
                    // La prima scheda è la casa della master (design 01/10): il valore resta `Tab.OVERVIEW` per non cambiare lo stato salvato.
                    icon = { Icon(Icons.Rounded.Person, null) }, label = { Text(stringResource(R.string.tab_master)) },
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
    ) { pad ->
        // Scorrere a sinistra o a destra passa fra Master, Sessioni e Diario seguendo il dito, come fra le sessioni
        // (segnalazione 02/10 06:54: prima la scheda scattava solo al rilascio). Con una scheda aperta il pager resta
        // fermo e lo scorrimento è quello fra le sessioni.
        val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = tab.ordinal) { Tab.entries.size }
        androidx.compose.runtime.LaunchedEffect(pager.settledPage) { Tab.entries[pager.settledPage].takeIf { it != tab }?.let(onTab) }
        androidx.compose.runtime.LaunchedEffect(tab) { if (pager.targetPage != tab.ordinal) pager.animateScrollToPage(tab.ordinal) }
        Box(Modifier.padding(pad).consumeWindowInsets(pad).fillMaxSize()) {
            // Sempre lo stesso contenitore, spento con una scheda aperta: cambiarlo ricreava il contenuto a metà del gesto
            // indietro e la scheda si riapriva al rilascio (segnalazioni 01/10 21:50 e 23:04).
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                refreshing, onRefresh ?: {}, Modifier.fillMaxSize(), enabled = onRefresh != null,
            ) {
                androidx.compose.foundation.pager.HorizontalPager(
                    pager, Modifier.fillMaxSize(), userScrollEnabled = swipeTabs, beyondViewportPageCount = 0, key = { Tab.entries[it] },
                ) { page -> content(Tab.entries[page]) }
            }
        }
    }
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

/** Il menu dell'app in alto a destra: Panoramica, Sessioni, Diario, Impostazioni (poi la Scrivania). */
@Composable
private fun AppMenu(onTab: (Tab) -> Unit, onSettings: () -> Unit, onQuadro: () -> Unit) {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Rounded.Menu, stringResource(R.string.menu), tint = CmColors.actionIcon) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = CmColors.surface) {
            DropdownMenuItem(text = { Text(stringResource(R.string.tab_master)) }, leadingIcon = { Icon(Icons.Rounded.Person, null) }, onClick = { open = false; onTab(Tab.OVERVIEW) })
            DropdownMenuItem(text = { Text(stringResource(R.string.quadro_title)) }, leadingIcon = { Icon(Icons.Rounded.Dashboard, null) }, onClick = { open = false; onQuadro() })
            DropdownMenuItem(text = { Text(stringResource(R.string.tab_sessions)) }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.List, null) }, onClick = { open = false; onTab(Tab.SESSIONS) })
            DropdownMenuItem(text = { Text(stringResource(R.string.tab_diary)) }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, null) }, onClick = { open = false; onTab(Tab.DIARY) })
            HorizontalDivider(color = CmColors.line)
            DropdownMenuItem(text = { Text(stringResource(R.string.settings)) }, leadingIcon = { Icon(Icons.Rounded.Settings, null) }, onClick = { open = false; onSettings() })
        }
    }
}
