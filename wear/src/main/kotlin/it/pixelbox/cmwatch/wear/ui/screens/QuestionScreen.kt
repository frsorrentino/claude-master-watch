package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.wear.ui.components.SpeakButton
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.Tier
import it.pixelbox.cmwatch.data.PendingStatus
import it.pixelbox.cmwatch.data.Snapshot
import it.pixelbox.cmwatch.rules.QuestionRules
import it.pixelbox.cmwatch.wear.ui.components.SessionHeader
import it.pixelbox.cmwatch.wear.ui.components.StaleChip
import it.pixelbox.cmwatch.wear.ui.components.WideButton
import it.pixelbox.cmwatch.wear.ui.components.CmEdgeButton
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.morph
import kotlinx.coroutines.delay

/**
 * Domanda a schermo intero: testo intero, un bottone largo per opzione uno sotto l'altro, tier high con
 * pressione lunga, testo libero con la tastiera di sistema. Se la domanda sparisce (risposta altrove) lo dice e chiude.
 */
@Composable
fun QuestionScreen(
    snapshot: Snapshot,
    name: String,
    now: Long,
    sentId: String?,
    onAnswer: (Int) -> Unit,
    onFreeText: () -> Unit,
    onAllowAll: () -> Unit,
    onRetry: (String) -> Unit,
    onAnsweredElsewhere: () -> Unit,
    onDone: () -> Unit,
    speaking: Boolean = false,
    onSpeak: () -> Unit = {},
    onChat: () -> Unit = {},
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val s: Session? = snapshot.state?.sessions?.firstOrNull { it.name == name }
    val q = s?.question
    val enabled = snapshot.freshness is Freshness.Fresh
    val pending = snapshot.pending.firstOrNull { it.cmd.id == sentId }
    var holdHint by rememberSaveable { mutableStateOf(false) }
    // Il bordo rosso del rischio alto fa un respiro solo all'arrivo della domanda, poi resta fermo (proposta 20, fase 1):
    // si nota senza restare ad animare addosso alla batteria. Con «riduci animazioni» il bordo è subito quello fermo.
    val respiro = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    val fermeAnimazioni = it.pixelbox.cmwatch.wear.ui.ambient.animationsOff()
    LaunchedEffect(q, fermeAnimazioni) {
        if (q?.tier == Tier.HIGH && !fermeAnimazioni) {
            respiro.snapTo(1f)
            respiro.animateTo(0f, androidx.compose.animation.core.tween(durationMillis = 900))
        } else {
            respiro.snapTo(0f)
        }
    }

    // Domanda sparita: se l'abbiamo mandata noi si chiude al risultato; altrimenti «già risposta» e si chiude.
    if (q == null) {
        LaunchedEffect(sentId, pending?.status) {
            if (sentId == null) { onAnsweredElsewhere(); delay(1500); onDone() }
            else if (pending == null) { delay(600); onDone() }
        }
    }

    ScreenScaffold(
        scrollState = listState,
        // Le opzioni restano bottoni in lista, perché sono contenuto; l'azione della schermata è «Scrivi».
        edgeButton = { CmEdgeButton(stringResource(R.string.question_write), onClick = onFreeText, enabled = enabled && pending == null) },
    ) { padding ->
        // La corona muove la Domanda un'opzione per volta, con lo scatto aptico del sistema (proposta 17, fase 1):
        // si sceglie senza coprire il testo con il dito.
        TransformingLazyColumn(
            state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize(),
            rotaryScrollableBehavior = androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults.snapBehavior(listState),
        ) {
            if (s == null || q == null) {
                item {
                    val text = when {
                        sentId != null && pending?.status == PendingStatus.FAILED -> stringResource(R.string.question_not_delivered)
                        sentId != null -> stringResource(R.string.question_sent)
                        else -> stringResource(R.string.question_answered_elsewhere)
                    }
                    Text(text, style = MaterialTheme.typography.titleLarge, color = CmColors.text, modifier = Modifier.fillMaxWidth().morph(this, spec))
                }
                if (sentId != null && pending?.status == PendingStatus.FAILED) {
                    item { WideButton(stringResource(R.string.question_retry), onClick = { onRetry(sentId) }, primary = true, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) }
                }
                return@TransformingLazyColumn
            }
            (snapshot.freshness as? Freshness.Stale)?.let { st -> item { StaleChip(st.minutes, Modifier.morph(this, spec)) } }
            item { SessionHeader(s, now, enabled, modifier = Modifier.morph(this, spec), trailing = { SpeakButton(speaking, onToggle = onSpeak) }) }
            // La domanda a tutta larghezza; il ▶ che la legge con le opzioni numerate sta in testata (Franz, 15/09 16:13).
            item { QuestionText(q.text, modifier = Modifier.fillMaxWidth().morph(this, spec)) }
            if (holdHint) {
                item { Text(stringResource(R.string.question_hold), style = MaterialTheme.typography.bodyMedium, color = CmColors.waiting, modifier = Modifier.fillMaxWidth().morph(this, spec)) }
            }
            q.options.forEachIndexed { i, opt ->
                item {
                    val primary = QuestionRules.isPrimary(i)
                    val long = QuestionRules.needsLongPress(q.tier)
                    WideButton(
                        QuestionRules.optionLabel(opt),
                        onClick = { if (long) holdHint = true else onAnswer(opt.n) },
                        onLongClick = if (long) ({ onAnswer(opt.n) }) else null,
                        primary = primary, enabled = enabled && pending == null,
                        transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                        // Il rischio alto si vede anche col colore, non solo con la pressione lunga (Franz, 15/09 18:14).
                        // A riposo resta il bordo di prima (2 dp pieno); il respiro lo ingrossa una volta sola all'arrivo.
                        border = if (q.tier == Tier.HIGH) androidx.compose.foundation.BorderStroke((2 + 2 * respiro.value).dp, CmColors.gone) else null,
                    )
                }
            }
            // «Chat about this» come nel terminale: si parla prima di scegliere (Franz, 15/09 18:14; contratto 1.10).
            item {
                WideButton(
                    stringResource(R.string.question_chat), onClick = onChat, enabled = enabled && pending == null,
                    transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                )
            }
            if (QuestionRules.allowAllVisible(q)) {
                item { WideButton(stringResource(R.string.question_allow_all), onClick = onAllowAll, enabled = enabled && pending == null, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) }
            }
        }
    }
}

/** Corpo della domanda a 18 sp; se l'ultima riga resterebbe con una parola sola, scende di uno scatto fino a 15 sp (master, 12/09). */
@Composable
private fun QuestionText(text: String, modifier: Modifier = Modifier) {
    var size by rememberSaveable(text) { mutableStateOf(18) }
    Text(
        text, color = CmColors.text, modifier = modifier,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = size.sp, lineHeight = (size + 5).sp),
        onTextLayout = { r ->
            if (r.lineCount > 1 && size > 15) {
                val last = text.substring(r.getLineStart(r.lineCount - 1), r.getLineEnd(r.lineCount - 1)).trim()
                if (!last.contains(' ')) size -= 1
            }
        },
    )
}
