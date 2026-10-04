package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

/** Monospazio con le cifre tabulari: i numeri non ballano quando cambiano. */
private val Mono = TextStyle(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum", fontSize = 12.sp, color = CmColors.text2)
private val MonoLabel = Mono.copy(fontSize = 11.sp, letterSpacing = 1.5.sp)

@Composable
private fun MiniBar(fraction: Float, modifier: Modifier = Modifier, color: Color = CmColors.briefRing) {
    Box(modifier.height(4.dp).clip(RoundedCornerShape(2.dp)).background(CmColors.briefTrack)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(color))
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
fun TabletQuotaPanel(ring: PhoneOverview.Ring, now: Long, dataStale: Boolean = false) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(CmColors.surfaceLow).border(1.dp, Color.White.copy(alpha = 0.12f), shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PanelTitle(stringResource(R.string.tablet_quota_label), ring.account, ring.h5?.let { "$it%" })
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
            // Senza ritmo si dice perché: un dato vecchio (si proietterebbe dal passato) o letture ancora troppo poche.
            Text(
                when {
                    reset == null -> stringResource(R.string.ov_no_quota)
                    dataStale || ring.stale -> stringResource(R.string.tablet_forecast_stale, hhmm(reset))
                    else -> stringResource(R.string.tablet_forecast_none, hhmm(reset))
                },
                style = MaterialTheme.typography.bodySmall, color = CmColors.text2,
            )
        }
        HorizontalDivider(color = CmColors.line, modifier = Modifier.padding(vertical = 2.dp))
        PanelTitle(stringResource(R.string.tablet_week_label), ring.account, ring.w7?.let { "$it%" })
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

/**
 * Il titolo di un pannello: «QUOTA 5H · PERSONAL» e la cifra a destra su una riga; quando non ci sta (carattere grande)
 * l'account va su una riga sua, senza «·», invece di spezzarsi a frammenti.
 */
@Composable
private fun PanelTitle(label: String, account: String, value: String?) {
    Layout(
        content = {
            Text(label.uppercase(), style = MonoLabel)
            Text("·", style = MonoLabel)
            Text(account.uppercase(), style = MonoLabel)
            Text(value.orEmpty(), style = Mono.copy(color = CmColors.text))
        },
        modifier = Modifier.fillMaxWidth(),
    ) { m, c ->
        // Si decide sulle larghezze intrinseche, poi ogni pezzo si misura una volta sola.
        val gap = 8.dp.roundToPx()
        val free = Constraints()
        val w = { i: Int -> m[i].maxIntrinsicWidth(Constraints.Infinity) }
        val v = m[3].measure(free)
        if (w(0) + gap + w(1) + gap + w(2) + gap + v.width <= c.maxWidth) {
            val labelP = m[0].measure(free); val dot = m[1].measure(free); val acc = m[2].measure(free)
            val h = maxOf(labelP.height, v.height)
            layout(c.maxWidth, h) {
                labelP.placeRelative(0, 0); dot.placeRelative(labelP.width + gap, 0); acc.placeRelative(labelP.width + 2 * gap + dot.width, 0)
                v.placeRelative(c.maxWidth - v.width, (h - v.height) / 2)
            }
        } else {
            val labelW = m[0].measure(Constraints(maxWidth = (c.maxWidth - v.width - gap).coerceAtLeast(0)))
            val accW = m[2].measure(Constraints(maxWidth = c.maxWidth))
            val top = maxOf(labelW.height, v.height)
            layout(c.maxWidth, top + accW.height) {
                labelW.placeRelative(0, 0); v.placeRelative(c.maxWidth - v.width, (top - v.height) / 2); accW.placeRelative(0, top)
            }
        }
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
@OptIn(ExperimentalLayoutApi::class)
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
        // Contratto 1.16: la sessione in bassa priorità (oltre il limite, o proposta); sul telefono sta sotto la testata.
        it.pixelbox.cmwatch.rules.SessionsText.priority(s, stringResource(R.string.tablet_priority_active), stringResource(R.string.tablet_priority_offered))?.let { p ->
            KeyValue(stringResource(R.string.tablet_insp_priority), p)
        }
        // Col carattere grande due per riga: le etichette restano grandi e vanno a capo fra le parole, mai dentro.
        val perRow = if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.25f) 2 else 3
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = perRow,
        ) {
            StatBox(s.context?.let { "$it%" } ?: "–", stringResource(R.string.tablet_insp_context), Modifier.weight(1f).fillMaxRowHeight())
            StatBox(i.prompts?.toString() ?: "–", stringResource(R.string.tablet_insp_prompts), Modifier.weight(1f).fillMaxRowHeight())
            StatBox(i.commits?.toString() ?: "–", stringResource(R.string.tablet_insp_commits), Modifier.weight(1f).fillMaxRowHeight())
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
        Text(label.uppercase(), style = MonoLabel.copy(fontSize = 10.sp, letterSpacing = 1.sp, lineBreak = androidx.compose.ui.text.style.LineBreak.Heading))
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
    DateTimeFormatter.ofPattern("EEEE'\u00A0'HH:mm", LocalConfiguration.current.locales[0]).format(Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()))

/**
 * La testata di una colonna: l'icona della sessione come sul telefono, il nome, × per toglierla; sotto lo stato, la barra e
 * la cifra del contesto. `drag` è la presa: trascinata sopra un'altra colonna, si scambiano di posto.
 */
@Composable
fun TabletColumnHeader(r: Summary.Row, now: Long, onClose: () -> Unit, drag: Modifier = Modifier) {
    val s = r.session
    Column(Modifier.fillMaxWidth().background(CmColors.bg).then(drag)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SessionBadge(s, 24.dp)
            Text(s.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.tablet_close_column, s.name), tint = CmColors.text2) }
        }
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                listOfNotNull(stringResource(groupWord(r.group)), rowAge(r, now).takeIf { r.group == Summary.Group.WORKING || r.group == Summary.Group.WAITING }).joinToString(" · ").uppercase(),
                style = MonoLabel.copy(color = groupTone(r.group)),
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
 * Il tablet, seconda versione (Franz, 04/10 14:31-16:36): una vista sola, senza barre. La home del telefono in una colonna
 * di lato, a sinistra o a destra con un clic su ⇄; da una a quattro sessioni in colonne affiancate, aggiunte toccando le
 * loro schede nella home. Una colonna trascinata per la testata scambia il posto con quella su cui la si lascia; i bordi
 * fra le colonne si trascinano a scatti di un dodicesimo (`Tablet.Shares`). Con `inspector`, i dettagli dall'altro lato.
 */
@Composable
fun TabletDesk(
    home: @Composable () -> Unit, homeRight: Boolean, onHomeSide: () -> Unit,
    columns: List<String>, shares: List<Int>, onSwap: (Int, Int) -> Unit, onShares: (List<Int>) -> Unit,
    column: @Composable (name: String, drag: Modifier) -> Unit,
    empty: @Composable () -> Unit,
    /** Al posto delle colonne, per esempio il Registro aperto dal menu della home. */
    override: (@Composable () -> Unit)? = null,
    inspector: (@Composable () -> Unit)? = null,
) {
    // La home cambia lato scivolando sopra le colonne, che scivolano dall'altra parte (Franz, 04/10 22:15: «spartane»). Le
    // parti restano nello stesso punto della composizione e cambiano solo posizione: la home non si ricrea e tiene lo
    // scorrimento.
    val off = animationsOff()
    val side by androidx.compose.animation.core.animateFloatAsState(
        if (homeRight) 1f else 0f,
        if (off) androidx.compose.animation.core.snap() else androidx.compose.animation.core.spring(dampingRatio = 0.86f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "homeSide",
    )
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding()) {
        val homeW = 400.dp
        val handleW = 28.dp
        val detailsW = if (inspector != null) 341.dp else 0.dp
        val total = maxWidth
        // Da sinistra: home, maniglia, colonne, dettagli; con la home a destra l'ordine si rovescia.
        val at = { left: androidx.compose.ui.unit.Dp, right: androidx.compose.ui.unit.Dp ->
            Modifier.offset { androidx.compose.ui.unit.IntOffset((left + (right - left) * side).roundToPx(), 0) }
        }
        Box(at(homeW + handleW, detailsW).width(total - homeW - handleW - detailsW).fillMaxHeight().dotGrid()) {
            if (override != null) override()
            else if (columns.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { empty() }
            else Columns(columns, shares, onSwap, onShares, column)
        }
        inspector?.let { i ->
            Row(at(total - detailsW, 0.dp).width(detailsW).fillMaxHeight()) {
                if (!homeRight) VerticalDivider(color = CmColors.line)
                Box(Modifier.weight(1f).fillMaxHeight()) { i() }
                if (homeRight) VerticalDivider(color = CmColors.line)
            }
        }
        Box(at(homeW, total - homeW - handleW).width(handleW).fillMaxHeight()) { HomeSideHandle(homeRight, onHomeSide) }
        // A metà strada la home passa sopra le colonne, con un'ombra che c'è solo mentre si sposta.
        Box(
            at(0.dp, total - homeW).width(homeW).fillMaxHeight().zIndex(1f)
                .graphicsLayer { shadowElevation = 32f * (1f - kotlin.math.abs(side * 2f - 1f)) }
                .background(CmColors.bg),
        ) { home() }
    }
}

/** Il filo fra la home e le colonne, con ⇄ in alto: un clic porta la home dall'altro lato. */
@Composable
private fun HomeSideHandle(homeRight: Boolean, onHomeSide: () -> Unit) {
    Box(Modifier.width(28.dp).fillMaxHeight()) {
        Box(Modifier.align(Alignment.Center).width(1.dp).fillMaxHeight().background(CmColors.line))
        Box(
            Modifier.align(Alignment.TopCenter).padding(top = 10.dp).size(28.dp).clip(CircleShape).background(CmColors.surface)
                .clickable(onClickLabel = stringResource(if (homeRight) R.string.tablet_home_left else R.string.tablet_home_right), onClick = onHomeSide),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.SwapHoriz, stringResource(if (homeRight) R.string.tablet_home_left else R.string.tablet_home_right), tint = CmColors.text2, modifier = Modifier.size(18.dp)) }
    }
}

/**
 * Le colonne a scatti. Larghezze in dodicesimi dello spazio; un bordo trascinato si vede muovere sotto il dito e al rilascio
 * scatta alla parte intera più vicina (mai sotto i due dodicesimi). Una testata trascinata porta con sé la sua colonna, in
 * primo piano; lasciata sopra un'altra, le due si scambiano.
 */
@Composable
private fun Columns(
    columns: List<String>, shares: List<Int>, onSwap: (Int, Int) -> Unit, onShares: (List<Int>) -> Unit,
    column: @Composable (name: String, drag: Modifier) -> Unit,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val off = animationsOff()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().padding(10.dp)) {
        val gapPx = with(density) { 12.dp.toPx() }
        val widthPx = constraints.maxWidth.toFloat() - gapPx * (columns.size - 1)
        val unit = widthPx / Tablet.Shares.TOTAL
        var border by remember { mutableStateOf(-1) }
        var borderPx by remember { mutableStateOf(0f) }
        var dragged by remember { mutableStateOf<String?>(null) }
        var draggedPx by remember { mutableStateOf(0f) }
        // Le larghezze di adesso: quelle salvate, più il bordo che si sta trascinando (fra i limiti dei due dodicesimi).
        val live = shares.map { it * unit }.toMutableList().also { w ->
            if (border in 0 until w.size - 1) {
                val pair = w[border] + w[border + 1]
                val left = (w[border] + borderPx).coerceIn(Tablet.Shares.MIN * unit, pair - Tablet.Shares.MIN * unit)
                w[border] = left; w[border + 1] = pair - left
            }
        }
        val lefts = live.runningFold(0f) { x, w -> x + w + gapPx }
        // La colonna sotto il centro di quella trascinata: lì cadrebbe, e intanto scivola già nel posto lasciato libero
        // (Franz, 04/10 22:15: lo scambio era «spartano»).
        val from = columns.indexOf(dragged)
        val over = if (from < 0) -1 else lefts.dropLast(1).indexOfLast { it <= lefts[from] + live[from] / 2 + draggedPx }.coerceIn(0, columns.size - 1)
        val overNow by androidx.compose.runtime.rememberUpdatedState(over)
        val leftsNow by androidx.compose.runtime.rememberUpdatedState(lefts)
        val spec: androidx.compose.animation.core.AnimationSpec<Float> =
            if (off) androidx.compose.animation.core.snap() else androidx.compose.animation.core.spring(dampingRatio = 0.82f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
        columns.forEachIndexed { i, name ->
            androidx.compose.runtime.key(name) {
                val slot = if (from >= 0 && i == over && over != from) from else i
                val dragging = name == dragged
                val resizing = border >= 0
                // Posizione e larghezza scivolano verso il loro posto: dopo uno scambio, al rilascio di un bordo (che scatta al
                // dodicesimo), quando una colonna arriva o se ne va. Sotto il dito invece seguono senza ritardo.
                val x = remember { androidx.compose.animation.core.Animatable(lefts[slot]) }
                val w = remember { androidx.compose.animation.core.Animatable(live[slot]) }
                androidx.compose.runtime.LaunchedEffect(lefts[slot], live[slot], dragging, resizing) {
                    when {
                        dragging -> w.snapTo(live[slot])
                        resizing -> { x.snapTo(lefts[slot]); w.snapTo(live[slot]) }
                        else -> { launch { x.animateTo(lefts[slot], spec) }; w.animateTo(live[slot], spec) }
                    }
                }
                // Presa per la testata la colonna si alza un poco; una colonna nuova entra sfumando.
                val lift by androidx.compose.animation.core.animateFloatAsState(if (dragging) 1f else 0f, if (off) androidx.compose.animation.core.snap() else androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow), label = "lift")
                val appear = remember { androidx.compose.animation.core.Animatable(if (off) 1f else 0f) }
                androidx.compose.runtime.LaunchedEffect(Unit) { appear.animateTo(1f, androidx.compose.animation.core.tween(220)) }
                val drag = Modifier.pointerInput(name, columns) {
                    detectDragGestures(
                        onDragStart = { dragged = name; draggedPx = 0f },
                        onDragEnd = {
                            val target = overNow
                            dragged = null; draggedPx = 0f
                            if (target >= 0 && target != i) onSwap(i, target)
                        },
                        onDragCancel = { dragged = null; draggedPx = 0f },
                    ) { change, amount ->
                        change.consume(); draggedPx += amount.x
                        scope.launch { x.snapTo(leftsNow[i] + draggedPx) }
                    }
                }
                Box(
                    Modifier.offset { androidx.compose.ui.unit.IntOffset(x.value.roundToInt(), 0) }
                        .width(with(density) { w.value.toDp() }).fillMaxHeight()
                        .zIndex(if (dragging) 2f else if (lift > 0f) 1f else 0f)
                        .graphicsLayer {
                            val s = (1f + 0.025f * lift) * (0.96f + 0.04f * appear.value)
                            scaleX = s; scaleY = s
                            shadowElevation = 28f * lift
                            alpha = appear.value
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, if (dragging) CmColors.actionIcon.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp)),
                ) { column(name, drag) }
            }
        }
        columns.indices.forEach { i ->
            // Il bordo dopo la colonna: si trascina, e al rilascio scatta al dodicesimo più vicino.
            if (i < columns.size - 1) Box(
                Modifier.offset { androidx.compose.ui.unit.IntOffset((lefts[i] + live[i]).roundToInt(), 0) }
                    .width(with(density) { gapPx.toDp() }).fillMaxHeight()
                    .pointerInput(columns, shares) {
                        detectHorizontalDragGestures(
                            onDragStart = { border = i; borderPx = 0f },
                            onDragEnd = { onShares(Tablet.Shares.drag(shares, i, Tablet.Shares.parts(borderPx, widthPx))); border = -1; borderPx = 0f },
                            onDragCancel = { border = -1; borderPx = 0f },
                        ) { change, amount -> change.consume(); borderPx += amount }
                    },
                contentAlignment = Alignment.Center,
            ) { Box(Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(if (border == i) CmColors.actionIcon else CmColors.line)) }
        }
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
