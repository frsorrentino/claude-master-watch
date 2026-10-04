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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    // Il tablet si chiama tablet; su uno schermo largo la colonna resta larga al massimo 640 dp, al centro.
    val tablet = androidx.compose.ui.platform.LocalConfiguration.current.smallestScreenWidthDp >= 600
    Box(Modifier.fillMaxSize().background(CmColors.bg), contentAlignment = Alignment.TopCenter) { Column(
        Modifier.widthIn(max = 640.dp).fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(if (ui.phase == Phase.DONE) R.string.pair_done_title else R.string.pair_title), style = MaterialTheme.typography.headlineMedium, color = CmColors.text)
        StepRow(stringResource(if (tablet) R.string.step_tablet else R.string.step_phone), null, ui.steps.getValue(Step.PHONE))
        // Un'aggiunta (contratto 1.30) non passa dall'orologio: il suo passo non si mostra.
        if (!ui.add) StepRow(stringResource(R.string.step_watch), watchNote(ui), ui.steps.getValue(Step.WATCH))
        StepRow(stringResource(R.string.step_pc), ui.host, ui.steps.getValue(Step.PC))
        ui.fail?.let { Text(stringResource(failText(it, ui.add)), style = MaterialTheme.typography.bodyLarge, color = CmColors.gone) }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { PairScheme(ui, tablet) }
        val big = Modifier.fillMaxWidth().height(56.dp)
        when {
            ui.phase == Phase.DONE -> Button(onDone, big) { Text(stringResource(R.string.pair_finish)) }
            ui.fail == PairFail.WATCH_APP_MISSING -> {
                Button(onInstallOnWatch, big) { Text(stringResource(R.string.pair_install_watch)) }
                OutlinedButton(onRetry, Modifier.fillMaxWidth()) { Text(stringResource(R.string.pair_retry)) }
                OutlinedButton(onWithoutWatch, Modifier.fillMaxWidth()) { Text(stringResource(R.string.pair_without_watch)) }
            }
            ui.fail == PairFail.NO_WATCH -> {
                Button(onRetry, big) { Text(stringResource(R.string.pair_retry)) }
                OutlinedButton(onWithoutWatch, Modifier.fillMaxWidth()) { Text(stringResource(R.string.pair_without_watch)) }
            }
            ui.fail in RESCAN -> Button(onRescan, big) { Text(stringResource(R.string.pair_rescan)) }
            ui.fail != null -> Button(onRetry, big) { Text(stringResource(R.string.pair_retry)) }
        }
    } }
}

/**
 * Il collegamento mentre si accoppia (Franz, 04/10 13:45: «anche nella sezione pair»): gli stessi cerchi e fili dello schema
 * delle Impostazioni, nel colore dei passi. Questo dispositivo a sinistra, il PC al centro, l'orologio a destra; in
 * un'aggiunta l'orologio non c'è, ha già la chiave.
 */
@Composable
private fun PairScheme(ui: PairUi, tablet: Boolean) {
    val phone = ui.steps.getValue(Step.PHONE)
    val watch = ui.steps.getValue(Step.WATCH)
    val pc = ui.steps.getValue(Step.PC)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CmColors.surfaceLow).dotGrid()
            .padding(start = 10.dp, end = 10.dp, top = 20.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        SchemeNode(
            if (tablet) Icons.Rounded.TabletAndroid else Icons.Rounded.PhoneAndroid, 72.dp, stepColor(phone), CmColors.line, dashed = false,
            stringResource(if (tablet) R.string.dev_tablet else R.string.dev_phone), stringResource(R.string.dev_this), null, null,
        )
        SchemeWire(stepColor(pc), dashed = pc == StepState.WAIT, moving = pc == StepState.WORKING, reverse = false, modifier = Modifier.weight(1f).padding(top = 32.dp))
        SchemeNode(
            Icons.Rounded.Computer, 84.dp, stepColor(pc), if (pc == StepState.WORKING) CmColors.actionIcon else CmColors.line, dashed = pc == StepState.WAIT,
            ui.host ?: stringResource(R.string.dev_pc), stepStatus(pc), null, null,
        )
        if (!ui.add) {
            val off = watch == StepState.WAIT || watch == StepState.SKIPPED
            SchemeWire(stepColor(watch), dashed = off, moving = watch == StepState.WORKING, reverse = true, modifier = Modifier.weight(1f).padding(top = 32.dp))
            SchemeNode(
                Icons.Rounded.Watch, 72.dp, stepColor(watch), if (watch == StepState.WORKING) CmColors.actionIcon else CmColors.line, dashed = off,
                ui.watchName ?: stringResource(R.string.dev_watch), stepStatus(watch), null, null,
            )
        }
    }
}

/** Il colore di un passo sul puntino e sul filo: fatto verde, in corso azzurro, in attesa ambra, fallito rosso. */
private fun stepColor(s: StepState): androidx.compose.ui.graphics.Color = when (s) {
    StepState.DONE -> CmColors.briefGood
    StepState.WORKING -> CmColors.actionIcon
    StepState.PENDING -> CmColors.waiting
    StepState.FAILED -> CmColors.gone
    StepState.WAIT, StepState.SKIPPED -> CmColors.briefTrack
}

@Composable
private fun stepStatus(s: StepState): String = when (s) {
    StepState.WAIT -> ""
    StepState.WORKING -> stringResource(R.string.pair_node_working)
    StepState.DONE -> stringResource(R.string.pair_node_done)
    StepState.PENDING -> stringResource(R.string.pair_node_pending)
    StepState.FAILED -> stringResource(R.string.pair_node_failed)
    StepState.SKIPPED -> stringResource(R.string.pair_node_skipped)
}

private val RESCAN = setOf<PairFail?>(PairFail.EXPIRED, PairFail.INVALID, PairFail.WATCH_UID_CHANGED, PairFail.WATCH_CODE)

@Composable
private fun watchNote(ui: PairUi): String? = when {
    ui.restarting -> stringResource(R.string.step_watch_restarting)
    ui.steps[Step.WATCH] == StepState.PENDING -> stringResource(R.string.step_watch_pending)
    ui.steps[Step.WATCH] == StepState.SKIPPED -> stringResource(R.string.step_watch_skipped)
    else -> ui.watchName
}

/**
 * Mai «rilancia relay pair» da solo: senza --add scollega gli altri dispositivi (Franz, 04/10 15:47). Un QR di un'aggiunta
 * scaduto chiede relay pair --add; uno qualunque dice di rilanciare lo stesso comando e che per uno in più c'è --add.
 */
@StringRes
private fun failText(f: PairFail, add: Boolean): Int = when (f) {
    PairFail.EXPIRED -> if (add) R.string.pair_err_expired_add else R.string.pair_err_expired
    PairFail.INVALID -> R.string.pair_err_invalid
    PairFail.WATCH_CODE -> R.string.pair_err_watch_code
    PairFail.NO_WATCH -> R.string.pair_err_no_watch
    PairFail.WATCH_APP_MISSING -> R.string.pair_err_watch_app
    PairFail.NETWORK -> R.string.pair_err_network
    PairFail.PC_NO_CONFIRM -> R.string.pair_err_pc
    PairFail.FULL -> R.string.pair_err_full
    PairFail.WATCH_FAILED -> R.string.pair_err_watch
    PairFail.WATCH_UID_CHANGED -> R.string.pair_err_watch_uid
    PairFail.FAILED -> R.string.pair_err_unexpected
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
