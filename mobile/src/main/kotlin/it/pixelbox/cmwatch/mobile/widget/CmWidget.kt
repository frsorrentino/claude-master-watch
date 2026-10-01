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
import androidx.compose.ui.unit.Dp
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
import it.pixelbox.cmwatch.rules.WidgetModel
import it.pixelbox.cmwatch.rules.WidgetModel.Metric
import it.pixelbox.cmwatch.rules.WorkPanel
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Il widget della schermata home (piano 30/09, Task 7; rifatto il 01/10 sulla Panoramica, Franz: «sfruttare l'intera
 * altezza, elementi grafici adeguati al dato»). In testata ▸ nome, ora e ↻; a sinistra il doppio anello della Panoramica
 * (5 ore fuori, settimana dentro) o il contesto della sessione; a destra un elemento per dato: la barra «Adesso» a
 * segmenti per chi aspetta, lavora ed è ferma, barre per settimana e contesto, icone per turno, notte ed esito. Alto
 * abbastanza, sotto le sessioni vive con stato e contesto. La configurazione sta nello stato Glance: salvarla lo
 * ridisegna subito.
 */
class CmWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as PhoneApp
        provideContent {
            val snap by app.repo.snapshot.collectAsState()
            val config = WidgetPrefs.read(currentState<Preferences>())
            Content(WidgetModel.cards(snap.state, config, System.currentTimeMillis() / 1000), config)
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
private const val MAX_PX = 160

@Composable
private fun Content(cards: List<WidgetModel.Card>, config: WidgetModel.Config) {
    val ctx = LocalContext.current
    val size = LocalSize.current
    val ink = Palette(config.mono)
    val main = cards.first()
    val header = size.height >= 88.dp
    val rows = cards.drop(1)
    // Righe delle sessioni solo dove c'è spazio dopo la card principale.
    val rowSpace = size.height - (if (header) 30.dp else 0.dp) - 20.dp - 110.dp
    val shown = if (rowSpace > 30.dp) rows.take((rowSpace.value / 34f).toInt()) else emptyList()
    Column(
        GlanceModifier.fillMaxSize().background(ink.bg.copy(alpha = config.opacity / 100f)).cornerRadius(config.corners.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp).clickable(actionStartActivity(open(ctx, main.session))),
    ) {
        if (header) Header(main, ink)
        if (main.updatedAt == null) {
            Box(GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.Center) {
                Text(ctx.getString(R.string.widget_waiting), style = TextStyle(color = ColorProvider(ink.text2), fontSize = 14.sp))
            }
            return@Column
        }
        Main(main, ink, GlanceModifier.fillMaxWidth().defaultWeight(), wide = size.width)
        shown.forEach { c -> Spacer(GlanceModifier.height(4.dp)); SessionRow(c, ink) }
    }
}

/** ▸ nome, l'ora dell'aggiornamento e ↻. */
@Composable
private fun Header(c: WidgetModel.Card, ink: Palette) {
    val ctx = LocalContext.current
    // Come ads-widget (segnalazione 01/10 13:07): ▸ piccolo e grigio, nome bianco in grassetto, ora grigia, ↻ in una pastiglia.
    Row(GlanceModifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("▸", style = TextStyle(color = ColorProvider(ink.text2), fontSize = 11.sp))
        Spacer(GlanceModifier.width(6.dp))
        Text(c.title.ifEmpty { ctx.getString(R.string.app_name) }, maxLines = 1,
            style = TextStyle(color = ColorProvider(ink.text), fontSize = 16.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.defaultWeight())
        Text(c.updatedAt?.let { HHMM.format(Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault())) } ?: "",
            style = TextStyle(color = ColorProvider(ink.text2), fontSize = 14.sp))
        Spacer(GlanceModifier.width(10.dp))
        Box(GlanceModifier.size(30.dp).cornerRadius(10.dp).background(ink.chip).clickable(actionRunCallback<RefreshAction>()), contentAlignment = Alignment.Center) {
            Image(ImageProvider(R.drawable.ic_w_refresh), ctx.getString(R.string.refresh), colorFilter = ColorFilter.tint(ColorProvider(ink.text2)), modifier = GlanceModifier.size(18.dp))
        }
    }
}

/**
 * Come ads-widget: l'arco a sinistra e una colonna per dato (icona e numero grande, etichetta sotto). Con altezza in più,
 * sotto la barra «Adesso» a segmenti e l'esito su due righe intere.
 */
@Composable
private fun Main(c: WidgetModel.Card, ink: Palette, modifier: GlanceModifier, wide: Dp) {
    val size = LocalSize.current
    val ring = minOf(size.height - 50.dp, 96.dp, wide * 0.28f).coerceAtLeast(52.dp)
    val tiles = c.columns.filter { it.first != Metric.OUTCOME }
    val outcome = c.columns.firstOrNull { it.first == Metric.OUTCOME }?.second?.takeIf { it != WidgetModel.NONE }
    val counts = c.columns.filter { it.first in COUNTS }
    val extra = size.height >= 150.dp
    Column(modifier) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Ring(c, ink, ring)
            Spacer(GlanceModifier.width(8.dp))
            tiles.forEach { (m, v) -> MetricTile(m, v, ink) }
        }
        if (extra && counts.size >= 2) { Spacer(GlanceModifier.height(8.dp)); NowBar(counts, ink) }
        if (extra && outcome != null) {
            Spacer(GlanceModifier.height(8.dp))
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Image(ImageProvider(icon(Metric.OUTCOME)), null, colorFilter = ColorFilter.tint(ColorProvider(ink.metric(Metric.OUTCOME))), modifier = GlanceModifier.size(18.dp))
                Spacer(GlanceModifier.width(8.dp))
                // L'esito su due righe intere, mai tagliato con i puntini.
                Text(outcome, maxLines = 2, style = TextStyle(color = ColorProvider(ink.text), fontSize = 14.sp), modifier = GlanceModifier.defaultWeight())
            }
        }
    }
}

private val COUNTS = setOf(Metric.WAITING, Metric.WORKING, Metric.IDLE)

/** Il doppio anello della Panoramica: 5 ore fuori (rosso dal 90 %), settimana dentro; per una sessione il contesto. */
@Composable
private fun Ring(c: WidgetModel.Card, ink: Palette, size: Dp) {
    val ctx = LocalContext.current
    val px = (size.value * ctx.resources.displayMetrics.density).toInt().coerceIn(16, MAX_PX)
    val bmp = remember(c.arcPct, c.innerPct, c.arcLabel, px, ink.mono) {
        val outer = c.arcPct?.let { if (c.arcLabel == WidgetModel.ARC_CTX) ink.context(it) else ink.quota(it) }
        rings(px, c.arcPct, outer, c.innerPct, ink)
    }
    Box(GlanceModifier.size(size), contentAlignment = Alignment.Center) {
        Image(ImageProvider(bmp), null, modifier = GlanceModifier.fillMaxSize())
        Text(c.arcPct?.let { "$it%" } ?: WidgetModel.NONE, style = TextStyle(color = ColorProvider(ink.text), fontSize = (size.value / 4.2f).sp, fontWeight = FontWeight.Bold))
        // L'etichetta nel varco in basso dell'arco, come «Spesa» in ads-widget.
        Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Text(c.arcLabel, style = TextStyle(color = ColorProvider(ink.text2), fontSize = 12.sp, fontWeight = FontWeight.Medium))
        }
    }
}

/** «Adesso» come nella Panoramica: la barra a segmenti nei colori dei badge; i numeri stanno nelle colonne sopra. */
@Composable
private fun NowBar(counts: List<Pair<Metric, String>>, ink: Palette) {
    val n = counts.map { it.first to (it.second.toIntOrNull() ?: 0) }
    val bmp = remember(n, ink.mono) { segments(n.map { ink.metric(it.first) to it.second }, ink) }
    Image(ImageProvider(bmp), null, modifier = GlanceModifier.fillMaxWidth().height(8.dp))
}

/** Una colonna come in ads-widget: icona colorata e numero grande sulla stessa riga, etichetta sotto, centrate. */
@Composable
private fun RowScope.MetricTile(m: Metric, v: String, ink: Palette) {
    val ctx = LocalContext.current
    Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(icon(m)), null, colorFilter = ColorFilter.tint(ColorProvider(ink.metric(m))), modifier = GlanceModifier.size(20.dp))
            Spacer(GlanceModifier.width(4.dp))
            Text(v, maxLines = 1, style = TextStyle(color = ColorProvider(ink.text), fontSize = 26.sp, fontWeight = FontWeight.Bold))
        }
        Text(ctx.getString(metricShort(m)), maxLines = 1, style = TextStyle(color = ColorProvider(ink.text2), fontSize = 14.sp, fontWeight = FontWeight.Medium))
    }
}

/** Una sessione viva: icona dello stato nel suo colore, nome, barra del contesto, età del turno. */
@Composable
private fun SessionRow(c: WidgetModel.Card, ink: Palette) {
    val ctx = LocalContext.current
    val seg = c.seg ?: WorkPanel.Seg.IDLE
    Row(GlanceModifier.fillMaxWidth().height(30.dp).cornerRadius(10.dp).background(ink.row).padding(horizontal = 8.dp)
        .clickable(actionStartActivity(open(ctx, c.session))), verticalAlignment = Alignment.CenterVertically) {
        Image(ImageProvider(segIcon(seg)), null, colorFilter = ColorFilter.tint(ColorProvider(ink.seg(seg))), modifier = GlanceModifier.size(16.dp))
        Spacer(GlanceModifier.width(8.dp))
        Text(c.title, maxLines = 1, style = TextStyle(color = ColorProvider(ink.text), fontSize = 13.sp, fontWeight = FontWeight.Medium), modifier = GlanceModifier.defaultWeight())
        c.arcPct?.let { pct ->
            val bmp = remember(pct, ink.mono) { bar(pct, ink.context(pct), ink) }
            Image(ImageProvider(bmp), null, modifier = GlanceModifier.width(44.dp).height(5.dp))
            Spacer(GlanceModifier.width(5.dp))
            Text("$pct%", style = TextStyle(color = ColorProvider(ink.text2), fontSize = 12.sp))
        }
        c.columns.firstOrNull { it.first == Metric.TURN_AGE }?.let { (_, v) ->
            Spacer(GlanceModifier.width(8.dp))
            Text(v, style = TextStyle(color = ColorProvider(ink.text2), fontSize = 12.sp))
        }
    }
}

private fun rings(px: Int, outerPct: Int?, outer: Color?, innerPct: Int?, ink: Palette): Bitmap {
    val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val cv = android.graphics.Canvas(b)
    val w = px * 0.095f
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = w; strokeCap = Paint.Cap.ROUND }
    fun ring(inset: Float, color: Color, pct: Int?) {
        val r = RectF(inset, inset, px - inset, px - inset)
        // Arco aperto in basso (270°), come ads-widget: il varco ospita l'etichetta.
        p.color = ink.track.toArgb(); cv.drawArc(r, 135f, 270f, false, p)
        if (pct != null && pct > 0) { p.color = color.toArgb(); cv.drawArc(r, 135f, 270f * pct.coerceAtMost(100) / 100f, false, p) }
    }
    ring(w / 2, outer ?: ink.track, outerPct)
    if (innerPct != null) ring(w / 2 + w + px * 0.03f, ink.metric(Metric.WEEK), innerPct)
    return b
}

private fun segments(parts: List<Pair<Color, Int>>, ink: Palette): Bitmap {
    val wpx = 240; val hpx = 12
    val b = Bitmap.createBitmap(wpx, hpx, Bitmap.Config.ARGB_8888)
    val cv = android.graphics.Canvas(b)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val total = parts.sumOf { it.second }
    if (total == 0) { p.color = ink.track.toArgb(); cv.drawRoundRect(RectF(0f, 0f, wpx.toFloat(), hpx.toFloat()), hpx / 2f, hpx / 2f, p); return b }
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

private fun bar(pct: Int, color: Color, ink: Palette): Bitmap {
    val wpx = 120; val hpx = 10
    val b = Bitmap.createBitmap(wpx, hpx, Bitmap.Config.ARGB_8888)
    val cv = android.graphics.Canvas(b)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = ink.track.toArgb(); cv.drawRoundRect(RectF(0f, 0f, wpx.toFloat(), hpx.toFloat()), hpx / 2f, hpx / 2f, p)
    val w = wpx * pct.coerceIn(0, 100) / 100f
    if (w > 0) { p.color = color.toArgb(); cv.drawRoundRect(RectF(0f, 0f, maxOf(w, hpx.toFloat()), hpx.toFloat()), hpx / 2f, hpx / 2f, p) }
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

private fun segIcon(s: WorkPanel.Seg) = when (s) {
    WorkPanel.Seg.WAITING -> R.drawable.ic_w_waiting
    WorkPanel.Seg.WORKING -> R.drawable.ic_w_working
    WorkPanel.Seg.IDLE -> R.drawable.ic_w_idle
}

/** I colori della Panoramica e dei badge; monocromo = grigi. */
private class Palette(val mono: Boolean) {
    val bg = CmColors.surfaceLow
    val row = CmColors.surfaceHigh
    val text = CmColors.text
    val text2 = CmColors.text2
    /** La pastiglia di ↻, un velo chiaro sul fondo. */
    val chip = Color.White.copy(alpha = 0.08f)
    val track = CmColors.briefTrack
    fun quota(pct: Int): Color = if (mono) CmColors.text else if (pct >= 90) CmColors.briefAlertRing else CmColors.briefRing
    /** Le soglie della card delle misure (`SessionMeters.contextTone`): ambra dal 75 %, rosso dal 90 %. */
    fun context(pct: Int): Color = if (mono) CmColors.text else when (it.pixelbox.cmwatch.rules.SessionMeters.contextTone(pct)) {
        it.pixelbox.cmwatch.rules.BriefCards.Tone.ALERT -> CmColors.briefAlertRing
        it.pixelbox.cmwatch.rules.BriefCards.Tone.WARN -> CmColors.waiting
        else -> CmColors.briefRing
    }
    fun seg(s: WorkPanel.Seg): Color = metric(when (s) { WorkPanel.Seg.WAITING -> Metric.WAITING; WorkPanel.Seg.WORKING -> Metric.WORKING; WorkPanel.Seg.IDLE -> Metric.IDLE })
    fun metric(m: Metric): Color = if (mono) CmColors.text2 else when (m) {
        Metric.WEEK -> CmColors.briefWeek
        Metric.WORKING -> CmColors.busy
        Metric.WAITING -> CmColors.waiting
        Metric.IDLE -> CmColors.idle
        Metric.NIGHT -> CmColors.briefChip
        Metric.CONTEXT -> CmColors.briefRing
        Metric.TURN_AGE -> CmColors.actionIcon
        Metric.OUTCOME -> CmColors.briefGood
    }
}
