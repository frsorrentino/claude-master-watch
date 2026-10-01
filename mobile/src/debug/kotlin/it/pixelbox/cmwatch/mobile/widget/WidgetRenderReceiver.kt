package it.pixelbox.cmwatch.mobile.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import it.pixelbox.cmwatch.mobile.PhoneApp
import it.pixelbox.cmwatch.rules.WidgetModel
import it.pixelbox.cmwatch.rules.WidgetModel.Metric
import it.pixelbox.cmwatch.rules.WidgetModel.Mode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * Solo debug: disegna il widget fuori dal launcher in PNG (files/widget-renders), con lo stato della Demo, alla densità
 * e alla scala dei caratteri del telefono di Franz (390 dpi, 1,3). Serve a confrontarlo con ads-widget pixel per pixel
 * senza il telefono in mano:
 * `adb shell am broadcast -n com.francescosorrentino.cmaster/it.pixelbox.cmwatch.mobile.widget.WidgetRenderReceiver`
 * poi `adb shell run-as com.francescosorrentino.cmaster tar c files/widget-renders > renders.tar`.
 */
class WidgetRenderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val done = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            try { render(context) } catch (e: Throwable) { android.util.Log.e("cmwatch", "widget render", e) } finally { done.finish() }
        }
    }

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private suspend fun render(base: Context) {
        val conf = Configuration(base.resources.configuration).apply { densityDpi = 390; fontScale = 1.3f }
        val ctx = base.createConfigurationContext(conf)
        val state = (base.applicationContext as PhoneApp).fake.state.first()
        val dir = File(base.filesDir, "widget-renders").apply { deleteRecursively(); mkdirs() }
        val sizes = listOf("strip" to (401f to 103f), "medium" to (401f to 200f), "large" to (401f to 300f), "small" to (190f to 190f))
        val d = WidgetPrefs.DEFAULT
        val configs = listOf(
            "board" to d,
            "board-mono" to d.copy(mono = true),
            "account" to d.copy(mode = Mode.ACCOUNT, target = "personal", metrics = WidgetModel.defaults(Mode.ACCOUNT)),
            "session" to d.copy(mode = Mode.SESSION, target = null, metrics = listOf(Metric.WORKING, Metric.TURN_AGE, Metric.OUTCOME)),
        )
        val density = conf.densityDpi / 160f
        for ((sn, wh) in sizes) for ((cn, cfg) in configs) {
            val (w, h) = wh
            val cards = WidgetModel.cards(state, cfg, state.ts)
            val rv = GlanceRemoteViews().compose(ctx, DpSize(w.dp, h.dp)) { WidgetContent(cards, cfg, interactive = false) }.remoteViews
            val host = FrameLayout(ctx)
            val view: View = rv.apply(ctx, host)
            val wp = (w * density).toInt(); val hp = (h * density).toInt(); val pad = (16 * density).toInt()
            view.measure(View.MeasureSpec.makeMeasureSpec(wp, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(hp, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, wp, hp)
            val bmp = Bitmap.createBitmap(wp + 2 * pad, hp + 2 * pad, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            // Uno sfondo da schermata home: scuro con una fascia colorata, per vedere la trasparenza.
            c.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat(),
                intArrayOf(0xFF05070C.toInt(), 0xFF16324F.toInt(), 0xFF6B3A1F.toInt()), null, Shader.TileMode.CLAMP) })
            c.translate(pad.toFloat(), pad.toFloat())
            view.draw(c)
            File(dir, "$sn-$cn.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        android.util.Log.i("cmwatch", "widget renders: ${dir.list()?.size}")
    }
}
