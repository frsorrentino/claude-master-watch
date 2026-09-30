package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Accounts
import it.pixelbox.cmwatch.rules.BriefCards
import it.pixelbox.cmwatch.rules.ModelText
import it.pixelbox.cmwatch.rules.SessionMeters
import it.pixelbox.cmwatch.rules.SessionsText
import it.pixelbox.cmwatch.ui.tokens.CmColors

fun stateColor(s: SessionState): Color = when (s) {
    SessionState.WAITING -> CmColors.waiting
    SessionState.BUSY, SessionState.AWAITING -> CmColors.busy
    SessionState.IDLE -> CmColors.stale
    SessionState.GONE -> CmColors.goneDim
}

fun stateGlyph(s: SessionState): String = when (s) {
    SessionState.WAITING -> "❓"
    SessionState.BUSY, SessionState.AWAITING -> "▶"
    SessionState.IDLE -> "✓"
    SessionState.GONE -> "✗"
}

@Composable
fun stateLabel(s: SessionState): String = stringResource(when (s) {
    SessionState.WAITING -> R.string.state_waiting
    SessionState.BUSY, SessionState.AWAITING -> R.string.state_busy
    SessionState.IDLE -> R.string.state_idle
    SessionState.GONE -> R.string.state_closed
})

@Composable
fun AccountDot(personal: Boolean, modifier: Modifier = Modifier) =
    Box(modifier.size(10.dp).background(if (personal) CmColors.accountPersonale else CmColors.accountAgenzia, CircleShape))

/**
 * Card della regia come la cella dell'orologio (restyling 30/09): account, nome, stato ed età in testa; sotto cosa sta
 * facendo e cosa segue (`SessionsText.cell`), l'obiettivo, la bassa priorità e la barretta del contesto. La forma segue lo
 * stato: chi aspetta te è più morbida, in rilievo e con il bordo ambra; la chiusa è piatta.
 */
@Composable
fun SessionCard(s: Session, now: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val working = s.state == SessionState.BUSY || s.state == SessionState.AWAITING
    val breath = if (working && !animationsOff()) {
        val t = rememberInfiniteTransition(label = "breath")
        val a by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(2400, easing = CmMotion.easing), RepeatMode.Reverse), label = "a")
        a
    } else 1f
    val since = when (s.state) {
        SessionState.WAITING -> s.question?.askedAt ?: s.since
        SessionState.BUSY, SessionState.AWAITING -> s.turnStarted ?: s.since
        else -> s.since
    }
    val cell = SessionsText.cell(s, now, stringResource(R.string.turn_running), stringResource(R.string.state_idle))
    val goal = SessionsText.goalLine(s, stringResource(R.string.goal))
    val priority = SessionsText.priority(s, stringResource(R.string.low_priority), stringResource(R.string.low_priority_offered))
    val waiting = s.state == SessionState.WAITING || s.question != null
    val closed = s.state == SessionState.GONE
    Surface(
        onClick = onClick,
        color = when { waiting -> CmColors.surfaceHigh; closed -> CmColors.surfaceLow; else -> CmColors.surface },
        shape = if (waiting) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large,
        border = when {
            waiting -> BorderStroke(2.dp, CmColors.waiting)
            s.followed -> BorderStroke(1.5.dp, CmColors.followed)
            else -> null
        },
        shadowElevation = if (waiting) 6.dp else 0.dp,
        modifier = modifier.fly("card-${s.id}").fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccountDot(Accounts.isPersonal(s))
                Text(
                    s.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (closed) CmColors.text2 else CmColors.text, modifier = Modifier.weight(1f),
                )
                StatePill(s.state, Durations.since(since, now), Modifier.alpha(breath))
            }
            cell.title?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge, color = if (closed) CmColors.text2 else CmColors.text)
            }
            cell.detail?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2) }
            goal?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = CmColors.briefLabel) }
            priority?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = CmColors.waiting) }
            if (!closed) s.context?.let { ContextBar(it, ModelText.short(s.model)) }
        }
    }
}

/** Lo stato come pillola colorata con l'icona e l'età, come il badge dell'orologio. */
@Composable
fun StatePill(state: SessionState, age: String, modifier: Modifier = Modifier) {
    Row(
        modifier.background(stateColor(state).copy(alpha = 0.16f), CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(stateIcon(state), null, tint = stateColor(state), modifier = Modifier.size(16.dp))
        Text("${stateLabel(state)} · $age", style = MaterialTheme.typography.labelLarge, color = stateColor(state))
    }
}

/** Le icone di stato con gli stessi significati di Telegram: ❓ aspetta, ▶ lavora, ✓ ferma, ✗ chiusa. */
fun stateIcon(s: SessionState): ImageVector = when (s) {
    SessionState.WAITING -> Icons.AutoMirrored.Rounded.HelpOutline
    SessionState.BUSY, SessionState.AWAITING -> Icons.Rounded.PlayArrow
    SessionState.IDLE -> Icons.Rounded.Check
    SessionState.GONE -> Icons.Rounded.Close
}

/** La barretta del contesto nel colore delle soglie (`SessionMeters`), con percentuale e modello accanto. */
@Composable
fun ContextBar(pct: Int, model: String?) {
    val tone = when (SessionMeters.contextTone(pct)) {
        BriefCards.Tone.ALERT -> CmColors.briefAlertRing
        BriefCards.Tone.WARN -> CmColors.briefWarn
        else -> CmColors.briefRing
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f).height(6.dp).background(CmColors.briefTrack, CircleShape)) {
            Box(Modifier.fillMaxWidth(SessionMeters.contextFraction(pct) ?: 0f).fillMaxHeight().background(tone, CircleShape))
        }
        Text(listOfNotNull(stringResource(R.string.context_pct, pct), model).joinToString(" · "), style = MaterialTheme.typography.labelLarge, color = CmColors.text2)
    }
}
