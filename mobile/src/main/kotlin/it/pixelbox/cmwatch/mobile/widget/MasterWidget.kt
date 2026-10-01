package it.pixelbox.cmwatch.mobile.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.MainActivity
import it.pixelbox.cmwatch.mobile.PhoneApp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.MasterWidgetModel
import it.pixelbox.cmwatch.ui.tokens.CmColors
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Il secondo widget, «Master» (spec 2026-10-01-telefono-widget-master-design.md, variante ibrida degli stati): in testata
 * l'icona a tratto e la parola dello stato della master; il suo ultimo messaggio, o la domanda; la barra «Scrivi alla
 * master» che apre il foglio di scrittura; nel 4×4 le sessioni come chip. In monocromo, come il primo widget di partenza.
 */
class MasterWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    /** L'anteprima nella lista dei widget alla misura della posa (4×2), come il primo widget. */
    override val previewSizeMode = SizeMode.Responsive(setOf(DpSize(401.dp, 200.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as PhoneApp
        provideContent {
            val snap by app.repo.snapshot.collectAsState()
            MasterContent(snap.state, interactive = true)
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val app = context.applicationContext as PhoneApp
        val state = app.repo.snapshot.value.state ?: app.fake.state.first()
        provideContent { MasterContent(state, interactive = false) }
    }
}

class MasterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MasterWidget()
}

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
/** Il monocromo di partenza (Franz, 01/10 23:16): testo chiaro, grigi, una sola tinta per l'attesa. */
private val INK = Color(0xFFE3E3E8)
private val INK2 = Color(0xFF9AA0A8)
private val WAIT = Color(0xFFFFB020)

@Composable
private fun MasterContent(state: State?, interactive: Boolean) {
    val ctx = LocalContext.current
    val size = LocalSize.current
    val large = size.height.value >= 180f
    val m = MasterWidgetModel.build(state, maxChips = if (large) 8 else 0, outcomeLabel = ctx.getString(R.string.outcome_label))
    val root = GlanceModifier.fillMaxSize().background(CmColors.widgetBg.copy(alpha = 0.92f)).cornerRadius(24.dp).padding(14.dp)
    Column(root) {
        // Testata: icona a tratto e parola dello stato (variante ibrida), «Master», l'ora del messaggio.
        val head = GlanceModifier.fillMaxWidth().let { if (interactive) it.clickable(actionStartActivity(openSession(ctx, m.master?.name))) else it }
        Row(head, verticalAlignment = Alignment.CenterVertically) {
            val (icon, word) = stateOf(m.master?.state)
            Image(ImageProvider(icon), null, modifier = GlanceModifier.size(16.dp), colorFilter = ColorFilter.tint(ColorProvider(if (m.asking) WAIT else INK)))
            Spacer(GlanceModifier.width(6.dp))
            Text(ctx.getString(word), style = TextStyle(color = ColorProvider(if (m.asking) WAIT else INK2), fontSize = 12.sp, fontWeight = FontWeight.Medium))
            Spacer(GlanceModifier.width(10.dp))
            Text(ctx.getString(R.string.tab_master), style = TextStyle(color = ColorProvider(INK), fontSize = 16.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.defaultWeight())
            m.at?.let { Text(HHMM.format(Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault())), style = TextStyle(color = ColorProvider(INK2), fontSize = 12.sp)) }
        }
        Spacer(GlanceModifier.height(8.dp))
        val body = when {
            state == null -> ctx.getString(R.string.widget_waiting)
            m.master == null -> ctx.getString(R.string.master_absent)
            else -> m.text ?: ""
        }
        if (m.asking) Text(ctx.getString(R.string.mw_asks).uppercase(), style = TextStyle(color = ColorProvider(WAIT), fontSize = 11.sp, fontWeight = FontWeight.Bold))
        Text(
            body, maxLines = if (large) 6 else 3,
            style = TextStyle(color = ColorProvider(INK), fontSize = 14.sp),
            modifier = GlanceModifier.fillMaxWidth().defaultWeight().let { if (interactive) it.clickable(actionStartActivity(openSession(ctx, m.master?.name))) else it },
        )
        if (large && m.chips.isNotEmpty()) {
            Spacer(GlanceModifier.height(8.dp))
            ChipRows(m, size.width.value - 28f, interactive)
        }
        Spacer(GlanceModifier.height(10.dp))
        // La barra: un tocco apre il foglio di scrittura, con la tastiera già aperta.
        val bar = GlanceModifier.fillMaxWidth().background(CmColors.widgetChip).cornerRadius(20.dp).padding(horizontal = 14.dp, vertical = 10.dp)
            .let { if (interactive && m.master != null) it.clickable(actionStartActivity(Intent(ctx, WriteToMasterActivity::class.java))) else it }
        Box(bar) {
            Text(ctx.getString(if (m.asking) R.string.mw_reply else R.string.mw_write), style = TextStyle(color = ColorProvider(INK2), fontSize = 14.sp))
        }
    }
}

/**
 * I chip in righe calcolate a larghezza fissa (Glance non ha un FlowRow): al massimo due righe, poi «+N». La larghezza di
 * un chip si stima dal nome (7 dp a carattere più margini): basta perché non vada a capo.
 */
@Composable
private fun ChipRows(m: MasterWidgetModel.Model, width: Float, interactive: Boolean) {
    val ctx = LocalContext.current
    val rows = mutableListOf(mutableListOf<MasterWidgetModel.Chip>())
    var used = 0f
    var hidden = m.more
    for ((i, c) in m.chips.withIndex()) {
        val w = 30f + c.name.length * 7f
        if (used + w > width && rows.last().isNotEmpty()) {
            if (rows.size == 2) { hidden += m.chips.size - i; break }
            rows += mutableListOf<MasterWidgetModel.Chip>(); used = 0f
        }
        rows.last() += c; used += w + 6f
    }
    Column(GlanceModifier.fillMaxWidth()) {
        rows.forEachIndexed { r, row ->
            if (r > 0) Spacer(GlanceModifier.height(6.dp))
            Row {
                row.forEachIndexed { i, c ->
                    if (i > 0) Spacer(GlanceModifier.width(6.dp))
                    Chip(c, interactive)
                }
                if (r == rows.lastIndex && hidden > 0) {
                    Spacer(GlanceModifier.width(6.dp))
                    val more = GlanceModifier.background(CmColors.widgetChip).cornerRadius(12.dp).padding(horizontal = 10.dp, vertical = 5.dp)
                        .let { if (interactive) it.clickable(actionStartActivity(openSession(ctx, null))) else it }
                    Box(more) { Text("+$hidden", style = TextStyle(color = ColorProvider(INK2), fontSize = 12.sp)) }
                }
            }
        }
    }
}

@Composable
private fun Chip(c: MasterWidgetModel.Chip, interactive: Boolean) {
    val ctx = LocalContext.current
    val waiting = c.state == SessionState.WAITING
    val bar = when (c.state) {
        SessionState.WAITING -> WAIT
        SessionState.BUSY, SessionState.AWAITING -> INK
        else -> INK2
    }
    val mod = GlanceModifier.background(if (waiting) WAIT.copy(alpha = 0.18f) else CmColors.widgetChip).cornerRadius(12.dp)
        .padding(start = 8.dp, end = 10.dp, top = 5.dp, bottom = 5.dp)
        .let { if (interactive) it.clickable(actionStartActivity(openSession(ctx, c.name))) else it }
    Row(mod, verticalAlignment = Alignment.CenterVertically) {
        // La barretta di colore della variante ibrida; la campanella solo per chi aspetta.
        Box(GlanceModifier.width(3.dp).height(12.dp).background(bar).cornerRadius(2.dp)) {}
        Spacer(GlanceModifier.width(6.dp))
        if (waiting) {
            Image(ImageProvider(R.drawable.ic_w_waiting), null, modifier = GlanceModifier.size(12.dp), colorFilter = ColorFilter.tint(ColorProvider(WAIT)))
            Spacer(GlanceModifier.width(4.dp))
        }
        Text(c.name, maxLines = 1, style = TextStyle(color = ColorProvider(INK), fontSize = 12.sp))
    }
}

/** Icona e parola dello stato della master (variante ibrida). */
private fun stateOf(s: SessionState?): Pair<Int, Int> = when (s) {
    SessionState.WAITING -> R.drawable.ic_w_waiting to R.string.mw_state_wait
    SessionState.BUSY, SessionState.AWAITING -> R.drawable.ic_w_working to R.string.mw_state_work
    SessionState.IDLE -> R.drawable.ic_w_idle to R.string.mw_state_idle
    else -> R.drawable.ic_w_night to R.string.mw_state_gone
}

/** La scheda di una sessione nell'app; null = la casa della master. */
private fun openSession(ctx: Context, session: String?) = Intent(ctx, MainActivity::class.java)
    .setData(android.net.Uri.parse("cmaster://master-widget/" + (session ?: "")))
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    .putExtra(MainActivity.EXTRA_SESSION, session)
    .putExtra(MainActivity.EXTRA_OVERVIEW, session == null)
