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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.MasterHome
import it.pixelbox.cmwatch.rules.ModelText
import it.pixelbox.cmwatch.rules.SessionsText
import it.pixelbox.cmwatch.rules.Summary
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Il riepilogo unico (design 03/10, tavola 4): ogni sessione una volta, nei gruppi del bisogno, una riga aperta alla
 * volta; aperta mostra anche i dettagli delle vecchie card di Sessioni. Sotto, «Chiuse · N» e le righe di servizio.
 */
@Composable
fun SummaryList(
    model: Summary.Model, now: Long, onOpen: (String) -> Unit, onAnswer: (String, Int) -> Unit, onStep: (String, String) -> Unit,
    onService: (MasterHome.Row) -> Unit, onClosed: () -> Unit, initiallyOpen: String? = null,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyOpen) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        model.rows.groupBy { it.group }.forEach { (group, rows) ->
            GroupHeader(stringResource(groupLabel(group), rows.size), groupTone(group))
            rows.forEach { r ->
                val s = r.session
                val id = group.name + ":" + s.name
                val kind = if (group == Summary.Group.WAITING) MasterHome.Kind.QUESTION else MasterHome.Kind.FINISHED
                AttentionRow(
                    MasterHome.Row(kind, s.name, r.text, s.name, key = r.key, at = r.at?.takeIf { it > 0 }),
                    now, expanded == id, onToggle = { expanded = if (expanded == id) null else id }, question = s.question,
                    onAnswer = { n -> onAnswer(s.name, n) }, onStep = { t -> onStep(s.name, t) }, onOpen = { onOpen(s.name) },
                    variant = group, context = s.context.takeIf { group == Summary.Group.WORKING }, details = details(s),
                )
            }
        }
        if (model.closed.isNotEmpty()) ClosedRow(model.closed.size, onClosed)
        if (model.service.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(CmColors.line))
            Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                model.service.forEach { ForYouRow(it, onService) }
            }
        }
    }
}

private fun groupLabel(g: Summary.Group) = when (g) {
    Summary.Group.WAITING -> R.string.summary_waiting
    Summary.Group.FINISHED -> R.string.summary_finished
    Summary.Group.WORKING -> R.string.summary_working
    Summary.Group.STILL -> R.string.summary_still
}

private fun groupTone(g: Summary.Group): Color = when (g) {
    Summary.Group.WAITING -> CmColors.briefWarn
    Summary.Group.FINISHED -> CmColors.briefGood
    Summary.Group.WORKING -> CmColors.briefRing
    Summary.Group.STILL -> CmColors.text2
}

/** Obiettivo, priorità e «modello · effort · contesto · account», come nelle card di Sessioni. */
@Composable
private fun details(s: Session): List<String> = listOfNotNull(
    SessionsText.goalLine(s, stringResource(R.string.goal)),
    SessionsText.priority(s, stringResource(R.string.low_priority), stringResource(R.string.low_priority_offered)),
    listOfNotNull(ModelText.short(s.model), s.effort, s.context?.let { "$it%" }, s.account).joinToString(" · ").ifEmpty { null },
)

/** «Chiuse · N»: una riga sola, anche con molte sessioni chiuse; apre l'elenco con «Riapri». */
@Composable
private fun ClosedRow(n: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.Close, null, tint = CmColors.text2, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.summary_closed, n), style = MaterialTheme.typography.bodyLarge, color = CmColors.text2, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = CmColors.text2)
    }
}
