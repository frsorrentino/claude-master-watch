package it.pixelbox.cmwatch.wear.tile

import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.CardColors
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.appCard
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.PrimaryLayoutMargins
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.LayoutColor
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
import it.pixelbox.cmwatch.rules.QuotaBar
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

    /**
     * Ruoli M3 in tema scuro con i valori misurati sulla tile di Gmail (13/09): card #2A313C, oggetto #EBF1FF
     * (contrasto 11,6:1), mittente #D3E3FD (10,1:1), ora #C2C6D2 (8:1), bottone di bordo #D3E3FD con testo blu notte.
     * I grigi di prima stavano a 5,5:1 e si leggevano male (Franz, 13/09 15:31).
     */
    private val scheme = ColorScheme(
        primary = 0xFFD3E3FD.toInt().argb, onPrimary = 0xFF0A2050.toInt().argb,
        primaryContainer = 0xFF2B4A9E.toInt().argb, onPrimaryContainer = 0xFFDCE6FF.toInt().argb,
        surfaceContainer = 0xFF2A313C.toInt().argb, surfaceContainerLow = 0xFF23272E.toInt().argb,
        onSurface = 0xFFEBF1FF.toInt().argb, onSurfaceVariant = 0xFFC2C6D2.toInt().argb,
        background = 0xFF000000.toInt().argb, onBackground = 0xFFFFFFFF.toInt().argb,
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
                        margins = PrimaryLayoutMargins.MIN_PRIMARY_LAYOUT_MARGIN,
                        // Niente titolo: «N sessioni» era la riga che valeva meno (il conteggio sta sul bottone di
                        // bordo e Wear OS scrive già il nome dell'app sopra), e la sua riga serviva alla card della
                        // quota, che restava tagliata in basso (Franz, 13/09 17:15).
                        titleSlot = null,
                        mainSlot = {
                            when {
                                question != null -> questionCard(question, now)
                                state == null || state.sessions.isEmpty() -> emptyCard()
                                else -> restCards(state, now, prefs.complicationAccount, stale)
                            }
                        },
                        bottomSlot = { edgeButton(edge, question?.name) },
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("cmwatch", "tile layout failed, fallback", e)
                materialScope(this, requestParams.deviceConfiguration, allowDynamicTheme = false, defaultColorScheme = scheme) {
                    primaryLayout(margins = PrimaryLayoutMargins.MIN_PRIMARY_LAYOUT_MARGIN, mainSlot = { emptyCard() }, bottomSlot = { edgeButton(TileTexts.Edge(TileTexts.Edge.Kind.SESSIONS, 0), null) })
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

    /** Come Gmail: fondo sulla superficie, titolo chiaro, etichette e ora in grigio. Il default di appCard usa il primario. */
    private fun MaterialScope.cardColors() = CardColors(
        backgroundColor = colorScheme.surfaceContainer,
        titleColor = colorScheme.onSurface,
        contentColor = colorScheme.onSurfaceVariant,
        timeColor = colorScheme.onSurfaceVariant,
        labelColor = LABEL.argb,
    )

    /** La domanda: chi e da quanto sopra, il testo intero sotto. */
    private fun MaterialScope.questionCard(s: Session, now: Long): LayoutElement = appCard(
        onClick = clickable(launch("cmwatch://question/${s.name}"), id = "q"),
        label = { small("${TileTexts.badge(s)} ${NameText.shorten(s.name, listOf(s.name), 16)}", LABEL.argb) },
        time = { small(Durations.since(s.question?.askedAt ?: s.since, now), AMBER.argb) },
        title = { text(s.question!!.text.layoutString, typography = Typography.TITLE_MEDIUM, color = colorScheme.onSurface, maxLines = 3) },
        colors = cardColors(),
    )

    /** A riposo: cosa succede adesso e la quota, due card come le due mail di Gmail. */
    private fun MaterialScope.restCards(state: State, now: Long, account: String, stale: Freshness.Stale?): LayoutElement {
        val q = state.quota[account]
        val col = LayoutElementBuilders.Column.Builder().setWidth(expand())
        // Con il dato vecchio le età si leggono sull'istante della fotografia, non su adesso: altrimenti tutto
        // sembrerebbe antico. La vecchiaia la dice l'etichetta in ambra (Franz, 13/09 18:27).
        val quando = if (stale != null) state.ts else now
        col.addContent(
            when (val rest = TileTexts.rest(state, quando)) {
                is TileTexts.Rest.Live -> sessionCard(rest.session, rest.busy, quando, stale)
                is TileTexts.Rest.Calm -> calmCard(rest, quando, stale)
            }
        )
        if (q != null) {
            col.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build())
            col.addContent(quotaCard(account, q))
        }
        return col.build()
    }

    /** La sessione: chi e da quanto sopra, cosa sta facendo o cosa ha fatto sotto. Mai l'etichetta secca dello stato. */
    private fun MaterialScope.sessionCard(s: Session, busy: Boolean, now: Long, stale: Freshness.Stale?): LayoutElement {
        val what = TileTexts.activity(s, busy, getString(R.string.tile_turn_running), getString(R.string.state_idle))
        return appCard(
            onClick = clickable(launch("cmwatch://session/${s.name}"), id = "s"),
            label = {
                if (stale != null) small(getString(R.string.tile_stale_label, stale.minutes), AMBER.argb)
                else small("${TileTexts.badge(s)} ${NameText.shorten(s.name, listOf(s.name), 16)}", LABEL.argb)
            },
            time = { small(Durations.since(if (busy) s.turnStarted ?: s.since else s.since, now), colorScheme.onSurfaceVariant) },
            // Due righe: la barra della quota è più bassa di una riga di testo e lo spazio guadagnato va qui (Franz, 13/09 16:14).
            title = { text(what.layoutString, typography = Typography.BODY_LARGE, color = colorScheme.onSurface, maxLines = 2) },
            colors = cardColors(),
        )
    }

    /**
     * Niente di recente: si dice lo stato vero. Ripescare un esito di ore prima faceva sembrare la tile vecchia e
     * inutile (Franz, 13/09 17:55).
     */
    private fun MaterialScope.calmCard(rest: TileTexts.Rest.Calm, now: Long, stale: Freshness.Stale?): LayoutElement = appCard(
        onClick = clickable(launch("cmwatch://sessions"), id = "calm"),
        label = {
            if (stale != null) small(getString(R.string.tile_stale_label, stale.minutes), AMBER.argb)
            else small(getString(R.string.tile_title_count, rest.sessions), LABEL.argb)
        },
        time = { small(rest.since?.let { Durations.since(it, now) } ?: "", colorScheme.onSurfaceVariant) },
        title = { text(getString(R.string.tile_all_idle).layoutString, typography = Typography.BODY_LARGE, color = colorScheme.onSurface, maxLines = 1) },
        colors = cardColors(),
    )

    /**
     * Quota: percentuale grande e barra lineare accanto, come nell'anteprima approvata da Franz (13/09). L'anello
     * stava dentro una `graphicDataCard`, più alta della card di Gmail, e faceva tagliare la seconda card.
     */
    private fun MaterialScope.quotaCard(account: String, q: QuotaAccount): LayoutElement = appCard(
        onClick = clickable(launch("cmwatch://quota"), id = "quota"),
        label = { small(getString(R.string.quota_label, account), LABEL.argb) },
        time = {
            small(
                q.resetW7?.let { getString(R.string.quota_reset_at, HHMM.format(Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()))) } ?: "",
                colorScheme.onSurfaceVariant,
            )
        },
        title = {
            LayoutElementBuilders.Row.Builder()
                .setWidth(expand())
                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                .addContent(
                    text(
                        getString(R.string.tile_quota_pct, q.h5 ?: 0).layoutString,
                        typography = Typography.TITLE_MEDIUM, color = colorScheme.onSurface, maxLines = 1,
                    )
                )
                .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(10f)).build())
                .addContent(bar(q.h5))
                .build()
        },
        colors = cardColors(),
    )

    /**
     * Barra lineare della quota. ProtoLayout Material 3 1.4.2 ha solo l'indicatore circolare, quindi la barra è una
     * riga di due pilloline pesate — riempita e traccia — con lo stacco in mezzo come negli indicatori lineari M3.
     */
    private fun MaterialScope.bar(pct: Int?): LayoutElement {
        val spec = QuotaBar.of(pct)
        val row = LayoutElementBuilders.Row.Builder().setWidth(expand()).setHeight(dp(BAR_H))
        if (spec.fill > 0) row.addContent(pill(weight(spec.fill.toFloat()), colorScheme.primary))
        if (spec.gap) row.addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(4f)).build())
        if (spec.track > 0) row.addContent(pill(weight(spec.track.toFloat()), TRACK.argb))
        return row.build()
    }

    private fun pill(width: DimensionBuilders.ExpandedDimensionProp, color: LayoutColor): LayoutElement =
        LayoutElementBuilders.Box.Builder()
            .setWidth(width).setHeight(dp(BAR_H))
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder().setBackground(
                    ModifiersBuilders.Background.Builder()
                        .setColor(color.prop)
                        .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(BAR_H / 2f)).build())
                        .build()
                ).build()
            ).build()

    private fun MaterialScope.emptyCard(): LayoutElement = appCard(
        onClick = clickable(launch("cmwatch://sessions"), id = "empty"),
        label = { small(getString(R.string.sessions_title), LABEL.argb) },
        title = { text(getString(R.string.sessions_empty).layoutString, typography = Typography.TITLE_MEDIUM, color = colorScheme.onSurface, maxLines = 2) },
        colors = cardColors(),
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
        const val RESOURCES = "14"
        private const val AMBER = 0xFFFFB020.toInt()
        private const val LABEL = 0xFFD3E3FD.toInt()   // mittente di Gmail: 10,1:1 sulla card
        private const val TRACK = 0xFF3C4452.toInt()   // traccia della barra: visibile sulla card #2A313C
        private const val BAR_H = 10f
        private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
        fun requestUpdate(app: CmApp) = getUpdater(app).requestUpdate(CmTileService::class.java)
    }
}
