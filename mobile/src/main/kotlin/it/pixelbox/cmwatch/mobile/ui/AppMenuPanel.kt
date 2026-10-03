package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

/**
 * Il menu dell'app (design 03/10, tavola 5): un pannello largo sotto la barra sopra lo sfondo oscurato. In testa lo stato
 * del collegamento; quattro voci grandi con icona su fondo tonale e una riga di spiegazione; in fondo Impostazioni.
 * Tocco fuori, ✕ o Indietro lo chiudono.
 */
@Composable
fun AppMenuPanel(
    host: String?, updated: String, stale: Boolean,
    onLaunch: () -> Unit, onRegister: () -> Unit, onQuadro: () -> Unit, onSearch: () -> Unit, onSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp), color = CmColors.surface, shadowElevation = 8.dp,
                modifier = Modifier.statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 56.dp).fillMaxWidth()
                    // Il tocco dentro il pannello non lo chiude.
                    .clickable(remember { MutableInteractionSource() }, indication = null) {},
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (stale) CmColors.waiting else CmColors.idle))
                        Text(
                            stringResource(R.string.menu_connected, host ?: stringResource(R.string.menu_pc), updated), style = MaterialTheme.typography.bodyMedium,
                            color = CmColors.text2, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = CmColors.text2) }
                    }
                    MenuItem(Icons.Rounded.RocketLaunch, R.string.menu_launch, R.string.menu_launch_sub, CmColors.accent.copy(alpha = 0.25f)) { onDismiss(); onLaunch() }
                    MenuItem(Icons.AutoMirrored.Rounded.MenuBook, R.string.menu_register, R.string.menu_register_sub) { onDismiss(); onRegister() }
                    MenuItem(Icons.Rounded.Dashboard, R.string.menu_quadro, R.string.menu_quadro_sub) { onDismiss(); onQuadro() }
                    MenuItem(Icons.Rounded.Search, R.string.menu_search, R.string.menu_search_sub) { onDismiss(); onSearch() }
                    HorizontalDivider(color = CmColors.line, modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable { onDismiss(); onSettings() }.padding(horizontal = 28.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Icon(Icons.Rounded.Settings, null, tint = CmColors.text2)
                        Text(stringResource(R.string.settings), style = MaterialTheme.typography.bodyLarge, color = CmColors.text2)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, title: Int, sub: Int, tone: Color = CmColors.surfaceHigh, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(tone), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = CmColors.actionIcon, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip)
            Text(stringResource(sub), style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1, overflow = TextOverflow.Clip)
        }
    }
}
