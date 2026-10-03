package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        model.rows.groupBy { it.group }.forEach { (group, rows) ->
            GroupHeader(stringResource(groupLabel(group), rows.size), groupTone(group))
            rows.forEach { r ->
                val s = r.session
                val id = group.name + ":" + s.name
                SummaryCard(
                    r, now, expanded == id, onToggle = { expanded = if (expanded == id) null else id },
                    onAnswer = { n -> onAnswer(s.name, n) }, onStep = { t -> onStep(s.name, t) }, onOpen = { onOpen(s.name) },
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

/** Obiettivo, priorità e «modello · effort · account» (il contesto sta in testa alla card). */
@Composable
private fun details(s: Session): List<String> = listOfNotNull(
    SessionsText.goalLine(s, stringResource(R.string.goal)),
    SessionsText.priority(s, stringResource(R.string.low_priority), stringResource(R.string.low_priority_offered)),
    listOfNotNull(ModelText.short(s.model), s.effort, s.account).joinToString(" · ").ifEmpty { null },
)

private val CARD_HM = java.time.format.DateTimeFormatter.ofPattern("HH:mm")

/**
 * Una sessione del riepilogo (Franz, 03/10 15:18, variante B): una card con il badge (colore della sessione, cerchio o
 * quadrato dell'account, glifo dello stato), nome, contesto ed età; sotto l'ultimo esito o la domanda su due righe e la
 * barretta del contesto. Aperta: il testo intero, i dettagli, le opzioni o i consigli e la conversazione.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SummaryCard(
    r: Summary.Row, now: Long, open: Boolean, onToggle: () -> Unit,
    onAnswer: (Int) -> Unit, onStep: (String) -> Unit, onOpen: () -> Unit,
) {
    val s = r.session
    val tone = groupTone(r.group)
    val waiting = r.group == Summary.Group.WAITING
    val parsed = androidx.compose.runtime.remember(r.text) { it.pixelbox.cmwatch.rules.NextSteps.parse(r.text.orEmpty()) }
    // L'esito per primo e senza l'etichetta «Esito:»; la domanda così com'è.
    val shown = if (waiting) parsed.text else s.outcome?.let { o -> it.pixelbox.cmwatch.rules.OutcomeText.summary(o) } ?: parsed.text
    val body = it.pixelbox.cmwatch.rules.Markdown.parse(shown).text.trim()
    val since: (Long) -> String = { t -> it.pixelbox.cmwatch.contract.Durations.since(t, now) }
    val age = r.at?.takeIf { it > 0 }?.let { t ->
        when (r.group) {
            Summary.Group.WAITING, Summary.Group.WORKING -> since(t)
            Summary.Group.FINISHED -> CARD_HM.format(java.time.Instant.ofEpochSecond(t).atZone(java.time.ZoneId.systemDefault()))
            Summary.Group.STILL -> stringResource(R.string.summary_since, since(t))
        }
    }
    val fill = if (waiting) androidx.compose.ui.graphics.lerp(CmColors.surfaceLow, CmColors.briefWarn, 0.06f) else CmColors.surfaceLow
    Column(
        // La chat si restringe verso la sua card durante il gesto indietro.
        Modifier.fly("card-${s.id}").fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(fill)
            .clickable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SessionBadge(s, 24.dp)
            Text(
                s.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = CmColors.text, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip, modifier = Modifier.weight(1f),
            )
            s.context?.let { Text("$it%", style = MonoSmall) }
            age?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = tone, maxLines = 1) }
        }
        if (body.isNotBlank()) Text(
            body, style = MaterialTheme.typography.bodyMedium, color = if (waiting || open) CmColors.text else CmColors.text2,
            maxLines = if (open) Int.MAX_VALUE else 2, overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
        )
        if (open) {
            details(s).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip) }
        }
        // Le opzioni della domanda subito, anche a card chiusa: rispondere è il motivo per cui la card è in cima.
        if (waiting) s.question?.let { q ->
            var holdHint by rememberSaveable(q.id) { mutableStateOf(false) }
            // Sul fondo della card il tonale di sempre quasi spariva (provini 03/10): un velo del colore primario.
            QuestionOptions(q, firstFilled = true, holdHint = holdHint, onHold = { holdHint = true }, onAnswer = onAnswer, tonal = CmColors.primary.copy(alpha = 0.12f))
        }
        if (open && !waiting && parsed.steps.isNotEmpty()) androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            parsed.steps.forEach { step ->
                androidx.compose.material3.OutlinedButton(onClick = { onStep(step) }, border = androidx.compose.foundation.BorderStroke(1.dp, tone.copy(alpha = 0.5f))) {
                    Text(step, color = CmColors.text)
                }
            }
        }
        if (open) androidx.compose.material3.TextButton(onClick = onOpen) { Text(stringResource(R.string.fy_open_conversation), color = CmColors.actionIcon) }
        s.context?.let { pct ->
            val bar = when (it.pixelbox.cmwatch.rules.SessionMeters.contextTone(pct)) {
                it.pixelbox.cmwatch.rules.BriefCards.Tone.ALERT -> CmColors.briefAlertRing
                it.pixelbox.cmwatch.rules.BriefCards.Tone.WARN -> CmColors.briefWarn
                else -> CmColors.briefRing
            }
            Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(CmColors.briefTrack)) {
                Box(Modifier.fillMaxWidth(it.pixelbox.cmwatch.rules.SessionMeters.contextFraction(pct) ?: 0f).fillMaxHeight().background(bar))
            }
        }
    }
}

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
