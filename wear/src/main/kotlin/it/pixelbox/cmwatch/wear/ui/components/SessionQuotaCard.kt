package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.BriefCards
import it.pixelbox.cmwatch.wear.ui.theme.CmColors

/**
 * Il riquadro di contesto nella Scheda (Franz, 16/09 00:14): la quota dell'account di questa sessione, con l'anello
 * concentrico delle 5 ore e della settimana, e sotto quanto contesto ha consumato, con quale modello e con quale
 * effort (contratto 1.11). Quello che il PC non manda non si disegna: niente numeri inventati su questa schermata.
 */
@Composable
fun SessionQuotaCard(
    state: State?,
    session: Session,
    transformation: SurfaceTransformation?,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    visible: Boolean = true,
) {
    val card = BriefCards.quota(state, labels(), locale = LocalConfiguration.current.locales[0])
        .firstOrNull { it.key == "quota-${session.account}" }
    // Le tre righe stanno DENTRO il riquadro, con i margini della card: a piena larghezza il bordo tondo mangiava le
    // etichette a sinistra e i valori a destra («Model» → «odel», «high» → «h»), visto nello snapshot delle 04:26.
    Column(
        modifier.fillMaxWidth().padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        card?.let { BriefCard(it, transformation, animate = animate, visible = visible) }
        val righe = listOfNotNull(
            session.context?.let { stringResource(R.string.card_context) to stringResource(R.string.card_context_pct, it) },
            session.model?.let { stringResource(R.string.card_model) to it.label },
            session.effort?.let { stringResource(R.string.card_effort) to it },
        )
        righe.forEach { (etichetta, valore) ->
            Row(
                // Nessun margine inventato qui: i margini laterali li porta la lista con `morph`, come per ogni voce.
                Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(etichetta, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1)
                Text(
                    valore, style = MaterialTheme.typography.bodySmall,
                    // Il contesto oltre tre quarti è da guardare, oltre il 90 % è il momento di chiudere il turno.
                    color = when {
                        etichetta != stringResource(R.string.card_context) -> CmColors.text
                        (session.context ?: 0) >= 90 -> CmColors.gone
                        (session.context ?: 0) >= 75 -> CmColors.waiting
                        else -> CmColors.text
                    },
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun labels() = BriefCards.Labels(
    quota = stringResource(R.string.quota_title), week = stringResource(R.string.quota_week),
    resetAt = stringResource(R.string.quota_reset_at), stale = stringResource(R.string.quota_stale),
    none = stringResource(R.string.quota_none), active = stringResource(R.string.brief_active),
    waitingPill = stringResource(R.string.brief_waiting), noQuestions = stringResource(R.string.brief_no_questions),
    questions = stringResource(R.string.brief_questions), oldest = stringResource(R.string.brief_oldest),
    night = stringResource(R.string.brief_night), running = stringResource(R.string.brief_running),
    nothingRunning = stringResource(R.string.brief_nothing_running), update = stringResource(R.string.brief_update),
    minutes = stringResource(R.string.brief_minutes), now = stringResource(R.string.brief_now),
    stopped = stringResource(R.string.brief_stopped), weekOnly = stringResource(R.string.quota_week),
)
