package it.pixelbox.cmwatch.wear.tile

import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.buttonGroup
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textDataCard
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
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.NameText
import it.pixelbox.cmwatch.rules.TileTexts
import it.pixelbox.cmwatch.wear.CmApp
import it.pixelbox.cmwatch.wear.MainActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Tile con l'anatomia standard di Wear OS (Franz via la master, 13/09 10:50): `primaryLayout` con i tre slot sempre
 * presenti e un `textEdgeButton` curvo in fondo. Tre stati: domanda aperta (badge + nome, testo della domanda, attesa,
 * «Rispondi»); nessuna domanda (contatori in `textDataCard` affiancate, «Sessioni»); PC fermo («Sessioni»).
 * Colori dai ruoli M3 della tile; i nostri restano dove significano qualcosa (ambra della domanda).
 */
class CmTileService : TileService() {

    private val scheme = ColorScheme(
        primary = 0xFF4C7DFF.toInt().argb, onPrimary = 0xFFFFFFFF.toInt().argb,
        primaryContainer = 0xFF2B4A9E.toInt().argb, onPrimaryContainer = 0xFFDCE6FF.toInt().argb,
        surfaceContainer = 0xFF23272E.toInt().argb, onSurface = 0xFFF2F4F7.toInt().argb, onSurfaceVariant = 0xFF9AA3B2.toInt().argb,
        background = 0xFF000000.toInt().argb, onBackground = 0xFFF2F4F7.toInt().argb,
    )

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        CallbackToFutureAdapter.getFuture { completer ->
            val app = application as CmApp
            val snap = app.repo.snapshot.value
            val seen = kotlinx.coroutines.runBlocking { app.prefs.current().seenQuestions }
            val state = snap.state
            val stale = snap.freshness as? Freshness.Stale
            val now = System.currentTimeMillis() / 1000
            val question = if (stale == null) state?.sessions?.firstOrNull { s -> s.question?.let { it.id !in seen } == true } else null

            val root = try {
                materialScope(this, requestParams.deviceConfiguration, allowDynamicTheme = false, defaultColorScheme = scheme) {
                    when {
                        question != null -> questionLayout(question, now)
                        stale != null -> staleLayout(stale, state, now)
                        state == null || state.sessions.isEmpty() -> emptyLayout()
                        else -> countersLayout(state, seen)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("cmwatch", "tile layout failed, fallback", e)
                materialScope(this, requestParams.deviceConfiguration, allowDynamicTheme = false, defaultColorScheme = scheme) { emptyLayout() }
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

    /** Stato 1: la domanda è l'unica cosa che conta. */
    private fun MaterialScope.questionLayout(s: Session, now: Long): LayoutElement = primaryLayout(
        titleSlot = { title("${TileTexts.badge(s)} ${NameText.shorten(s.name, listOf(s.name), 20)}") },
        mainSlot = {
            LayoutElementBuilders.Column.Builder().setWidth(expand()).setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(text(s.question!!.text.layoutString, typography = Typography.TITLE_MEDIUM, color = colorScheme.onSurface, maxLines = 3, alignment = LayoutElementBuilders.TEXT_ALIGN_CENTER))
                .addContent(text(TileTexts.waitingFor(s, now, getString(R.string.tile_waiting_for)).layoutString, typography = Typography.BODY_MEDIUM, color = 0xFFFFB020.toInt().argb, maxLines = 1))
                .build()
        },
        bottomSlot = { edge(R.string.card_reply, "cmwatch://question/${s.name}") },
    )

    /** Stato 2: quante sessioni e in che stato, in card affiancate. */
    private fun MaterialScope.countersLayout(state: State, seen: Set<String>): LayoutElement = primaryLayout(
        titleSlot = { title(getString(R.string.sessions_title)) },
        mainSlot = {
            val cards = TileTexts.cards(state, seen)
            buttonGroup(width = expand(), height = expand(), spacing = 4f) {
                for (c in cards) buttonGroupItem {
                    textDataCard(
                        onClick = clickable(launch("cmwatch://sessions"), id = c.kind.name),
                        width = weight(1f), height = expand(),
                        title = { text(c.count.toString().layoutString, typography = Typography.NUMERAL_LARGE, color = colorScheme.onSurface) },
                        content = { text(getString(label(c.kind)).layoutString, typography = Typography.BODY_MEDIUM, color = colorScheme.onSurfaceVariant, maxLines = 1) },
                    )
                }
            }
        },
        bottomSlot = { edge(R.string.tile_sessions, "cmwatch://sessions") },
    )

    /** Stato 3: il PC non batte più. */
    private fun MaterialScope.staleLayout(stale: Freshness.Stale, state: State?, now: Long): LayoutElement = primaryLayout(
        titleSlot = { title(getString(R.string.sessions_title)) },
        mainSlot = {
            val last = state?.ts?.let { DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault())) }
            LayoutElementBuilders.Column.Builder().setWidth(expand()).setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(text(getString(R.string.tile_stale_short).layoutString, typography = Typography.DISPLAY_SMALL, color = colorScheme.onSurface, maxLines = 1))
                .addContent(text(getString(R.string.tile_stale_detail, Durations.since(0, stale.minutes * 60L), last ?: "—").layoutString, typography = Typography.BODY_MEDIUM, color = colorScheme.onSurfaceVariant, maxLines = 2, alignment = LayoutElementBuilders.TEXT_ALIGN_CENTER))
                .build()
        },
        bottomSlot = { edge(R.string.tile_sessions, "cmwatch://sessions") },
    )

    private fun MaterialScope.emptyLayout(): LayoutElement = primaryLayout(
        titleSlot = { title(getString(R.string.sessions_title)) },
        mainSlot = { text(getString(R.string.sessions_empty).layoutString, typography = Typography.TITLE_LARGE, color = colorScheme.onSurfaceVariant, maxLines = 2, alignment = LayoutElementBuilders.TEXT_ALIGN_CENTER) },
        bottomSlot = { edge(R.string.tile_sessions, "cmwatch://sessions") },
    )

    private fun MaterialScope.title(s: String): LayoutElement =
        text(s.layoutString, typography = Typography.LABEL_MEDIUM, color = colorScheme.onSurfaceVariant, maxLines = 1)

    private fun MaterialScope.edge(label: Int, uri: String): LayoutElement =
        textEdgeButton(onClick = clickable(launch(uri), id = "edge"), labelContent = { text(getString(label).layoutString) })

    private fun label(k: TileTexts.Card.Kind) = when (k) {
        TileTexts.Card.Kind.ACTIVE -> R.string.tile_card_active
        TileTexts.Card.Kind.IDLE -> R.string.tile_card_idle
        TileTexts.Card.Kind.GONE -> R.string.tile_card_gone
    }

    private fun launch(uri: String): ActionBuilders.LaunchAction = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder().setPackageName(packageName).setClassName(MainActivity::class.java.name)
                .addKeyToExtraMapping(MainActivity.EXTRA_URI, ActionBuilders.stringExtra(uri)).build()
        ).build()

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        CallbackToFutureAdapter.getFuture { c -> c.set(ResourceBuilders.Resources.Builder().setVersion(RESOURCES).build()); "res" }

    companion object {
        const val RESOURCES = "6"
        fun requestUpdate(app: CmApp) = getUpdater(app).requestUpdate(CmTileService::class.java)
    }
}
