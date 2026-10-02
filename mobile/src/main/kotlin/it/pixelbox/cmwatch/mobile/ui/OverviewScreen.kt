package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.BriefCards
import it.pixelbox.cmwatch.rules.DayBars
import it.pixelbox.cmwatch.rules.PhoneOverview
import it.pixelbox.cmwatch.rules.QuotaHistory
import it.pixelbox.cmwatch.rules.SessionMeters
import it.pixelbox.cmwatch.rules.WorkPanel
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * La Panoramica (restyling 30/09): il brief dell'orologio in grande, schermata d'apertura del telefono. Quota con il
 * doppio anello e il ritmo, poi il lavoro: «Adesso», le domande, il contesto, «Oggi», la notte, l'ora dell'aggiornamento.
 */
@Composable
fun OverviewScreen(
    model: PhoneOverview.Model, stale: Freshness, onQuestion: () -> Unit, onSession: (name: String) -> Unit,
    /** Invii programmati per account (piano 30/09, Task 4): quanti e il primo a partire. */
    scheduled: Map<String, Pair<Int, Long>> = emptyMap(),
) {
    LazyColumn(
        Modifier.fillMaxSize().background(CmColors.bg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        (stale as? Freshness.Stale)?.let { st ->
            item(key = "stale") {
                Text(
                    stringResource(R.string.stale_data, st.minutes), color = CmColors.briefWarnInk, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.fillMaxWidth().background(CmColors.briefWarn, MaterialTheme.shapes.small).padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
        item(key = "h-quota") { SectionTitle(stringResource(R.string.ov_quota)) }
        if (model.rings.isEmpty()) item(key = "no-quota") {
            Text(stringResource(R.string.ov_no_quota), color = CmColors.text2, style = MaterialTheme.typography.bodyLarge)
        }
        items(model.rings, key = { "ring-" + it.account }) { QuotaRingCard(it, scheduled[it.account]) }
        item(key = "h-work") { SectionTitle(stringResource(R.string.ov_work)) }
        item(key = "now") { NowCard(model.now) }
        model.questions?.let { q -> item(key = "questions") { QuestionsCard(q, onQuestion) } }
        if (model.contexts.isNotEmpty()) item(key = "context") { ContextCard(model.contexts, onSession) }
        item(key = "today") { TodayCard(model.today) }
        if (model.nightQueued > 0) item(key = "night") { NightCard(model.nightQueued) }
        item(key = "updated") { UpdatedFooter(model.updated) }
    }
}

@Composable
private fun SectionTitle(text: String) =
    Text(text, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text, modifier = Modifier.padding(top = 8.dp, start = 4.dp))

/** La card del brief: superficie alta, angoli ampi, etichetta verde in testa. */
@Composable
private fun BriefShell(
    label: String, modifier: Modifier = Modifier, container: Color = CmColors.briefCard, labelColor: Color = CmColors.briefLabel,
    onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit,
) {
    val m = modifier.fillMaxWidth()
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = labelColor)
            content()
        }
    }
    if (onClick != null) Surface(onClick = onClick, color = container, shape = MaterialTheme.shapes.large, modifier = m) { body() }
    else Surface(color = container, shape = MaterialTheme.shapes.large, modifier = m) { body() }
}

/** Da 0 al valore quando la card entra in vista, con la molla lenta di Expressive; con le animazioni spente, subito. */
@Composable
private fun fillOnEntry(target: Float): Float {
    val off = animationsOff()
    var shown by remember { mutableStateOf(off) }
    LaunchedEffect(Unit) { shown = true }
    val v by animateFloatAsState(if (shown) target else 0f, if (off) androidx.compose.animation.core.snap() else MaterialTheme.motionScheme.slowSpatialSpec(), label = "fill")
    return v
}

/** Numero grande che rotola quando cambia, con l'unità piccola accanto. */
@Composable
private fun BigNumber(value: String, unit: String?, color: Color = CmColors.briefBig) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val off = animationsOff()
        AnimatedContent(value, transitionSpec = {
            if (off) fadeIn(androidx.compose.animation.core.snap()) togetherWith fadeOut(androidx.compose.animation.core.snap())
            else (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
        }, label = "roll") { v -> Text(v, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold), color = color) }
        unit?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = CmColors.briefSecondary, modifier = Modifier.padding(bottom = 6.dp)) }
    }
}

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
private fun hhmm(epoch: Long) = HHMM.format(Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()))

/** Giorno e ora del reset settimanale nella lingua del telefono, come la pillola «7d» del polso. */
@Composable
private fun dayTime(epoch: Long): String = DateTimeFormatter.ofPattern("EEE HH:mm", androidx.compose.ui.platform.LocalConfiguration.current.locales[0])
    .format(Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()))

/** Doppio anello come sul polso: 5 ore fuori (azzurro), settimana dentro (lavanda). Accanto numeri, reset e ritmo. */
@Composable
private fun QuotaRingCard(r: PhoneOverview.Ring, scheduled: Pair<Int, Long>? = null) {
    val h5 = fillOnEntry((r.h5 ?: 0) / 100f)
    val w7 = fillOnEntry((r.w7 ?: 0) / 100f)
    val outer = if ((r.h5 ?: 0) >= 90) CmColors.briefAlertRing else CmColors.briefRing
    BriefShell(r.account) {
        // Su uno schermo stretto (720 px, carattere grande) i numeri vanno sotto l'anello: accanto si spezzavano in
        // colonne di frammenti (revisione 30/09).
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val narrow = maxWidth < 280.dp
            val ring: @Composable () -> Unit = {
                // Più basse (Franz, 02/10 15:45: «compattiamo un po' in altezza queste 2 schede»): anello 92 dp, numeri più stretti.
                Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = 9.dp.toPx()
                        fun arc(inset: Float, color: Color, sweep: Float) = drawArc(
                            color, -90f, sweep, false, topLeft = Offset(inset, inset),
                            size = Size(size.width - 2 * inset, size.height - 2 * inset), style = Stroke(w, cap = StrokeCap.Round),
                        )
                        arc(w / 2, CmColors.briefTrack, 360f)
                        if (r.h5 != null) arc(w / 2, outer, 360f * h5)
                        val inner = w / 2 + w + 4.dp.toPx()
                        arc(inner, CmColors.briefTrack, 360f)
                        if (r.w7 != null) arc(inner, CmColors.briefWeek, 360f * w7)
                    }
                    AccountMark(r.personal, size = 18.dp)
                }
            }
            val numbers: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BigNumber(r.h5?.let { "$it%" } ?: "–", stringResource(R.string.ov_five_hours))
                    r.w7?.let { w ->
                        Text(
                            r.weekResetAt?.let { stringResource(R.string.ov_week_reset, w, dayTime(it)) } ?: stringResource(R.string.ov_week, w), style = MaterialTheme.typography.labelLarge, color = CmColors.briefWeekInk,
                            modifier = Modifier.background(CmColors.briefWeek, CircleShape).padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                    r.resetAt?.let { Text(stringResource(R.string.quota_resets_at, hhmm(it)), style = MaterialTheme.typography.bodyMedium, color = CmColors.briefSecondary) }
                    // Gli invii programmati alla ripartenza: l'anello è in percentuale, l'ora si dice a parole.
                    scheduled?.let { (n, at) ->
                        Text(pluralStringResource(R.plurals.ov_scheduled, n, n, hhmm(at)), style = MaterialTheme.typography.bodyMedium, color = CmColors.briefRing)
                    }
                    // Senza lettura recente niente numeri inventati: lo si dice, come la riga «dato vecchio» della regia.
                    if (r.stale) Text(stringResource(R.string.quota_old), style = MaterialTheme.typography.bodyMedium, color = CmColors.briefWarn)
                }
            }
            if (narrow) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ring()
                numbers(Modifier.fillMaxWidth())
            } else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ring()
                numbers(Modifier.weight(1f))
            }
        }
        r.pace?.let { PaceLine(it, r.resetAt!!) }
    }
}

/** Il ritmo della finestra: la linea dei campioni e, se si può dire, la proiezione tratteggiata fino al reset. */
@Composable
private fun PaceLine(p: QuotaHistory.Pace, resetAt: Long) {
    val start = resetAt - QuotaHistory.WINDOW_S
    val reveal = fillOnEntry(1f)
    // 22 dp: con la scala 0-100 la linea sta quasi sempre in basso, e 40 dp lasciavano un vuoto (Franz, 02/10 15:45).
    Canvas(Modifier.fillMaxWidth().height(22.dp)) {
        fun at(ts: Long, pct: Int) = Offset(size.width * (ts - start) / QuotaHistory.WINDOW_S.toFloat(), size.height * (1 - pct / 100f))
        drawLine(CmColors.briefTrack, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        val pts = p.points.map { at(it.ts, it.pct) }
        pts.zipWithNext().forEach { (a, b) ->
            drawLine(CmColors.briefRing, a, Offset(a.x + (b.x - a.x) * reveal, a.y + (b.y - a.y) * reveal), 2.5.dp.toPx(), StrokeCap.Round)
        }
        val last = p.points.lastOrNull()
        if (last != null && p.projected != null && p.at != null) drawLine(
            CmColors.briefSecondary, at(last.ts, last.pct), at(p.at!!, p.projected!!), 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)), alpha = reveal,
        )
    }
    if (p.projected != null && p.at != null) {
        Text(stringResource(R.string.ov_pace, p.projected!!, hhmm(p.at!!)), style = MaterialTheme.typography.bodyMedium, color = CmColors.briefSecondary)
    }
}

private fun segColour(s: WorkPanel.Seg): Color = when (s) {
    WorkPanel.Seg.WAITING -> CmColors.briefWarn
    WorkPanel.Seg.WORKING -> CmColors.briefRing
    WorkPanel.Seg.IDLE -> CmColors.briefGood
}

/** «Adesso»: quante lavorano, la barra a segmenti nei colori di stato, i tre contatori con il loro colore. */
@Composable
private fun NowCard(now: WorkPanel.Now) {
    BriefShell(stringResource(R.string.ov_now)) {
        BigNumber(now.working.toString(), pluralStringResource(R.plurals.ov_working, now.working))
        if (now.segments.isEmpty()) {
            Text(stringResource(R.string.ov_none_live), style = MaterialTheme.typography.bodyMedium, color = CmColors.briefSecondary)
            return@BriefShell
        }
        val grow = fillOnEntry(1f)
        Row(Modifier.fillMaxWidth().height(12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            now.segments.forEach { seg ->
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                    Box(Modifier.fillMaxWidth(grow).fillMaxHeight().background(segColour(seg), CircleShape))
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (now.waiting > 0) Legend(WorkPanel.Seg.WAITING, pluralStringResource(R.plurals.ov_legend_waiting, now.waiting, now.waiting))
            if (now.working > 0) Legend(WorkPanel.Seg.WORKING, pluralStringResource(R.plurals.ov_legend_working, now.working, now.working))
            if (now.idle > 0) Legend(WorkPanel.Seg.IDLE, pluralStringResource(R.plurals.ov_legend_idle, now.idle, now.idle))
        }
    }
}

@Composable
private fun Legend(seg: WorkPanel.Seg, text: String) = Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Box(Modifier.size(8.dp).background(segColour(seg), CircleShape))
    Text(text, style = MaterialTheme.typography.bodyMedium, color = CmColors.briefSecondary)
}

/** Le domande aperte: card tonale ambra, tocco sulla più vecchia. */
@Composable
private fun QuestionsCard(q: WorkPanel.Questions, onClick: () -> Unit) {
    BriefShell(stringResource(R.string.ov_questions), container = CmColors.briefWarn, labelColor = CmColors.briefWarnInk, onClick = onClick) {
        BigNumber(q.count.toString(), null, color = CmColors.briefWarnInk)
        Text(stringResource(R.string.ov_question_oldest, q.oldest, q.age), style = MaterialTheme.typography.bodyLarge, color = CmColors.briefWarnInk)
    }
}

private fun toneColour(t: BriefCards.Tone): Color = when (t) {
    BriefCards.Tone.ALERT -> CmColors.briefAlertRing
    BriefCards.Tone.WARN -> CmColors.briefWarn
    else -> CmColors.briefRing
}

/** Il contesto per sessione: nome e percentuale, modello ed effort, la barra nel colore delle soglie. */
@Composable
private fun ContextCard(rows: List<PhoneOverview.ContextRow>, onSession: (String) -> Unit) {
    BriefShell(stringResource(R.string.ov_context)) {
        rows.forEach { r ->
            val fill = fillOnEntry(SessionMeters.contextFraction(r.pct) ?: 0f)
            Column(Modifier.fillMaxWidth().clickable { onSession(r.name) }.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.name, style = MaterialTheme.typography.titleMedium, color = CmColors.briefBig, modifier = Modifier.weight(1f))
                    Text("${r.pct}%", style = MaterialTheme.typography.titleMedium, color = toneColour(r.tone))
                }
                listOfNotNull(r.model, r.effort).takeIf { it.isNotEmpty() }?.let {
                    Text(it.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = CmColors.briefSecondary)
                }
                Box(Modifier.fillMaxWidth().height(6.dp).background(CmColors.briefTrack, CircleShape)) {
                    Box(Modifier.fillMaxWidth(fill).fillMaxHeight().background(toneColour(r.tone), CircleShape))
                }
            }
        }
    }
}

/** «Oggi»: 24 colonne per ora; le ore non ancora arrivate sono un puntino, non una colonna vuota. */
@Composable
private fun TodayCard(bars: List<DayBars.Bar>) {
    val total = bars.sumOf { it.count }
    val peak = DayBars.peak(bars).coerceAtLeast(1)
    BriefShell(stringResource(R.string.ov_today)) {
        BigNumber(total.toString(), pluralStringResource(R.plurals.ov_today_events, total))
        val grow = fillOnEntry(1f)
        Canvas(Modifier.fillMaxWidth().height(64.dp)) {
            val step = size.width / 24f
            val w = step * 0.6f
            for (h in 0 until 24) {
                val x = h * step + (step - w) / 2
                val count = bars.firstOrNull { it.hour == h }?.count
                if (count == null) {
                    drawCircle(CmColors.briefTrack, 2.dp.toPx(), Offset(x + w / 2, size.height - 2.dp.toPx()))
                    continue
                }
                val tall = if (count == 0) 3.dp.toPx() else size.height * count / peak * grow
                drawRoundRect(
                    if (count == 0) CmColors.briefTrack else CmColors.briefRing, Offset(x, size.height - tall), Size(w, tall),
                    CornerRadius(w / 2, w / 2),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("0", "6", "12", "18").forEach {
                Text(it, style = MaterialTheme.typography.labelMedium, color = CmColors.briefSecondary, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NightCard(queued: Int) = BriefShell(stringResource(R.string.ov_night)) {
    BigNumber(queued.toString(), pluralStringResource(R.plurals.ov_night_queued, queued))
}

/** L'ora dell'aggiornamento; con il PC fermo diventa l'avviso rosso, come sul polso. */
@Composable
private fun UpdatedFooter(u: WorkPanel.Updated) {
    val text = when {
        u.stale -> stringResource(R.string.ov_stale, u.minutes, u.host)
        u.minutes <= 0 -> stringResource(R.string.ov_updated_now, u.host)
        else -> stringResource(R.string.ov_updated_ago, u.minutes, u.host)
    }
    Text(
        text, style = MaterialTheme.typography.bodyMedium, color = if (u.stale) CmColors.briefAlert else CmColors.text2,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}
