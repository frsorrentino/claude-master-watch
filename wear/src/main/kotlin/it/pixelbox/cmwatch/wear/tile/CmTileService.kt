package it.pixelbox.cmwatch.wear.tile

import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.appCard
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.NameText
import it.pixelbox.cmwatch.rules.QuotaText
import it.pixelbox.cmwatch.rules.TileTexts
import it.pixelbox.cmwatch.wear.CmApp
import it.pixelbox.cmwatch.wear.MainActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Tile costruita sull'anatomia della tile di Gmail (letta dalla sua anteprima, 13/09): card `appCard` da due righe
 * (chi e quando sopra, cosa sotto) e un `textEdgeButton` pastello con testo scuro che porta anche il conteggio.
 * Contenuti scelti da Franz: la domanda quando c'è (proposta 1), altrimenti l'ultima attività (3) e la quota (4).
 */
class CmTileService : TileService() {

    /** Ruoli M3 in tema scuro: il primario è la tinta chiara, il testo sopra è scuro (come l'EdgeButton di Gmail). */
    private val scheme = ColorScheme(
        primary = 0xFFA8C7FA.toInt().argb, onPrimary = 0xFF0A2050.toInt().argb,
        primaryContainer = 0xFF2B4A9E.toInt().argb, onPrimaryContainer = 0xFFDCE6FF.toInt().argb,
        surfaceContainer = 0xFF29303D.toInt().argb, surfaceContainerLow = 0xFF23272E.toInt().argb,
        onSurface = 0xFFEBF1FF.toInt().argb, onSurfaceVariant = 0xFF9AA8BE.toInt().argb,
        background = 0xFF000000.toInt().argb, onBackground = 0xFFEBF1FF.toInt().argb,
    )

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        CallbackToFutureAdapter.getFuture { completer ->
            val app = application as CmApp
            val snap = app.repo.snapshot.value
            val prefs = kotlinx.coroutines.runBlocking { app.prefs.current() }
            val state = snap.state
            val stale = snap.freshness as? Freshness.Stale
            val now = System.currentTimeMillis() / 1000
            val question = if (stale == null) state?.sessions?.firstOrNull { s -> s.question?.let { it.id !in prefs.seenQuestions } == true } else null
            val edge = TileTexts.edge(state, snap.freshness, prefs.seenQuestions)

            val root = try {
                materialScope(this, requestParams.deviceConfiguration, allowDynamicTheme = false, defaultColorScheme = scheme) {
                    primaryLayout(
                        mainSlot = {
                            when {
                                question != null -> questionCard(question, now)
                                stale != null -> staleCard(stale, state)
                                state == null || state.sessions.isEmpty() -> emptyCard()
                                else -> restCards(state, now, prefs.complicationAccount)
                            }
                        },
                        bottomSlot = { edgeButton(edge, question?.name) },
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("cmwatch", "tile layout failed, fallback", e)
                materialScope(this, requestParams.deviceConfiguration, allowDynamicTheme = false, defaultColorScheme = scheme) {
                    primaryLayout(mainSlot = { emptyCard() }, bottomSlot = { edgeButton(TileTexts.Edge(TileTexts.Edge.Kind.SESSIONS, 0), null) })
                }
            }

            completer.set(
                TileBuilders.Tile.Builder()
                    .setResourcesVersion(RESOURCES)
                    .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(root))
                    .setFreshnessIntervalMillis(state?.let { TileTexts.freshnessMs(it) } ?: 15 * 60_000L)
                    .build()
            )
            "tile"
        }

    /** La domanda: chi e da quanto sopra, il testo intero sotto. */
    private fun MaterialScope.questionCard(s: Session, now: Long): LayoutElement = appCard(
        onClick = clickable(launch("cmwatch://question/${s.name}"), id = "q"),
        label = { small("${TileTexts.badge(s)} ${NameText.shorten(s.name, listOf(s.name), 14)}", colorScheme.onSurfaceVariant) },
        time = { small(Durations.since(s.question?.askedAt ?: s.since, now), AMBER.argb) },
        title = { text(s.question!!.text.layoutString, typography = Typography.TITLE_MEDIUM, color = colorScheme.onSurface, maxLines = 3) },
        height = expand(),
    )

    /** A riposo: l'ultima attività e la quota, due card come le due mail di Gmail. */
    private fun MaterialScope.restCards(state: State, now: Long, account: String): LayoutElement {
        val s = state.sessions.firstOrNull { it.followed && it.state != SessionState.GONE }
            ?: state.sessions.firstOrNull { it.state == SessionState.BUSY || it.state == SessionState.AWAITING }
            ?: state.sessions.filter { it.state != SessionState.GONE }.maxByOrNull { maxOf(it.since, it.turnStarted ?: 0, it.outcome?.at ?: 0) }
        val q = state.quota[account]
        val col = LayoutElementBuilders.Column.Builder().setWidth(expand())
        if (s != null) {
            val busy = s.state == SessionState.BUSY || s.state == SessionState.AWAITING
            val what = if (busy) s.tool ?: getString(R.string.state_busy) else s.outcome?.short ?: getString(R.string.state_idle)
            col.addContent(
                appCard(
                    onClick = clickable(launch("cmwatch://session/${s.name}"), id = "s"),
                    label = { small("${TileTexts.badge(s)} ${NameText.shorten(s.name, listOf(s.name), 14)}", colorScheme.onSurfaceVariant) },
                    time = { small(Durations.since(if (busy) s.turnStarted ?: s.since else s.since, now), colorScheme.onSurfaceVariant) },
                    title = { text(what.layoutString, typography = Typography.BODY_LARGE, color = colorScheme.onSurface, maxLines = 2) },
                )
            )
            if (q != null) col.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(6f)).build())
        }
        if (q != null) col.addContent(quotaCard(account, q))
        return col.build()
    }

    private fun MaterialScope.quotaCard(account: String, q: QuotaAccount): LayoutElement = appCard(
        onClick = clickable(launch("cmwatch://quota"), id = "quota"),
        label = { small(getString(R.string.tile_quota_label, account), colorScheme.onSurfaceVariant) },
        time = { small(q.resetW7?.let { getString(R.string.quota_reset) + " " + DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault())) } ?: "", colorScheme.onSurfaceVariant) },
        title = { text((q.h5?.let { "$it %" } ?: "—").layoutString, typography = Typography.NUMERAL_SMALL, color = colorScheme.onSurface, maxLines = 1) },
    )

    private fun MaterialScope.staleCard(stale: Freshness.Stale, state: State?): LayoutElement = appCard(
        onClick = clickable(launch("cmwatch://sessions"), id = "stale"),
        label = { small(getString(R.string.tile_stale_short), colorScheme.onSurfaceVariant) },
        time = { small(state?.ts?.let { DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault())) } ?: "", colorScheme.onSurfaceVariant) },
        title = { text(getString(R.string.tile_stale, stale.minutes).layoutString, typography = Typography.TITLE_MEDIUM, color = colorScheme.onSurface, maxLines = 2) },
        height = expand(),
    )

    private fun MaterialScope.emptyCard(): LayoutElement = appCard(
        onClick = clickable(launch("cmwatch://sessions"), id = "empty"),
        label = { small(getString(R.string.sessions_title), colorScheme.onSurfaceVariant) },
        title = { text(getString(R.string.sessions_empty).layoutString, typography = Typography.TITLE_MEDIUM, color = colorScheme.onSurface, maxLines = 2) },
        height = expand(),
    )

    private fun MaterialScope.small(s: String, color: androidx.wear.protolayout.types.LayoutColor): LayoutElement =
        text(s.layoutString, typography = Typography.BODY_SMALL, color = color, maxLines = 1)

    private fun MaterialScope.edgeButton(edge: TileTexts.Edge, questionSession: String?): LayoutElement {
        val label = when (edge.kind) {
            TileTexts.Edge.Kind.REPLY -> getString(R.string.card_reply)
            TileTexts.Edge.Kind.QUESTIONS -> getString(R.string.tile_edge_questions, edge.count)
            TileTexts.Edge.Kind.ACTIVE -> getString(R.string.tile_edge_active, edge.count)
            TileTexts.Edge.Kind.SESSIONS -> getString(R.string.tile_sessions)
        }
        val uri = if (edge.kind == TileTexts.Edge.Kind.REPLY && questionSession != null) "cmwatch://question/$questionSession" else "cmwatch://sessions"
        return textEdgeButton(onClick = clickable(launch(uri), id = "edge"), labelContent = { text(label.layoutString) })
    }

    private fun launch(uri: String): ActionBuilders.LaunchAction = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder().setPackageName(packageName).setClassName(MainActivity::class.java.name)
                .addKeyToExtraMapping(MainActivity.EXTRA_URI, ActionBuilders.stringExtra(uri)).build()
        ).build()

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        CallbackToFutureAdapter.getFuture { c -> c.set(ResourceBuilders.Resources.Builder().setVersion(RESOURCES).build()); "res" }

    companion object {
        const val RESOURCES = "8"
        private const val AMBER = 0xFFFFB020.toInt()
        fun requestUpdate(app: CmApp) = getUpdater(app).requestUpdate(CmTileService::class.java)
    }
}
