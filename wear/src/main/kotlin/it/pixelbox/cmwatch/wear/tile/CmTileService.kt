package it.pixelbox.cmwatch.wear.tile

import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
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
import it.pixelbox.cmwatch.rules.TileTexts
import it.pixelbox.cmwatch.wear.CmApp
import it.pixelbox.cmwatch.wear.MainActivity

/**
 * Tile come quelle di Google (ProtoLayout M3 `primaryLayout`): etichetta dei conteggi in alto, la sessione che conta
 * al centro (nome grande + domanda o stato), un solo bottone curvo sul bordo (Rispondi / Sessioni). Solo LaunchAction.
 */
class CmTileService : TileService() {

    private val scheme = ColorScheme(
        primary = 0xFF4C7DFF.toInt().argb, onPrimary = 0xFFF2F4F7.toInt().argb,
        primaryContainer = 0xFF4C7DFF.toInt().argb, onPrimaryContainer = 0xFFF2F4F7.toInt().argb,
        surfaceContainer = 0xFF121417.toInt().argb, onSurface = 0xFFF2F4F7.toInt().argb, onSurfaceVariant = 0xFF9AA3B2.toInt().argb,
        background = 0xFF000000.toInt().argb, onBackground = 0xFFF2F4F7.toInt().argb,
    )

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        CallbackToFutureAdapter.getFuture { completer ->
            val app = application as CmApp
            val snap = app.repo.snapshot.value
            val labels = TileTexts.Labels(none = getString(R.string.sessions_empty), stale = getString(R.string.tile_stale), works = getString(R.string.state_busy).lowercase())
            val g = TileTexts.glance(snap.state, snap.freshness, System.currentTimeMillis() / 1000, labels)
            val accent = when (g.accent) {
                TileTexts.Accent.QUESTION -> 0xFFFFB020; TileTexts.Accent.BUSY -> 0xFF7FA1FF
                TileTexts.Accent.IDLE -> 0xFF34C759; TileTexts.Accent.STALE -> 0xFF6B7280
            }.toInt().argb
            val root = materialScope(this, requestParams.deviceConfiguration, allowDynamicTheme = false, defaultColorScheme = scheme) {
                primaryLayout(
                    titleSlot = if (g.counts.isEmpty()) null else ({ text(g.counts.layoutString, typography = Typography.LABEL_MEDIUM, color = colorScheme.onSurfaceVariant, maxLines = 1) }),
                    mainSlot = {
                        val col = LayoutElementBuilders.Column.Builder().setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                        g.name?.let { col.addContent(text(it.layoutString, typography = Typography.TITLE_MEDIUM, color = accent, maxLines = 1)) }
                        col.addContent(text(g.body.layoutString, typography = if (g.name == null) Typography.TITLE_MEDIUM else Typography.BODY_MEDIUM, color = colorScheme.onSurface, maxLines = 3, alignment = LayoutElementBuilders.TEXT_ALIGN_CENTER))
                        col.build()
                    },
                    bottomSlot = { edgeButton(g) },
                )
            }
            val tile = TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(root))
                .setFreshnessIntervalMillis(snap.state?.let { TileTexts.freshnessMs(it) } ?: 15 * 60_000L)
                .build()
            completer.set(tile)
            "tile"
        }

    private fun MaterialScope.edgeButton(g: TileTexts.Glance): LayoutElement {
        val label = when (g.button) {
            TileTexts.Button.REPLY -> R.string.card_reply
            TileTexts.Button.OPEN -> R.string.tile_open
            TileTexts.Button.SESSIONS -> R.string.tile_sessions
            TileTexts.Button.QUOTA -> R.string.tile_quota
        }
        return textEdgeButton(onClick = clickable(launch(g.target), id = g.button.name), labelContent = { text(getString(label).layoutString) })
    }

    private fun launch(uri: String): ActionBuilders.LaunchAction = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder().setPackageName(packageName).setClassName(MainActivity::class.java.name)
                .addKeyToExtraMapping(MainActivity.EXTRA_URI, ActionBuilders.stringExtra(uri)).build()
        ).build()

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        CallbackToFutureAdapter.getFuture { c -> c.set(ResourceBuilders.Resources.Builder().setVersion(RESOURCES).build()); "res" }

    companion object {
        const val RESOURCES = "2"
        fun requestUpdate(app: CmApp) = getUpdater(app).requestUpdate(CmTileService::class.java)
    }
}
