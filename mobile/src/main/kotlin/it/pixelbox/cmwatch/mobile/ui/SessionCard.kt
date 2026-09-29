package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Accounts
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

/** Card della regia: account, stato, nome intero; sotto cosa sta facendo o l'esito breve (design 29/09). */
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
    val detail = when {
        working -> s.toolNote ?: s.tool
        s.state == SessionState.IDLE -> s.outcome?.short
        s.state == SessionState.WAITING -> s.question?.text?.lineSequence()?.firstOrNull()
        else -> null
    }
    Surface(color = CmColors.surface, shape = RoundedCornerShape(20.dp), modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().alpha(breath).background(stateColor(s.state)))
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountDot(Accounts.isPersonal(s))
                    Text(stateGlyph(s.state), color = stateColor(s.state))
                    Text(s.name, style = MaterialTheme.typography.titleMedium, color = CmColors.text)
                }
                Text("${stateLabel(s.state)} · ${Durations.since(since, now)}", style = MaterialTheme.typography.bodyMedium, color = stateColor(s.state))
                detail?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2) }
            }
        }
    }
}
