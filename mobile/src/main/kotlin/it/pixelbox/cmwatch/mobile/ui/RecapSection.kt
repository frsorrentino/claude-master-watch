package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.RecapActions
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** «06/10» dal giorno ISO del recap; vuoto se non si legge. */
internal fun recapDay(date: String): String =
    runCatching { java.time.LocalDate.parse(date).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")) }.getOrDefault("")

/**
 * Un'azione della sezione Recap come tasto (mockup approvato l'08/10, tavola 1): il testo e, piccolo accanto, da dove viene
 * (la sessione, o «recap 06/10»). Mandata, resta grigia con la spunta finché lo stato non la toglie.
 */
@Composable
internal fun RecapActionChip(a: RecapActions.Action, day: String, sent: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.step_sent, a.text)
    Surface(
        onClick = onClick, enabled = !sent, shape = CircleShape,
        color = if (sent) CmColors.idle.copy(alpha = 0.14f) else CmColors.surfaceLow, contentColor = if (sent) CmColors.text2 else CmColors.text,
        modifier = if (sent) Modifier.semantics { contentDescription = label } else Modifier,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (sent) Icon(Icons.Rounded.Check, null, tint = CmColors.idle, modifier = Modifier.size(16.dp))
            Text(a.text, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (a.recap) stringResource(R.string.recap_from_recap, day).trim() else a.from,
                style = MaterialTheme.typography.bodySmall, color = CmColors.text2,
            )
        }
    }
}

/**
 * «Mandare questa azione?» (tavola 3): da dove viene, il testo che parte, a chi va — la sessione viva del progetto, o la
 * master con «Riprendi …». Un solo tasto pieno, «Manda».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecapSendSheet(a: RecapActions.Action, day: String, onDismiss: () -> Unit, onSend: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CmColors.surface) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.recap_send_title), style = MaterialTheme.typography.headlineSmall, color = CmColors.text)
            Text(
                if (a.recap) stringResource(R.string.recap_send_from_recap, day, a.from) else stringResource(R.string.recap_send_from_session, a.from),
                style = MaterialTheme.typography.bodyMedium, color = CmColors.text2,
            )
            Text(
                a.send, style = MaterialTheme.typography.bodyLarge, color = CmColors.text,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CmColors.surfaceLow).padding(horizontal = 18.dp, vertical = 14.dp),
            )
            val to = stringResource(if (a.viaMaster) R.string.recap_send_to_master else R.string.recap_send_to_session, a.to)
            Text(
                buildAnnotatedString {
                    val i = to.indexOf(a.to)
                    if (i < 0) append(to) else {
                        append(to.substring(0, i)); withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = CmColors.text)) { append(a.to) }; append(to.substring(i + a.to.length))
                    }
                },
                style = MaterialTheme.typography.bodyMedium, color = CmColors.text2,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = CmColors.actionIcon) }
                androidx.compose.material3.Button(
                    onClick = onSend,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
                ) { Text(stringResource(R.string.recap_send)) }
            }
        }
    }
}
