package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
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
import it.pixelbox.cmwatch.wear.ui.components.CmEdgeButton
import it.pixelbox.cmwatch.wear.ui.components.WideButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Terminal
import androidx.wear.compose.material3.Icon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.SwitchButton
import it.pixelbox.cmwatch.rules.SessionsText
import it.pixelbox.cmwatch.rules.TileTexts
import it.pixelbox.cmwatch.rules.ToolText
import it.pixelbox.cmwatch.wear.ui.components.IconAction
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.morph

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
    onRelaunch: (() -> Unit)? = null,
    speaking: Boolean = false,
    onListen: (() -> Unit)? = null,
) {
    val running = stringResource(R.string.tile_turn_running)
    val idleLabel = stringResource(R.string.state_idle)
    val tools = ToolText.Labels(
        run = stringResource(R.string.tool_run), read = stringResource(R.string.tool_read),
        edit = stringResource(R.string.tool_edit), write = stringResource(R.string.tool_write),
        search = stringResource(R.string.tool_search), web = stringResource(R.string.tool_web),
        message = stringResource(R.string.tool_message), delegate = stringResource(R.string.tool_delegate),
        plan = stringResource(R.string.tool_plan), other = stringResource(R.string.tool_other),
    )
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val s = snapshot.state?.sessions?.firstOrNull { it.name == name }
    val enabled = snapshot.freshness is Freshness.Fresh
    ScreenScaffold(
        scrollState = listState,
        // Azione contestuale: «Rispondi» se c'è una domanda, «Riavvia» se la sessione è chiusa, altrimenti «Scrivi».
        edgeButton = {
            when {
                s?.question != null -> CmEdgeButton(stringResource(R.string.card_reply), onClick = onReply, enabled = enabled)
                s?.state == SessionState.GONE && onRelaunch != null ->
                    CmEdgeButton(stringResource(R.string.card_relaunch), onClick = onRelaunch, enabled = enabled)
                s != null -> CmEdgeButton(stringResource(R.string.card_write), onClick = onWrite, enabled = enabled)
                else -> CmEdgeButton(stringResource(R.string.sessions_title), onClick = onBackToSessions)
            }
        },
    ) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            if (s == null) {
                item { Text(stringResource(R.string.card_missing), color = CmColors.text2, modifier = Modifier.morph(this, spec)) }
                item { WideButton(stringResource(R.string.sessions_title), onClick = onBackToSessions, primary = true, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) }
                return@TransformingLazyColumn
            }
            (snapshot.freshness as? Freshness.Stale)?.let { st -> item { StaleChip(st.minutes, Modifier.morph(this, spec)) } }
            // Scheda rifatta (review UX, scelta da Franz il 13/09): intestazione, UNA card con quello che sta
            // facendo, «Segui» come interruttore, poi le azioni con la loro icona. Prima erano quattro bottoni
            // larghi identici che davano lo stesso peso a tutto, con l'informazione in due righe minuscole.
            item { SessionHeader(s, now, enabled, modifier = Modifier.morph(this, spec)) }
            val cell = SessionsText.cell(s, now, running, idleLabel, tools)
            if (cell.title != null || cell.detail != null) {
                item {
                    Card(
                        onClick = if (s.outcome != null) onOutcome else ({}),
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        shape = RoundedCornerShape(21.dp),
                        colors = CardDefaults.cardColors(containerColor = CmColors.surfaceHigh, contentColor = CmColors.text),
                        contentPadding = PaddingValues(14.dp),
                        transformation = SurfaceTransformation(spec),
                    ) {
                        // Il testo intero: la card cresce e la lista scorre, mai «…» (Franz, 15/09 10:56).
                        cell.title?.let {
                            Text(
                                TileTexts.breakable(it), style = MaterialTheme.typography.titleMedium, color = CmColors.text,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        cell.detail?.let {
                            Text(
                                TileTexts.breakable(it), style = MaterialTheme.typography.bodySmall, color = CmColors.text2,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            if (s.state != SessionState.GONE) {
                item {
                    SwitchButton(
                        checked = s.followed,
                        onCheckedChange = { onFollow(it) },
                        enabled = enabled,
                        label = { Text(stringResource(R.string.card_follow), maxLines = 1) },
                        // La campanella della lista (Franz, 14/09 20:32): giallina se seguita, grigia se no.
                        icon = { Icon(Icons.Rounded.Notifications, contentDescription = null, tint = if (s.followed) CmColors.followed else CmColors.text2) },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                    )
                }
                item {
                    IconAction(
                        label = stringResource(R.string.card_terminal), icon = Icons.Rounded.Terminal,
                        onClick = onTerminal, enabled = enabled,
                        transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                    )
                }
                if (onListen != null) {
                    item {
                        // ▶ sulla scheda: la risposta intera chiesta al PC, letta a voce (Franz, 14/09 12:17).
                        IconAction(
                            label = stringResource(if (speaking) R.string.tts_stop else R.string.card_listen),
                            icon = if (speaking) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            onClick = onListen, enabled = true,
                            transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                        )
                    }
                }
                if (s.outcome != null) {
                    item {
                        IconAction(
                            label = stringResource(R.string.card_outcome), icon = Icons.Rounded.Check,
                            onClick = onOutcome, enabled = true,
                            transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                        )
                    }
                }
            }
        }
    }
}
