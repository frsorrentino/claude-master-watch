package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.AppLanguage
import it.pixelbox.cmwatch.rules.SettingsDevices
import it.pixelbox.cmwatch.rules.SpeechRate
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Impostazioni (mockup A, scelto da Franz il 03/10 alle 21:59): come le altre pagine, testata con ← e sezioni per
 * argomento, ognuna con la sua etichetta e il filo. Collegamento: lo schema dei dispositivi da toccare (`DeviceScheme`) e
 * «Rifai l'accoppiamento»; Lettura ad alta voce: voce e velocità in pillole; App: lingua, notifiche, Demo; Informazioni:
 * privacy e versione.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    host: String?, phoneName: String, watchName: String?, watchPending: Boolean, demo: Boolean, version: String,
    onRepair: () -> Unit, onDemo: (Boolean) -> Unit, onNotifications: () -> Unit, onPrivacy: () -> Unit,
    language: AppLanguage.Choice = AppLanguage.Choice.SYSTEM, onLanguage: (AppLanguage.Choice) -> Unit = {},
    /** La voce che legge (Franz, 02/10 00:01): le voci italiane del motore, quella scelta (null = predefinita), la prova. */
    voices: List<String> = emptyList(), voice: String? = null, onVoice: (String?) -> Unit = {}, onTryVoice: () -> Unit = {},
    /** La velocità della voce (Franz, 02/10 15:49: «un po' troppo rapida»), una delle `SpeechRate.choices`. */
    rate: Float = SpeechRate.NORMAL, onRate: (Float) -> Unit = {},
    /** Lo stato dei dispositivi per lo schema; senza, quello che si sa dall'abbinamento. */
    devices: SettingsDevices.Model = SettingsDevices.build(host, null, null, 0, phoneName, version, true, watchName, watchPending, null),
    onBack: () -> Unit = {},
    /** Il dispositivo con la scheda aperta all'inizio (i provini); di solito il PC. */
    initialDevice: DeviceNode = DeviceNode.PC,
) {
    var choosing by remember { mutableStateOf(false) }
    var choosingVoice by remember { mutableStateOf(false) }
    var device by rememberSaveable { mutableStateOf(initialDevice) }
    Column(Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.settings_back), tint = CmColors.text) }
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GroupHeader(stringResource(R.string.sec_link), CmColors.text2)
            DeviceScheme(devices, device, onSelect = { device = it }) {
                // Non accoppiati (Demo): accoppiare è l'azione principale, sotto lo schema tratteggiato.
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.connect_pc), style = MaterialTheme.typography.titleLarge, color = CmColors.text)
                    Text(stringResource(R.string.connect_pc_sub), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
                    Button(
                        onClick = onRepair, colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text(stringResource(R.string.pair_button)) }
                }
            }
            // Una voce della lista, non un bottone pieno: là sembrava «Salva» (Franz, 30/09 20:26).
            if (host != null) SectionCard { SettingsRow(Icons.Rounded.QrCodeScanner, stringResource(R.string.paired_repair), stringResource(R.string.repair_sub), onRepair) }

            GroupHeader(stringResource(R.string.sec_reading), CmColors.text2)
            SectionCard {
                SettingsRow(Icons.Rounded.GraphicEq, stringResource(R.string.voice), voice ?: stringResource(R.string.voice_default), onClick = { choosingVoice = true })
                SettingsRow(Icons.Rounded.Speed, stringResource(R.string.speech_rate), stringResource(R.string.rate_hint), onClick = null)
                // Le velocità come pillole, la scelta vale subito (mockup A): niente dialogo da aprire e chiudere.
                FlowRow(
                    Modifier.fillMaxWidth().padding(start = 76.dp, end = 16.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SpeechRate.choices.forEach { r ->
                        val on = r == rate
                        Box(
                            Modifier.heightIn(min = 36.dp).widthIn(min = 44.dp).clip(CircleShape)
                                .background(if (on) CmColors.actionIcon else CmColors.surfaceLow)
                                .border(1.dp, if (on) CmColors.actionIcon else CmColors.line, CircleShape)
                                .clickable { onRate(r) }.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(rateShort(r), style = MaterialTheme.typography.labelLarge, color = if (on) CmColors.onPrimary else CmColors.text) }
                    }
                }
            }

            GroupHeader(stringResource(R.string.sec_app), CmColors.text2)
            SectionCard {
                SettingsRow(Icons.Rounded.Language, stringResource(R.string.language), languageName(language), onClick = { choosing = true })
                SettingsRow(Icons.Rounded.Notifications, stringResource(R.string.notifications), stringResource(R.string.notifications_sub), onNotifications)
                SettingsRow(Icons.Rounded.Science, stringResource(R.string.demo_mode), stringResource(R.string.demo_mode_sub), onClick = { onDemo(!demo) }) {
                    Switch(checked = demo, onCheckedChange = onDemo, colors = cmSwitchColors())
                }
            }

            GroupHeader(stringResource(R.string.sec_info), CmColors.text2)
            SectionCard {
                SettingsRow(Icons.Rounded.PrivacyTip, stringResource(R.string.privacy), stringResource(R.string.privacy_sub), onPrivacy)
                SettingsRow(Icons.Rounded.Info, stringResource(R.string.version_title), stringResource(R.string.version_value, version), onClick = null)
            }
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

/** Una card di sezione: le righe una sotto l'altra sul fondo basso, angoli da 20 dp. */
@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CmColors.surfaceLow).padding(vertical = 6.dp), content = content)
}

/** Una riga come nel menu ≡: icona nel tondo, titolo e valore attuale; la freccia se porta altrove, o un controllo a destra. */
@Composable
private fun SettingsRow(
    icon: ImageVector, title: String, sub: String, onClick: (() -> Unit)?, trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(CmColors.surface), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = CmColors.actionIcon, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = CmColors.text2)
        }
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = CmColors.text2)
        }
    }
}

/** «1,25×» con la virgola della lingua dell'app, come la pillola accanto a ■. */
@Composable
private fun rateShort(r: Float): String {
    val n = java.text.NumberFormat.getInstance(androidx.compose.ui.platform.LocalConfiguration.current.locales[0])
        .apply { maximumFractionDigits = 2 }.format(r)
    return stringResource(R.string.speech_rate_short, n)
}

@Composable
private fun languageName(c: AppLanguage.Choice): String = stringResource(when (c) {
    AppLanguage.Choice.SYSTEM -> R.string.language_system
    AppLanguage.Choice.ITALIAN -> R.string.language_it
    AppLanguage.Choice.ENGLISH -> R.string.language_en
})
