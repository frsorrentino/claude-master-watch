package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.SurfaceTransformation
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.BriefCards

/**
 * Il riquadro di contesto nella Scheda (Franz, 16/09 00:14 e 03:06): la quota dell'account di quella sessione, con
 * l'anello concentrico delle 5 ore e della settimana, così mentre leggi cosa sta facendo vedi anche quanto margine hai.
 * Modello, effort e contesto della sessione entrano qui appena arriva il contratto 1.11: finché il PC non li manda non
 * si disegnano, perché un dato inventato su questa schermata varrebbe meno di niente.
 */
@Composable
fun SessionQuotaCard(
    state: State?,
    account: String,
    transformation: SurfaceTransformation?,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    visible: Boolean = true,
) {
    val card = BriefCards.quota(state, labels(), locale = LocalConfiguration.current.locales[0])
        .firstOrNull { it.key == "quota-$account" } ?: return
    BriefCard(card, transformation, modifier, animate = animate, visible = visible)
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
