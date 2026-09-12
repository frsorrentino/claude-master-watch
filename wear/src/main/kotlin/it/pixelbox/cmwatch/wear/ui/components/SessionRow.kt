package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.rules.SessionsText
import it.pixelbox.cmwatch.wear.ui.theme.CmColors

/** Una riga a tutta larghezza: icona di stato, pallino dell'account, «nome · durata»; sotto, la domanda se c'è. */
@Composable
fun SessionRow(
    s: Session,
    now: Long,
    fresh: Boolean,
    onClick: () -> Unit,
    transformation: SurfaceTransformation?,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        transformation = transformation,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = CmColors.surface, contentColor = CmColors.text, secondaryContentColor = CmColors.text2),
        border = BorderStroke(1.dp, CmColors.line),
        icon = { StateIcon(s.state, fresh) },
        label = {
            AccountDot(s.account)
            Spacer(Modifier.width(6.dp))
            Text(SessionsText.row(s, now), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
        },
        secondaryLabel = s.question?.let { q -> { Text(q.text, maxLines = 1, overflow = TextOverflow.Ellipsis, color = CmColors.text2) } },
    )
}
