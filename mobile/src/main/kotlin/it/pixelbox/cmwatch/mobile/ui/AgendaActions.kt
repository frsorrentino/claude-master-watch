package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.AgendaRow
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.ContextActions
import it.pixelbox.cmwatch.rules.RecapActions
import it.pixelbox.cmwatch.rules.RecapAgenda
import it.pixelbox.cmwatch.rules.RecapAgenda.Item
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Le Azioni sulle schede del Recap (piano approvato da Franz il 09/10, «Approvo, prosegui»): quale foglio è aperto. Uno
 * per volta: il menu, «Approfondisci», la scelta del giorno di «Rimanda», la conferma.
 */
class AgendaActions {
    var menuFor by mutableStateOf<AgendaRow?>(null)
    var deepenFor by mutableStateOf<AgendaRow?>(null)
    var postponeFor by mutableStateOf<AgendaRow?>(null)
    /** Un testo che parte (Fallo, Approfondisci senza dettaglio, il file del rimando), con la riga «Da …» del foglio. */
    var send by mutableStateOf<Pair<RecapActions.Action, String>?>(null)
    var edit by mutableStateOf<RecapAgenda.Edit?>(null)
    fun menu(r: AgendaRow) { menuFor = r }
    fun deepen(r: AgendaRow) { deepenFor = r }
}

/** I fogli delle Azioni; `onSend` manda un testo, `onTalk` apre la master col testo nel campo, `onEdit` scrive nell'agenda. */
@Composable
fun AgendaActionsLayer(
    s: AgendaActions, canWrite: Boolean, today: java.time.LocalDate,
    onSend: (RecapActions.Action) -> Unit, onTalk: (String) -> Unit, onEdit: (RecapAgenda.Edit) -> Unit,
) {
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    val doText = stringResource(R.string.recap_do_text); val doRef = stringResource(R.string.recap_do_ref)
    val deepenT = stringResource(R.string.agenda_deepen_text); val talkT = stringResource(R.string.agenda_talk_text)
    val fileT = stringResource(R.string.agenda_file_text)
    val fromScope = stringResource(R.string.agenda_from_scope)
    fun fromAgenda(r: AgendaRow) = fromScope.format(r.scope.trim())
    fun toMaster(r: AgendaRow, send: String) = RecapActions.Action(r.title, send, ContextActions.MASTER, "agenda", viaMaster = true, agenda = true)
    fun pick(r: AgendaRow, item: Item) {
        s.menuFor = null
        when (item) {
            Item.DEEPEN -> s.deepenFor = r
            Item.DO -> s.send = RecapAgenda.doIt(r, doText, doRef) to fromAgenda(r)
            Item.TALK -> onTalk(RecapAgenda.talkText(r, talkT))
            Item.OPEN_REF -> RecapAgenda.url(r.ref)?.let { u -> runCatching { uri.openUri(u) } }
                ?: run { s.send = toMaster(r, fileT.format(r.ref.trim(), r.title)) to fromAgenda(r) }
            Item.POSTPONE -> s.postponeFor = r
            Item.DONE, Item.REMOVE, Item.PASS_CLAUDE, Item.PASS_ME -> s.edit = RecapAgenda.Edit(item, r)
        }
    }
    s.menuFor?.let { r -> AgendaMenuSheet(r, RecapAgenda.menu(r, canWrite, today), onDismiss = { s.menuFor = null }) { pick(r, it) } }
    s.deepenFor?.let { r ->
        AgendaDeepenSheet(
            r, onDismiss = { s.deepenFor = null },
            onAsk = { s.deepenFor = null; s.send = toMaster(r, RecapAgenda.deepenText(r, deepenT)) to fromAgenda(r) },
            onActions = { s.deepenFor = null; s.menuFor = r },
        )
    }
    s.postponeFor?.let { r -> AgendaPostponeSheet(today, onDismiss = { s.postponeFor = null }) { d -> s.postponeFor = null; s.edit = RecapAgenda.Edit(Item.POSTPONE, r, d) } }
    s.send?.let { (a, from) -> RecapSendSheet(a, "", onDismiss = { s.send = null }, from = from) { s.send = null; onSend(a) } }
    s.edit?.let { e -> AgendaEditSheet(e, onDismiss = { s.edit = null }) { s.edit = null; onEdit(e) } }
}

private fun icon(i: Item): ImageVector = when (i) {
    Item.DEEPEN -> Icons.Rounded.Info
    Item.DO -> Icons.Rounded.PlayArrow
    Item.TALK -> Icons.AutoMirrored.Rounded.Chat
    Item.OPEN_REF -> Icons.AutoMirrored.Rounded.OpenInNew
    Item.DONE -> Icons.Rounded.CheckCircle
    Item.POSTPONE -> Icons.Rounded.Event
    Item.PASS_CLAUDE -> Icons.Rounded.SmartToy
    Item.PASS_ME -> Icons.Rounded.Person
    Item.REMOVE -> Icons.Rounded.DeleteOutline
}

@Composable
private fun label(i: Item): String = stringResource(
    when (i) {
        Item.DEEPEN -> R.string.agenda_deepen
        Item.DO -> R.string.recap_do
        Item.TALK -> R.string.agenda_talk
        Item.OPEN_REF -> R.string.agenda_open_ref
        Item.DONE -> R.string.agenda_done
        Item.POSTPONE -> R.string.agenda_postpone
        Item.PASS_CLAUDE -> R.string.agenda_pass_claude
        Item.PASS_ME -> R.string.agenda_pass_me
        Item.REMOVE -> R.string.agenda_remove
    },
)

/** Il menu «Azioni»: in testa la scheda, poi le voci; le scritture nell'agenda separate da una riga. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgendaMenuSheet(r: AgendaRow, items: List<Item>, onDismiss: () -> Unit, onPick: (Item) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CmColors.surface) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Row(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AgendaMark(r.scope)
                Text(r.title, style = MaterialTheme.typography.titleMedium, color = CmColors.text)
            }
            items.forEachIndexed { i, item ->
                if (i > 0 && item == Item.DONE) androidx.compose.material3.HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 4.dp), color = CmColors.line)
                Row(
                    Modifier.fillMaxWidth().handCursor().clickable { onPick(item) }.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(icon(item), null, tint = if (item == Item.REMOVE) CmColors.gone else CmColors.actionIcon, modifier = Modifier.size(22.dp))
                    Text(label(item), style = MaterialTheme.typography.bodyLarge, color = CmColors.text)
                }
            }
        }
    }
}

/**
 * «Approfondisci»: la scheda per intero col dettaglio salvato, senza passare da Claude. Senza dettaglio un tasto lo chiede
 * alla master, che lo scrive nell'agenda: la volta dopo c'è.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgendaDeepenSheet(r: AgendaRow, onDismiss: () -> Unit, onAsk: () -> Unit, onActions: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CmColors.surface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AgendaMark(r.scope)
                Text(r.title, style = MaterialTheme.typography.headlineSmall, color = CmColors.text)
            }
            val facts = listOfNotNull(
                r.scope.trim().takeIf { it.isNotEmpty() }, r.state.trim().takeIf { it.isNotEmpty() },
                r.blocks.trim().takeIf { it.isNotEmpty() }?.let { stringResource(R.string.recap_blocks, it) },
                r.until?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.agenda_until, untilLabel(it)) },
            ).joinToString(" · ")
            Text(facts, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
            if (r.ref.isNotBlank()) {
                val uri = androidx.compose.ui.platform.LocalUriHandler.current
                val u = RecapAgenda.url(r.ref)
                Text(
                    r.ref.trim(), style = MaterialTheme.typography.bodyMedium, color = if (u != null) CmColors.actionIcon else CmColors.text2,
                    modifier = if (u != null) Modifier.handCursor().clickable { runCatching { uri.openUri(u) } } else Modifier,
                )
            }
            val detail = r.detail?.trim().orEmpty()
            if (detail.isNotEmpty()) Text(
                detail, style = MaterialTheme.typography.bodyLarge, color = CmColors.text,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CmColors.surfaceLow).padding(horizontal = 18.dp, vertical = 14.dp),
            ) else Text(stringResource(R.string.agenda_no_detail), style = MaterialTheme.typography.bodyLarge, color = CmColors.text2)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.TextButton(onClick = onActions) { Text(stringResource(R.string.agenda_actions), color = CmColors.actionIcon) }
                if (detail.isEmpty()) androidx.compose.material3.Button(
                    onClick = onAsk,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
                ) { Text(stringResource(R.string.agenda_ask_detail)) }
            }
        }
    }
}

/** «Rimanda»: domani, la settimana prossima (lunedì) o un giorno scelto; la scheda torna quel giorno. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgendaPostponeSheet(today: java.time.LocalDate, onDismiss: () -> Unit, onPick: (java.time.LocalDate) -> Unit) {
    var calendar by androidx.compose.runtime.remember { mutableStateOf(false) }
    if (calendar) {
        val zone = java.time.ZoneOffset.UTC
        val st = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = RecapAgenda.tomorrow(today).atStartOfDay(zone).toInstant().toEpochMilli(),
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = java.time.Instant.ofEpochMilli(utcTimeMillis).atZone(zone).toLocalDate().isAfter(today)
            },
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { calendar = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    st.selectedDateMillis?.let { ms -> onPick(java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()) }
                }) { Text(stringResource(R.string.agenda_postpone), color = CmColors.actionIcon) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { calendar = false }) { Text(stringResource(R.string.cancel), color = CmColors.text2) } },
        ) { androidx.compose.material3.DatePicker(st) }
        return
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CmColors.surface) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(stringResource(R.string.agenda_postpone_title), style = MaterialTheme.typography.titleMedium, color = CmColors.text, modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp))
            listOf(
                Triple(Icons.Rounded.Today, stringResource(R.string.agenda_postpone_tomorrow, untilLabel(RecapAgenda.tomorrow(today).toString()))) { onPick(RecapAgenda.tomorrow(today)) },
                Triple(Icons.Rounded.Weekend, stringResource(R.string.agenda_postpone_week, untilLabel(RecapAgenda.nextWeek(today).toString()))) { onPick(RecapAgenda.nextWeek(today)) },
                Triple(Icons.Rounded.Event, stringResource(R.string.agenda_postpone_date)) { calendar = true },
            ).forEach { (ic, text, go) ->
                Row(
                    Modifier.fillMaxWidth().handCursor().clickable(onClick = go).padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(ic, null, tint = CmColors.actionIcon, modifier = Modifier.size(22.dp))
                    Text(text, style = MaterialTheme.typography.bodyLarge, color = CmColors.text)
                }
            }
        }
    }
}

/** La conferma di una scrittura nell'agenda: cosa cambia, su quale scheda, dove. Un solo tasto pieno. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgendaEditSheet(e: RecapAgenda.Edit, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val (title, change, button) = when (e.item) {
        Item.DONE -> Triple(R.string.agenda_done_title, stringResource(R.string.agenda_done_change), R.string.agenda_done)
        Item.POSTPONE -> Triple(R.string.agenda_postpone_confirm, stringResource(R.string.agenda_postpone_change, untilLabel(e.until?.toString())), R.string.agenda_postpone)
        Item.REMOVE -> Triple(R.string.agenda_remove_title, stringResource(R.string.agenda_remove_change), R.string.agenda_remove)
        Item.PASS_CLAUDE -> Triple(R.string.agenda_pass_claude_title, stringResource(R.string.agenda_pass_change, "claude"), R.string.agenda_pass)
        else -> Triple(R.string.agenda_pass_me_title, stringResource(R.string.agenda_pass_change, "franz"), R.string.agenda_pass)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CmColors.surface) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, color = CmColors.text)
            Text(stringResource(R.string.agenda_from_scope, e.row.scope.trim()), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
            Text(
                e.row.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = CmColors.text,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CmColors.surfaceLow).padding(horizontal = 18.dp, vertical = 14.dp),
            )
            Text(change, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = CmColors.actionIcon) }
                androidx.compose.material3.Button(
                    onClick = onConfirm,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (e.item == Item.REMOVE) CmColors.gone else CmColors.primary,
                        contentColor = if (e.item == Item.REMOVE) androidx.compose.ui.graphics.Color.White else CmColors.onPrimary,
                    ),
                ) { Text(stringResource(button)) }
            }
        }
    }
}
