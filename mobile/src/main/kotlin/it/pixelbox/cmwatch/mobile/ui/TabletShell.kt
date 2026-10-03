package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.ViewSidebar
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.TimelineEvent
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.ModelText
import it.pixelbox.cmwatch.rules.PhoneOverview
import it.pixelbox.cmwatch.rules.SessionMeters
import it.pixelbox.cmwatch.rules.Summary
import it.pixelbox.cmwatch.rules.Tablet
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * Il tablet (piano 04/10, mockup approvati da Franz il 04/10 00:03): sugli schermi da 840 dp la plancia. In alto la riga
 * di stato, a sinistra la barra di navigazione, poi le sessioni con le quote, al centro la conversazione (la `SessionSheet`
 * del telefono), a destra l'ispettore. I conti in `Tablet` (core).
 */

/** Le due viste del tablet: la plancia (una conversazione con l'ispettore) e le colonne (più conversazioni affiancate). */
enum class TabletView { BOARD, COLUMNS }

/** Le voci della barra di navigazione che aprono qualcosa fuori dalla vista. */
data class RailActions(
    val onQuadro: () -> Unit, val onRegister: () -> Unit, val onSearch: () -> Unit, val onLaunch: () -> Unit, val onSettings: () -> Unit,
)

/** Monospazio con le cifre tabulari: i numeri non ballano quando cambiano. */
private val Mono = TextStyle(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum", fontSize = 12.sp, color = CmColors.text2)
private val MonoLabel = Mono.copy(fontSize = 11.sp, letterSpacing = 1.5.sp)

/**
 * La plancia: riga di stato, barra, colonna delle sessioni (con `quota` in fondo), `center` e, se c'è posto,
 * `inspector`. `register` al posto della conversazione e dell'ispettore quando il Registro è aperto.
 */
@Composable
fun TabletShell(
    status: Tablet.Status, watch: Boolean?, view: TabletView, onView: (TabletView) -> Unit, registerOpen: Boolean, rail: RailActions,
    now: Long, sessions: @Composable () -> Unit, center: @Composable () -> Unit, inspector: (@Composable () -> Unit)?,
) {
    // Ctrl+K cerca, mentre si scrive in un campo (pezzo 6).
    Column(Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding().onPreviewKeyEvent { e ->
        if (e.isCtrlPressed && e.key == Key.K && e.type == KeyEventType.KeyDown) { rail.onSearch(); true } else false
    }) {
        TabletStatusBar(status, watch, now)
        HorizontalDivider(color = CmColors.line)
        Row(Modifier.weight(1f).fillMaxWidth()) {
            TabletRail(view, registerOpen, onView, rail)
            VerticalDivider(color = CmColors.line)
            if (view == TabletView.BOARD || registerOpen) {
                Box(Modifier.width(300.dp).fillMaxHeight()) { sessions() }
                VerticalDivider(color = CmColors.line)
            }
            Box(Modifier.weight(1f).fillMaxHeight()) { center() }
            if (inspector != null && view == TabletView.BOARD && !registerOpen) {
                VerticalDivider(color = CmColors.line)
                Box(Modifier.width(360.dp).fillMaxHeight()) { inspector() }
            }
        }
    }
}

/** Un pezzo della riga di stato: `priority` più bassa esce per prima quando manca posto; `end` lo mette a destra. */
private data class Slot(val priority: Int, val end: Boolean = false)

/**
 * La riga di stato, una riga sola (standard della master, 04/10): quando manca posto, col carattere grande o in una
 * finestra stretta, escono prima il PC, la notte e l'orologio, poi la settimana e gli altri account; restano la quota 5h,
 * chi ti aspetta e l'ora.
 */
@Composable
fun TabletStatusBar(status: Tablet.Status, watch: Boolean?, now: Long) {
    val sep = @Composable { Text("·", style = Mono, color = CmColors.text2.copy(alpha = 0.6f)) }
    PriorityRow(Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.tablet_brand), style = Mono.copy(color = CmColors.text, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp), modifier = Modifier.layoutId(Slot(80)))
        Row(Modifier.layoutId(Slot(10)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Led(status.stale, dot = 6.dp)
            Text(status.host, style = Mono)
            sep()
            Text(if (status.minutes == 0) stringResource(R.string.tablet_updated_now) else stringResource(R.string.tablet_updated_ago, status.minutes), style = Mono, color = if (status.stale) CmColors.waiting else CmColors.text2)
        }
        status.accounts.forEachIndexed { i, a ->
            // Il primo account (il personale) resta fino all'ultimo; gli altri escono prima della settimana.
            Row(Modifier.layoutId(Slot(if (i == 0) 95 else 30)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(a.account.uppercase(), style = MonoLabel)
                val h5 = a.h5
                if (a.stale || h5 == null) Text(stringResource(R.string.tablet_quota_stale), style = Mono, color = CmColors.waiting)
                else {
                    MiniBar(h5 / 100f, Modifier.width(44.dp))
                    Text(stringResource(R.string.quota_h5_short, h5) + (a.resetAt?.let { " → " + hhmm(it) } ?: ""), style = Mono, color = CmColors.text)
                }
            }
            a.w7?.takeIf { !a.stale }?.let { w ->
                Text(stringResource(R.string.tablet_quota_w7, w), style = Mono, modifier = Modifier.layoutId(Slot(if (i == 0) 40 else 25)))
            }
        }
        Text(
            listOf(
                pluralStringResource(R.plurals.tablet_open, status.open, status.open),
                pluralStringResource(R.plurals.tablet_working, status.working, status.working),
                pluralStringResource(R.plurals.tablet_finished, status.finished, status.finished),
            ).joinToString(" · ").uppercase(),
            style = MonoLabel, modifier = Modifier.layoutId(Slot(50)),
        )
        Text(
            stringResource(R.string.tablet_waiting, status.waiting).uppercase(),
            style = MonoLabel.copy(color = if (status.waiting > 0) CmColors.waiting else CmColors.text2, fontWeight = if (status.waiting > 0) FontWeight.Bold else null),
            modifier = Modifier.layoutId(Slot(90)),
        )
        Text(stringResource(R.string.tablet_night, status.night).uppercase(), style = MonoLabel, modifier = Modifier.layoutId(Slot(20)))
        if (watch != null) Row(Modifier.layoutId(Slot(15)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (watch) CmColors.idle else CmColors.stale))
            Text(stringResource(R.string.tablet_watch).uppercase(), style = MonoLabel)
        }
        Box(Modifier.layoutId(Slot(100, end = true))) { TabletClock(now) }
    }
}

/**
 * Una riga sola di pezzi con priorità (`Slot` come `layoutId`): si tengono dal più importante finché ci stanno, nell'ordine
 * scritto; quelli con `end` vanno a destra.
 */
@Composable
private fun PriorityRow(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gap = 18.dp.roundToPx()
        val loose = Constraints(maxHeight = constraints.maxHeight)
        val placeables = measurables.map { it.measure(loose) }
        val slots = measurables.map { it.layoutId as Slot }
        // Si tengono i pezzi dal più importante finché ci stanno, con lo spazio fra l'uno e l'altro.
        var used = 0
        val kept = mutableSetOf<Int>()
        slots.indices.sortedByDescending { slots[it].priority }.forEach { i ->
            val w = placeables[i].width + if (kept.isEmpty()) 0 else gap
            if (used + w <= constraints.maxWidth) { kept += i; used += w }
        }
        val height = placeables.filterIndexed { i, _ -> i in kept }.maxOfOrNull { it.height }?.coerceAtLeast(constraints.minHeight) ?: constraints.minHeight
        layout(constraints.maxWidth, height) {
            var x = 0
            slots.indices.filter { it in kept && !slots[it].end }.forEach { i ->
                placeables[i].placeRelative(x, (height - placeables[i].height) / 2); x += placeables[i].width + gap
            }
            var right = constraints.maxWidth
            slots.indices.filter { it in kept && slots[it].end }.reversed().forEach { i ->
                right -= placeables[i].width
                placeables[i].placeRelative(right, (height - placeables[i].height) / 2); right -= gap
            }
        }
    }
}

/** L'ora della riga di stato: ricompone solo sé stessa, allo scatto di ogni minuto; nei provini ferma su `still`. */
@Composable
private fun TabletClock(still: Long) {
    val frozen = LocalStill.current
    val now by produceState(if (frozen) still else System.currentTimeMillis() / 1000, frozen) {
        if (!frozen) while (true) {
            value = System.currentTimeMillis() / 1000
            kotlinx.coroutines.delay(60_000 - System.currentTimeMillis() % 60_000)
        }
    }
    Text(hhmm(now), style = Mono.copy(fontSize = 13.sp, color = CmColors.text))
}

@Composable
private fun MiniBar(fraction: Float, modifier: Modifier = Modifier, color: Color = CmColors.briefRing) {
    Box(modifier.height(4.dp).clip(RoundedCornerShape(2.dp)).background(CmColors.briefTrack)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(color))
    }
}

/** La barra di navigazione: plancia, colonne, utilizzo, registro, cerca; in fondo «+» per lanciare e le impostazioni. */
@Composable
private fun TabletRail(view: TabletView, registerOpen: Boolean, onView: (TabletView) -> Unit, rail: RailActions) {
    Column(
        Modifier.width(64.dp).fillMaxHeight().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RailItem(Icons.Rounded.GridView, stringResource(R.string.tablet_view_board), view == TabletView.BOARD && !registerOpen) { onView(TabletView.BOARD) }
        RailItem(Icons.Rounded.ViewColumn, stringResource(R.string.tablet_view_columns), view == TabletView.COLUMNS && !registerOpen) { onView(TabletView.COLUMNS) }
        RailItem(Icons.Rounded.BarChart, stringResource(R.string.menu_quadro), false, rail.onQuadro)
        RailItem(Icons.AutoMirrored.Rounded.Article, stringResource(R.string.menu_register), registerOpen, rail.onRegister)
        RailItem(Icons.Rounded.Search, stringResource(R.string.menu_search), false, rail.onSearch)
        Spacer(Modifier.weight(1f))
        FilledTonalIconButton(onClick = rail.onLaunch, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Add, stringResource(R.string.menu_launch)) }
        RailItem(Icons.Rounded.Settings, stringResource(R.string.settings), false, rail.onSettings)
    }
}

@Composable
private fun RailItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(if (selected) CmColors.surface else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick) { Icon(icon, label, tint = if (selected) CmColors.text else CmColors.text2) }
    }
}

/** La colonna delle sessioni: titolo con i conti, i gruppi del bisogno, la sessione scelta in una card; `bottom` in fondo. */
@Composable
fun TabletSessions(
    groups: List<Pair<Summary.Group, List<Summary.Row>>>, open: Int, closed: Int, current: String?, now: Long,
    onPick: (String) -> Unit, bottom: (@Composable () -> Unit)?,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.tab_sessions), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text)
            Text(stringResource(R.string.tablet_sessions_counts, open, closed), style = Mono, modifier = Modifier.weight(1f).padding(bottom = 3.dp))
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
            groups.forEach { (g, rows) ->
                item(key = "g-${g.name}") {
                    Box(Modifier.padding(start = 6.dp, end = 6.dp, top = 12.dp, bottom = 6.dp)) {
                        RuledLabel(stringResource(groupLabel(g), rows.size), groupTone(g))
                    }
                }
                items(rows, key = { "s-" + it.session.id }) { r -> TabletSessionRow(r, r.session.name == current, now) { onPick(r.session.name) } }
            }
        }
        bottom?.let { Box(Modifier.padding(10.dp)) { it() } }
    }
}

@Composable
private fun TabletSessionRow(r: Summary.Row, selected: Boolean, now: Long, onClick: () -> Unit) {
    val s = r.session
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(shape)
            .then(if (selected) Modifier.background(CmColors.surfaceLow).border(1.dp, CmColors.primary.copy(alpha = 0.7f), shape) else Modifier)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(groupTone(r.group)))
            Text(s.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
            Text(rowAge(r, now), style = Mono)
        }
        s.context?.let { c ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniBar(c / 100f, Modifier.weight(1f), contextColor(c))
                Text(stringResource(R.string.ctx_short, c), style = Mono)
            }
        }
        Tablet.line(r)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1, overflow = TextOverflow.Clip) }
    }
}

/** L'età della riga: da quanto aspetta o lavora, l'ora in cui ha finito, da quanto è ferma. */
@Composable
private fun rowAge(r: Summary.Row, now: Long): String = when (r.group) {
    Summary.Group.WAITING -> Durations.since(r.session.question?.askedAt ?: r.session.since, now)
    Summary.Group.WORKING -> Durations.since(r.session.turnStarted ?: r.session.since, now)
    Summary.Group.FINISHED -> r.at?.let { hhmm(it) }.orEmpty()
    Summary.Group.STILL -> Durations.since(r.session.since, now)
}

private fun contextColor(pct: Int): Color = when (SessionMeters.contextTone(pct)) {
    it.pixelbox.cmwatch.rules.BriefCards.Tone.ALERT -> CmColors.briefAlertRing
    it.pixelbox.cmwatch.rules.BriefCards.Tone.WARN -> CmColors.briefWarn
    else -> CmColors.briefRing
}

/**
 * Il pannello delle quote in fondo alla colonna: la finestra di 5 ore con la previsione al ritmo di adesso fino alla
 * ripartenza (piena fino a ora, tratteggiata dopo, soglie 80 e 100), poi la settimana con il ritmo medio al rinnovo.
 */
@Composable
fun TabletQuotaPanel(ring: PhoneOverview.Ring, now: Long) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(CmColors.surfaceLow).border(1.dp, Color.White.copy(alpha = 0.12f), shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PanelTitle(stringResource(R.string.tablet_quota_title, ring.account.uppercase()), ring.h5?.let { "$it%" })
        val reset = ring.resetAt
        val pace = ring.pace
        if (reset != null && pace != null) {
            val f = Tablet.forecast(pace, reset, now)
            ForecastChart(f, ring.h5 ?: f.points.lastOrNull()?.second ?: 0, Modifier.fillMaxWidth().height(96.dp))
            Text(
                f.projected?.let { stringResource(R.string.tablet_forecast, it, hhmm(reset)) } ?: stringResource(R.string.tablet_forecast_flat, hhmm(reset)),
                style = MaterialTheme.typography.bodySmall, color = CmColors.text,
            )
        } else {
            ring.h5?.let { MiniBar(it / 100f, Modifier.fillMaxWidth()) }
            Text(
                if (reset != null) stringResource(R.string.tablet_forecast_none, hhmm(reset)) else stringResource(R.string.ov_no_quota),
                style = MaterialTheme.typography.bodySmall, color = CmColors.text2,
            )
        }
        HorizontalDivider(color = CmColors.line, modifier = Modifier.padding(vertical = 2.dp))
        PanelTitle(stringResource(R.string.tablet_week_title, ring.account.uppercase()), ring.w7?.let { "$it%" })
        ring.w7?.let { w ->
            val projected = Tablet.weekProjected(w, ring.weekResetAt, now)
            WeekBar(w, projected)
            ring.weekResetAt?.let { at ->
                Text(
                    projected?.let { stringResource(R.string.tablet_week_forecast, it, dayTime(at)) } ?: stringResource(R.string.tablet_week_reset, dayTime(at)),
                    style = MaterialTheme.typography.bodySmall, color = CmColors.text,
                )
            }
        }
    }
}

@Composable
private fun PanelTitle(text: String, value: String?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MonoLabel, modifier = Modifier.weight(1f))
        value?.let { Text(it, style = Mono.copy(color = CmColors.text)) }
    }
}

/** La settimana: la parte usata piena, fino alla previsione al rinnovo più chiara, la tacca dell'80. */
@Composable
private fun WeekBar(pct: Int, projected: Int?) {
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
        drawRoundRect(CmColors.briefTrack, cornerRadius = r)
        projected?.let { drawRoundRect(CmColors.briefWeek.copy(alpha = 0.35f), size = Size(size.width * it / 100f, size.height), cornerRadius = r) }
        drawRoundRect(CmColors.briefWeek, size = Size(size.width * pct.coerceIn(0, 100) / 100f, size.height), cornerRadius = r)
        val x80 = size.width * 0.8f
        drawLine(CmColors.briefWarn, Offset(x80, -2f), Offset(x80, size.height + 2f), strokeWidth = 1.5.dp.toPx())
    }
}

/** Il grafico della finestra di 5 ore: 100, 80 tratteggiato, 50; i campioni pieni, la previsione tratteggiata; «ora». */
@Composable
private fun ForecastChart(f: Tablet.Forecast, nowPct: Int, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val label = Mono.copy(fontSize = 10.sp)
    val nowLabel = stringResource(R.string.tablet_now_pct, nowPct)
    val startLabel = hhmm(f.start)
    val endLabel = hhmm(f.resetAt)
    Canvas(modifier) {
        val left = 30.dp.toPx()
        val bottomPad = 16.dp.toPx()
        val w = size.width - left - 8.dp.toPx()
        val h = size.height - bottomPad - 4.dp.toPx()
        val top = 4.dp.toPx()
        fun x(f: Float) = left + w * f
        fun y(p: Int) = top + h * (1f - p.coerceIn(0, 100) / 100f)
        listOf(100 to CmColors.line, 50 to CmColors.line.copy(alpha = 0.6f)).forEach { (p, c) ->
            drawLine(c, Offset(left, y(p)), Offset(left + w, y(p)), strokeWidth = 1.dp.toPx())
            drawText(measurer, "$p", Offset(0f, y(p) - 7.sp.toPx()), label)
        }
        drawLine(CmColors.briefWarn, Offset(left, y(80)), Offset(left + w, y(80)), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        drawText(measurer, "80", Offset(0f, y(80) - 7.sp.toPx()), label.copy(color = CmColors.briefWarn))
        // «ora»: la riga verticale punteggiata.
        drawLine(CmColors.text2.copy(alpha = 0.6f), Offset(x(f.nowX), top), Offset(x(f.nowX), top + h), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 4f)))
        if (f.points.isNotEmpty()) {
            val line = Path().apply {
                f.points.forEachIndexed { i, (px, p) -> if (i == 0) moveTo(x(px), y(p)) else lineTo(x(px), y(p)) }
            }
            val fill = Path().apply { addPath(line); lineTo(x(f.points.last().first), top + h); lineTo(x(f.points.first().first), top + h); close() }
            drawPath(fill, CmColors.briefRing.copy(alpha = 0.12f))
            drawPath(line, CmColors.briefRing, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            val last = Offset(x(f.nowX), y(nowPct))
            f.projected?.let { p ->
                val end = Offset(x(1f), y(p))
                drawLine(CmColors.briefRing, last, end, strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                drawCircle(CmColors.bg, 4.5.dp.toPx(), end)
                drawCircle(CmColors.briefRing, 4.5.dp.toPx(), end, style = Stroke(2.dp.toPx()))
                val t = measurer.measure("$p%", label.copy(color = CmColors.text))
                drawText(t, topLeft = Offset((end.x - t.size.width).coerceAtLeast(left), end.y - t.size.height - 6.dp.toPx()))
            }
            drawCircle(CmColors.briefRing, 3.5.dp.toPx(), last)
        }
        val base = top + h + 3.dp.toPx()
        drawText(measurer, startLabel, Offset(left, base), label)
        val n = measurer.measure(nowLabel, label.copy(color = CmColors.text))
        drawText(n, topLeft = Offset((x(f.nowX) - n.size.width / 2f).coerceIn(left, left + w - n.size.width), base))
        val e = measurer.measure(endLabel, label)
        // L'ora della ripartenza a destra, se non si sovrappone a «ora».
        if (x(f.nowX) + n.size.width / 2f + 6.dp.toPx() < left + w - e.size.width) drawText(e, topLeft = Offset(left + w - e.size.width, base))
    }
}

/** La testata della conversazione al centro: il puntino, il nome, lo stato con da quanto. */
@Composable
fun RowScope.TabletConversationLead(r: Summary.Row?, s: Session, now: Long) {
    val g = r?.group ?: Summary.Group.STILL
    Box(Modifier.size(9.dp).clip(CircleShape).background(groupTone(g)))
    Spacer(Modifier.width(10.dp))
    Text(s.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f, fill = false))
    Spacer(Modifier.width(10.dp))
    val chip = stringResource(groupWord(g)).uppercase() + " · " + (r?.let { rowAge(it, now) } ?: Durations.since(s.since, now)).uppercase()
    Text(
        chip, style = MonoLabel.copy(color = groupTone(g)), maxLines = 1,
        modifier = Modifier.border(1.dp, groupTone(g).copy(alpha = 0.7f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

private fun groupWord(g: Summary.Group) = when (g) {
    Summary.Group.WAITING -> R.string.tablet_state_waiting
    Summary.Group.FINISHED -> R.string.tablet_state_finished
    Summary.Group.WORKING -> R.string.tablet_state_working
    Summary.Group.STILL -> R.string.tablet_state_still
}

/**
 * L'ispettore a destra: la sessione (progetto, account, aperta da, turno), tre numeri (contesto, prompt e commit di oggi),
 * l'obiettivo e la cronologia di oggi dal contratto 1.29. `loading` finché la prima cronologia non arriva.
 */
@Composable
fun TabletInspector(i: Tablet.Inspector, quotaH5: Int?, loading: Boolean) {
    val s = i.session
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.tablet_insp_title).uppercase(), style = MonoLabel)
        KeyValue(stringResource(R.string.tablet_insp_project), s.project)
        KeyValue(stringResource(R.string.tablet_insp_account), listOfNotNull(s.account, quotaH5?.let { stringResource(R.string.quota_h5_short, it) }).joinToString(" · "))
        KeyValue(stringResource(R.string.tablet_insp_opened), stringResource(R.string.tablet_insp_opened_value, hhmm(i.openedAt), Durations.since(i.openedAt, i.openedAt + i.openFor)))
        KeyValue(
            stringResource(R.string.tablet_insp_turn),
            i.turn?.let { t -> listOfNotNull(Durations.since(0, t), s.tool).joinToString(" · ") } ?: stringResource(R.string.tablet_insp_turn_none),
        )
        ModelText.short(s.model)?.let { m -> KeyValue(stringResource(R.string.tablet_insp_model), listOfNotNull(m, s.effort).joinToString(" · ")) }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox(s.context?.let { "$it%" } ?: "–", stringResource(R.string.tablet_insp_context), Modifier.weight(1f))
            StatBox(i.prompts?.toString() ?: "–", stringResource(R.string.tablet_insp_prompts), Modifier.weight(1f))
            StatBox(i.commits?.toString() ?: "–", stringResource(R.string.tablet_insp_commits), Modifier.weight(1f))
        }
        s.goal?.let { gl ->
            Text(stringResource(R.string.goal).uppercase(), style = MonoLabel, modifier = Modifier.padding(top = 6.dp))
            Text(gl.text, style = MaterialTheme.typography.bodyMedium, color = CmColors.briefLabel)
        }
        Box(Modifier.padding(top = 8.dp)) { RuledLabel(stringResource(R.string.tablet_insp_today), CmColors.text2) }
        when {
            i.today.isNotEmpty() -> i.today.forEach { e -> TimelineRow(e) }
            loading -> Text(stringResource(R.string.tablet_insp_today_loading), style = MaterialTheme.typography.bodySmall, color = CmColors.text2)
            else -> Text(stringResource(R.string.tablet_insp_today_none), style = MaterialTheme.typography.bodySmall, color = CmColors.text2)
        }
    }
}

@Composable
private fun KeyValue(key: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(key, style = Mono.copy(fontSize = 13.sp))
        Text(value, style = Mono.copy(fontSize = 13.sp, color = CmColors.text), textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(modifier.clip(shape).background(CmColors.surfaceLow).border(1.dp, Color.White.copy(alpha = 0.10f), shape).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(value, style = Mono.copy(fontSize = 22.sp, color = CmColors.text, fontWeight = FontWeight.Medium))
        Text(label.uppercase(), style = MonoLabel.copy(fontSize = 10.sp, letterSpacing = 1.sp))
    }
}

/** Una riga della cronologia: l'ora, il tipo in una pillola (verde o rossa per un test), il testo; il commit con il suo hash. */
@Composable
private fun TimelineRow(e: TimelineEvent) {
    val tone = when {
        e.kind == "test" && e.ok == false -> CmColors.briefAlertRing
        e.kind == "test" || (e.kind == "task" && e.ok == true) -> CmColors.briefGood
        e.kind == "outcome" -> CmColors.briefLabel
        else -> CmColors.briefRing
    }
    val kind = when (e.kind) {
        "commit" -> R.string.tablet_kind_commit
        "prompt" -> R.string.tablet_kind_prompt
        "test" -> R.string.tablet_kind_test
        "outcome" -> R.string.tablet_kind_outcome
        "task" -> R.string.tablet_kind_task
        else -> null
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(hhmm(e.at), style = Mono.copy(fontSize = 13.sp), modifier = Modifier.padding(top = 2.dp))
        Text(
            (kind?.let { stringResource(it) } ?: e.kind).uppercase(), style = MonoLabel.copy(color = tone, fontSize = 10.sp), maxLines = 1,
            modifier = Modifier.border(1.dp, tone.copy(alpha = 0.8f), RoundedCornerShape(5.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Text(
            if (e.kind == "commit" && e.ref != null) "${e.ref} ${e.text}" else e.text,
            style = MaterialTheme.typography.bodyMedium, color = CmColors.text, modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun hhmm(epoch: Long): String =
    DateTimeFormatter.ofPattern("HH:mm", LocalConfiguration.current.locales[0]).format(Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()))

@Composable
private fun dayTime(epoch: Long): String =
    DateTimeFormatter.ofPattern("EEEE HH:mm", LocalConfiguration.current.locales[0]).format(Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()))

/**
 * La vista a colonne (mockup 4-7): in alto la riga delle colonne, a sinistra la barra delle sessioni, fissa o richiudibile
 * (chiusa resta una striscia di puntini), poi da una a quattro colonne affiancate con conversazione e campo ciascuna.
 * `column` disegna la colonna di una sessione (la `SessionSheet` compatta); «Colonne» in alto e Indietro tornano alla plancia.
 */
@Composable
fun TabletColumns(
    status: Tablet.Status, ring: PhoneOverview.Ring?, now: Long, groups: List<Pair<Summary.Group, List<Summary.Row>>>, open: Int,
    columns: List<String>, onToggle: (String) -> Unit, onBoard: () -> Unit,
    barFixed: Boolean, barOpen: Boolean, onBar: () -> Unit, onBarFixed: (Boolean) -> Unit,
    column: @Composable (Summary.Row) -> Unit,
) {
    val rows = groups.flatMap { it.second }
    Column(Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding()) {
        ColumnsBar(status, ring, now, rows.filter { it.session.name in columns }.sortedBy { columns.indexOf(it.session.name) }, barOpen, onBar, onBoard)
        HorizontalDivider(color = CmColors.line)
        Row(Modifier.weight(1f).fillMaxWidth()) {
            if (barOpen) {
                Column(Modifier.width(300.dp).fillMaxHeight()) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.tab_sessions), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text)
                        Text(pluralStringResource(R.plurals.tablet_open, open, open), style = Mono, modifier = Modifier.padding(bottom = 3.dp))
                    }
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(rows, key = { "c-" + it.session.id }) { r -> ColumnsBarRow(r, r.session.name in columns, now) { onToggle(r.session.name) } }
                    }
                    BarModePanel(barFixed, onBarFixed)
                }
            } else {
                // La barra chiusa: un puntino per sessione, nel colore del suo stato; il tocco la mette o la toglie dalle colonne.
                Column(Modifier.width(52.dp).fillMaxHeight().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rows.forEach { r ->
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).clickable(onClickLabel = r.session.name) { onToggle(r.session.name) }
                                .then(if (r.session.name in columns) Modifier.background(CmColors.surface) else Modifier),
                            contentAlignment = Alignment.Center,
                        ) { Box(Modifier.size(14.dp).clip(CircleShape).background(groupTone(r.group))) }
                    }
                }
            }
            VerticalDivider(color = CmColors.line)
            if (columns.isEmpty()) Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.tablet_columns_empty), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
            } else Row(Modifier.weight(1f).fillMaxHeight().dotGrid().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                columns.forEach { name ->
                    val r = rows.firstOrNull { it.session.name == name } ?: return@forEach
                    // Chi ti aspetta ha il bordo arancio, come nel mockup.
                    val shape = RoundedCornerShape(18.dp)
                    val border = if (r.group == Summary.Group.WAITING) CmColors.waiting else Color.White.copy(alpha = 0.12f)
                    androidx.compose.runtime.key(name) {
                        Box(Modifier.weight(1f).fillMaxHeight().clip(shape).border(1.dp, border, shape)) { column(r) }
                    }
                }
            }
        }
    }
}

/** La riga in alto delle colonne: il tasto della barra, il marchio, PC, quota 5h con la previsione, «Colonne», le pillole. */
@Composable
private fun ColumnsBar(status: Tablet.Status, ring: PhoneOverview.Ring?, now: Long, pinned: List<Summary.Row>, barOpen: Boolean, onBar: () -> Unit, onBoard: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (barOpen) CmColors.surface else Color.Transparent), contentAlignment = Alignment.Center) {
            IconButton(onClick = onBar) {
                Icon(
                    Icons.Rounded.ViewSidebar, stringResource(if (barOpen) R.string.tablet_bar_close else R.string.tablet_bar_open), tint = CmColors.text,
                    modifier = Modifier.graphicsLayer(scaleX = -1f),
                )
            }
        }
        PriorityRow(Modifier.weight(1f).heightIn(min = 52.dp).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.tablet_brand), style = Mono.copy(color = CmColors.text, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp), modifier = Modifier.layoutId(Slot(80)))
            Row(Modifier.layoutId(Slot(10)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Led(status.stale, dot = 6.dp)
                Text(status.host, style = Mono)
                Text("·", style = Mono)
                Text(if (status.minutes == 0) stringResource(R.string.tablet_updated_now) else stringResource(R.string.tablet_updated_ago, status.minutes), style = Mono)
            }
            ring?.h5?.let { h5 ->
                val projected = ring.pace?.projected
                val reset = ring.resetAt
                Text(
                    stringResource(R.string.quota_h5_short, h5) + if (projected != null && reset != null) " → " + stringResource(R.string.tablet_projected_at, projected, hhmm(reset)) else "",
                    style = Mono.copy(color = CmColors.text), modifier = Modifier.layoutId(Slot(95)),
                )
            }
            ring?.w7?.let { Text(stringResource(R.string.tablet_quota_w7, it), style = Mono, modifier = Modifier.layoutId(Slot(40))) }
            Text(
                stringResource(R.string.tablet_view_columns).uppercase(), style = MonoLabel,
                modifier = Modifier.layoutId(Slot(60)).clip(RoundedCornerShape(6.dp)).clickable(onClickLabel = stringResource(R.string.tablet_view_board), onClick = onBoard).padding(horizontal = 6.dp, vertical = 4.dp),
            )
            pinned.forEachIndexed { i, r ->
                Row(
                    Modifier.layoutId(Slot(70 - i)).border(1.dp, CmColors.primary.copy(alpha = 0.6f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(groupTone(r.group)))
                    Text(r.session.name, style = MaterialTheme.typography.labelLarge, color = CmColors.text, maxLines = 1)
                }
            }
            Box(Modifier.layoutId(Slot(100, end = true))) { TabletClock(now) }
        }
    }
}

/** Una sessione nella barra delle colonne: stato, contesto, e «in colonna» o «+ colonna» a destra. */
@Composable
private fun ColumnsBarRow(r: Summary.Row, inColumn: Boolean, now: Long, onClick: () -> Unit) {
    val s = r.session
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .then(if (inColumn) Modifier.background(CmColors.surfaceLow).border(1.dp, CmColors.primary.copy(alpha = 0.7f), shape) else Modifier)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(groupTone(r.group)))
        Column(Modifier.weight(1f)) {
            Text(s.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip)
            Text(stateLine(r, now), style = MonoLabel)
        }
        Text(
            stringResource(if (inColumn) R.string.tablet_in_column else R.string.tablet_add_column).uppercase(),
            style = MonoLabel.copy(color = if (inColumn) CmColors.actionIcon else CmColors.text2),
        )
    }
}

/** «AL LAVORO · 6 MIN · CTX 61%»: lo stato a parole, da quanto (non per chi ha finito, che dice l'ora), il contesto. */
@Composable
private fun stateLine(r: Summary.Row, now: Long): String = listOfNotNull(
    stringResource(groupWord(r.group)),
    rowAge(r, now).takeIf { r.group == Summary.Group.WORKING || r.group == Summary.Group.WAITING },
    r.session.context?.let { stringResource(R.string.ctx_short, it) },
).joinToString(" · ").uppercase()

/** Barra fissa o richiudibile, ricordata nelle preferenze. */
@Composable
private fun BarModePanel(fixed: Boolean, onFixed: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().padding(10.dp).clip(shape).background(CmColors.surfaceLow).border(1.dp, Color.White.copy(alpha = 0.10f), shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.tablet_bar).uppercase(), style = MonoLabel)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CmColors.bg).padding(3.dp)) {
            listOf(true to R.string.tablet_bar_fixed, false to R.string.tablet_bar_collapsible).forEach { (v, label) ->
                val on = fixed == v
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (on) CmColors.primary else Color.Transparent)
                        .clickable(onClick = { onFixed(v) }).padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = if (on) CmColors.onPrimary else CmColors.text) }
            }
        }
        Text(stringResource(R.string.tablet_bar_hint), style = MaterialTheme.typography.bodySmall, color = CmColors.text2)
    }
}

/** La testata di una colonna: puntino, nome, × per toglierla; sotto lo stato, la barra e la cifra del contesto. */
@Composable
fun TabletColumnHeader(r: Summary.Row, now: Long, onClose: () -> Unit) {
    val s = r.session
    Column(Modifier.fillMaxWidth().background(CmColors.bg)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(groupTone(r.group)))
            Text(s.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.tablet_close_column, s.name), tint = CmColors.text2) }
        }
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                listOfNotNull(stringResource(groupWord(r.group)), rowAge(r, now).takeIf { r.group == Summary.Group.WORKING || r.group == Summary.Group.WAITING }).joinToString(" · ").uppercase(),
                style = MonoLabel.copy(color = groupTone(r.group)), maxLines = 1,
            )
            s.context?.let { c ->
                MiniBar(c / 100f, Modifier.weight(1f), contextColor(c))
                Text(stringResource(R.string.ctx_short, c), style = Mono)
            }
        }
        HorizontalDivider(color = CmColors.line)
    }
}

/**
 * Le bozze del campo, una per sessione e domanda come la `rememberSaveable` della scheda, tenute sopra l'interruttore dei
 * 840 dp: la scheda del telefono e quella della plancia stanno in punti diversi della composizione, e la bozza si perdeva
 * quando la finestra cambiava larghezza (standard della master, 04/10).
 */
class DraftStore(initial: Map<String, String> = emptyMap()) {
    private val map = androidx.compose.runtime.mutableStateMapOf<String, String>().apply { putAll(initial) }

    fun state(s: Session): androidx.compose.runtime.MutableState<String> {
        val key = s.id + "/" + s.question?.id.orEmpty()
        return object : androidx.compose.runtime.MutableState<String> {
            override var value: String
                get() = map[key].orEmpty()
                set(v) { if (v.isEmpty()) map.remove(key) else map[key] = v }
            override fun component1() = value
            override fun component2(): (String) -> Unit = { value = it }
        }
    }

    companion object {
        val Saver = androidx.compose.runtime.saveable.Saver<DraftStore, ArrayList<String>>(
            save = { st -> ArrayList(st.map.flatMap { listOf(it.key, it.value) }) },
            restore = { l -> DraftStore(l.chunked(2).filter { it.size == 2 }.associate { it[0] to it[1] }) },
        )
    }
}
