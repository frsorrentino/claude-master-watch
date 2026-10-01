package it.pixelbox.cmwatch.mobile.widget

import android.content.Context
import it.pixelbox.cmwatch.rules.WidgetModel

/** La configurazione di ogni widget posato, per id: scelta alla posa, cambiabile toccando a lungo il widget. */
object WidgetPrefs {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("cm_widget", Context.MODE_PRIVATE)

    /** Senza configurazione: la regia, opaca all'85 %, angoli da 24 dp, a colori. */
    val DEFAULT = WidgetModel.Config(WidgetModel.Mode.BOARD, null, WidgetModel.defaults(WidgetModel.Mode.BOARD), 85, 24, false)

    fun load(ctx: Context, id: Int): WidgetModel.Config {
        val p = prefs(ctx)
        val mode = p.getString("$id.mode", null)?.let { runCatching { WidgetModel.Mode.valueOf(it) }.getOrNull() } ?: return DEFAULT
        val metrics = p.getString("$id.metrics", "").orEmpty().split(',').mapNotNull { runCatching { WidgetModel.Metric.valueOf(it) }.getOrNull() }
        return WidgetModel.Config(
            mode, p.getString("$id.target", null), metrics.ifEmpty { WidgetModel.defaults(mode) },
            p.getInt("$id.opacity", DEFAULT.opacity), p.getInt("$id.corners", DEFAULT.corners), p.getBoolean("$id.mono", false),
        )
    }

    fun save(ctx: Context, id: Int, c: WidgetModel.Config) = prefs(ctx).edit()
        .putString("$id.mode", c.mode.name).putString("$id.target", c.target)
        .putString("$id.metrics", c.metrics.joinToString(",") { it.name })
        .putInt("$id.opacity", c.opacity).putInt("$id.corners", c.corners).putBoolean("$id.mono", c.mono)
        .apply()

    fun delete(ctx: Context, id: Int) {
        val p = prefs(ctx)
        p.edit().apply { p.all.keys.filter { it.startsWith("$id.") }.forEach { remove(it) } }.apply()
    }
}
