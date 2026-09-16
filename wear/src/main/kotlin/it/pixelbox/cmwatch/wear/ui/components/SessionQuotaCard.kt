package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.SurfaceTransformation
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.BriefCards

/**
 * La quota dell'account di questa sessione nella Scheda. Stessa card della pagina Quota ma senza la riga della
 * ripartenza settimanale in fondo, così ha la struttura della card del contesto che la segue: etichetta, numero
 * grande, una riga (qui il reset delle 5 ore, là la barra), una pillola (Franz, 16/09 11:31: la versione in una riga
 * era «troppo ridotta», serve una via di mezzo uniforme alla card del contesto). L'anello resta pieno, a destra.
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
        .firstOrNull { it.key == "quota-${session.account}" } ?: return
    // La ripartenza settimanale resta nella pagina Quota: nella Scheda era la riga che rendeva la card più alta di tutte.
    BriefCard(card.copy(note = null), transformation, modifier, animate = animate, visible = visible)
}

@Composable
private fun labels() = BriefCards.Labels(
    quota = stringResource(R.string.quota_title),
    // Con il segnaposto: «settimana 36 %». Con il testo senza numero la pillola diceva solo «settimana» (foto 10:44).
    week = stringResource(R.string.quota_week_chip),
    resetAt = stringResource(R.string.quota_reset_at), stale = stringResource(R.string.quota_stale),
    none = stringResource(R.string.quota_none), active = stringResource(R.string.brief_active),
    waitingPill = stringResource(R.string.brief_waiting), noQuestions = stringResource(R.string.brief_no_questions),
    questions = stringResource(R.string.brief_questions), oldest = stringResource(R.string.brief_oldest),
    night = stringResource(R.string.brief_night), running = stringResource(R.string.brief_running),
    nothingRunning = stringResource(R.string.brief_nothing_running), update = stringResource(R.string.brief_update),
    minutes = stringResource(R.string.brief_minutes), now = stringResource(R.string.brief_now),
    stopped = stringResource(R.string.brief_stopped), weekOnly = stringResource(R.string.quota_week),
)
