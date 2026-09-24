package it.pixelbox.cmwatch.wear.ui.screens

import it.pixelbox.cmwatch.rules.VoiceRules
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.ShortText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.RadioButtonDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.SwitchButtonDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.settings.Settings
import it.pixelbox.cmwatch.BuildConfig
import it.pixelbox.cmwatch.ui.tokens.CmColors

/*
 * Impostazioni nello stile di quelle di Google su Wear OS (Franz, 16/09 14:27, con lo screenshot di «Gesti»): voci
 * raggruppate sotto intestazioni, pillole tonali a tutta larghezza con il titolo e sotto il valore attuale, interruttori
 * con la spunta su una pillola grigio-blu quando sono accesi. Prima erano righe «Soglia lettura vocale: 120 caratteri» e
 * interruttori con i colori del nostro primario, chiarissimi.
 */

/** Pillola spenta e accesa come nelle impostazioni di sistema: grigio scuro, e grigio-blu quando la voce è attiva. */
private val Spenta = Color(0xFF2B2E34)
private val Accesa = Color(0xFF444B59)

@Composable
private fun switchColors() = SwitchButtonDefaults.switchButtonColors(
    checkedContainerColor = Accesa, checkedContentColor = CmColors.text, checkedSecondaryContentColor = CmColors.text2,
    checkedTrackColor = CmColors.primary, checkedTrackBorderColor = CmColors.primary,
    checkedThumbColor = CmColors.onPrimary, checkedThumbIconColor = CmColors.primary,
    uncheckedContainerColor = Spenta, uncheckedContentColor = CmColors.text, uncheckedSecondaryContentColor = CmColors.text2,
)

@Composable
private fun radioColors() = RadioButtonDefaults.radioButtonColors(
    selectedContainerColor = Accesa, selectedContentColor = CmColors.text, selectedControlColor = CmColors.primary,
    unselectedContainerColor = Spenta, unselectedContentColor = CmColors.text,
)

@Composable
private fun tonalColors() = ButtonDefaults.filledTonalButtonColors(
    containerColor = Spenta, contentColor = CmColors.text, secondaryContentColor = CmColors.text2, iconColor = CmColors.primary,
)

@Composable
private fun TransformingLazyColumnItemScope.Header(text: String, spec: TransformationSpec) {
    ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) { Text(text) }
}

/** Voce che apre o cambia qualcosa: titolo, valore attuale sotto, icona a sinistra. */
@Composable
private fun TransformingLazyColumnItemScope.ValueButton(
    label: String, value: String?, icon: ImageVector, spec: TransformationSpec, onClick: () -> Unit, enabled: Boolean = true,
) {
    Button(
        onClick = onClick, enabled = enabled, colors = tonalColors(),
        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec), transformation = SurfaceTransformation(spec),
        icon = { Icon(icon, contentDescription = null) },
        secondaryLabel = value?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        label = { Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
    )
}

@Composable
private fun TransformingLazyColumnItemScope.Toggle(label: String, checked: Boolean, spec: TransformationSpec, onChange: (Boolean) -> Unit) {
    SwitchButton(
        checked = checked, onCheckedChange = onChange, colors = switchColors(),
        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec), transformation = SurfaceTransformation(spec),
        label = { Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
    )
}

/** Impostazioni: lettura vocale, vibrazioni per tipo, account della complication, nuovo pairing; in debug le fixture. */
@Composable
fun SettingsScreen(settings: Settings, onChange: (Settings) -> Unit, onRepair: () -> Unit, notificationsEnabled: Boolean = true, onNotificationSettings: () -> Unit = {}, voices: List<String> = emptyList(), onVoice: (String?) -> Unit = {}, accounts: List<String> = emptyList()) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { Header(stringResource(R.string.settings_title), spec) }
            if (!notificationsEnabled) item {
                // L'unica voce che chiede di fare qualcosa: rossa, come gli avvisi nelle impostazioni di sistema.
                Button(
                    onClick = onNotificationSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = CmColors.gone, contentColor = CmColors.text, iconColor = CmColors.text),
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec), transformation = SurfaceTransformation(spec),
                    icon = { Icon(Icons.Rounded.NotificationsOff, contentDescription = null) },
                    label = { Text(stringResource(R.string.settings_notifications_off), maxLines = 3) },
                )
            }

            item { Header(stringResource(R.string.settings_section_voice), spec) }
            item {
                ValueButton(
                    stringResource(R.string.settings_threshold_label), stringResource(R.string.settings_threshold_value, settings.ttsMinChars),
                    Icons.Rounded.ShortText, spec,
                    onClick = { onChange(settings.copy(ttsMinChars = if (settings.ttsMinChars >= 300) 40 else settings.ttsMinChars + 20)) },
                )
            }
            item {
                // Android non dice se una voce è maschile: si provano a orecchio, ogni tocco passa alla successiva e la fa sentire.
                val pos = VoiceRules.position(voices, settings.ttsVoice)
                ValueButton(
                    stringResource(R.string.settings_voice_label),
                    if (pos == 0) stringResource(R.string.settings_voice_default_value) else stringResource(R.string.settings_voice_value, pos, voices.size),
                    Icons.Rounded.RecordVoiceOver, spec, onClick = { onVoice(VoiceRules.next(voices, settings.ttsVoice)) }, enabled = voices.isNotEmpty(),
                )
            }

            item { Header(stringResource(R.string.settings_section_haptics), spec) }
            item { Toggle(stringResource(R.string.settings_haptic_question_short), settings.hapticQuestion, spec) { onChange(settings.copy(hapticQuestion = it)) } }
            item { Toggle(stringResource(R.string.settings_haptic_outcome_short), settings.hapticOutcome, spec) { onChange(settings.copy(hapticOutcome = it)) } }
            item { Toggle(stringResource(R.string.settings_haptic_gone_short), settings.hapticGone, spec) { onChange(settings.copy(hapticGone = it)) } }

            item { Header(stringResource(R.string.settings_complication_account), spec) }
            // Gli account veri dello stato (contratto 1.8: i nomi sono liberi); senza stato, almeno quello scelto.
            for (account in accounts.ifEmpty { listOf(settings.complicationAccount) }) {
                item {
                    RadioButton(
                        selected = settings.complicationAccount == account, onSelect = { onChange(settings.copy(complicationAccount = account)) },
                        colors = radioColors(), modifier = Modifier.fillMaxWidth().transformedHeight(this, spec), transformation = SurfaceTransformation(spec),
                        label = { Text(account) },
                    )
                }
            }

            item { Header(stringResource(R.string.settings_section_pc), spec) }
            item { ValueButton(stringResource(R.string.settings_repair), stringResource(R.string.settings_repair_hint), Icons.Rounded.Link, spec, onClick = onRepair) }

            if (BuildConfig.DEBUG) {
                item { Header(stringResource(R.string.settings_demo), spec) }
                for (fx in listOf("state-1-question", "state-2-idle", "state-3-stale")) {
                    item {
                        RadioButton(
                            selected = settings.demoFixture == fx, onSelect = { onChange(settings.copy(demoFixture = fx)) },
                            colors = radioColors(), modifier = Modifier.fillMaxWidth().transformedHeight(this, spec), transformation = SurfaceTransformation(spec),
                            label = { Text(fx.removePrefix("state-")) },
                        )
                    }
                }
            }
        }
    }
}
