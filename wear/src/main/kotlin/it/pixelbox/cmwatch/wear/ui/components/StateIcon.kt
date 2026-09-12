package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.wear.ui.theme.stateColor

/** ❓ ▶ ✓ ✗ come icone, stessi significati di Telegram. */
@Composable
fun StateIcon(state: SessionState, fresh: Boolean = true, modifier: Modifier = Modifier) {
    val (vector, label) = when (state) {
        SessionState.WAITING -> Icons.Rounded.Help to R.string.state_waiting
        SessionState.BUSY -> Icons.Rounded.PlayArrow to R.string.state_busy
        SessionState.IDLE, SessionState.AWAITING -> Icons.Rounded.Check to R.string.state_idle
        SessionState.GONE -> Icons.Rounded.Close to R.string.state_gone
    }
    Icon(imageVector = vector, contentDescription = stringResource(label), tint = stateColor(state, fresh), modifier = modifier.size(20.dp))
}
