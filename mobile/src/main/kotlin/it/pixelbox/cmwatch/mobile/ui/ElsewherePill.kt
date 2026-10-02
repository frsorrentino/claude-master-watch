package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Elsewhere
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * L'avviso delle altre sessioni sotto la barra della chat (Franz, 02/10 20:47, variante A dei mockup): una riga sola,
 * la mano ambra per chi ti aspetta (resta finché la domanda c'è), la bandierina verde per un turno finito, con ✕.
 * Il tocco apre la sessione, o la fila «Ti aspettano» quando sono più d'una.
 */
@Composable
fun ElsewherePill(alert: Elsewhere.Alert, onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val waiting = alert is Elsewhere.Waiting
    val tone = if (waiting) CmColors.briefWarn else CmColors.briefGood
    val text = when (alert) {
        is Elsewhere.Waiting -> if (alert.sessions.size == 1) stringResource(R.string.elsewhere_waiting, alert.sessions[0])
            else stringResource(R.string.elsewhere_waiting_many, alert.sessions.size)
        is Elsewhere.Finished -> stringResource(R.string.elsewhere_finished, alert.session)
    }
    Surface(
        onClick = onOpen, color = tone.copy(alpha = 0.16f), shape = CircleShape,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).heightIn(min = 44.dp),
    ) {
        Row(Modifier.padding(start = 14.dp, end = if (waiting) 10.dp else 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(if (waiting) R.drawable.ic_hand else R.drawable.ic_flag), null, tint = tone, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = CmColors.text,
                maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
            Text(stringResource(if (waiting) R.string.elsewhere_reply else R.string.elsewhere_open), style = MaterialTheme.typography.labelLarge, color = tone)
            if (waiting) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = tone, modifier = Modifier.size(20.dp))
            // Una domanda non si chiude da qui: resta finché qualcuno risponde.
            else IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = CmColors.text2, modifier = Modifier.size(18.dp)) }
        }
    }
}
