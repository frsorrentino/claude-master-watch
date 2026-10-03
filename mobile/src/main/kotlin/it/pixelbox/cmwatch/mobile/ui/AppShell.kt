package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
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
    /** La master espansa a tutta pagina nella home (Franz, 03/10 16:44): la quota sotto la barra si toglie per farle spazio. */
    masterChat: Boolean = false,
    /** Per l'età delle sessioni nel menu in alto; «Chiuse · N» del menu apre l'elenco delle chiuse. */
    now: Long = 0, onClosed: () -> Unit = {},
    /** Solo per i provini: il menu già aperto, o quello delle sessioni. */
    menuStartOpen: Boolean = false, sessionMenuStartOpen: Boolean = false,
    /**
     * Ogni pagina della home e delle sessioni disegna la sua testata (`PageHeader`), che scorre con lei (Franz, 03/10
     * 19:19: con la testata unica, allo scorrimento si vedeva uno scatto). Qui restano la barra di stato e la fascia Demo.
     */
    pagedHeaders: Boolean = false,
    content: @Composable (Tab) -> Unit,
) {
    val summary = current == null && tab == Tab.OVERVIEW
    val menu = MenuActions(host, updated, stale, onLaunch, onRegister = { onTab(Tab.DIARY) }, onQuadro, onSearch, onSettings)
    Scaffold(
        containerColor = CmColors.bg,
        topBar = {
            Column(Modifier.background(CmColors.bg).statusBarsPadding()) {
                when {
                    pagedHeaders && tab == Tab.OVERVIEW -> {}
                    tab == Tab.DIARY && current == null -> Row(Modifier.fillMaxWidth().height(56.dp).padding(start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onTab(Tab.OVERVIEW) }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = CmColors.text) }
                        Text(stringResource(R.string.menu_register), style = MaterialTheme.typography.titleLarge, color = CmColors.text, modifier = Modifier.weight(1f))
                        AppMenu(menu, menuStartOpen)
                    }
                    else -> PageHeader(
                        current, sessions, openCount, now, onPick, onClosed, menu, quota = quota, showQuota = summary && !masterChat,
                        menuStartOpen = menuStartOpen, sessionMenuStartOpen = sessionMenuStartOpen,
                    )
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

/**
 * Il menu delle sessioni in alto (Franz, 03/10 16:44, variante A): il nome della sessione aperta apre un pannello come il
 * menu ≡. In testa la home («Master»), poi le sessioni nei gruppi del riepilogo con stato, età e contesto, la sessione
 * aperta evidenziata con ✓; in fondo «Chiuse · N». La master non c'è: sta nella home.
 */
@Composable
private fun SessionMenu(
    sessions: List<Session>, current: String?, now: Long, onPick: (String?) -> Unit, onClosed: () -> Unit, modifier: Modifier = Modifier,
    startOpen: Boolean = false,
) {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(startOpen) }
    val cur = sessions.firstOrNull { it.name == current }
    Box(modifier) {
        Row(
            Modifier.clip(CircleShape).clickable { open = true }.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            cur?.let { SessionBadge(it, size = 18.dp) }
            Text(
                cur?.name ?: stringResource(R.string.all_sessions), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Icons.Rounded.ArrowDropDown, null, tint = CmColors.text2)
        }
    }
    if (open) {
        val model = androidx.compose.runtime.remember(sessions) { it.pixelbox.cmwatch.rules.SessionsMenu.of(sessions) }
        val pick: (String?) -> Unit = { n -> open = false; onPick(n) }
        PanelShell(onDismiss = { open = false }, header = {
            Text(stringResource(R.string.menu_goto), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, modifier = Modifier.weight(1f))
        }) {
            // La home in testa: il riepilogo con la master.
            Row(
                Modifier.fillMaxWidth().clickable { pick(null) }.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(CmColors.accent.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Home, null, tint = CmColors.actionIcon, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.titleMedium, color = CmColors.text)
                    Text(stringResource(R.string.menu_home_sub), style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1, overflow = TextOverflow.Clip)
                }
            }
            model.groups.forEach { (group, list) ->
                val tone = when (group) {
                    it.pixelbox.cmwatch.rules.Summary.Group.WAITING -> CmColors.briefWarn
                    it.pixelbox.cmwatch.rules.Summary.Group.WORKING -> CmColors.briefRing
                    else -> CmColors.text2
                }
                Text(
                    stringResource(when (group) {
                        it.pixelbox.cmwatch.rules.Summary.Group.WAITING -> R.string.summary_waiting
                        it.pixelbox.cmwatch.rules.Summary.Group.WORKING -> R.string.summary_working
                        else -> R.string.summary_still
                    }, list.size).uppercase(),
                    style = MonoSmall.copy(color = tone), modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 2.dp),
                )
                list.forEach { s -> SessionMenuRow(s, group, now, s.name == current) { pick(s.name) } }
            }
            if (model.closed > 0) {
                HorizontalDivider(color = CmColors.line, modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { open = false; onClosed() }.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(Icons.Rounded.Close, null, tint = CmColors.text2, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.summary_closed, model.closed), style = MaterialTheme.typography.bodyLarge, color = CmColors.text2, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = CmColors.text2)
                }
            }
        }
    }
}

/** Una sessione nel menu: badge, nome, «domanda · 5 m» / «al lavoro · 1 m» / «ferma da 1 g», contesto; ✓ sulla aperta. */
@Composable
private fun SessionMenuRow(s: Session, group: it.pixelbox.cmwatch.rules.Summary.Group, now: Long, current: Boolean, onClick: () -> Unit) {
    val since: (Long) -> String = { t -> it.pixelbox.cmwatch.contract.Durations.since(t, now) }
    val status = when (group) {
        it.pixelbox.cmwatch.rules.Summary.Group.WAITING -> stringResource(R.string.menu_row_waiting, since(s.question?.askedAt ?: s.since))
        it.pixelbox.cmwatch.rules.Summary.Group.WORKING -> stringResource(R.string.menu_row_working, since(s.turnStarted ?: s.since))
        else -> stringResource(R.string.menu_row_still, since(s.since))
    }
    Row(
        Modifier.fillMaxWidth().background(if (current) CmColors.primary.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SessionBadge(s, size = 24.dp)
        Column(Modifier.weight(1f)) {
            Text(s.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip)
            if (now > 0) Text(status, style = MaterialTheme.typography.bodySmall, color = if (group == it.pixelbox.cmwatch.rules.Summary.Group.WAITING) CmColors.briefWarn else CmColors.text2, maxLines = 1, overflow = TextOverflow.Clip)
        }
        s.context?.let { Text(stringResource(R.string.ctx_short, it), style = MonoSmall) }
        if (current) Icon(Icons.Rounded.Check, null, tint = CmColors.actionIcon, modifier = Modifier.size(18.dp))
    }
}

/** Le voci del menu ≡ e del collegamento in testa, le stesse in ogni testata. */
data class MenuActions(
    val host: String?, val updated: String, val stale: Boolean, val onLaunch: () -> Unit, val onRegister: () -> Unit,
    val onQuadro: () -> Unit, val onSearch: (() -> Unit)?, val onSettings: () -> Unit,
)

/**
 * La testata di una pagina (Franz, 03/10 19:19): della home («Master · N aperte», lente, ≡ e sotto la quota) o di una
 * sessione (←, il menu delle sessioni con il suo nome, ≡). Ogni pagina la porta con sé, così scorre e vola insieme a lei.
 */
@Composable
fun PageHeader(
    page: String?, sessions: List<Session>, openCount: Int, now: Long, onPick: (String?) -> Unit, onClosed: () -> Unit,
    menu: MenuActions, quota: (@Composable () -> Unit)? = null, showQuota: Boolean = false,
    menuStartOpen: Boolean = false, sessionMenuStartOpen: Boolean = false,
) {
    Column(Modifier.fillMaxWidth().background(CmColors.bg)) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (page != null) {
                // Dalla pagina di una sessione un tocco riporta al riepilogo, dove sta la master (Franz, 03/10 15:25).
                IconButton(onClick = { onPick(null) }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back_to_summary), tint = CmColors.text) }
                SessionMenu(sessions, page, now, onPick, onClosed, Modifier.weight(1f), startOpen = sessionMenuStartOpen)
            } else {
                Row(Modifier.weight(1f).padding(start = 12.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.titleLarge, color = CmColors.text)
                    Text(
                        stringResource(R.string.summary_open, openCount), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2,
                        maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
                // La lente solo nella home: la ricerca è di tutte le sessioni, e in una sessione la testa ha già abbastanza comandi.
                menu.onSearch?.let { IconButton(onClick = it) { Icon(Icons.Rounded.Search, stringResource(R.string.search), tint = CmColors.text2) } }
            }
            AppMenu(menu, menuStartOpen)
        }
        if (page == null && showQuota) quota?.let { Box(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)) { it() } }
    }
}

/** Il menu ≡ dell'app: apre il pannello (`AppMenuPanel`) al posto della vecchia tendina. */
@Composable
private fun AppMenu(menu: MenuActions, startOpen: Boolean) {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(startOpen) }
    IconButton(onClick = { open = true }) { Icon(Icons.Rounded.Menu, stringResource(R.string.menu), tint = CmColors.actionIcon) }
    if (open) AppMenuPanel(menu.host, menu.updated, menu.stale, menu.onLaunch, menu.onRegister, menu.onQuadro, menu.onSearch ?: {}, menu.onSettings, onDismiss = { open = false })
}
