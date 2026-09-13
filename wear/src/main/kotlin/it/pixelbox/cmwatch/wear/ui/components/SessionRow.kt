package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import androidx.compose.ui.res.stringResource
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.rules.NameText
import it.pixelbox.cmwatch.rules.SessionsText
import it.pixelbox.cmwatch.rules.TileTexts
import it.pixelbox.cmwatch.rules.ToolText
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.SessionNameStyle
import it.pixelbox.cmwatch.wear.ui.theme.cmMarquee

/**
 * Riga della lista come nel selettore app di Wear OS: badge grande a sinistra, nome su una o due righe intere,
 * durata e domanda su righe proprie in grigio. Il nome non viene mai tagliato in coda quando un'altra sessione
 * visibile condivide il prefisso (Franz, 13/09): in quel caso l'ellissi sta in mezzo e la coda distintiva resta.
 */
@Composable
fun SessionRow(
    s: Session,
    now: Long,
    fresh: Boolean,
    onClick: () -> Unit,
    transformation: SurfaceTransformation?,
    modifier: Modifier = Modifier,
    siblings: List<String> = emptyList(),
    ambient: Boolean = false,
    marquee: Boolean = false,
    tools: ToolText.Labels? = null,
) {
    val age = SessionsText.sub(s, now, stringResource(R.string.session_closed))
    val overflow = if (NameText.sharesPrefix(s.name, siblings)) TextOverflow.MiddleEllipsis else TextOverflow.Ellipsis
    // Anatomia di Gmail (Franz, 13/09 20:54): badge, nome con l'età a destra, poi cosa fa o cosa ha fatto su due
    // righe. Le stesse parole della tile, così le due superfici non dicono cose diverse.
    val what = if (ambient) null else when {
        s.question != null -> s.question?.text
        s.state == SessionState.GONE -> null
        else -> TileTexts.activity(
            s,
            busy = s.state == SessionState.BUSY || s.state == SessionState.AWAITING,
            running = stringResource(R.string.tile_turn_running),
            idle = stringResource(R.string.state_idle),
            now = now,
            tools = tools,
        )
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ambient) CmColors.bg else CmColors.surface,
            contentColor = CmColors.text,
        ),
        contentPadding = PaddingValues(start = 8.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
        transformation = transformation,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
            SessionBadge(s, size = 36.dp, ambient = ambient)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        // Mono 13 sp nella lista: con il badge e l'età, a 14 sp «claude-master» finiva troncato.
                        s.name, style = SessionNameStyle, color = CmColors.text, maxLines = 1, overflow = overflow,
                        softWrap = false, modifier = Modifier.weight(1f, fill = false).cmMarquee(marquee),
                    )
                    age?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1)
                    }
                }
                what?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it, style = MaterialTheme.typography.bodySmall, color = CmColors.text2,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
