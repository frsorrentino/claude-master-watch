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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
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
fun ForYouCard(
    all: List<MasterHome.Row>, onAction: (MasterHome.Row) -> Unit, now: Long = 0L,
    /** La domanda aperta di una sessione, per le opzioni della riga aperta. */
    question: (String) -> it.pixelbox.cmwatch.contract.Question? = { null },
    onAnswer: (session: String, n: Int) -> Unit = { _, _ -> }, onStep: (session: String, text: String) -> Unit = { _, _ -> },
    /** Chi lavora adesso (`MasterHome.working`), il gruppo «al lavoro» che ha preso il posto di «In corso» (02/10 21:29). */
    working: List<MasterHome.Running> = emptyList(), onSession: (String) -> Unit = {},
) {
    if (all.isEmpty() && working.isEmpty()) return
    var open by rememberSaveable { mutableStateOf(false) }
    // Una riga aperta alla volta (variante 3, Franz 02/10 21:11).
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    val item: @Composable (MasterHome.Row, (MasterHome.Row) -> Unit) -> Unit = { row, act ->
        val s = row.session
        if (s != null && (row.kind == MasterHome.Kind.QUESTION || row.kind == MasterHome.Kind.FINISHED)) {
            val id = row.kind.name + ":" + s
            AttentionRow(row, now, expanded == id, onToggle = { expanded = if (expanded == id) null else id }, question = question(s),
                onAnswer = { n -> onAnswer(s, n) }, onStep = { t -> onStep(s, t) }, onOpen = { act(row) })
        } else ForYouRow(row, act)
    }
    GlassCard(tint = CmColors.waiting) {
        RuledLabel(stringResource(R.string.fy_title), CmColors.waiting)
        // Prima chi ti aspetta e chi ha finito, poi chi lavora (non toglie posto alle prime), in fondo le righe di servizio.
        val shown = all.take(MasterHome.MAX)
        val (attention, service) = shown.partition { it.kind == MasterHome.Kind.QUESTION || it.kind == MasterHome.Kind.FINISHED }
        attention.forEach { row -> item(row, onAction) }
        if (working.isNotEmpty()) WorkingGroup(working, now, onSession)
        service.forEach { row -> item(row, onAction) }
        val more = all.size - MasterHome.MAX
        if (more > 0) TextButton(onClick = { open = true }) { Text(stringResource(R.string.fy_more, more), color = CmColors.actionIcon) }
    }
    if (open) androidx.compose.material3.ModalBottomSheet(onDismissRequest = { open = false }, containerColor = CmColors.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RuledLabel(stringResource(R.string.fy_title), CmColors.waiting)
            all.forEach { row -> item(row) { open = false; onAction(it) } }
        }
    }
}

/** «Al lavoro · N» dentro «Per te»: fulmine, nome, da quanto lavora, cosa sta facendo, contesto; il tocco apre la sessione. */
@Composable
private fun WorkingGroup(rows: List<MasterHome.Running>, now: Long, onSession: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.fy_working, rows.size).uppercase(), style = MonoSmall.copy(color = CmColors.briefRing))
        Box(Modifier.weight(1f).height(1.dp).background(CmColors.briefRing.copy(alpha = 0.25f)))
    }
    rows.forEach { r ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onSession(r.session.name) }.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_w_working), null, tint = CmColors.briefRing, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    androidx.compose.ui.text.buildAnnotatedString {
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)); append(r.session.name); pop()
                        (r.session.turnStarted ?: r.session.since).takeIf { it > 0 }?.let { from ->
                            pushStyle(androidx.compose.ui.text.SpanStyle(color = CmColors.text2)); append(" · " + it.pixelbox.cmwatch.contract.Durations.since(from, now)); pop()
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge, color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip,
                )
                r.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1, overflow = TextOverflow.Clip) }
            }
            r.session.context?.let { Text("$it%", style = MonoSmall.copy(color = if (it >= 75) CmColors.waiting else CmColors.briefRing)) }
        }
    }
}

/**
 * Variante 3 dei mockup di «Per te» (Franz, 02/10 21:11): chi ti aspetta (mano) e chi ha finito (bandierina), una riga per
 * sessione con l'inizio della domanda o dell'esito; il tocco la apre sul posto. Aperta: la domanda intera e le opzioni,
 * che rispondono subito (la prima piena, come nella chat), oppure l'esito e i consigli `Prossimi:`, che vanno a quella
 * sessione; sotto, la conversazione.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun AttentionRow(
    row: MasterHome.Row, now: Long, open: Boolean, onToggle: () -> Unit, question: it.pixelbox.cmwatch.contract.Question?,
    onAnswer: (Int) -> Unit, onStep: (String) -> Unit, onOpen: () -> Unit,
) {
    val waiting = row.kind == MasterHome.Kind.QUESTION
    val tone = if (waiting) CmColors.briefWarn else CmColors.briefGood
    val parsed = androidx.compose.runtime.remember(row.detail) { it.pixelbox.cmwatch.rules.NextSteps.parse(row.detail.orEmpty()) }
    val body = it.pixelbox.cmwatch.rules.Markdown.parse(parsed.text).text.trim()
    val age = row.at?.let { t -> if (waiting) it.pixelbox.cmwatch.contract.Durations.since(t, now) else hm(t) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (open) tone.copy(alpha = 0.08f) else Color.Transparent)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(if (waiting) R.drawable.ic_hand else R.drawable.ic_flag), null, tint = tone, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    androidx.compose.ui.text.buildAnnotatedString {
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)); append(row.title); pop()
                        age?.let { pushStyle(androidx.compose.ui.text.SpanStyle(color = tone)); append(" · $it"); pop() }
                    },
                    style = MaterialTheme.typography.bodyLarge, color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip,
                )
                if (!open) Text(body.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, maxLines = 1, overflow = TextOverflow.Clip)
            }
            androidx.compose.material3.Icon(
                if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                stringResource(if (open) R.string.steps_close else R.string.steps_open), tint = CmColors.text2,
            )
        }
        if (open) Column(Modifier.padding(start = 40.dp, end = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(body, style = MaterialTheme.typography.bodyMedium, color = CmColors.text)
            if (waiting) question?.options?.forEachIndexed { i, o ->
                val label = "${o.n} · ${o.label}"
                if (i == 0) Button(
                    onClick = { onAnswer(o.n) }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = tone, contentColor = CmColors.briefWarnInk),
                ) { Text(label, modifier = Modifier.fillMaxWidth()) }
                else FilledTonalButton(onClick = { onAnswer(o.n) }, modifier = Modifier.fillMaxWidth()) { Text(label, modifier = Modifier.fillMaxWidth()) }
            }
            if (!waiting && parsed.steps.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                parsed.steps.forEach { step ->
                    androidx.compose.material3.OutlinedButton(onClick = { onStep(step) }, border = androidx.compose.foundation.BorderStroke(1.dp, tone.copy(alpha = 0.5f))) {
                        Text(step, color = CmColors.text)
                    }
                }
            }
            TextButton(onClick = onOpen) { Text(stringResource(R.string.fy_open_conversation), color = CmColors.actionIcon) }
        }
    }
}

@Composable
private fun ForYouRow(row: MasterHome.Row, onAction: (MasterHome.Row) -> Unit) {
    val (title, detail, action) = when (row.kind) {
        MasterHome.Kind.QUESTION -> Triple(stringResource(R.string.fy_question, row.title), row.detail, R.string.fy_btn_answer)
        // Di solito sta in `AttentionRow`; qui solo una riga senza sessione.
        MasterHome.Kind.FINISHED -> Triple(stringResource(R.string.elsewhere_finished, row.title), row.detail?.lineSequence()?.firstOrNull { it.isNotBlank() }, R.string.elsewhere_open)
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

