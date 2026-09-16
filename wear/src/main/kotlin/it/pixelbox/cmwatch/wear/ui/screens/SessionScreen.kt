package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import it.pixelbox.cmwatch.wear.ui.theme.bottomGlow
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.data.Snapshot
import it.pixelbox.cmwatch.rules.CardText
import it.pixelbox.cmwatch.rules.OutcomeText
import it.pixelbox.cmwatch.wear.ui.components.SpeakButton
import it.pixelbox.cmwatch.wear.ui.components.TextAroundTrailing
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

/** Scheda: testata con ▶; card con quello che fa o l'esito intero, che apre il Terminale; Segui; Rispondi / Scrivi / Riavvia. */
@Composable
fun SessionScreen(
    snapshot: Snapshot,
    name: String,
    now: Long,
    onReply: () -> Unit,
    onWrite: () -> Unit,
    onTerminal: () -> Unit,
    onFollow: (Boolean) -> Unit,
    onBackToSessions: () -> Unit,
    onRelaunch: (() -> Unit)? = null,
    onReopen: (() -> Unit)? = null,
    reopen: it.pixelbox.cmwatch.rules.ReopenText.Status? = null,
    /** L'ultimo blocco del terminale, chiesto al PC quando la sessione lavora senza uno strumento in vista. */
    live: String? = null,
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
                // Una chiusa si riprende nella sua conversazione (contratto 1.9); «Riavvia» da capo resta sotto.
                s?.state == SessionState.GONE && onReopen != null ->
                    // «Avvio in corso» spento finché il PC rilancia (30-60 s) e la sessione torna (Franz, 15/09 19:14).
                    if (reopen is it.pixelbox.cmwatch.rules.ReopenText.Status.Starting) CmEdgeButton(stringResource(R.string.reopen_starting), onClick = {}, enabled = false)
                    else CmEdgeButton(stringResource(R.string.notif_resume), onClick = onReopen, enabled = enabled)
                s?.state == SessionState.GONE && onRelaunch != null ->
                    CmEdgeButton(stringResource(R.string.card_relaunch), onClick = onRelaunch, enabled = enabled)
                s != null -> CmEdgeButton(stringResource(R.string.card_write), onClick = onWrite, enabled = enabled)
                else -> CmEdgeButton(stringResource(R.string.sessions_title), onClick = onBackToSessions)
            }
        },
    ) { padding ->
        // Bagliore in fondo mentre la sessione lavora (proposta 44, fase 1): respira piano e si spegne da solo quando
        // la sessione si ferma. In ambient e con «riduci animazioni» resta una luce ferma e fioca.
        TransformingLazyColumn(
            state = listState, contentPadding = padding,
            modifier = Modifier.fillMaxSize().bottomGlow(CmColors.busy, visible = s?.state == SessionState.BUSY),
        ) {
            if (s == null) {
                item { Text(stringResource(R.string.card_missing), color = CmColors.text2, modifier = Modifier.morph(this, spec)) }
                item { WideButton(stringResource(R.string.sessions_title), onClick = onBackToSessions, primary = true, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) }
                return@TransformingLazyColumn
            }
            (snapshot.freshness as? Freshness.Stale)?.let { st -> item { StaleChip(st.minutes, Modifier.morph(this, spec)) } }
            // Scheda rifatta (review UX, scelta da Franz il 13/09): intestazione, UNA card con quello che sta
            // facendo, «Segui» come interruttore, poi le azioni con la loro icona. Prima erano quattro bottoni
            // larghi identici che davano lo stesso peso a tutto, con l'informazione in due righe minuscole.
            // La card contiene anche l'Esito: chi è fermo lo mostra intero, senza un tap in più (Franz, 15/09 17:02).
            val cell = SessionsText.sheet(s, now, running, idleLabel, tools, live = live)
            // La riga dello strumento in testata tace se la card sotto la dice già intera (Franz, 15/09 16:00).
            val toolRepeated = cell.says(ToolText.describe(s.toolNote, s.tool, tools))
            // Il ▶ dentro la card del testo, in alto a destra (Franz, 16/09 13:18, soluzione A): in testata stringeva il nome
            // su tre righe («claude- / master- / watch»). Nella card sta accanto a quello che legge.
            val listen: (@Composable () -> Unit)? = onListen?.let { l -> { SpeakButton(speaking, onToggle = l, modifier = Modifier.size(38.dp)) } }
            item { SessionHeader(s, now, enabled, modifier = Modifier.morph(this, spec), showTool = !toolRepeated) }
            (reopen as? it.pixelbox.cmwatch.rules.ReopenText.Status.Failed)?.let { f ->
                item {
                    Text(
                        stringResource(R.string.reopen_failed, f.text ?: stringResource(R.string.question_not_delivered)),
                        style = MaterialTheme.typography.bodySmall, color = CmColors.goneDim, modifier = Modifier.fillMaxWidth().morph(this, spec),
                    )
                }
            }
            if (cell.title != null || cell.detail != null) {
                item {
                    Card(
                        // Toccare la card apre il Terminale: il livello «tutto», dopo il riassunto (15/09 17:02).
                        onClick = onTerminal,
                        onLongClick = if (s.state != SessionState.GONE) ({ onFollow(!s.followed) }) else null,
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        shape = RoundedCornerShape(21.dp),
                        colors = CardDefaults.cardColors(containerColor = CmColors.surfaceHigh, contentColor = CmColors.text),
                        // Seguita = «accesa», come la riga della lista (Franz, 16/09 01:58): bordo giallino qui e
                        // campanella in testata, così la pressione lunga si vede anche nella Scheda.
                        border = if (s.followed) androidx.compose.foundation.BorderStroke(1.5.dp, CmColors.followed) else null,
                        contentPadding = PaddingValues(14.dp),
                        transformation = SurfaceTransformation(spec),
                    ) {
                        // Il testo intero: la card cresce e la lista scorre, mai «…» (Franz, 15/09 10:56). Il ▶ sta accanto
                        // alla prima riga di testo; quello che segue torna a tutta larghezza.
                        // Il primo testo scorre attorno al ▶: accanto al tasto solo le righe che ne coprono l'altezza, poi
                        // di nuovo a tutta larghezza (Franz, 16/09 13:39).
                        val primo = cell.title?.let { TileTexts.breakable(it) } ?: cell.body
                        val stile = when {
                            // Grande se è breve, più piccolo se è una frase lunga (contratto 1.6, fino a 200 caratteri).
                            cell.title != null -> if (OutcomeText.bigTitle(cell.title!!)) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
                            else -> MaterialTheme.typography.bodyMedium
                        }
                        when {
                            primo != null && listen != null ->
                                TextAroundTrailing(primo, stile, CmColors.text, trailingSize = 38.dp, trailing = listen)
                            primo != null -> Text(primo, style = stile, color = CmColors.text, modifier = Modifier.fillMaxWidth())
                            listen != null -> Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) { listen() }
                        }
                        if (cell.title != null) cell.body?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = CmColors.text, modifier = Modifier.fillMaxWidth())
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
            // Il riquadro di contesto (Franz, 16/09 00:14): per ora la quota dell'account di questa sessione, con
            // l'anello concentrico di 5 ore e settimana. Modello, effort e contesto entrano qui con il contratto 1.11.
            item {
                it.pixelbox.cmwatch.wear.ui.components.SessionQuotaCard(
                    snapshot.state, s, SurfaceTransformation(spec),
                    // `morph` e non solo `transformedHeight`: porta i margini laterali che hanno tutte le voci della
                    // lista. Senza, il bordo tondo mangiava etichette e valori (snapshot 16/09 04:26 e 04:39).
                    Modifier.morph(this, spec),
                    // Con le animazioni spente l'arco si disegna subito al suo valore: è il caso di Paparazzi, dove il
                    // primo fotogramma lo coglierebbe ancora a zero (16/09 03:44), e dell'ambient.
                    animate = !it.pixelbox.cmwatch.wear.ui.ambient.animationsOff(),
                )
            }
            // Le misure della sessione in una voce propria: una voce per card, così ognuna si deforma scorrendo come
            // tutte le altre (Franz, 16/09 10:44). Senza dati la card non si disegna e la voce resta vuota.
            item {
                it.pixelbox.cmwatch.wear.ui.components.SessionMetersCard(
                    s, SurfaceTransformation(spec), Modifier.morph(this, spec),
                    animate = !it.pixelbox.cmwatch.wear.ui.ambient.animationsOff(),
                )
            }
            if (s.state != SessionState.GONE) {
                // Niente interruttore «Segui»: occupava mezzo schermo (Franz, 15/09 19:04). Si segue con la pressione
                // lunga sulla card o sulla riga della lista; lo stato lo dice la campanella in testata.
                // Niente bottoni «Terminale» e «Ascolta»: la card apre il Terminale, il ▶ sta in testata (Franz, 15/09 17:02).
            } else if (onReopen != null && onRelaunch != null) {
                // Se la conversazione non si può riprendere (il relay dice «use launch»), si riparte da capo.
                item {
                    WideButton(
                        stringResource(R.string.card_relaunch), onClick = onRelaunch, enabled = enabled,
                        transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                    )
                }
            }
        }
    }
}
