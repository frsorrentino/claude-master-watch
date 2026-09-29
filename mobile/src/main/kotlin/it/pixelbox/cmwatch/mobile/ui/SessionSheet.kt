package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.QuestionKind
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.data.Pending
import it.pixelbox.cmwatch.data.PendingStatus
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Accounts
import it.pixelbox.cmwatch.rules.PhonePrimary
import it.pixelbox.cmwatch.rules.QuestionRules
import it.pixelbox.cmwatch.ui.tokens.CmColors

data class SheetActions(
    val answer: (Int) -> Unit, val allowAll: () -> Unit, val send: (PhonePrimary.Target, String) -> Unit,
    val follow: (Boolean) -> Unit, val reopen: () -> Unit, val terminal: () -> Unit, val openInClaude: () -> Unit,
    val speak: (String) -> Unit, val retry: (cmdId: String) -> Unit,
)

/** La scheda sessione (design 29/09, schermata 2): domanda, esito, testo libero, azioni, un solo bottone pieno. */
@Composable
fun SessionSheet(s: Session, now: Long, pending: List<Pending>, ttsMinChars: Int, actions: SheetActions) {
    // Legata anche alla domanda: una domanda nuova non eredita la bozza scritta per quella di prima (revisione 29/09).
    var draft by rememberSaveable(s.id, s.question?.id) { mutableStateOf("") }
    Column(Modifier.fly("card-${s.id}").fillMaxSize().background(CmColors.bg)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(s.name, style = MaterialTheme.typography.headlineSmall, color = CmColors.text)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountDot(Accounts.isPersonal(s))
                Text("${s.account} · ${s.project}", style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
            }
            Text("${stateGlyph(s.state)} ${stateLabel(s.state)} · ${Durations.since(s.since, now)}", color = stateColor(s.state), style = MaterialTheme.typography.titleSmall)

            s.question?.let { q ->
                Surface(color = CmColors.surface, shape = MaterialTheme.shapes.large) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Speakable(q.text, speak = true, actions.speak)
                        q.options.forEach { o ->
                            FilledTonalButton(onClick = { actions.answer(o.n) }, modifier = Modifier.fillMaxWidth()) { Text(QuestionRules.optionLabel(o)) }
                        }
                        if (q.kind == QuestionKind.PERMISSION) {
                            OutlinedButton(onClick = actions.allowAll, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.allow_all)) }
                        }
                    }
                }
            }
            s.outcome?.let { Speakable(it.full, speak = true, actions.speak) }

            OutlinedTextField(
                value = draft, onValueChange = { draft = it }, minLines = 2, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(if (s.question != null) R.string.answer_free else R.string.write_prompt)) },
                enabled = s.state != SessionState.GONE,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = actions.terminal) { Text(stringResource(R.string.terminal), color = CmColors.actionIcon) }
                TextButton(onClick = { actions.follow(!s.followed) }) {
                    Text(stringResource(if (s.followed) R.string.unfollow else R.string.follow), color = CmColors.actionIcon)
                }
                if (s.link.isNotBlank()) TextButton(onClick = actions.openInClaude) { Text(stringResource(R.string.open_in_claude), color = CmColors.actionIcon) }
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
        when (PhonePrimary.button(s, draft)) {
            PhonePrimary.Button.SEND -> Button(onClick = {
                PhonePrimary.target(s, draft)?.let { actions.send(it, draft.trim()) }
                draft = ""
            }, colors = filled, modifier = mod) { Text(stringResource(R.string.send)) }
            PhonePrimary.Button.REOPEN -> Button(onClick = actions.reopen, colors = filled, modifier = mod) { Text(stringResource(R.string.reopen)) }
            PhonePrimary.Button.NONE -> Unit
        }
    }
}

/** Testo con il tasto ▶ accanto (regola di Franz 12/09: testi lunghi, esiti, risposte e domande). */
@Composable
fun Speakable(text: String, speak: Boolean, onSpeak: (String) -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = CmColors.text, modifier = Modifier.weight(1f))
        if (speak) IconButton(onClick = { onSpeak(text) }) { Icon(Icons.Rounded.PlayArrow, stringResource(R.string.read_aloud), tint = CmColors.actionIcon) }
    }
}
