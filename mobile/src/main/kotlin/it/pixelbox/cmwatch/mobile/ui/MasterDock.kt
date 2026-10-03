package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.MasterHome
import it.pixelbox.cmwatch.rules.ModelText
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DOCK_HM = DateTimeFormatter.ofPattern("HH:mm")

/**
 * La master agganciata sopra «Scrivi alla master» (design 03/10, tavola 4): badge, «MASTER · ora · modello · contesto»,
 * il titolo dell'ultimo esito su una riga, ▶ per ascoltarlo e il tondo della conversazione. Non scorre con la lista.
 */
@Composable
fun MasterDock(master: Session, hero: MasterHome.Hero?, onSpeak: () -> Unit, onConversation: () -> Unit) {
    val time = hero?.at?.let { DOCK_HM.format(Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault())) }
    val label = listOfNotNull(stringResource(R.string.dock_master), time, ModelText.short(master.model), master.context?.let { "$it%" }).joinToString(" · ")
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(CmColors.line))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(CmColors.surfaceLow)
                .clickable(onClick = onConversation).padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SessionBadge(master, 20.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MonoSmall, maxLines = 1, overflow = TextOverflow.Clip)
                hero?.let {
                    Text(
                        linked(it.headline), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip,
                    )
                }
            }
            if (hero != null) FilledTonalIconButton(onClick = onSpeak, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Rounded.PlayArrow, stringResource(R.string.dock_listen), tint = CmColors.actionIcon)
            }
            FilledTonalIconButton(onClick = onConversation, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Rounded.Forum, stringResource(R.string.dock_conversation), tint = CmColors.actionIcon)
            }
        }
    }
}
