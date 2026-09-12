package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.data.Snapshot
import it.pixelbox.cmwatch.rules.CardText
import it.pixelbox.cmwatch.wear.ui.components.SessionHeader
import it.pixelbox.cmwatch.wear.ui.components.StaleChip
import it.pixelbox.cmwatch.wear.ui.components.WideButton
import it.pixelbox.cmwatch.wear.ui.theme.CmColors

/** Scheda: riga nome · account · stato · durata; → prossimo; esito; Rispondi / Scrivi / Terminale / Segui. */
@Composable
fun SessionScreen(
    snapshot: Snapshot,
    name: String,
    now: Long,
    onReply: () -> Unit,
    onWrite: () -> Unit,
    onTerminal: () -> Unit,
    onFollow: (Boolean) -> Unit,
    onOutcome: () -> Unit,
    onBackToSessions: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val s = snapshot.state?.sessions?.firstOrNull { it.name == name }
    val enabled = snapshot.freshness is Freshness.Fresh
    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            if (s == null) {
                item { Text(stringResource(R.string.card_missing), color = CmColors.text2, modifier = Modifier.transformedHeight(this, spec)) }
                item { WideButton(stringResource(R.string.sessions_title), onClick = onBackToSessions, primary = true, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) }
                return@TransformingLazyColumn
            }
            (snapshot.freshness as? Freshness.Stale)?.let { st -> item { StaleChip(st.minutes, Modifier.transformedHeight(this, spec)) } }
            item { SessionHeader(s, now, enabled, modifier = Modifier.transformedHeight(this, spec)) }
            CardText.next(s)?.let { next ->
                item { Text(next, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, modifier = Modifier.fillMaxWidth().transformedHeight(this, spec)) }
            }
            s.outcome?.let { o ->
                item {
                    WideButton(o.short, onClick = onOutcome, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec))
                }
            }
            if (s.question != null) {
                item { WideButton(stringResource(R.string.card_reply), onClick = onReply, primary = true, enabled = enabled, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) }
            }
            if (s.state != SessionState.GONE) {
                item { WideButton(stringResource(R.string.card_write), onClick = onWrite, enabled = enabled, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) }
                item { WideButton(stringResource(R.string.card_terminal), onClick = onTerminal, enabled = false, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) }
                item {
                    WideButton(
                        stringResource(if (s.followed) R.string.card_unfollow else R.string.card_follow),
                        onClick = { onFollow(!s.followed) }, enabled = enabled,
                        transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                    )
                }
            }
        }
    }
}
