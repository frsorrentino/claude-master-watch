package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.QuestionKind
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.Tier
import it.pixelbox.cmwatch.data.Pending
import it.pixelbox.cmwatch.data.PendingStatus
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Accounts
import it.pixelbox.cmwatch.rules.ModelText
import it.pixelbox.cmwatch.rules.SessionMeters
import it.pixelbox.cmwatch.rules.SessionsText
import it.pixelbox.cmwatch.rules.PhonePrimary
import it.pixelbox.cmwatch.rules.QuestionRules
import it.pixelbox.cmwatch.ui.tokens.CmColors

data class SheetActions(
    val answer: (Int) -> Unit, val allowAll: () -> Unit, val send: (PhonePrimary.Target, String) -> Unit,
    val follow: (Boolean) -> Unit, val reopen: () -> Unit, val terminal: () -> Unit, val openInClaude: () -> Unit,
    val speak: (String) -> Unit, val retry: (cmdId: String) -> Unit,
    /** «Chat about this» (contratto 1.10), come sull'orologio: la domanda resta gestibile anche senza opzioni. */
    val chat: () -> Unit = {},
)

/**
 * La scheda sessione (design 29/09, schermata 2; restyling 30/09): testata con badge e contatori, la domanda come
 * sull'orologio (prima opzione piena, pressione lunga per il rischio alto), esito, testo libero, azioni. Un solo bottone
 * pieno: la prima opzione quando la domanda ne ha, altrimenti «Invia» o «Riapri».
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun SessionSheet(s: Session, now: Long, pending: List<Pending>, ttsMinChars: Int, actions: SheetActions) {
    // Legata anche alla domanda: una domanda nuova non eredita la bozza scritta per quella di prima (revisione 29/09).
    var draft by rememberSaveable(s.id, s.question?.id) { mutableStateOf("") }
    var holdHint by rememberSaveable(s.question?.id) { mutableStateOf(false) }
    val primary = PhonePrimary.button(s, draft)
    Column(Modifier.fly("card-${s.id}").fillMaxSize().background(CmColors.bg)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetHeader(s, now)

            s.question?.let { q ->
                Surface(color = CmColors.surfaceHigh, shape = MaterialTheme.shapes.extraLarge, border = BorderStroke(2.dp, if (q.tier == Tier.HIGH) CmColors.gone else CmColors.waiting)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Speakable(q.text, speak = true, actions.speak)
                        val long = QuestionRules.needsLongPress(q.tier)
                        if (long) Text(
                            stringResource(if (holdHint) R.string.question_hold else R.string.question_high_risk),
                            style = MaterialTheme.typography.labelLarge, color = if (holdHint) CmColors.waiting else CmColors.briefAlert,
                        )
                        q.options.forEachIndexed { i, o ->
                            OptionButton(
                                QuestionRules.optionLabel(o), filled = i == 0 && primary == PhonePrimary.Button.OPTION,
                                onClick = { if (long) holdHint = true else actions.answer(o.n) },
                                onLongClick = if (long) ({ actions.answer(o.n) }) else null,
                            )
                        }
                        val allowAll = QuestionRules.allowAllVisible(q)
                        ButtonGroup(
                            overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            customItem(
                                buttonGroupContent = {
                                    val src = remember { MutableInteractionSource() }
                                    OutlinedButton(onClick = actions.chat, interactionSource = src, modifier = Modifier.weight(1f).animateWidth(src)) {
                                        Text(stringResource(R.string.chat_about_this), maxLines = 1)
                                    }
                                },
                                menuContent = { m -> DropdownMenuItem(text = { Text(stringResource(R.string.chat_about_this)) }, onClick = { m.dismiss(); actions.chat() }) },
                            )
                            if (allowAll) customItem(
                                buttonGroupContent = {
                                    val src = remember { MutableInteractionSource() }
                                    OutlinedButton(onClick = actions.allowAll, interactionSource = src, modifier = Modifier.weight(1f).animateWidth(src)) {
                                        Text(stringResource(R.string.allow_all), maxLines = 1)
                                    }
                                },
                                menuContent = { m -> DropdownMenuItem(text = { Text(stringResource(R.string.allow_all)) }, onClick = { m.dismiss(); actions.allowAll() }) },
                            )
                        }
                    }
                }
            }
            s.outcome?.let { Speakable(it.full, speak = true, actions.speak) }

            OutlinedTextField(
                value = draft, onValueChange = { draft = it }, minLines = 2, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(if (s.question != null) R.string.answer_free else R.string.write_prompt)) },
                enabled = s.state != SessionState.GONE, shape = MaterialTheme.shapes.medium,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionChip(stringResource(R.string.terminal), Icons.Rounded.Terminal, actions.terminal)
                ActionChip(stringResource(if (s.followed) R.string.unfollow else R.string.follow), if (s.followed) Icons.Rounded.NotificationsOff else Icons.Rounded.NotificationsActive) { actions.follow(!s.followed) }
                if (s.link.isNotBlank()) ActionChip(stringResource(R.string.open_in_claude), Icons.AutoMirrored.Rounded.OpenInNew, actions.openInClaude)
            }
            pending.filter { it.cmd.session == s.id || it.cmd.session == s.name }.filter { it.status == PendingStatus.FAILED }.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.not_delivered), color = CmColors.goneDim, modifier = Modifier.weight(1f))
                    TextButton(onClick = { actions.retry(p.cmd.id) }) { Text(stringResource(R.string.retry), color = CmColors.actionIcon) }
                }
            }
        }
        val filled = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary)
        val mod = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(56.dp)
        val send = {
            PhonePrimary.target(s, draft)?.let { actions.send(it, draft.trim()) }
            draft = ""
        }
        when (primary) {
            PhonePrimary.Button.SEND -> Button(onClick = send, colors = filled, modifier = mod) { Text(stringResource(R.string.send)) }
            PhonePrimary.Button.REOPEN -> Button(onClick = actions.reopen, colors = filled, modifier = mod) { Text(stringResource(R.string.reopen)) }
            // La prima opzione è il bottone pieno: «Invia» resta tonale e compare solo con del testo scritto.
            PhonePrimary.Button.OPTION -> if (draft.isNotBlank()) FilledTonalButton(onClick = send, modifier = mod) { Text(stringResource(R.string.send)) }
            PhonePrimary.Button.NONE -> Unit
        }
    }
}

/** Testata: nome, account e progetto, lo stato come pillola, e i contatori della sessione (modello, effort, contesto). */
@Composable
private fun SheetHeader(s: Session, now: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(s.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountDot(Accounts.isPersonal(s))
            Text("${s.account} · ${s.project}", style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
        }
        StatePill(s.state, Durations.since(s.since, now))
        val model = ModelText.short(s.model)
        val effort = SessionMeters.effortStep(s.effort)
        if (model != null || effort != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            model?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = CmColors.text) }
            effort?.let { step ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.effort), style = MaterialTheme.typography.labelLarge, color = CmColors.text2)
                    repeat(SessionMeters.EFFORT_STEPS) { i ->
                        Box(Modifier.size(width = 10.dp, height = 6.dp).background(if (i < step) CmColors.briefRing else CmColors.briefTrack, CircleShape))
                    }
                }
            }
        }
        s.context?.let { ContextBar(it, null) }
        SessionsText.goalLine(s, stringResource(R.string.goal))?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = CmColors.briefLabel) }
        SessionsText.priority(s, stringResource(R.string.low_priority), stringResource(R.string.low_priority_offered))?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = CmColors.waiting)
        }
    }
}

/** Un'opzione della domanda: piena la prima, tonali le altre; con il rischio alto risponde solo la pressione lunga. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OptionButton(label: String, filled: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)?) {
    Surface(
        color = if (filled) CmColors.primary else CmColors.surface, contentColor = if (filled) CmColors.onPrimary else CmColors.text,
        shape = CircleShape,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(CircleShape).combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), contentAlignment = Alignment.CenterStart) {
            Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize))
        }
    }
}

@Composable
private fun ActionChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) =
    AssistChip(
        onClick = onClick, label = { Text(label) }, leadingIcon = { Icon(icon, null, tint = CmColors.actionIcon) },
        shape = CircleShape, colors = AssistChipDefaults.assistChipColors(containerColor = CmColors.surface, labelColor = CmColors.text),
        border = null,
    )

/** Testo con il tasto ▶ accanto (regola di Franz 12/09: testi lunghi, esiti, risposte e domande). */
@Composable
fun Speakable(text: String, speak: Boolean, onSpeak: (String) -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = CmColors.text, modifier = Modifier.weight(1f))
        if (speak) IconButton(onClick = { onSpeak(text) }) { Icon(Icons.Rounded.PlayArrow, stringResource(R.string.read_aloud), tint = CmColors.actionIcon) }
    }
}
