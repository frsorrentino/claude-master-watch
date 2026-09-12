package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.rules.SessionsText
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.MonoStyle

/** Testata di Scheda e Domanda: icona di stato, pallino dell'account, «nome · durata» su una riga; il tool su una riga propria. */
@Composable
fun SessionHeader(s: Session, now: Long, fresh: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StateIcon(s.state, fresh)
            Spacer(Modifier.width(6.dp))
            AccountDot(s.account)
            Spacer(Modifier.width(6.dp))
            val row = SessionsText.row(s, now)
            val tail = row.removePrefix(s.name)
            Text(s.name, style = MonoStyle, color = CmColors.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            if (tail.isNotEmpty()) Text(tail, style = MonoStyle, color = CmColors.text, maxLines = 1, softWrap = false)
        }
        val tool = s.tool
        if (s.state == SessionState.BUSY && tool != null) {
            Text(tool, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
