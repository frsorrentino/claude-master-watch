package it.pixelbox.cmwatch.mobile.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.PhoneApp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.cmSwitchColors
import it.pixelbox.cmwatch.rules.WidgetModel
import it.pixelbox.cmwatch.rules.WidgetModel.Metric
import it.pixelbox.cmwatch.rules.WidgetModel.Mode
import it.pixelbox.cmwatch.ui.tokens.CmColors
import kotlinx.coroutines.launch

/**
 * La personalizzazione alla posa del widget (spec «Widget»): modo, account o sessione, colonne (fino a tre, nell'ordine
 * in cui si toccano), opacità, angoli, monocromo. Uscire senza «Metti il widget» non lo posa.
 */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val app = application as PhoneApp
        lifecycleScope.launch {
            // La configurazione sta nello stato Glance del widget: scriverla e chiamare update lo ridisegna subito.
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(id)
            val initial = WidgetPrefs.read(getAppWidgetState(this@WidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId))
            setContent {
                CmPhoneTheme {
                    // Dallo stato che arriva: a freddo Room lo carica dopo l'apertura (revisione finale 01/10, I3).
                    val snap by app.repo.snapshot.collectAsState()
                    WidgetConfigScreen(snap.state, initial) { c ->
                        lifecycleScope.launch {
                            updateAppWidgetState(this@WidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId) { p ->
                                p.toMutablePreferences().apply { WidgetPrefs.write(this, c) }
                            }
                            CmWidget().update(this@WidgetConfigActivity, glanceId)
                            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                            finish()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetConfigScreen(state: State?, initial: WidgetModel.Config, onSave: (WidgetModel.Config) -> Unit) {
    var mode by remember { mutableStateOf(initial.mode) }
    var target by remember { mutableStateOf(initial.target) }
    var metrics by remember { mutableStateOf(initial.metrics) }
    var opacity by remember { mutableFloatStateOf(initial.opacity.toFloat()) }
    var corners by remember { mutableFloatStateOf(initial.corners.toFloat()) }
    var mono by remember { mutableStateOf(initial.mono) }
    Column(Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.wcfg_title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text)
            Label(R.string.wcfg_mode)
            listOf(Mode.BOARD to R.string.wcfg_board, Mode.ACCOUNT to R.string.wcfg_account, Mode.SESSION to R.string.wcfg_session).forEach { (m, l) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(mode == m, onClick = { if (mode != m) { mode = m; target = null; metrics = WidgetModel.defaults(m) } })
                    Text(stringResource(l), color = CmColors.text)
                }
            }
            val choices = when (mode) {
                Mode.ACCOUNT -> state?.quota?.keys?.sorted().orEmpty()
                Mode.SESSION -> state?.sessions?.filter { s -> s.state != SessionState.GONE }?.map { it.name }.orEmpty()
                Mode.BOARD -> emptyList()
            }
            if (mode != Mode.BOARD) {
                Label(R.string.wcfg_which)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (mode == Mode.SESSION) FilterChip(target == null, onClick = { target = null }, label = { Text(stringResource(R.string.wcfg_followed)) })
                    choices.forEach { c -> FilterChip(target == c, onClick = { target = c }, label = { Text(c) }) }
                }
            }
            Label(R.string.wcfg_columns)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric.entries.forEach { m ->
                    val on = m in metrics
                    FilterChip(on, onClick = {
                        metrics = if (on) metrics - m else (metrics + m).takeLast(WidgetModel.MAX_COLUMNS)
                    }, label = { Text(stringResource(metricLabel(m))) })
                }
            }
            Label(R.string.wcfg_opacity)
            Slider(opacity, { opacity = it }, valueRange = 20f..100f)
            Label(R.string.wcfg_corners)
            Slider(corners, { corners = it }, valueRange = 0f..32f)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.wcfg_mono), color = CmColors.text, modifier = Modifier.weight(1f))
                Switch(mono, { mono = it }, colors = cmSwitchColors())
            }
        }
        Button(
            onClick = { onSave(WidgetModel.Config(mode, target, metrics.ifEmpty { WidgetModel.defaults(mode) }, opacity.toInt(), corners.toInt(), mono)) },
            colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
        ) { Text(stringResource(R.string.wcfg_save)) }
    }
}

@Composable
private fun Label(id: Int) = Text(stringResource(id), style = MaterialTheme.typography.titleSmall, color = CmColors.text2)

fun metricLabel(m: Metric) = when (m) {
    Metric.WEEK -> R.string.w_week
    Metric.WORKING -> R.string.w_working
    Metric.WAITING -> R.string.w_waiting
    Metric.IDLE -> R.string.w_idle
    Metric.NIGHT -> R.string.w_night
    Metric.CONTEXT -> R.string.w_context
    Metric.TURN_AGE -> R.string.w_turn
    Metric.OUTCOME -> R.string.w_outcome
}
