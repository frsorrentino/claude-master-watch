package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.MasterHome
import it.pixelbox.cmwatch.rules.PhoneOverview
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * I due blocchi in cima alla casa della master (design 01/10, approvato alle 23:02): «Per te» e il Quadro compatto.
 */

private val HM = DateTimeFormatter.ofPattern("HH:mm")
private fun hm(epoch: Long) = HM.format(Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()))

/**
 * «Per te»: al massimo tre righe con un tasto tonale ciascuna (il bottone pieno della schermata è Invia della chat);
 * «+N» apre tutte le righe in un foglio (revisione finale 02/10).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ForYouCard(all: List<MasterHome.Row>, onAction: (MasterHome.Row) -> Unit) {
    if (all.isEmpty()) return
    var open by rememberSaveable { mutableStateOf(false) }
    GlassCard(tint = CmColors.waiting) {
        RuledLabel(stringResource(R.string.fy_title), CmColors.waiting)
        all.take(MasterHome.MAX).forEach { row -> ForYouRow(row, onAction) }
        val more = all.size - MasterHome.MAX
        if (more > 0) TextButton(onClick = { open = true }) { Text(stringResource(R.string.fy_more, more), color = CmColors.actionIcon) }
    }
    if (open) androidx.compose.material3.ModalBottomSheet(onDismissRequest = { open = false }, containerColor = CmColors.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RuledLabel(stringResource(R.string.fy_title), CmColors.waiting)
            all.forEach { row -> ForYouRow(row) { open = false; onAction(it) } }
        }
    }
}

@Composable
private fun ForYouRow(row: MasterHome.Row, onAction: (MasterHome.Row) -> Unit) {
    val (title, detail, action) = when (row.kind) {
        MasterHome.Kind.QUESTION -> Triple(stringResource(R.string.fy_question, row.title), row.detail, R.string.fy_btn_answer)
        MasterHome.Kind.CONTEXT -> Triple(stringResource(R.string.fy_context, row.title, row.number ?: 0), stringResource(R.string.fy_context_detail), R.string.fy_btn_handoff)
        MasterHome.Kind.NIGHT_REPORT -> Triple(row.title, row.detail?.lineSequence()?.firstOrNull { it.isNotBlank() }, R.string.fy_btn_listen)
        MasterHome.Kind.NIGHT -> Triple(
            if ((row.number ?: 0) > 0) stringResource(R.string.fy_night_queued, row.number ?: 0) else stringResource(R.string.fy_night_empty),
            stringResource(R.string.fy_night_detail), R.string.fy_btn_add,
        )
        MasterHome.Kind.NEXT_STEP -> Triple(stringResource(R.string.fy_next, row.title), row.detail, R.string.fy_btn_start)
        MasterHome.Kind.SCHEDULED -> Triple(
            pluralStringResource(R.plurals.fy_scheduled, row.number ?: 1, row.number ?: 1),
            row.at?.let { stringResource(R.string.fy_scheduled_detail, hm(it)) }, R.string.fy_btn_see,
        )
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = CmColors.text)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 2, overflow = TextOverflow.Clip) }
        }
        FilledTonalButton(onClick = { onAction(row) }) { Text(stringResource(action)) }
    }
}

/**
 * Il Quadro compatto: led e «aggiornato», le quote con numeri da strumento, i chip delle sessioni (senza la master), con
 * la percentuale del contesto da 75 %. Tocco = il Quadro completo (il foglio della Panoramica).
 */
@Composable
fun QuadroStrip(model: PhoneOverview.Model, sessions: List<Session>, onOpen: () -> Unit, onSession: (String) -> Unit) {
    GlassCard(Modifier.clickable(onClick = onOpen)) {
        RuledLabel(stringResource(R.string.quadro_title), CmColors.idle, rule = false) {
            Led(stale = model.updated.stale)
            Text(
                if (model.updated.minutes <= 0) stringResource(R.string.ov_updated_now, model.updated.host)
                else stringResource(R.string.ov_updated_ago, model.updated.minutes, model.updated.host),
                style = MonoSmall, modifier = Modifier.weight(1f),
            )
            Text(stringResource(R.string.quadro_all), style = MaterialTheme.typography.labelLarge, color = CmColors.actionIcon)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            model.rings.forEach { r ->
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniGauge(r.h5, r.w7, r.stale)
                    Column {
                        InstrumentNumber(r.h5?.toString() ?: "–", "%")
                        Text(stringResource(R.string.quadro_week, r.account, r.w7 ?: 0), style = MaterialTheme.typography.labelSmall, color = CmColors.text2)
                        val note = when {
                            r.stale -> stringResource(R.string.quadro_old)
                            r.resetAt != null -> stringResource(R.string.quadro_resets, hm(r.resetAt!!))
                            else -> null
                        }
                        note?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = if (r.stale) CmColors.waiting else CmColors.text2) }
                    }
                }
            }
        }
        if (sessions.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            sessions.forEach { s -> SessionChip(s) { onSession(s.name) } }
        }
    }
}

/** Il chip di una sessione: barretta nel colore dello stato (con alone se lavora), nome, contesto da 75 %. */
@Composable
private fun SessionChip(s: Session, onClick: () -> Unit) {
    val color = when (s.state) {
        SessionState.WAITING -> CmColors.waiting
        SessionState.BUSY, SessionState.AWAITING -> CmColors.busy
        else -> CmColors.idle
    }
    val shape = RoundedCornerShape(50)
    val waiting = s.state == SessionState.WAITING
    Row(
        Modifier.clip(shape).background(if (waiting) CmColors.waiting.copy(alpha = 0.14f) else CmColors.surfaceHigh).clickable(onClick = onClick).height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val working = s.state == SessionState.BUSY || s.state == SessionState.AWAITING
        Box(Modifier.width(4.dp).fillMaxHeight().then(if (working) Modifier.shadow(6.dp, ambientColor = color, spotColor = color) else Modifier).background(color))
        Text(s.name, style = MaterialTheme.typography.labelLarge, color = CmColors.text, modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp, end = if ((s.context ?: 0) >= 75) 4.dp else 10.dp))
        s.context?.takeIf { it >= 75 }?.let { Text("$it%", style = MonoSmall.copy(color = CmColors.waiting), modifier = Modifier.padding(end = 10.dp)) }
    }
}

/** Due anelli aperti come sul polso: fuori le 5 ore, dentro la settimana; grigi con un dato vecchio. */
@Composable
private fun MiniGauge(h5: Int?, w7: Int?, stale: Boolean) {
    Canvas(Modifier.size(38.dp)) {
        val w = 4.dp.toPx()
        fun arc(inset: Float, pct: Int?, color: Color) {
            val tl = androidx.compose.ui.geometry.Offset(inset, inset)
            val sz = androidx.compose.ui.geometry.Size(size.width - 2 * inset, size.height - 2 * inset)
            drawArc(CmColors.briefTrack, 135f, 270f, false, topLeft = tl, size = sz, style = Stroke(w, cap = StrokeCap.Round))
            if (pct != null && pct > 0) drawArc(color, 135f, 270f * (pct.coerceIn(0, 100) / 100f), false, topLeft = tl, size = sz, style = Stroke(w, cap = StrokeCap.Round))
        }
        arc(w / 2, h5, if (stale) CmColors.text2 else CmColors.briefRing)
        arc(w / 2 + w * 1.6f, w7, if (stale) CmColors.text2.copy(alpha = 0.6f) else CmColors.briefWeek)
    }
}

/** Senza master aperta: la casa resta utile, con il tasto per riaprirla. */
@Composable
fun MasterAbsent(onReopen: () -> Unit) {
    GlassCard {
        Text(stringResource(R.string.master_absent), style = MaterialTheme.typography.titleMedium, color = CmColors.text)
        Text(stringResource(R.string.master_absent_detail), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
        Button(onClick = onReopen, colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary)) { Text(stringResource(R.string.reopen)) }
    }
}

/**
 * Casa A (mockup approvato da Franz, 02/10 07:38): l'ultimo esito della master in grande con i consigli, poi «Per te»,
 * le sessioni in corso una riga ciascuna e la quota in due barre. La conversazione intera è a un tocco.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HeroCard(hero: MasterHome.Hero?, master: Session, onSpeak: () -> Unit, onConversation: () -> Unit, onStep: (String) -> Unit, onSendStep: (String) -> Unit) {
    GlassCard(Modifier.clickable(onClick = onConversation)) {
        RuledLabel(hero?.at?.let { stringResource(R.string.home_last, hm(it)) } ?: stringResource(R.string.home_last_bare), CmColors.idle, rule = false) {
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(stateColor(master.state)))
        }
        hero?.let { h ->
            Text(h.headline, style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = CmColors.text)
            if (h.body.isNotBlank()) Text(h.body, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, maxLines = 4, overflow = TextOverflow.Clip)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (hero != null) TextButton(onClick = onSpeak) { Text("▶ " + stringResource(R.string.fy_btn_listen), color = CmColors.actionIcon) }
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            TextButton(onClick = onConversation) { Text(stringResource(R.string.home_conversation), color = CmColors.actionIcon) }
        }
        if (hero != null && hero.steps.isNotEmpty()) {
            androidx.compose.material3.HorizontalDivider(color = CmColors.line)
            Text(stringResource(R.string.next_steps), style = MonoSmall)
            // Come sotto l'ultima risposta: tocco = nel campo, pressione lunga = invio subito.
            hero.steps.forEach { step ->
                Text(
                    "↳ $step", style = MaterialTheme.typography.bodyLarge, color = CmColors.actionIcon,
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                        .combinedClickable(onClick = { onStep(step) }, onLongClick = { onSendStep(step) }).padding(vertical = 4.dp),
                )
            }
        }
    }
}

/** Le sessioni in corso, una riga ciascuna: stato, nome, che cosa fa, contesto. Tocco = la sua scheda. */
@Composable
fun RunningList(rows: List<MasterHome.Running>, onSession: (String) -> Unit) {
    if (rows.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RuledLabel(stringResource(R.string.home_running), CmColors.idle, rule = false) {
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text(rows.size.toString(), style = MonoSmall)
        }
        val shape = RoundedCornerShape(16.dp)
        Column(Modifier.fillMaxWidth().clip(shape).border(1.dp, CmColors.line, shape)) {
            rows.forEachIndexed { i, r ->
                if (i > 0) androidx.compose.material3.HorizontalDivider(color = CmColors.line)
                Row(
                    Modifier.fillMaxWidth().clickable { onSession(r.session.name) }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(stateColor(r.session.state)))
                    Text(r.session.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = CmColors.text, maxLines = 1)
                    Text(r.detail.orEmpty(), style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
                    r.session.context?.let { Text("$it%", style = MonoSmall.copy(color = if (it >= 75) CmColors.waiting else CmColors.text2)) }
                }
            }
        }
    }
}

/** La quota in due barre (le 5 ore), con la settimana nel testo; tocco = il Quadro completo. */
@Composable
fun QuotaBars(rings: List<PhoneOverview.Ring>, onOpen: () -> Unit, stacked: Boolean = false) {
    if (rings.isEmpty()) return
    // `stacked`: una barra per riga, a tutta larghezza (nel Registro, dentro un blocco che ha già il fondo).
    val cell: @Composable (PhoneOverview.Ring, Modifier) -> Unit = { r, mod ->
            Column(
                mod.clip(RoundedCornerShape(12.dp)).background(if (stacked) CmColors.bg else CmColors.surfaceLow).clickable(onClick = onOpen).padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    if (r.stale) stringResource(R.string.home_quota_old, r.account) else stringResource(R.string.home_quota, r.account, r.h5 ?: 0, r.w7 ?: 0),
                    style = MaterialTheme.typography.labelMedium, color = if (r.stale) CmColors.waiting else CmColors.text2, maxLines = 1, overflow = TextOverflow.Clip,
                )
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(CmColors.briefTrack)) {
                    Box(Modifier.fillMaxWidth(((r.h5 ?: 0).coerceIn(0, 100)) / 100f).fillMaxHeight().background(if (r.stale) CmColors.text2 else CmColors.briefRing))
                }
            }
    }
    if (stacked) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { rings.forEach { cell(it, Modifier.fillMaxWidth()) } }
    else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { rings.forEach { cell(it, Modifier.weight(1f)) } }
}

