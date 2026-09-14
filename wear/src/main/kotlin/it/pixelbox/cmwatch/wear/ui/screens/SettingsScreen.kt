package it.pixelbox.cmwatch.wear.ui.screens

import it.pixelbox.cmwatch.rules.VoiceRules
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.settings.Settings
import it.pixelbox.cmwatch.BuildConfig
import it.pixelbox.cmwatch.wear.ui.components.WideButton

/** Impostazioni: soglia TTS, vibrazioni per tipo, account della complication, nuovo pairing; in debug il selettore delle fixture. */
@Composable
fun SettingsScreen(settings: Settings, onChange: (Settings) -> Unit, onRepair: () -> Unit, notificationsEnabled: Boolean = true, onNotificationSettings: () -> Unit = {}, voices: List<String> = emptyList(), onVoice: (String?) -> Unit = {}, accounts: List<String> = emptyList()) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) { Text(stringResource(R.string.settings_title)) } }
            if (!notificationsEnabled) item {
                WideButton(stringResource(R.string.settings_notifications_off), onClick = onNotificationSettings, primary = true, fill = it.pixelbox.cmwatch.wear.ui.theme.CmColors.gone, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec))
            }
            item {
                WideButton(
                    stringResource(R.string.settings_tts_threshold, settings.ttsMinChars),
                    onClick = { onChange(settings.copy(ttsMinChars = if (settings.ttsMinChars >= 300) 40 else settings.ttsMinChars + 20)) },
                    transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                )
            }
            item {
                // Android non dice se una voce è maschile: si provano a orecchio, ogni tocco passa alla successiva e la fa sentire.
                val pos = VoiceRules.position(voices, settings.ttsVoice)
                WideButton(
                    if (pos == 0) stringResource(R.string.settings_tts_voice_default) else stringResource(R.string.settings_tts_voice, pos, voices.size),
                    onClick = { onVoice(VoiceRules.next(voices, settings.ttsVoice)) },
                    enabled = voices.isNotEmpty(),
                    transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                )
            }
            item {
                SwitchButton(checked = settings.hapticQuestion, onCheckedChange = { onChange(settings.copy(hapticQuestion = it)) }, modifier = Modifier.fillMaxWidth().transformedHeight(this, spec), transformation = SurfaceTransformation(spec), label = { Text(stringResource(R.string.settings_haptic_question)) })
            }
            item {
                SwitchButton(checked = settings.hapticOutcome, onCheckedChange = { onChange(settings.copy(hapticOutcome = it)) }, modifier = Modifier.fillMaxWidth().transformedHeight(this, spec), transformation = SurfaceTransformation(spec), label = { Text(stringResource(R.string.settings_haptic_outcome)) })
            }
            item {
                SwitchButton(checked = settings.hapticGone, onCheckedChange = { onChange(settings.copy(hapticGone = it)) }, modifier = Modifier.fillMaxWidth().transformedHeight(this, spec), transformation = SurfaceTransformation(spec), label = { Text(stringResource(R.string.settings_haptic_gone)) })
            }
            item { ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) { Text(stringResource(R.string.settings_complication_account)) } }
            // Gli account veri dello stato (contratto 1.8: i nomi sono liberi); senza stato, almeno quello scelto.
            for (account in accounts.ifEmpty { listOf(settings.complicationAccount) }) {
                item {
                    RadioButton(selected = settings.complicationAccount == account, onSelect = { onChange(settings.copy(complicationAccount = account)) }, modifier = Modifier.fillMaxWidth().transformedHeight(this, spec), transformation = SurfaceTransformation(spec), label = { Text(account) })
                }
            }
            item { WideButton(stringResource(R.string.settings_repair), onClick = onRepair, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) }
            if (BuildConfig.DEBUG) {
                item { ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) { Text(stringResource(R.string.settings_demo)) } }
                for (fx in listOf("state-1-question", "state-2-idle", "state-3-stale")) {
                    item {
                        RadioButton(selected = settings.demoFixture == fx, onSelect = { onChange(settings.copy(demoFixture = fx)) }, modifier = Modifier.fillMaxWidth().transformedHeight(this, spec), transformation = SurfaceTransformation(spec), label = { Text(fx.removePrefix("state-")) })
                    }
                }
            }
        }
    }
}
