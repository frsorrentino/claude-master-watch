package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** Accoppiato: il PC e i due dispositivi (design 24/09, schermata 4); la base della regia del pezzo 3. */
@Composable
fun PairedScreen(host: String, phoneName: String, watchName: String?, watchPending: Boolean, onRepair: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.paired_title, host), style = MaterialTheme.typography.headlineMedium, color = CmColors.text)
        Surface(color = CmColors.surface, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DeviceRow(Icons.Rounded.PhoneAndroid, phoneName, stringResource(R.string.paired_this_phone), CmColors.text2)
                watchName?.let {
                    DeviceRow(Icons.Rounded.Watch, it, stringResource(if (watchPending) R.string.step_watch_pending else R.string.paired_key_delivered),
                        if (watchPending) CmColors.waiting else CmColors.text2)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onRepair, Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(R.string.paired_repair)) }
    }
}

@Composable
private fun DeviceRow(icon: ImageVector, name: String, note: String, noteColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, tint = CmColors.actionIcon)
        Column {
            Text(name, style = MaterialTheme.typography.titleMedium, color = CmColors.text)
            Text(note, style = MaterialTheme.typography.bodyMedium, color = noteColor)
        }
    }
}
