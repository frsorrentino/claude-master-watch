package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** Una voce dei menu a pannello: icona su fondo tonale, titolo e una riga che spiega cosa fa. */
data class MenuEntry(val icon: ImageVector, val title: String, val sub: String, val accent: Boolean = false, val onClick: () -> Unit)

/**
 * Il menu a pannello (design 03/10, tavola 5), condiviso dal menu dell'app e da quello della sessione: un pannello largo
 * sotto la barra sopra lo sfondo oscurato; in testa una riga con ✕; voci grandi; in fondo, separata, una voce di servizio.
 * Tocco fuori, ✕ o Indietro lo chiudono; ogni voce lo chiude prima di agire.
 */
@Composable
fun MenuPanel(
    onDismiss: () -> Unit, entries: List<MenuEntry>, footer: MenuEntry? = null,
    header: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) = PanelShell(onDismiss, header) {
    entries.forEach { e -> MenuItem(e.icon, e.title, e.sub, if (e.accent) CmColors.accent.copy(alpha = 0.25f) else CmColors.surfaceHigh) { onDismiss(); e.onClick() } }
    footer?.let { f ->
        HorizontalDivider(color = CmColors.line, modifier = Modifier.padding(vertical = 4.dp))
        Row(
            Modifier.fillMaxWidth().clickable { onDismiss(); f.onClick() }.padding(horizontal = 28.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Icon(f.icon, null, tint = CmColors.text2)
            Text(f.title, style = MaterialTheme.typography.bodyLarge, color = CmColors.text2)
        }
    }
}

/**
 * Il guscio dei pannelli: sfondo oscurato a tutto schermo (tocco = chiudi), pannello largo sotto la barra con angoli da
 * 28 dp, in testa una riga con ✕; il contenuto scorre se è più alto dello schermo. Indietro chiude.
 */
@Composable
fun PanelShell(
    onDismiss: () -> Unit, header: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Popup(onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp), color = CmColors.surface, shadowElevation = 8.dp,
                modifier = Modifier.statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 56.dp, bottom = 24.dp).fillMaxWidth()
                    // Il tocco dentro il pannello non lo chiude.
                    .clickable(remember { MutableInteractionSource() }, indication = null) {},
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        header()
                        IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = CmColors.text2) }
                    }
                    Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), content = content)
                }
            }
        }
    }
}

/** Il menu dell'app: in testa lo stato del collegamento; Lancia, Registro, Quadro, Cerca; in fondo Impostazioni. */
@Composable
fun AppMenuPanel(
    host: String?, updated: String, stale: Boolean,
    onLaunch: () -> Unit, onRegister: () -> Unit, onQuadro: () -> Unit, onSearch: () -> Unit, onSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    MenuPanel(
        onDismiss,
        entries = listOf(
            MenuEntry(Icons.Rounded.RocketLaunch, stringResource(R.string.menu_launch), stringResource(R.string.menu_launch_sub), accent = true, onClick = onLaunch),
            MenuEntry(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.menu_register), stringResource(R.string.menu_register_sub), onClick = onRegister),
            MenuEntry(Icons.Rounded.Dashboard, stringResource(R.string.menu_quadro), stringResource(R.string.menu_quadro_sub), onClick = onQuadro),
            MenuEntry(Icons.Rounded.Search, stringResource(R.string.menu_search), stringResource(R.string.menu_search_sub), onClick = onSearch),
        ),
        footer = MenuEntry(Icons.Rounded.Settings, stringResource(R.string.settings), "", onClick = onSettings),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (stale) CmColors.waiting else CmColors.idle))
        Text(
            stringResource(R.string.menu_connected, host ?: stringResource(R.string.menu_pc), updated), style = MaterialTheme.typography.bodyMedium,
            color = CmColors.text2, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MenuItem(icon: ImageVector, title: String, sub: String, tone: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(tone), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = CmColors.actionIcon, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1, overflow = TextOverflow.Clip)
        }
    }
}
