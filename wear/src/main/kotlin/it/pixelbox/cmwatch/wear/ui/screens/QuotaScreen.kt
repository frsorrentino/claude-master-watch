package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
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
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.BriefCards
import it.pixelbox.cmwatch.wear.ui.ambient.animationsOff
import it.pixelbox.cmwatch.wear.ui.ambient.rememberAmbient
import it.pixelbox.cmwatch.wear.ui.components.BriefCard
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.morph

/**
 * Quota e stato del lavoro nello stile del «brief mattutino» di Wear OS, che Franz vuole identico (13/09 16:16):
 * una card per dato, con etichetta verde, numero grande, pillolina e anello. Due sezioni: la quota di ogni account,
 * poi il lavoro (attive, domande, coda della notte, freschezza del PC). Cosa si vede lo decide `BriefCards`.
 */
@Composable
fun QuotaScreen(state: State?, freshness: Freshness, now: Long = System.currentTimeMillis() / 1000) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val labels = BriefCards.Labels(
        quota = stringResource(R.string.quota_label), week = stringResource(R.string.quota_week_chip),
        resetAt = stringResource(R.string.quota_reset_at), stale = stringResource(R.string.quota_stale),
        none = stringResource(R.string.quota_none),
        active = stringResource(R.string.brief_active), waitingPill = stringResource(R.string.brief_waiting),
        noQuestions = stringResource(R.string.brief_no_questions), questions = stringResource(R.string.brief_questions),
        oldest = stringResource(R.string.brief_oldest), night = stringResource(R.string.brief_night),
        running = stringResource(R.string.brief_running), nothingRunning = stringResource(R.string.brief_nothing_running),
        update = stringResource(R.string.brief_update), minutes = stringResource(R.string.brief_minutes),
        now = stringResource(R.string.brief_now), stopped = stringResource(R.string.brief_stopped),
        weekOnly = stringResource(R.string.quota_week),
    )
    // Niente animazioni in ambient né con le animazioni di sistema spente (design, sezione 3).
    val animate = !rememberAmbient() && !animationsOff()
    // Giorni della settimana nella lingua delle risorse: con l'inglese «reset Thu 02:00», non «gio» (14/09 23:43).
    val quota = BriefCards.quota(state, labels, locale = LocalConfiguration.current.locales[0])
    val work = BriefCards.work(state, freshness, now, labels)
    // Quali elementi sono nell'inquadratura: l'arco di una card si riempie quando la sua entra (Franz, 16/09 01:45).
    // L'indice 0 è l'intestazione, poi le card della quota, poi l'intestazione del lavoro e le sue card.
    // Finché la lista non ha misurato niente (primo disegno, e in Paparazzi sempre) si considerano tutte visibili:
    // altrimenti l'arco resterebbe a zero e le immagini del README mostrerebbero anelli vuoti (16/09 03:31).
    val misurata = listState.layoutInfo.visibleItems.isNotEmpty()
    val visibili = listState.layoutInfo.visibleItems.map { it.index }.toSet()
    fun visibile(i: Int) = !misurata || i in visibili
    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item {
                ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) {
                    Text(stringResource(R.string.quota_title))
                }
            }
            items(quota.size) { i ->
                BriefCard(quota[i], SurfaceTransformation(spec), Modifier.transformedHeight(this, spec), animate = animate, visible = visibile(1 + i))
            }
            if (work.isNotEmpty()) {
                item {
                    ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) {
                        Text(stringResource(R.string.brief_section_work))
                    }
                }
                items(work.size) { i ->
                    // Dopo l'intestazione della quota, le sue card e l'intestazione del lavoro: 2 + quante sono le card sopra.
                    BriefCard(work[i], SurfaceTransformation(spec), Modifier.transformedHeight(this, spec), animate = animate, visible = visibile(2 + quota.size + i))
                }
            }
            if (quota.isEmpty() && work.isEmpty()) {
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
