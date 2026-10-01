package it.pixelbox.cmwatch.mobile.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import it.pixelbox.cmwatch.rules.WidgetModel

/**
 * La configurazione di ogni widget posato, nel suo stato Glance (`PreferencesGlanceStateDefinition`): scriverla e poi
 * chiamare `update` lo ridisegna subito (Franz, 01/10 12:37: con un file a parte le scelte arrivavano dopo tempo).
 */
object WidgetPrefs {
    /** Senza configurazione: la regia, opaca all'85 %, angoli da 24 dp, a colori. */
    /** Monocromo acceso di partenza (Franz, 01/10 23:16): anche l'anteprima nella lista dei widget. */
    val DEFAULT = WidgetModel.Config(WidgetModel.Mode.BOARD, null, WidgetModel.defaults(WidgetModel.Mode.BOARD), 85, 24, true)

    private val MODE = stringPreferencesKey("mode")
    private val TARGET = stringPreferencesKey("target")
    private val METRICS = stringPreferencesKey("metrics")
    private val OPACITY = intPreferencesKey("opacity")
    private val CORNERS = intPreferencesKey("corners")
    private val MONO = booleanPreferencesKey("mono")

    fun read(p: Preferences): WidgetModel.Config {
        val mode = p[MODE]?.let { runCatching { WidgetModel.Mode.valueOf(it) }.getOrNull() } ?: return DEFAULT
        val metrics = p[METRICS].orEmpty().split(',').mapNotNull { runCatching { WidgetModel.Metric.valueOf(it) }.getOrNull() }
        return WidgetModel.Config(
            mode, p[TARGET], metrics.ifEmpty { WidgetModel.defaults(mode) },
            p[OPACITY] ?: DEFAULT.opacity, p[CORNERS] ?: DEFAULT.corners, p[MONO] ?: DEFAULT.mono,
        )
    }

    fun write(p: MutablePreferences, c: WidgetModel.Config) {
        p[MODE] = c.mode.name
        c.target?.let { p[TARGET] = it } ?: p.remove(TARGET)
        p[METRICS] = c.metrics.joinToString(",") { it.name }
        p[OPACITY] = c.opacity
        p[CORNERS] = c.corners
        p[MONO] = c.mono
    }
}
