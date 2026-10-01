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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import it.pixelbox.cmwatch.mobile.MainActivity
import it.pixelbox.cmwatch.mobile.PhoneApp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.WidgetModel
import it.pixelbox.cmwatch.rules.WidgetModel.Metric
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Il widget della schermata home (piano 30/09, Task 7; spec «Widget»), nello stile di ads-widget: fondo scuro
 * traslucido, in testata ▸ nome, ora dell'aggiornamento e ↻, poi l'arco a sinistra e tre colonne. Quattro taglie:
 * striscia (solo la riga dei numeri), piccola (l'arco), media (la card), grande (la card e le sessioni vive). Si ridisegna
 * a ogni stato che arriva (`PhoneApp`), non ogni 30 minuti; ↻ chiede lo stato al PC.
 */
class CmWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(STRIP, SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as PhoneApp
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val snap by app.repo.snapshot.collectAsState()
            val config = WidgetPrefs.load(context, widgetId)
            val cards = WidgetModel.cards(snap.state, config, System.currentTimeMillis() / 1000)
            Content(cards, config)
        }
    }

    companion object {
        val STRIP = DpSize(250.dp, 56.dp)
        val SMALL = DpSize(110.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 110.dp)
        val LARGE = DpSize(250.dp, 250.dp)
    }
}

class CmWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = CmWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetPrefs.delete(context, it) }
    }
}

/** ↻: lo stato chiesto al PC; il widget si ridisegna quando arriva. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        (context.applicationContext as PhoneApp).repo.refresh()
    }
}

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

@Composable
private fun Content(cards: List<WidgetModel.Card>, config: WidgetModel.Config) {
    val ctx = LocalContext.current
    val size = LocalSize.current
    val ink = Palette(config.mono)
    val bg = Color(0xFF12141A).copy(alpha = config.opacity / 100f)
    val main = cards.first()
    Column(
        GlanceModifier.fillMaxSize().background(bg).cornerRadius(config.corners.dp).padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(actionStartActivity(open(ctx, main.session))),
    ) {
        if (size.height >= CmWidget.MEDIUM.height || size.width < CmWidget.MEDIUM.width) Header(main, ink)
        when {
            size.width < CmWidget.MEDIUM.width -> Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) { Arc(main, ink, 72) }
            size.height < CmWidget.MEDIUM.height -> Numbers(main, ink, arc = 40, value = 18)
            else -> {
                Numbers(main, ink, arc = 60, value = 24)
                if (size.height >= CmWidget.LARGE.height) cards.drop(1).take(4).forEach { c ->
                    Spacer(GlanceModifier.height(6.dp))
                    SessionRow(c, ink)
                }
            }
        }
    }
}

/** ▸ nome, l'ora dell'aggiornamento (o «in attesa del PC») e ↻. */
@Composable
private fun Header(c: WidgetModel.Card, ink: Palette) {
    val ctx = LocalContext.current
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("▸ " + c.title.ifEmpty { ctx.getString(R.string.app_name) }, maxLines = 1, style = TextStyle(color = ColorProvider(ink.text2), fontSize = 14.sp, fontWeight = FontWeight.Medium), modifier = GlanceModifier.defaultWeight())
        Text(
            c.updatedAt?.let { HHMM.format(Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault())) } ?: ctx.getString(R.string.widget_waiting),
            style = TextStyle(color = ColorProvider(ink.text2), fontSize = 13.sp),
        )
        Image(
            ImageProvider(R.drawable.ic_w_refresh), ctx.getString(R.string.refresh), colorFilter = ColorFilter.tint(ColorProvider(ink.text2)),
            modifier = GlanceModifier.size(28.dp).padding(5.dp).clickable(actionRunCallback<RefreshAction>()),
        )
    }
}

/** L'arco e le colonne su una riga. */
@Composable
private fun Numbers(c: WidgetModel.Card, ink: Palette, arc: Int, value: Int) {
    Row(GlanceModifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Arc(c, ink, arc)
        c.columns.forEach { (m, v) -> Column(c = m, v = v, ink = ink, size = value) }
    }
}

@Composable
private fun RowScope.Column(c: Metric, v: String, ink: Palette, size: Int) {
    val ctx = LocalContext.current
    androidx.glance.layout.Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(R.drawable.ic_w_dot), null, colorFilter = ColorFilter.tint(ColorProvider(ink.metric(c))), modifier = GlanceModifier.size(10.dp))
            Spacer(GlanceModifier.width(5.dp))
            Text(v, maxLines = 1, style = TextStyle(color = ColorProvider(ink.text), fontSize = size.sp, fontWeight = FontWeight.Bold))
        }
        Text(ctx.getString(label(c)), maxLines = 1, style = TextStyle(color = ColorProvider(ink.text2), fontSize = 12.sp))
    }
}

/** Una sessione nella taglia grande: l'arco del contesto piccolo, nome, età del turno ed esito. */
@Composable
private fun SessionRow(c: WidgetModel.Card, ink: Palette) {
    val ctx = LocalContext.current
    Row(GlanceModifier.fillMaxWidth().clickable(actionStartActivity(open(ctx, c.session))), verticalAlignment = Alignment.CenterVertically) {
        Arc(c, ink, 30, showValue = false)
        Spacer(GlanceModifier.width(8.dp))
        Text(c.title, maxLines = 1, style = TextStyle(color = ColorProvider(ink.text), fontSize = 14.sp, fontWeight = FontWeight.Medium), modifier = GlanceModifier.defaultWeight())
        Text(c.columns.joinToString(" · ") { it.second }, maxLines = 1, style = TextStyle(color = ColorProvider(ink.text2), fontSize = 12.sp))
    }
}

/** L'arco come immagine: Glance non disegna archi. Valore al centro ed etichetta sotto, come in ads-widget. */
@Composable
private fun Arc(c: WidgetModel.Card, ink: Palette, sizeDp: Int, showValue: Boolean = true) {
    val ctx = LocalContext.current
    val px = (sizeDp * ctx.resources.displayMetrics.density).toInt().coerceAtLeast(8)
    val bmp = remember(c.arcPct, px, ink.mono) { arcBitmap(px, c.arcPct, ink) }
    Box(GlanceModifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Image(ImageProvider(bmp), null, modifier = GlanceModifier.fillMaxSize())
        if (showValue) androidx.glance.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(c.arcPct?.let { "$it%" } ?: WidgetModel.NONE, style = TextStyle(color = ColorProvider(ink.text), fontSize = (sizeDp / 4).sp, fontWeight = FontWeight.Bold))
            if (sizeDp >= 56) Text(c.arcLabel, style = TextStyle(color = ColorProvider(ink.text2), fontSize = 10.sp))
        }
    }
}

private fun arcBitmap(px: Int, pct: Int?, ink: Palette): Bitmap {
    val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val cv = android.graphics.Canvas(b)
    val stroke = px * 0.11f
    val r = RectF(stroke / 2, stroke / 2, px - stroke / 2, px - stroke / 2)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND }
    p.color = ink.track.toArgb(); cv.drawArc(r, 135f, 270f, false, p)
    if (pct != null && pct > 0) { p.color = ink.arc(pct).toArgb(); cv.drawArc(r, 135f, 270f * pct.coerceAtMost(100) / 100f, false, p) }
    return b
}

/** L'app aperta sulla scheda della sessione, o sulla Panoramica. */
private fun open(ctx: Context, session: String?) = Intent(ctx, MainActivity::class.java)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    .putExtra(MainActivity.EXTRA_SESSION, session)
    .putExtra(MainActivity.EXTRA_OVERVIEW, session == null)

private fun label(m: Metric) = when (m) {
    Metric.WEEK -> R.string.w_week
    Metric.WORKING -> R.string.w_working
    Metric.WAITING -> R.string.w_waiting
    Metric.IDLE -> R.string.w_idle
    Metric.NIGHT -> R.string.w_night
    Metric.CONTEXT -> R.string.w_context
    Metric.TURN_AGE -> R.string.w_turn
    Metric.OUTCOME -> R.string.w_outcome
}

/** I colori dell'app e dell'orologio; monocromo = grigi. */
private class Palette(val mono: Boolean) {
    val text = CmColors.text
    val text2 = CmColors.text2
    val track = CmColors.briefTrack
    fun arc(pct: Int): Color = if (mono) CmColors.text else if (pct >= 90) CmColors.briefAlertRing else CmColors.briefRing
    fun metric(m: Metric): Color = if (mono) CmColors.text2 else when (m) {
        Metric.WEEK -> CmColors.briefWeek
        Metric.WORKING -> CmColors.busy
        Metric.WAITING -> CmColors.waiting
        Metric.IDLE -> CmColors.idle
        Metric.NIGHT -> CmColors.briefChip
        Metric.CONTEXT -> CmColors.briefRing
        Metric.TURN_AGE -> CmColors.text2
        Metric.OUTCOME -> CmColors.briefGood
    }
}
