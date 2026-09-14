package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.ui.res.stringResource
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.rules.ToolText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import it.pixelbox.cmwatch.wear.ui.theme.cmMarquee
import it.pixelbox.cmwatch.wear.ui.ambient.animationsOff

/** Testata di Scheda e Domanda: icona di stato, pallino dell'account, «nome · durata» su una riga; il tool su una riga propria. */
@Composable
fun SessionHeader(s: Session, now: Long, fresh: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 16 dp come nella lista (misura sulle celle di notifica di Wear OS): da 32 il tondo lo tagliava (Franz, 14/09 17:20).
            SessionBadge(s, size = 16.dp)
            Spacer(Modifier.width(8.dp))
            val row = SessionsText.row(s, now)
            val tail = row.removePrefix(s.name).removePrefix(" · ")
            Column(Modifier.weight(1f)) {
                Text(s.name, style = MonoStyle, color = CmColors.text, maxLines = 1, softWrap = false, overflow = TextOverflow.MiddleEllipsis, modifier = Modifier.cmMarquee(!animationsOff()))
                if (tail.isNotEmpty()) Text(tail, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1)
            }
        }
        // Cosa sta facendo, detto per esteso: la description del comando (contratto 1.5) o la frase dallo strumento.
        val tool = ToolText.describe(s.toolNote, s.tool, toolLabels())
        if (s.state == SessionState.BUSY && tool != null) {
            Text(tool, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun toolLabels() = ToolText.Labels(
    run = stringResource(R.string.tool_run), read = stringResource(R.string.tool_read),
    edit = stringResource(R.string.tool_edit), write = stringResource(R.string.tool_write),
    search = stringResource(R.string.tool_search), web = stringResource(R.string.tool_web),
    message = stringResource(R.string.tool_message), delegate = stringResource(R.string.tool_delegate),
    plan = stringResource(R.string.tool_plan), other = stringResource(R.string.tool_other),
)
