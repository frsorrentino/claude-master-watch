package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.rules.DoubleConfirm
import it.pixelbox.cmwatch.rules.LiveCard
import it.pixelbox.cmwatch.rules.LiveCard.Kind
import it.pixelbox.cmwatch.rules.LiveTap
import it.pixelbox.cmwatch.rules.LiveTap.Action
import it.pixelbox.cmwatch.ui.tokens.CmColors
import it.pixelbox.cmwatch.wear.ui.components.CmEdgeButton
import it.pixelbox.cmwatch.wear.ui.components.WideButton
import it.pixelbox.cmwatch.wear.ui.theme.morph

/**
 * La scheda della modalità live (specifica live, §3 e §7): la notizia in corso e i suoi tasti. Opzioni di una domanda,
 * Prossimi di un esito (il «!» col bordo ambra), candidati di una scelta; poi Ripeti, Dopo, Salta e, sul bordo, Parla.
 * La richiesta di ok ha «Approva» sul bordo; la doppia conferma un tasto diverso da tenere premuto 2 secondi. `now` in ms
 * epoch, per i conti alla rovescia di `until`.
 */
@Composable
fun LiveScreen(card: LiveCard?, now: Long, onTap: (LiveTap) -> Unit, onTalk: () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val kind = card?.kind ?: Kind.OFF
    val cancel = stringResource(R.string.live_cancel)
    val repeat = stringResource(R.string.live_repeat)
    val later = stringResource(R.string.live_later)
    val skip = stringResource(R.string.live_skip)
    val round = stringResource(R.string.live_round)
    val actionsLabel = stringResource(R.string.live_actions)
    val blockingLabel = stringResource(if (card?.onlyBlocking == true) R.string.live_all else R.string.live_only_blocking)
    val stop = stringResource(R.string.live_stop)
    val approve = stringResource(R.string.live_approve)
    val talk = stringResource(R.string.live_talk)
    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            fun button(text: String, a: Action, index: Int = 0, blocking: Boolean = false) = item {
                WideButton(
                    text, onClick = { onTap(LiveTap(a, index)) }, transformation = SurfaceTransformation(spec),
                    modifier = Modifier.transformedHeight(this, spec), border = if (blocking) BorderStroke(2.dp, CmColors.waiting) else null,
                )
            }
            if (card == null || kind == Kind.OFF) {
                item { Text(stringResource(R.string.live_off), style = MaterialTheme.typography.bodyLarge, color = CmColors.text2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().morph(this, spec)) }
                return@TransformingLazyColumn
            }
            if (card.title.isNotBlank()) {
                item { Text(card.title, style = MaterialTheme.typography.titleMedium, color = CmColors.actionIcon, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().morph(this, spec)) }
            }
            val body = card.text.ifBlank { if (kind == Kind.IDLE) null else card.text }
            if (kind == Kind.IDLE && card.text.isBlank()) {
                item { Text(stringResource(R.string.live_idle), style = MaterialTheme.typography.bodyLarge, color = CmColors.text2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().morph(this, spec)) }
            } else if (!body.isNullOrBlank()) {
                item { Text(body, style = MaterialTheme.typography.bodyLarge, color = CmColors.text, modifier = Modifier.fillMaxWidth().morph(this, spec)) }
            }
            when (kind) {
                Kind.QUESTION -> card.options.forEachIndexed { i, o -> button("${i + 1} · $o", Action.OPTION, i + 1) }
                Kind.OUTCOME -> card.options.forEachIndexed { i, o -> button(o, Action.STEP, i + 1, card.blocking.getOrElse(i) { false }) }
                Kind.PICK -> card.options.forEachIndexed { i, o -> button("${i + 1} · $o", Action.PICK, i + 1) }
                Kind.CONFIRM -> {
                    val left = ((card.until - now + 999) / 1000).coerceIn(0, DoubleConfirm.TIMEOUT_MS / 1000)
                    item { Text(stringResource(R.string.live_hold_left, left.toInt()), style = MaterialTheme.typography.bodyMedium, color = CmColors.waiting, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().morph(this, spec)) }
                    item { HoldButton(stringResource(R.string.live_hold), onPress = { onTap(LiveTap(Action.PRESS)) }, onRelease = { onTap(LiveTap(Action.RELEASE)) }, modifier = Modifier.morph(this, spec)) }
                    button(cancel, Action.CANCEL)
                }
                Kind.TELL -> {
                    val left = ((card.until - now + 999) / 1000).coerceAtLeast(0)
                    item { Text(stringResource(R.string.live_leaving, left.toInt()), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().morph(this, spec)) }
                    button(cancel, Action.CANCEL)
                }
                else -> Unit
            }
            if (kind == Kind.PICK) button(cancel, Action.CANCEL)
            if (kind in setOf(Kind.NEWS, Kind.QUESTION, Kind.OUTCOME, Kind.APPROVAL)) {
                button(repeat, Action.REPEAT)
                button(later, Action.LATER)
                button(skip, Action.SKIP)
            }
            if (kind == Kind.IDLE || kind == Kind.NEWS) {
                button(round, Action.ROUND)
                // Le azioni in attesa, da scegliere e mandare (Franz, 08/10 21:17).
                button(actionsLabel, Action.ACTIONS)
                button(blockingLabel, Action.ONLY_BLOCKING)
                button(stop, Action.STOP)
            }
            // Un solo tasto pieno, sul bordo: Approva sulla richiesta di ok, Parla altrove; niente durante un'interazione.
            when (kind) {
                Kind.APPROVAL -> item { CmEdgeButton(approve, onClick = { onTap(LiveTap(Action.APPROVE)) }) }
                Kind.IDLE, Kind.NEWS, Kind.QUESTION, Kind.OUTCOME -> item { CmEdgeButton(talk, onClick = onTalk) }
                else -> Unit
            }
        }
    }
}

/** Il secondo tasto della doppia conferma: conta la pressione, non il tocco; il telefono misura i 2 secondi. */
@Composable
private fun HoldButton(text: String, onPress: () -> Unit, onRelease: () -> Unit, modifier: Modifier = Modifier) {
    var down by remember { mutableStateOf(false) }
    Box(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(28.dp))
            .background(if (down) CmColors.accentPressed else CmColors.waiting)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    down = true; onPress()
                    tryAwaitRelease()
                    down = false; onRelease()
                })
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = CmColors.onPrimary)
    }
}
