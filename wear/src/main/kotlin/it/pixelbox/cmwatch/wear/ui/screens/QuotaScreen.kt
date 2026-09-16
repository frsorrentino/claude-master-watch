package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.BriefCards
import it.pixelbox.cmwatch.rules.DayBars
import it.pixelbox.cmwatch.rules.QuotaHistory
import it.pixelbox.cmwatch.rules.WorkPanel
import it.pixelbox.cmwatch.wear.ui.ambient.animationsOff
import it.pixelbox.cmwatch.wear.ui.ambient.rememberAmbient
import it.pixelbox.cmwatch.wear.ui.components.BriefCard
import it.pixelbox.cmwatch.wear.ui.components.ContextListCard
import it.pixelbox.cmwatch.wear.ui.components.NightWorkCard
import it.pixelbox.cmwatch.wear.ui.components.NowCard
import it.pixelbox.cmwatch.wear.ui.components.PaceCard
import it.pixelbox.cmwatch.wear.ui.components.QuestionsWorkCard
import it.pixelbox.cmwatch.wear.ui.components.TodayCard
import it.pixelbox.cmwatch.wear.ui.components.UpdatedFooter
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.morph
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Quota e lavoro nello stile del «brief mattutino» di Wear OS (Franz, 13/09 16:16). Sezione Quota: una card per account
 * con il doppio anello, e sotto il ritmo della finestra di 5 ore. Sezione Lavoro rifatta (16/09 13:00, «ok tutte»):
 * una grafica per dato — «Adesso» a segmenti, le domande quando ci sono, il contesto per sessione, «Oggi» a colonne, la
 * notte quando c'è — e in fondo la riga dell'aggiornamento al posto della card «Aggiornato».
 */
@Composable
fun QuotaScreen(
    state: State?,
    freshness: Freshness,
    now: Long = System.currentTimeMillis() / 1000,
    /** Gli eventi salvati sull'orologio (7 giorni): le colonne di «Oggi». */
    events: List<Event> = emptyList(),
    /** I campioni della quota registrati dall'orologio, per account: il ritmo della finestra. */
    samples: Map<String, List<QuotaHistory.Sample>> = emptyMap(),
    onOpenQuestion: (String) -> Unit = {},
    onOpenSessions: () -> Unit = {},
    /**
     * Negli snapshot si passa `false`: Paparazzi fotografa il primo fotogramma, e con l'animazione attiva archi e barre
     * sono ancora a zero (visto il 16/09 03:44). In app resta `null`: niente in ambient, niente con le animazioni spente.
     */
    animateOverride: Boolean? = null,
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val labels = BriefCards.Labels(
        quota = stringResource(R.string.quota_label), week = stringResource(R.string.quota_week_pill),
        resetAt = stringResource(R.string.quota_reset_at), stale = stringResource(R.string.quota_stale),
        none = stringResource(R.string.quota_none),
        active = stringResource(R.string.brief_active), waitingPill = stringResource(R.string.brief_waiting),
        noQuestions = stringResource(R.string.brief_no_questions), questions = stringResource(R.string.brief_questions),
        oldest = stringResource(R.string.brief_oldest), night = stringResource(R.string.brief_night),
        running = stringResource(R.string.brief_running), nothingRunning = stringResource(R.string.brief_nothing_running),
        update = stringResource(R.string.brief_update), minutes = stringResource(R.string.brief_minutes),
        now = stringResource(R.string.brief_now), stopped = stringResource(R.string.brief_stopped),
        weekOnly = stringResource(R.string.tile_quota_tag_week), quotaTitle = stringResource(R.string.quota_title),
    )
    val animate = animateOverride ?: (!rememberAmbient() && !animationsOff())
    val locale = LocalConfiguration.current.locales[0]
    val zone = ZoneId.systemDefault()
    val quota = BriefCards.quota(state, labels, locale = locale)
    // Il ritmo per gli account che hanno la lettura delle 5 ore e la sua ripartenza: senza, non c'è una finestra da seguire.
    val ritmi = state?.quota.orEmpty().filter { (_, q) -> q.h5 != null && q.resetH5 != null }.map { (account, q) ->
        Triple(q, QuotaHistory.pace(samples[account].orEmpty(), q.resetH5!!, now), account)
    }
    val oraReset = DateTimeFormatter.ofPattern("HH:mm", locale)
    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            // «Panoramica» (Franz, 16/09 14:33): la pagina è quota, ritmo e lavoro insieme; «Quota» diventa la sua prima sezione.
            item {
                ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) {
                    Text(stringResource(R.string.overview_title))
                }
            }
            items(quota.size) { i ->
                BriefCard(quota[i], SurfaceTransformation(spec), Modifier.transformedHeight(this, spec), animate = animate)
            }
            items(ritmi.size) { i ->
                val (q, pace, account) = ritmi[i]
                PaceCard(
                    pace = pace, current = q.h5!!, resetAt = q.resetH5!!, now = now,
                    startLabel = oraReset.format(Instant.ofEpochSecond(q.resetH5!! - QuotaHistory.WINDOW_S).atZone(zone)),
                    resetLabel = oraReset.format(Instant.ofEpochSecond(q.resetH5!!).atZone(zone)),
                    shape = if (it.pixelbox.cmwatch.rules.Accounts.isPersonalQuota(account, q)) BriefCards.Shape.CIRCLE else BriefCards.Shape.SQUARE,
                    transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec), animate = animate,
                )
            }
            if (state != null) {
                item {
                    ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) {
                        Text(stringResource(R.string.brief_section_work))
                    }
                }
                item {
                    NowCard(WorkPanel.now(state), SurfaceTransformation(spec), Modifier.transformedHeight(this, spec), animate = animate, onClick = onOpenSessions)
                }
                WorkPanel.questions(state, now)?.let { q ->
                    item {
                        QuestionsWorkCard(q, SurfaceTransformation(spec), Modifier.transformedHeight(this, spec), animate = animate, onClick = { onOpenQuestion(q.oldest) })
                    }
                }
                val contesti = WorkPanel.contexts(state)
                if (contesti.isNotEmpty()) item {
                    ContextListCard(contesti, SurfaceTransformation(spec), Modifier.transformedHeight(this, spec), animate = animate)
                }
                item {
                    TodayCard(DayBars.today(events, now, zone), SurfaceTransformation(spec), Modifier.transformedHeight(this, spec), animate = animate)
                }
                WorkPanel.night(state)?.let { n ->
                    item { NightWorkCard(n, SurfaceTransformation(spec), Modifier.transformedHeight(this, spec), animate = animate) }
                }
                item {
                    UpdatedFooter(WorkPanel.updated(state, freshness, now), Modifier.morph(this, spec).padding(top = 4.dp))
                }
            }
            if (quota.isEmpty() && state == null) {
                item {
                    Text(
                        stringResource(R.string.quota_none), style = MaterialTheme.typography.bodyMedium,
                        color = CmColors.text2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().morph(this, spec),
                    )
                }
            }
        }
    }
}
