package it.pixelbox.cmwatch.wear.tile

import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.buttonGroup
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.rules.TileTexts
import it.pixelbox.cmwatch.wear.CmApp
import it.pixelbox.cmwatch.wear.MainActivity

/** Tile fissa (ProtoLayout M3): «5 sessioni · 1? · 1✗», la sessione ferma su una riga intera, due bottoni. Solo LaunchAction. */
class CmTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        CallbackToFutureAdapter.getFuture { completer ->
            val app = application as CmApp
            val snap = app.repo.snapshot.value
            val state = snap.state
            val now = System.currentTimeMillis() / 1000
            val stale = snap.freshness as? Freshness.Stale
            val root = materialScope(this, requestParams.deviceConfiguration) {
                primaryLayout(
                    titleSlot = { text((state?.let { TileTexts.header(it, getString(R.string.sessions_label)) } ?: getString(R.string.app_name)).layoutString, typography = Typography.LABEL_MEDIUM, maxLines = 1) },
                    mainSlot = {
                        val line = when {
                            state == null -> getString(R.string.sessions_empty)
                            stale != null -> TileTexts.staleLine(stale, getString(R.string.tile_stale))
                            else -> TileTexts.line(state, now) ?: getString(R.string.sessions_empty)
                        }
                        text(line.layoutString, typography = Typography.BODY_MEDIUM, maxLines = 3)
                    },
                    bottomSlot = {
                        val buttons = state?.let { TileTexts.buttons(it) } ?: listOf(TileTexts.Button.SESSIONS, TileTexts.Button.QUOTA)
                        buttonGroup(width = expand(), height = wrap()) {
                            for (b in buttons) buttonGroupItem { tileButton(b, state?.sessions?.firstOrNull { it.question != null }?.name) }
                        }
                    },
                )
            }
            val tile = TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(root))
                .setFreshnessIntervalMillis(state?.let { TileTexts.freshnessMs(it) } ?: 15 * 60_000L)
                .build()
            completer.set(tile)
            "tile"
        }

    private fun MaterialScope.tileButton(b: TileTexts.Button, questionSession: String?): LayoutElement {
        val (label, uri) = when (b) {
            TileTexts.Button.OPEN -> R.string.tile_open to "cmwatch://question/${questionSession.orEmpty()}"
            TileTexts.Button.SESSIONS -> R.string.tile_sessions to "cmwatch://sessions"
            TileTexts.Button.QUOTA -> R.string.tile_quota to "cmwatch://quota"
        }
        return textButton(onClick = clickable(launch(uri), id = b.name), labelContent = { text(getString(label).layoutString) })
    }

    private fun launch(uri: String): ActionBuilders.LaunchAction = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder().setPackageName(packageName).setClassName(MainActivity::class.java.name)
                .addKeyToExtraMapping(MainActivity.EXTRA_URI, ActionBuilders.stringExtra(uri)).build()
        ).build()

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        CallbackToFutureAdapter.getFuture { c -> c.set(ResourceBuilders.Resources.Builder().setVersion(RESOURCES).build()); "res" }

    companion object {
        const val RESOURCES = "1"
        fun requestUpdate(app: CmApp) = getUpdater(app).requestUpdate(CmTileService::class.java)
    }
}
