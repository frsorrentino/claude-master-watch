package it.pixelbox.cmwatch.wear.ui.screens

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
import it.pixelbox.cmwatch.wear.ui.theme.roundListPadding
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
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val s: Session? = snapshot.state?.sessions?.firstOrNull { it.name == name }
    val q = s?.question
    val enabled = snapshot.freshness is Freshness.Fresh
    val pending = snapshot.pending.firstOrNull { it.cmd.id == sentId }
    var holdHint by rememberSaveable { mutableStateOf(false) }

    // Domanda sparita: se l'abbiamo mandata noi si chiude al risultato; altrimenti «già risposta» e si chiude.
    if (q == null) {
        LaunchedEffect(sentId, pending?.status) {
            if (sentId == null) { onAnsweredElsewhere(); delay(1500); onDone() }
            else if (pending == null) { delay(600); onDone() }
        }
    }

    ScreenScaffold(
        scrollState = listState,
        contentPadding = roundListPadding(),
        // Le opzioni restano bottoni in lista, perché sono contenuto; l'azione della schermata è «Scrivi».
        edgeButton = { CmEdgeButton(stringResource(R.string.question_write), onClick = onFreeText, enabled = enabled && pending == null) },
    ) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            if (s == null || q == null) {
                item {
                    val text = when {
                        sentId != null && pending?.status == PendingStatus.FAILED -> stringResource(R.string.question_not_delivered)
                        sentId != null -> stringResource(R.string.question_sent)
                        else -> stringResource(R.string.question_answered_elsewhere)
                    }
                    Text(text, style = MaterialTheme.typography.titleLarge, color = CmColors.text, modifier = Modifier.fillMaxWidth())
                }
                if (sentId != null && pending?.status == PendingStatus.FAILED) {
                    item { WideButton(stringResource(R.string.question_retry), onClick = { onRetry(sentId) }, primary = true, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) }
                }
                return@TransformingLazyColumn
            }
            (snapshot.freshness as? Freshness.Stale)?.let { st -> item { StaleChip(st.minutes, Modifier) } }
            item { SessionHeader(s, now, enabled, modifier = Modifier) }
            item { QuestionText(q.text, modifier = Modifier.fillMaxWidth()) }
            if (holdHint) {
                item { Text(stringResource(R.string.question_hold), style = MaterialTheme.typography.bodyMedium, color = CmColors.waiting, modifier = Modifier.fillMaxWidth()) }
            }
            q.options.forEachIndexed { i, opt ->
                item {
                    val primary = QuestionRules.isPrimary(i)
                    val fill = when (q.tier) { Tier.LOW -> CmColors.accent; Tier.MEDIUM -> CmColors.waiting; Tier.HIGH -> CmColors.gone }
                    val long = QuestionRules.needsLongPress(q.tier)
                    WideButton(
                        QuestionRules.optionLabel(opt),
                        onClick = { if (long) holdHint = true else onAnswer(opt.n) },
                        onLongClick = if (long) ({ onAnswer(opt.n) }) else null,
                        primary = primary, fill = fill, enabled = enabled && pending == null,
                        transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                    )
                }
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
