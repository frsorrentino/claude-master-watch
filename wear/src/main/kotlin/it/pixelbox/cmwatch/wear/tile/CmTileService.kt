package it.pixelbox.cmwatch.wear.tile

import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.card
import androidx.wear.protolayout.material3.circularProgressIndicator
import androidx.wear.protolayout.material3.icon
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.background
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.clip
import androidx.wear.protolayout.modifiers.padding
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.rules.QuotaText
import it.pixelbox.cmwatch.rules.TileTexts
import it.pixelbox.cmwatch.wear.CmApp
import it.pixelbox.cmwatch.wear.MainActivity

/**
 * Tile nello stile della tile «Passi» di Fitbit (Franz, 12/09 20:27): icona + etichetta in alto; una grande scheda scura
 * arrotondata con il dato grande a sinistra (domande aperte, o sessioni al lavoro), un chip con la sessione che conta e,
 * a destra, l'anello della quota 5 h dell'account scelto; sotto la scheda etichetta + valore. Tap sulla scheda → app.
 */
class CmTileService : TileService() {

    private val scheme = ColorScheme(
        primary = 0xFF4C7DFF.toInt().argb, onPrimary = 0xFFF2F4F7.toInt().argb,
        primaryContainer = 0xFF83CEFF.toInt().argb, onPrimaryContainer = 0xFF001D33.toInt().argb,
        surfaceContainer = 0xFF2B303A.toInt().argb, onSurface = 0xFFF2F4F7.toInt().argb, onSurfaceVariant = 0xFF9AA3B2.toInt().argb,
        background = 0xFF000000.toInt().argb, onBackground = 0xFFF2F4F7.toInt().argb,
    )

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        CallbackToFutureAdapter.getFuture { completer ->
            val app = application as CmApp
            val snap = app.repo.snapshot.value
            val prefs = kotlinx.coroutines.runBlocking { app.prefs.current() }
            val labels = TileTexts.Labels(none = getString(R.string.sessions_empty), stale = getString(R.string.tile_stale), works = getString(R.string.state_busy).lowercase())
            val g = TileTexts.glance(snap.state, snap.freshness, System.currentTimeMillis() / 1000, labels, prefs.seenQuestions)
            val state = snap.state
            val questions = state?.sessions?.count { s -> s.question?.let { q -> q.id !in prefs.seenQuestions } == true } ?: 0
            val busy = state?.sessions?.count { it.state == SessionState.BUSY || it.state == SessionState.AWAITING } ?: 0
            val big = when { snap.freshness is Freshness.Stale || state == null -> "—"; questions > 0 -> "$questions ❓"; else -> "$busy ▶" }
            val quota = state?.quota?.get(prefs.complicationAccount)
            val accent = when (g.accent) {
                TileTexts.Accent.QUESTION -> 0xFFFFB020; TileTexts.Accent.BUSY -> 0xFF7FA1FF
                TileTexts.Accent.IDLE -> 0xFF34C759; TileTexts.Accent.STALE -> 0xFF6B7280
            }.toInt().argb
            val bottomLabel = when {
                snap.freshness is Freshness.Stale -> getString(R.string.tile_stale_short)
                g.accent == TileTexts.Accent.QUESTION -> getString(R.string.tile_label_question)
                else -> getString(R.string.tile_label_last)
            }
            val root = materialScope(this, requestParams.deviceConfiguration, allowDynamicTheme = false, defaultColorScheme = scheme) {
                primaryLayout(
                    titleSlot = {
                        LayoutElementBuilders.Column.Builder().setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                            .addContent(icon(APP_ICON_RES, APP_ICON, dp(22f), dp(22f), colorScheme.onSurface))
                            .addContent(text(getString(R.string.sessions_title).layoutString, typography = Typography.LABEL_MEDIUM, color = colorScheme.onSurface, maxLines = 1))
                            .build()
                    },
                    mainSlot = {
                        card(
                            onClick = clickable(launch(g.target), id = "card"), width = expand(), height = wrap(),
                            contentPadding = ModifiersBuilders.Padding.Builder().setStart(dp(16f)).setEnd(dp(14f)).setTop(dp(12f)).setBottom(dp(12f)).build(),
                        ) {
                            LayoutElementBuilders.Row.Builder().setWidth(expand()).setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                                .addContent(
                                    LayoutElementBuilders.Column.Builder().setWidth(weight(1f)).setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_START)
                                        .addContent(text(big.layoutString, typography = Typography.DISPLAY_SMALL, color = colorScheme.onSurface, maxLines = 1))
                                        .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(6f)).build())
                                        .addContent(chip(g.name ?: g.body, accent))
                                        .build()
                                )
                                .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(8f)).build())
                                .addContent(ring(quota?.h5))
                                .build()
                        }
                    },
                    bottomSlot = {
                        LayoutElementBuilders.Column.Builder().setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                            .addContent(text(bottomLabel.layoutString, typography = Typography.BODY_MEDIUM, color = colorScheme.onSurface, maxLines = 1))
                            .addContent(text((if (snap.freshness is Freshness.Stale) g.body else g.name ?: g.body).layoutString, typography = Typography.TITLE_MEDIUM, color = accent, maxLines = 1))
                            .build()
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

    /** Chip come «Ancora 3…» della tile Passi: pillola chiara con il testo scuro. */
    private fun MaterialScope.chip(label: String, accent: androidx.wear.protolayout.types.LayoutColor): LayoutElement =
        text(
            label.layoutString, typography = Typography.LABEL_MEDIUM, color = colorScheme.onPrimaryContainer, maxLines = 1,
            modifier = LayoutModifier.background(colorScheme.primaryContainer).clip(14f)
                .padding(ModifiersBuilders.Padding.Builder().setStart(dp(10f)).setEnd(dp(10f)).setTop(dp(4f)).setBottom(dp(4f)).build()),
        )

    /** Anello della quota 5 h dell'account scelto (come l'obiettivo dei passi). */
    private fun MaterialScope.ring(h5: Int?): LayoutElement =
        circularProgressIndicator(staticProgress = QuotaText.fraction(h5), size = dp(64f), strokeWidth = 7f)

    private fun launch(uri: String): ActionBuilders.LaunchAction = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder().setPackageName(packageName).setClassName(MainActivity::class.java.name)
                .addKeyToExtraMapping(MainActivity.EXTRA_URI, ActionBuilders.stringExtra(uri)).build()
        ).build()

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        CallbackToFutureAdapter.getFuture { c ->
            c.set(ResourceBuilders.Resources.Builder().setVersion(RESOURCES).addIdToImageMapping(APP_ICON, APP_ICON_RES).build()); "res"
        }

    companion object {
        const val RESOURCES = "3"
        const val APP_ICON = "app"
        val APP_ICON_RES: ResourceBuilders.ImageResource = ResourceBuilders.ImageResource.Builder()
            .setAndroidResourceByResId(ResourceBuilders.AndroidImageResourceByResId.Builder().setResourceId(R.drawable.ic_notification).build()).build()
        fun requestUpdate(app: CmApp) = getUpdater(app).requestUpdate(CmTileService::class.java)
    }
}
