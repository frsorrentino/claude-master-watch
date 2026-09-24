package it.pixelbox.cmwatch.mobile.ui

import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.mobile.pair.PairFail
import it.pixelbox.cmwatch.mobile.pair.PairUi
import it.pixelbox.cmwatch.mobile.pair.Phase
import it.pixelbox.cmwatch.mobile.pair.Step
import it.pixelbox.cmwatch.mobile.pair.StepState
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * I tre passi che si accendono uno dopo l'altro, l'errore sotto, un solo bottone pieno (design 24/09, schermata 3). Con un QR
 * scaduto, non valido o un orologio cambiato riprovare lo stesso QR non serve: si legge di nuovo.
 */
@Composable
fun PairingScreen(ui: PairUi, onRetry: () -> Unit, onRescan: () -> Unit, onWithoutWatch: () -> Unit, onInstallOnWatch: () -> Unit, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(if (ui.phase == Phase.DONE) R.string.pair_done_title else R.string.pair_title), style = MaterialTheme.typography.headlineMedium, color = CmColors.text)
        StepRow(stringResource(R.string.step_phone), null, ui.steps.getValue(Step.PHONE))
        StepRow(stringResource(R.string.step_watch), watchNote(ui), ui.steps.getValue(Step.WATCH))
        StepRow(stringResource(R.string.step_pc), ui.host, ui.steps.getValue(Step.PC))
        ui.fail?.let { Text(stringResource(failText(it)), style = MaterialTheme.typography.bodyLarge, color = CmColors.gone) }
        Spacer(Modifier.weight(1f))
        val big = Modifier.fillMaxWidth().height(56.dp)
        when {
            ui.phase == Phase.DONE -> Button(onDone, big) { Text(stringResource(R.string.pair_finish)) }
            ui.fail == PairFail.WATCH_APP_MISSING -> {
                Button(onInstallOnWatch, big) { Text(stringResource(R.string.pair_install_watch)) }
                OutlinedButton(onRetry, Modifier.fillMaxWidth()) { Text(stringResource(R.string.pair_retry)) }
            }
            ui.fail == PairFail.NO_WATCH -> {
                Button(onRetry, big) { Text(stringResource(R.string.pair_retry)) }
                OutlinedButton(onWithoutWatch, Modifier.fillMaxWidth()) { Text(stringResource(R.string.pair_without_watch)) }
            }
            ui.fail in RESCAN -> Button(onRescan, big) { Text(stringResource(R.string.pair_rescan)) }
            ui.fail != null -> Button(onRetry, big) { Text(stringResource(R.string.pair_retry)) }
        }
    }
}

private val RESCAN = setOf<PairFail?>(PairFail.EXPIRED, PairFail.INVALID, PairFail.WATCH_UID_CHANGED)

@Composable
private fun watchNote(ui: PairUi): String? = when {
    ui.restarting -> stringResource(R.string.step_watch_restarting)
    ui.steps[Step.WATCH] == StepState.PENDING -> stringResource(R.string.step_watch_pending)
    ui.steps[Step.WATCH] == StepState.SKIPPED -> stringResource(R.string.step_watch_skipped)
    else -> ui.watchName
}

@StringRes
private fun failText(f: PairFail): Int = when (f) {
    PairFail.EXPIRED -> R.string.pair_err_expired
    PairFail.INVALID -> R.string.pair_err_invalid
    PairFail.NO_WATCH -> R.string.pair_err_no_watch
    PairFail.WATCH_APP_MISSING -> R.string.pair_err_watch_app
    PairFail.NETWORK -> R.string.pair_err_network
    PairFail.PC_NO_CONFIRM -> R.string.pair_err_pc
    PairFail.WATCH_FAILED -> R.string.pair_err_watch
    PairFail.WATCH_UID_CHANGED -> R.string.pair_err_watch_uid
}

@Composable
private fun StepRow(label: String, note: String?, state: StepState) {
    val off = animationsOff()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Crossfade(targetState = state, animationSpec = CmMotion.spec<Float>(off), label = "passo") { s -> StepMark(s, off) }
        Column {
            Text(label, style = MaterialTheme.typography.titleMedium, color = if (state == StepState.WAIT) CmColors.text2 else CmColors.text)
            note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = if (state == StepState.PENDING) CmColors.waiting else CmColors.text2) }
        }
    }
}

@Composable
private fun StepMark(s: StepState, off: Boolean) {
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        when (s) {
            StepState.WAIT -> Box(Modifier.size(12.dp).border(1.dp, CmColors.line, CircleShape))
            StepState.WORKING -> Breathing(off)
            StepState.DONE -> Icon(Icons.Rounded.CheckCircle, null, tint = CmColors.briefGood, modifier = Modifier.size(28.dp))
            StepState.PENDING -> Icon(Icons.Rounded.Schedule, null, tint = CmColors.waiting, modifier = Modifier.size(28.dp))
            StepState.SKIPPED -> Icon(Icons.Rounded.RemoveCircleOutline, null, tint = CmColors.text2, modifier = Modifier.size(28.dp))
            StepState.FAILED -> Icon(Icons.Rounded.Cancel, null, tint = CmColors.gone, modifier = Modifier.size(28.dp))
        }
    }
}

/** Il passo in corso respira, come il bagliore dell'orologio: l'unica animazione che si ripete (design 24/09). */
@Composable
private fun Breathing(off: Boolean) {
    val alpha = if (off) 0.7f else rememberInfiniteTransition(label = "respiro").animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = CmMotion.easing), RepeatMode.Reverse), label = "luce",
    ).value
    Box(Modifier.size(12.dp).background(CmColors.accent.copy(alpha = alpha), CircleShape))
}
