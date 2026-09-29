package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.mobile.ui.art.PairedScene
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** Impostazioni (design 29/09, schermata 7): accoppiamento, Demo, notifiche, privacy, versione. */
@Composable
fun SettingsScreen(
    host: String?, phoneName: String, watchName: String?, watchPending: Boolean, demo: Boolean, version: String,
    onRepair: () -> Unit, onDemo: (Boolean) -> Unit, onNotifications: () -> Unit, onPrivacy: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium, color = CmColors.text)
            if (host != null) {
                PairedScene(watch = watchName != null, modifier = Modifier.fillMaxWidth().height(160.dp))
                Text(stringResource(R.string.paired_title, host), style = MaterialTheme.typography.titleLarge, color = CmColors.text)
                Surface(color = CmColors.surface, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Device(Icons.Rounded.PhoneAndroid, phoneName, stringResource(R.string.paired_this_phone), false)
                        watchName?.let { Device(Icons.Rounded.Watch, it, stringResource(if (watchPending) R.string.step_watch_pending else R.string.paired_key_delivered), watchPending) }
                    }
                }
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.demo_mode)) }, supportingContent = { Text(stringResource(R.string.demo_mode_sub)) },
                trailingContent = { Switch(checked = demo, onCheckedChange = onDemo) },
                colors = ListItemDefaults.colors(containerColor = CmColors.bg),
            )
            TextButton(onClick = onNotifications) { Text(stringResource(R.string.notifications), color = CmColors.actionIcon) }
            TextButton(onClick = onPrivacy) { Text(stringResource(R.string.privacy), color = CmColors.actionIcon) }
            Text(stringResource(R.string.version, version), color = CmColors.text2, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = onRepair, colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(56.dp),
        ) { Text(stringResource(if (host != null) R.string.paired_repair else R.string.pair_button)) }
    }
}

@Composable
private fun Device(icon: androidx.compose.ui.graphics.vector.ImageVector, name: String, note: String, pending: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, tint = CmColors.actionIcon)
        Column {
            Text(name, style = MaterialTheme.typography.titleMedium, color = CmColors.text)
            Text(note, style = MaterialTheme.typography.bodyMedium, color = if (pending) CmColors.waiting else CmColors.text2)
        }
    }
}
