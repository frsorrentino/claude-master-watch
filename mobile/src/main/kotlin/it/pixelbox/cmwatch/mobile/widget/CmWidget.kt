package it.pixelbox.cmwatch.mobile.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import it.pixelbox.cmwatch.mobile.MainActivity
import it.pixelbox.cmwatch.mobile.PhoneApp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.BriefCards
import it.pixelbox.cmwatch.rules.SessionMeters
import it.pixelbox.cmwatch.rules.WidgetModel
import it.pixelbox.cmwatch.rules.WidgetModel.Metric
import it.pixelbox.cmwatch.rules.WorkPanel
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Il widget della schermata home (piano 30/09, Task 7), accordato al widget di ads-widget (Franz, 01/10 13:22): stesse
 * misure (`WidgetLayout`), stessa tavolozza, stesso ↻. Sotto una riga è una striscia: testata compatta, arco aperto in
 * basso e quattro colonne uguali con icona, numero ed etichetta. Più alto: testata piena, anello e card dei dati, poi
 * la barra «Adesso» e le sessioni vive come le righe delle campagne. La configurazione sta nello stato Glance: salvarla
 * lo ridisegna subito.
 */
class CmWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as PhoneApp
        provideContent {
            val snap by app.repo.snapshot.collectAsState()
            val config = WidgetPrefs.read(currentState<Preferences>())
            WidgetContent(WidgetModel.cards(snap.state, config, System.currentTimeMillis() / 1000), config)
        }
    }
}

class CmWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = CmWidget()
}

/** ↻: lo stato chiesto al PC; il widget si ridisegna quando arriva. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        (context.applicationContext as PhoneApp).repo.refresh()
    }
}

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
/** Le immagini viaggiano nel RemoteViews: piccole (revisione 01/10, I6). */
private const val MAX_PX = 200

/** `interactive` = falso solo per le anteprime disegnate fuori dal launcher, dove non ci sono azioni da agganciare. */
@Composable
internal fun WidgetContent(cards: List<WidgetModel.Card>, config: WidgetModel.Config, interactive: Boolean = true) {
    val ctx = LocalContext.current
    val size = LocalSize.current
    val m = WidgetLayout.of(size.width.value, size.height.value)
    val ink = Palette(config.mono)
    val main = cards.first()
    val root = GlanceModifier.fillMaxSize().background(ink.bg.copy(alpha = config.opacity / 100f)).cornerRadius(config.corners.dp)
        .padding(horizontal = m.padH.dp, vertical = m.padV.dp)
    Column(if (interactive) root.clickable(actionStartActivity(open(ctx, main.session))) else root) {
        Header(main, ink, m, interactive)
        if (main.updatedAt == null) {
            Box(GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.Center) {
                Text(ctx.getString(R.string.widget_waiting), style = TextStyle(color = ColorProvider(ink.text2), fontSize = m.label.sp))
            }
            return@Column
        }
        if (m.ticker) Strip(main, ink, m) else Tall(cards, ink, m, size.height.value, interactive)
    }
}

/** ▶, nome, ora e ↻ in una pastiglia, come la testata di ads-widget. */
@Composable
private fun Header(c: WidgetModel.Card, ink: Palette, m: WidgetLayout.M, interactive: Boolean) {
    val ctx = LocalContext.current
    Row(GlanceModifier.fillMaxWidth().height(m.headerHeight.dp).padding(start = m.headerInset.dp, end = ((if (m.ticker) 2 else 4) * m.sf).dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("▶", style = TextStyle(color = ColorProvider(ink.accent), fontSize = m.arrow.sp))
        Spacer(GlanceModifier.width((2 * m.sf).dp))
        Text(c.title.ifEmpty { ctx.getString(R.string.app_name) }, maxLines = 1,
            style = TextStyle(color = ColorProvider(ink.text), fontSize = m.title.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.defaultWeight())
        c.updatedAt?.let {
            Text(HHMM.format(Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault())), maxLines = 1, style = TextStyle(color = ColorProvider(ink.text), fontSize = m.time.sp))
        }
        Spacer(GlanceModifier.width(m.gap.dp))
        val box = GlanceModifier.width(m.refreshBox.dp).fillMaxHeight()
        Box(if (interactive) box.clickable(actionRunCallback<RefreshAction>()) else box, contentAlignment = Alignment.Center) {
            Box(GlanceModifier.size(m.chip.dp).cornerRadius(m.chipRadius.dp).background(ink.chip), contentAlignment = Alignment.Center) {
                Text("↻", style = TextStyle(color = ColorProvider(ink.text), fontSize = m.refreshGlyph.sp))
            }
        }
    }
}

/** La striscia: quattro colonne uguali, l'arco nella prima, i dati nelle altre, tutto centrato. */
@Composable
private fun ColumnScope.Strip(c: WidgetModel.Card, ink: Palette, m: WidgetLayout.M) {
    val size = LocalSize.current
    val body = size.height.value - 2 * m.padV - m.headerHeight
    val ring = minOf(m.ring.toFloat(), body - 2).coerceAtLeast(24f).toInt()
    Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
        Box(GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) { Ring(c, ink, m, ring) }
        c.columns.take(3).forEach { (metric, v) -> Kpi(metric, v, ink, m) }
    }
}

/** Più alto: anello e card dei dati, poi la barra «Adesso», l'esito e le sessioni vive dove c'è posto. */
@Composable
private fun Tall(cards: List<WidgetModel.Card>, ink: Palette, m: WidgetLayout.M, height: Float, interactive: Boolean) {
    val c = cards.first()
    Spacer(GlanceModifier.height(m.headerBottom.dp))
    Row(GlanceModifier.fillMaxWidth().height((m.ring + 4).dp), verticalAlignment = Alignment.CenterVertically) {
        Ring(c, ink, m, m.ring)
        Spacer(GlanceModifier.width((6 * m.sf).dp))
        c.columns.filter { it.first != Metric.OUTCOME }.take(3).forEach { (metric, v) -> KpiCard(metric, v, ink, m) }
    }
    var left = height - 2 * m.padV - m.headerHeight - m.headerBottom - (m.ring + 4)
    val counts = c.columns.filter { it.first in COUNTS }
    if (counts.size >= 2 && left >= 20) {
        Spacer(GlanceModifier.height((6 * m.sf).dp)); NowBar(counts, ink); left -= 16
    }
    c.columns.firstOrNull { it.first == Metric.OUTCOME }?.second?.takeIf { it != WidgetModel.NONE && left >= 40 }?.let { text ->
        Spacer(GlanceModifier.height((6 * m.sf).dp))
        Row(GlanceModifier.fillMaxWidth().padding(horizontal = (4 * m.sf).dp), verticalAlignment = Alignment.Top) {
            Image(ImageProvider(icon(Metric.OUTCOME)), null, colorFilter = ColorFilter.tint(ColorProvider(ink.metric(Metric.OUTCOME))), modifier = GlanceModifier.size(m.icon.dp))
            Spacer(GlanceModifier.width(m.gap.dp))
            // L'esito su due righe intere, mai tagliato con i puntini.
            Text(text, maxLines = 2, style = TextStyle(color = ColorProvider(ink.text), fontSize = (m.label + 2).sp), modifier = GlanceModifier.defaultWeight())
        }
        left -= 44
    }
    val rowH = (22 * m.sf + 10 * m.sf + 2 * m.sf)
    val rows = cards.drop(1).take(((left - 6) / rowH).toInt().coerceAtLeast(0))
    if (rows.isNotEmpty()) Spacer(GlanceModifier.height((6 * m.sf).dp))
    rows.forEach { SessionRow(it, ink, m, interactive) }
}

private val COUNTS = setOf(Metric.WAITING, Metric.WORKING, Metric.IDLE)

/**
 * L'arco: nella striscia aperto in basso (240°) con l'etichetta nel varco, più in alto un anello intero con l'etichetta
 * sotto il valore, come la «Spesa» di ads-widget. Per account e regia c'è anche l'anello interno della settimana.
 */
@Composable
private fun Ring(c: WidgetModel.Card, ink: Palette, m: WidgetLayout.M, size: Int) {
    val ctx = LocalContext.current
    val px = (size * ctx.resources.displayMetrics.density).toInt().coerceIn(16, MAX_PX)
    val outer = c.arcPct?.let { if (c.arcLabel == WidgetModel.ARC_CTX) ink.context(it) else ink.quota(it) } ?: ink.track
    val week = ink.metric(Metric.WEEK)
    val bmp = remember(c.arcPct, c.innerPct, outer, week, px, m.gauge, m.ringStroke) {
        rings(px, c.arcPct, outer, c.innerPct, week, ink.track, if (m.gauge) 240f else 360f, m.ringStroke * px / size.toFloat())
    }
    Box(GlanceModifier.size(size.dp), contentAlignment = Alignment.Center) {
        Image(ImageProvider(bmp), null, modifier = GlanceModifier.fillMaxSize())
        Column(GlanceModifier.fillMaxSize().padding(bottom = if (m.gauge) (size * 0.12f).dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
            Value(c.arcPct?.let { "$it%" } ?: WidgetModel.NONE, m.ringValue, ink)
            if (!m.gauge) Text(c.arcLabel, style = TextStyle(color = ColorProvider(ink.text2), fontSize = m.ringLabel.sp, fontWeight = FontWeight.Medium))
        }
        if (m.gauge) Box(GlanceModifier.fillMaxSize().padding(bottom = 2.dp), contentAlignment = Alignment.BottomCenter) {
            Text(c.arcLabel, style = TextStyle(color = ColorProvider(ink.text2), fontSize = m.ringLabel.sp, fontWeight = FontWeight.Medium))
        }
    }
}

/** Un numero come in ads-widget: la parte intera grande, il resto (%, unità) più piccolo e grigio. */
@Composable
private fun Value(v: String, size: Int, ink: Palette) {
    val cut = v.indexOfFirst { !it.isDigit() }.let { if (it <= 0) v.length else it }
    val main = v.substring(0, cut)
    val rest = v.substring(cut).replace(" ", "")
    Row(verticalAlignment = Alignment.Bottom) {
        Text(main, maxLines = 1, style = TextStyle(color = ColorProvider(ink.text), fontSize = size.sp, fontWeight = FontWeight.Bold))
        if (rest.isNotEmpty()) Text(rest, maxLines = 1,
            style = TextStyle(color = ColorProvider(ink.text2), fontSize = (size * 0.6f).toInt().sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.padding(bottom = (size * 0.12f).dp))
    }
}

/** Una colonna della striscia: icona e numero sulla stessa riga, etichetta sotto, centrate. */
@Composable
private fun RowScope.Kpi(metric: Metric, v: String, ink: Palette, m: WidgetLayout.M) {
    val ctx = LocalContext.current
    Column(GlanceModifier.defaultWeight().padding(horizontal = (2 * m.sf).dp), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(icon(metric)), null, colorFilter = ColorFilter.tint(ColorProvider(ink.metric(metric))), modifier = GlanceModifier.size(m.icon.dp))
            Spacer(GlanceModifier.width(m.gap.dp))
            if (metric == Metric.OUTCOME) Text(v, maxLines = 2, style = TextStyle(color = ColorProvider(ink.text), fontSize = m.label.sp, fontWeight = FontWeight.Medium))
            else Value(v, m.value, ink)
        }
        Text(ctx.getString(metricShort(metric)), maxLines = 1, style = TextStyle(color = ColorProvider(ink.text2), fontSize = m.label.sp, fontWeight = FontWeight.Medium))
    }
}

/** Una card dei dati più in alto, come la KpiCard di ads-widget: icona ed etichetta sopra, numero sotto. */
@Composable
private fun RowScope.KpiCard(metric: Metric, v: String, ink: Palette, m: WidgetLayout.M) {
    val ctx = LocalContext.current
    Column(GlanceModifier.defaultWeight().padding(horizontal = (2 * m.sf).dp), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(icon(metric)), null, colorFilter = ColorFilter.tint(ColorProvider(ink.metric(metric))), modifier = GlanceModifier.size(m.icon.dp))
            Spacer(GlanceModifier.width(m.gap.dp))
            Text(ctx.getString(metricShort(metric)), maxLines = 1, style = TextStyle(color = ColorProvider(ink.text2), fontSize = m.label.sp, fontWeight = FontWeight.Medium))
        }
        Spacer(GlanceModifier.height(m.gap.dp))
        Value(v, m.value, ink)
    }
}

/** «Adesso» come nella Panoramica: la barra a segmenti nei colori dei badge. */
@Composable
private fun NowBar(counts: List<Pair<Metric, String>>, ink: Palette) {
    val n = counts.map { it.first to (it.second.toIntOrNull() ?: 0) }
    val colors = n.map { ink.metric(it.first) }
    val bmp = remember(n, colors) { segments(n.map { ink.metric(it.first) to it.second }, ink.track) }
    Image(ImageProvider(bmp), null, modifier = GlanceModifier.fillMaxWidth().height(8.dp))
}

/** Una sessione viva come una riga campagna di ads-widget: pallino dello stato, nome, contesto ed età del turno. */
@Composable
private fun SessionRow(c: WidgetModel.Card, ink: Palette, m: WidgetLayout.M, interactive: Boolean) {
    val ctx = LocalContext.current
    val seg = c.seg ?: WorkPanel.Seg.IDLE
    val base = GlanceModifier.fillMaxWidth().cornerRadius((14 * m.sf).dp).background(ink.chip)
        .padding(horizontal = (6 * m.sf).dp, vertical = (5 * m.sf).dp)
    Row(if (interactive) base.clickable(actionStartActivity(open(ctx, c.session))) else base, verticalAlignment = Alignment.CenterVertically) {
        Text("●", style = TextStyle(color = ColorProvider(ink.seg(seg)), fontSize = (7 * m.sf).sp))
        Spacer(GlanceModifier.width((4 * m.sf).dp))
        Text(c.title, maxLines = 1, style = TextStyle(color = ColorProvider(ink.text), fontSize = (10 * m.sf).sp, fontWeight = FontWeight.Medium), modifier = GlanceModifier.defaultWeight())
        c.arcPct?.let { Text("$it%", style = TextStyle(color = ColorProvider(ink.text2), fontSize = (10 * m.sf).sp)) }
        c.columns.firstOrNull { it.first == Metric.TURN_AGE }?.let { (_, v) ->
            Spacer(GlanceModifier.width((8 * m.sf).dp))
            Text(v, style = TextStyle(color = ColorProvider(ink.text2), fontSize = (10 * m.sf).sp))
        }
    }
    Spacer(GlanceModifier.height((2 * m.sf).dp))
}

private fun rings(px: Int, outerPct: Int?, outer: Color, innerPct: Int?, inner: Color, track: Color, sweep: Float, stroke: Float): Bitmap {
    val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val cv = android.graphics.Canvas(b)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND }
    // Aperto in basso: parte da sinistra in basso e gira in senso orario.
    val start = 90f + (360f - sweep) / 2f
    fun ring(inset: Float, color: Color, pct: Int?) {
        val r = RectF(inset, inset, px - inset, px - inset)
        p.color = track.toArgb(); cv.drawArc(r, start, sweep, false, p)
        if (pct != null && pct > 0) { p.color = color.toArgb(); cv.drawArc(r, start, sweep * pct.coerceAtMost(100) / 100f, false, p) }
    }
    // Come ads-widget: la linea corre a uno spessore dal bordo (raggio = (lato - 2 spessori) / 2).
    ring(stroke, outer, outerPct)
    if (innerPct != null) ring(stroke + stroke * 1.6f, inner, innerPct)
    return b
}

private fun segments(parts: List<Pair<Color, Int>>, track: Color): Bitmap {
    val wpx = 240; val hpx = 12
    val b = Bitmap.createBitmap(wpx, hpx, Bitmap.Config.ARGB_8888)
    val cv = android.graphics.Canvas(b)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val total = parts.sumOf { it.second }
    if (total == 0) { p.color = track.toArgb(); cv.drawRoundRect(RectF(0f, 0f, wpx.toFloat(), hpx.toFloat()), hpx / 2f, hpx / 2f, p); return b }
    val gap = 4f
    val live = parts.filter { it.second > 0 }
    val usable = wpx - gap * (live.size - 1)
    var x = 0f
    live.forEach { (c, n) ->
        val w = usable * n / total
        p.color = c.toArgb(); cv.drawRoundRect(RectF(x, 0f, x + w, hpx.toFloat()), hpx / 2f, hpx / 2f, p)
        x += w + gap
    }
    return b
}

/** L'app aperta sulla scheda della sessione, o sulla Panoramica; `data` diverso per riga, così ogni tocco è suo. */
private fun open(ctx: Context, session: String?) = Intent(ctx, MainActivity::class.java)
    .setData(android.net.Uri.parse("cmaster://widget/" + (session ?: "")))
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    .putExtra(MainActivity.EXTRA_SESSION, session)
    .putExtra(MainActivity.EXTRA_OVERVIEW, session == null)

private fun icon(m: Metric) = when (m) {
    Metric.WEEK -> R.drawable.ic_w_week
    Metric.WORKING -> R.drawable.ic_w_working
    Metric.WAITING -> R.drawable.ic_w_waiting
    Metric.IDLE -> R.drawable.ic_w_idle
    Metric.NIGHT -> R.drawable.ic_w_night
    Metric.CONTEXT -> R.drawable.ic_w_context
    Metric.TURN_AGE -> R.drawable.ic_w_turn
    Metric.OUTCOME -> R.drawable.ic_w_outcome
}

/** Le etichette corte delle colonne: una parola, mai tagliata. */
private fun metricShort(m: Metric) = when (m) {
    Metric.WEEK -> R.string.ws_week
    Metric.WORKING -> R.string.ws_working
    Metric.WAITING -> R.string.ws_waiting
    Metric.IDLE -> R.string.ws_idle
    Metric.NIGHT -> R.string.ws_night
    Metric.CONTEXT -> R.string.ws_context
    Metric.TURN_AGE -> R.string.ws_turn
    Metric.OUTCOME -> R.string.ws_outcome
}

/**
 * La tavolozza di ads-widget (tema scuro) con i colori dei dati della Panoramica. Monocromo come in ads-widget: ogni
 * colore diventa il suo grigio di luminanza, così le icone restano distinguibili.
 */
private class Palette(val mono: Boolean) {
    private fun c(color: Color): Color = if (!mono) color else {
        val l = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
        Color(l, l, l, color.alpha)
    }
    val bg = CmColors.widgetBg
    val chip = CmColors.widgetChip
    val track = CmColors.widgetTrack
    val text = c(CmColors.widgetText)
    val text2 = c(CmColors.widgetText2)
    val accent = c(CmColors.widgetAccent)
    fun quota(pct: Int): Color = c(if (pct >= 90) CmColors.briefAlertRing else CmColors.briefRing)
    /** Le soglie della card delle misure (`SessionMeters.contextTone`): ambra dal 75 %, rosso dal 90 %. */
    fun context(pct: Int): Color = c(when (SessionMeters.contextTone(pct)) {
        BriefCards.Tone.ALERT -> CmColors.briefAlertRing
        BriefCards.Tone.WARN -> CmColors.waiting
        else -> CmColors.briefRing
    })
    fun seg(s: WorkPanel.Seg): Color = metric(when (s) { WorkPanel.Seg.WAITING -> Metric.WAITING; WorkPanel.Seg.WORKING -> Metric.WORKING; WorkPanel.Seg.IDLE -> Metric.IDLE })
    fun metric(m: Metric): Color = c(when (m) {
        Metric.WEEK -> CmColors.briefWeek
        Metric.WORKING -> CmColors.busy
        Metric.WAITING -> CmColors.waiting
        Metric.IDLE -> CmColors.idle
        Metric.NIGHT -> CmColors.briefChip
        Metric.CONTEXT -> CmColors.briefRing
        Metric.TURN_AGE -> CmColors.actionIcon
        Metric.OUTCOME -> CmColors.briefGood
    })
}
