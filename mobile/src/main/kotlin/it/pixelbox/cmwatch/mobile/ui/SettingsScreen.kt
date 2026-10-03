package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.mobile.ui.art.PairedScene
import it.pixelbox.cmwatch.rules.AppLanguage
import it.pixelbox.cmwatch.rules.SpeechRate
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** Impostazioni (design 29/09, schermata 7): accoppiamento, lingua (restyling 30/09), Demo, notifiche, privacy, versione. */
@Composable
fun SettingsScreen(
    host: String?, phoneName: String, watchName: String?, watchPending: Boolean, demo: Boolean, version: String,
    onRepair: () -> Unit, onDemo: (Boolean) -> Unit, onNotifications: () -> Unit, onPrivacy: () -> Unit,
    language: AppLanguage.Choice = AppLanguage.Choice.SYSTEM, onLanguage: (AppLanguage.Choice) -> Unit = {},
    /** La voce che legge (Franz, 02/10 00:01): le voci italiane del motore, quella scelta (null = predefinita), la prova. */
    voices: List<String> = emptyList(), voice: String? = null, onVoice: (String?) -> Unit = {}, onTryVoice: () -> Unit = {},
    /** La velocità della voce (Franz, 02/10 15:49: «un po' troppo rapida»), una delle `SpeechRate.choices`. */
    rate: Float = SpeechRate.NORMAL, onRate: (Float) -> Unit = {},
) {
    var choosing by remember { mutableStateOf(false) }
    var choosingVoice by remember { mutableStateOf(false) }
    var choosingRate by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = CmColors.text)
            // Non accoppiati (Demo): accoppiare è l'azione principale, in una card in cima.
            if (host == null) Surface(color = CmColors.briefCard, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.connect_pc), style = MaterialTheme.typography.titleLarge, color = CmColors.text)
                    Text(stringResource(R.string.connect_pc_sub), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
                    Button(
                        onClick = onRepair, colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text(stringResource(R.string.pair_button)) }
                }
            }
            if (host != null) {
                PairedScene(watch = watchName != null, modifier = Modifier.fillMaxWidth().height(160.dp))
                Text(stringResource(R.string.paired_title, host), style = MaterialTheme.typography.titleLarge, color = CmColors.text)
                Surface(color = CmColors.briefCard, shape = MaterialTheme.shapes.large) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Device(Icons.Rounded.PhoneAndroid, phoneName, stringResource(R.string.paired_this_phone), false)
                        watchName?.let { Device(Icons.Rounded.Watch, it, stringResource(if (watchPending) R.string.step_watch_pending else R.string.paired_key_delivered), watchPending) }
                        // Una voce della card, non un bottone pieno in fondo: là sembrava «Salva» (Franz, 30/09 20:26).
                        TextButton(onClick = onRepair) {
                            Icon(Icons.Rounded.QrCodeScanner, null, tint = CmColors.actionIcon)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.paired_repair), color = CmColors.actionIcon)
                        }
                    }
                }
            }
            Surface(color = CmColors.briefCard, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.language)) },
                        supportingContent = { Text(languageName(language)) },
                        modifier = Modifier.clickable { choosing = true },
                        colors = ListItemDefaults.colors(containerColor = CmColors.briefCard),
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.voice)) },
                        supportingContent = { Text(voice ?: stringResource(R.string.voice_default)) },
                        modifier = Modifier.clickable { choosingVoice = true },
                        colors = ListItemDefaults.colors(containerColor = CmColors.briefCard),
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.speech_rate)) },
                        supportingContent = { Text(rateLabel(rate)) },
                        modifier = Modifier.clickable { choosingRate = true },
                        colors = ListItemDefaults.colors(containerColor = CmColors.briefCard),
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.demo_mode)) }, supportingContent = { Text(stringResource(R.string.demo_mode_sub)) },
                        trailingContent = { Switch(checked = demo, onCheckedChange = onDemo, colors = cmSwitchColors()) },
                        colors = ListItemDefaults.colors(containerColor = CmColors.briefCard),
                    )
                }
            }
            TextButton(onClick = onNotifications) { Text(stringResource(R.string.notifications), color = CmColors.actionIcon) }
            TextButton(onClick = onPrivacy) { Text(stringResource(R.string.privacy), color = CmColors.actionIcon) }
            Text(stringResource(R.string.version, version), color = CmColors.text2, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (choosingVoice) {
        AlertDialog(
            onDismissRequest = { choosingVoice = false },
            confirmButton = { TextButton(onClick = onTryVoice) { Text(stringResource(R.string.voice_try), color = CmColors.actionIcon) } },
            dismissButton = { TextButton(onClick = { choosingVoice = false }) { Text(stringResource(R.string.close), color = CmColors.actionIcon) } },
            title = { Text(stringResource(R.string.voice)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    (listOf<String?>(null) + voices).forEach { v ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onVoice(v) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(selected = v == voice, onClick = null)
                            Text(v ?: stringResource(R.string.voice_default), color = CmColors.text)
                        }
                    }
                }
            },
        )
    }
    // Come la voce: la scelta vale subito e il dialogo resta aperto per provarla.
    if (choosingRate) {
        AlertDialog(
            onDismissRequest = { choosingRate = false },
            confirmButton = { TextButton(onClick = onTryVoice) { Text(stringResource(R.string.voice_try), color = CmColors.actionIcon) } },
            dismissButton = { TextButton(onClick = { choosingRate = false }) { Text(stringResource(R.string.close), color = CmColors.actionIcon) } },
            title = { Text(stringResource(R.string.speech_rate)) },
            text = {
                Column {
                    SpeechRate.choices.forEach { r ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onRate(r) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(selected = r == rate, onClick = null)
                            Text(rateLabel(r), color = CmColors.text)
                        }
                    }
                }
            },
        )
    }
    if (choosing) {
        AlertDialog(
            onDismissRequest = { choosing = false }, confirmButton = {},
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column {
                    AppLanguage.Choice.entries.forEach { c ->
                        Row(
                            Modifier.fillMaxWidth().clickable { choosing = false; onLanguage(c) }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(selected = c == language, onClick = null)
                            Text(languageName(c), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            containerColor = CmColors.surface,
        )
    }
}

/** «0,9×» con la virgola della lingua dell'app; la velocità del motore si dice «normale». */
@Composable
private fun rateLabel(r: Float): String {
    val n = java.text.NumberFormat.getInstance(androidx.compose.ui.platform.LocalConfiguration.current.locales[0])
        .apply { minimumFractionDigits = 1; maximumFractionDigits = 2 }.format(r)
    return stringResource(if (r == SpeechRate.NORMAL) R.string.speech_rate_normal else R.string.speech_rate_value, n)
}

@Composable
private fun languageName(c: AppLanguage.Choice): String = stringResource(when (c) {
    AppLanguage.Choice.SYSTEM -> R.string.language_system
    AppLanguage.Choice.ITALIAN -> R.string.language_it
    AppLanguage.Choice.ENGLISH -> R.string.language_en
})

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
