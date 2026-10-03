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

/** Lo stato come pillola colorata con l'icona e l'età, come il badge dell'orologio. */
@Composable
fun StatePill(state: SessionState, age: String, modifier: Modifier = Modifier) {
    Row(
        modifier.background(stateColor(state).copy(alpha = 0.16f), CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("${stateLabel(state)} · $age", style = MaterialTheme.typography.labelLarge, color = stateColor(state))
    }
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
