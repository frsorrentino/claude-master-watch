package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.contract.AgendaPage
import it.pixelbox.cmwatch.contract.AgendaRow
import it.pixelbox.cmwatch.rules.RecapActions
import it.pixelbox.cmwatch.rules.RecapAgenda
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** «06/10» dal giorno ISO del recap; vuoto se non si legge. */
internal fun recapDay(date: String): String =
    runCatching { java.time.LocalDate.parse(date).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")) }.getOrDefault("")

/**
 * Un'azione della sezione Recap come tasto (mockup approvato l'08/10, tavola 1): il testo e, piccolo accanto, da dove viene
 * (la sessione, o «recap 06/10»). Mandata, resta grigia con la spunta finché lo stato non la toglie.
 */
@Composable
internal fun RecapActionChip(a: RecapActions.Action, day: String, sent: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.step_sent, a.text)
    Surface(
        onClick = onClick, enabled = !sent, shape = CircleShape,
        color = if (sent) CmColors.idle.copy(alpha = 0.14f) else CmColors.surfaceLow, contentColor = if (sent) CmColors.text2 else CmColors.text,
        modifier = if (sent) Modifier.semantics { contentDescription = label } else Modifier,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (sent) Icon(Icons.Rounded.Check, null, tint = CmColors.idle, modifier = Modifier.size(16.dp))
            Text(a.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false))
            // L'etichetta resta su una riga: se manca spazio va a capo il testo dell'azione, non «recap 12/09».
            Text(
                if (a.recap) stringResource(R.string.recap_from_recap, day).trim() else a.from,
                style = MaterialTheme.typography.bodySmall, color = CmColors.text2, softWrap = false, maxLines = 1,
            )
        }
    }
}

/**
 * «Mandare questa azione?» (tavola 3): da dove viene, il testo che parte, a chi va — la sessione viva del progetto, o la
 * master con «Riprendi …». Un solo tasto pieno, «Manda».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecapSendSheet(a: RecapActions.Action, day: String, onDismiss: () -> Unit, from: String? = null, onSend: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CmColors.surface) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.recap_send_title), style = MaterialTheme.typography.headlineSmall, color = CmColors.text)
            Text(
                from ?: when {
                    a.agenda -> stringResource(R.string.recap_send_from_agenda)
                    a.recap -> stringResource(R.string.recap_send_from_recap, day, a.from)
                    else -> stringResource(R.string.recap_send_from_session, a.from)
                },
                style = MaterialTheme.typography.bodyMedium, color = CmColors.text2,
            )
            Text(
                a.send, style = MaterialTheme.typography.bodyLarge, color = CmColors.text,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CmColors.surfaceLow).padding(horizontal = 18.dp, vertical = 14.dp),
            )
            val to = stringResource(if (a.agenda) R.string.recap_send_to_master_agenda else if (a.viaMaster) R.string.recap_send_to_master else R.string.recap_send_to_session, a.to)
            Text(
                buildAnnotatedString {
                    val i = to.indexOf(a.to)
                    if (i < 0) append(to) else {
                        append(to.substring(0, i)); withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = CmColors.text)) { append(a.to) }; append(to.substring(i + a.to.length))
                    }
                },
                style = MaterialTheme.typography.bodyMedium, color = CmColors.text2,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = CmColors.actionIcon) }
                androidx.compose.material3.Button(
                    onClick = onSend,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
                ) { Text(stringResource(R.string.recap_send)) }
            }
        }
    }
}

/** Il segno a sinistra di una riga dell'agenda: tondo personale, quadrato agenzia, rombo postazione. */
@Composable
internal fun AgendaMark(scope: String) {
    val m = RecapAgenda.mark(scope)
    val border = androidx.compose.foundation.BorderStroke(2.dp, CmColors.text2)
    val mod = Modifier.padding(top = 3.dp).size(18.dp)
    when (m) {
        RecapAgenda.Mark.PERSONAL -> androidx.compose.foundation.layout.Box(mod.border(border, CircleShape))
        RecapAgenda.Mark.AGENCY -> androidx.compose.foundation.layout.Box(mod.border(border, RoundedCornerShape(5.dp)))
        RecapAgenda.Mark.DESK -> androidx.compose.foundation.layout.Box(mod.graphicsLayer { rotationZ = 45f; scaleX = 0.8f; scaleY = 0.8f }.border(border, RoundedCornerShape(3.dp)))
        RecapAgenda.Mark.NONE -> androidx.compose.foundation.layout.Box(mod)
    }
}

/**
 * Una riga dell'agenda (tavole 1 e 2): segno, titolo, sotto il rimando — o «blocca: …» se è ferma su altro, o lo stato se
 * non è aperta. Come le card delle sessioni (Franz, 10/10 09:24, variante A, al posto del tasto «Azioni»): ˅ la apre sul
 * posto, con il dettaglio salvato, le azioni come pillole («Fallo» piena) e il riferimento; il tocco sul titolo apre
 * «Approfondisci».
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun AgendaRowCard(
    row: AgendaRow, other: Boolean = false, onOpen: (() -> Unit)? = null,
    /** Le voci del menu della scheda (`RecapAgenda.menu`); senza voci niente ˅. */
    items: List<RecapAgenda.Item> = emptyList(),
    /** Solo per i provini: la card già aperta. */
    startOpen: Boolean = false,
    onItem: (RecapAgenda.Item) -> Unit = {},
) {
    var open by rememberSaveable(row.key.ifBlank { row.title }) { mutableStateOf(startOpen) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CmColors.surface).padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            AgendaMark(row.scope)
            Column(
                Modifier.weight(1f).then(if (onOpen != null) Modifier.clip(RoundedCornerShape(10.dp)).handCursor().clickable(onClick = onOpen) else Modifier),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(row.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = CmColors.text)
                val sub = when {
                    row.state.trim().lowercase() == "sospeso" && !row.until.isNullOrBlank() -> stringResource(R.string.agenda_until, untilLabel(row.until))
                    !RecapAgenda.isOpen(row) -> listOf(row.state.trim(), row.ref.trim()).filter { it.isNotEmpty() }.joinToString(" · ")
                    other -> stringResource(R.string.recap_blocks, row.blocks.trim())
                    else -> row.ref.trim()
                }
                if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
            }
            if (items.isNotEmpty()) androidx.compose.material3.IconButton(onClick = { open = !open }, modifier = Modifier.size(40.dp).handCursor()) {
                Icon(
                    if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    stringResource(if (open) R.string.agenda_card_close else R.string.agenda_card_open), tint = CmColors.text2,
                )
            }
        }
        if (open && items.isNotEmpty()) Column(Modifier.padding(end = 10.dp, top = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.HorizontalDivider(color = CmColors.line)
            val detail = row.detail?.trim()?.takeIf { it.isNotEmpty() }
            Text(detail ?: stringResource(R.string.agenda_no_detail), style = MaterialTheme.typography.bodyMedium, color = if (detail != null) CmColors.text else CmColors.text2)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.filter { it != RecapAgenda.Item.DEEPEN && it != RecapAgenda.Item.OPEN_REF }.forEach { item -> AgendaChip(item) { onItem(item) } }
            }
            if (RecapAgenda.Item.OPEN_REF in items) Text(
                stringResource(R.string.agenda_open_ref) + " ↗", style = MaterialTheme.typography.bodyMedium, color = CmColors.actionIcon,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).handCursor().clickable { onItem(RecapAgenda.Item.OPEN_REF) }.padding(vertical = 4.dp),
            )
        }
    }
}

/** Una voce della card aperta: «Fallo» piena (un solo tasto pieno), le altre col bordo, «Rimuovi» in rosso. */
@Composable
private fun AgendaChip(item: RecapAgenda.Item, onClick: () -> Unit) {
    val filled = item == RecapAgenda.Item.DO
    Surface(
        onClick = onClick, shape = RoundedCornerShape(999.dp), color = if (filled) CmColors.primary else androidx.compose.ui.graphics.Color.Transparent,
        border = if (filled) null else androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF3A4150)), modifier = Modifier.handCursor(),
    ) {
        Text(
            label(item), style = MaterialTheme.typography.labelLarge,
            color = when { filled -> CmColors.onPrimary; item == RecapAgenda.Item.REMOVE -> CmColors.gone; else -> CmColors.text },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/** «ven 10/10» da `AAAA-MM-GG`; com'è se non si legge. */
@Composable
internal fun untilLabel(until: String?): String {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    return runCatching { java.time.LocalDate.parse(until!!.trim()).format(java.time.format.DateTimeFormatter.ofPattern("EEE dd/MM", locale)) }.getOrDefault(until.orEmpty())
}

/** Il titoletto colorato di un gruppo: «ASPETTA TE · 8» ambra, «PUÒ FARLO CLAUDE · 6» celeste, gli altri grigi. */
@Composable
internal fun RecapGroup(text: String, tone: androidx.compose.ui.graphics.Color) = GroupHeader(text, tone)

/** Le Azioni come tasti, a capo quando non stanno. */
@Composable
internal fun RecapActionChips(actions: List<RecapActions.Action>, day: String, sent: List<String>, onAsk: (RecapActions.Action) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEach { a -> RecapActionChip(a, day, sentKey(a) in sent) { onAsk(a) } }
    }
}

internal fun sentKey(a: RecapActions.Action) = a.to + "\n" + a.send

/**
 * La pagina Recap (tavola 2): dal menu ≡ o da «Tutto il recap». Filtri per ambito in cima; le Azioni; poi chi deve muoversi;
 * in fondo, richiuso, fatto e sospeso. `agenda` null = in arrivo; `error` = il rifiuto del relay, già in parole.
 */
@Composable
fun RecapScreen(
    actions: List<RecapActions.Action>, recapDate: String, agenda: AgendaPage?, error: String?, loading: Boolean,
    onBack: () -> Unit, onSend: (RecapActions.Action) -> Unit,
    /** Contratto 1.47: il relay scrive nell'agenda (Fatto, Rimanda, Rimuovi, Passa). */
    canWrite: Boolean = false, onTalk: (String) -> Unit = {}, onEdit: (RecapAgenda.Edit) -> Unit = {},
    today: java.time.LocalDate = java.time.LocalDate.now(),
    /** false = nel cruscotto del tablet (variante B, 09/10): niente ← né barre di sistema, il titolo come le sezioni. */
    topBar: Boolean = true,
    /** ▶ sul dettaglio di «Approfondisci» (09/10 16:31). */
    onSpeak: ((String) -> Unit)? = null,
) {
    var scope by rememberSaveable { mutableStateOf<String?>(null) }
    var restOpen by rememberSaveable { mutableStateOf(false) }
    var asking by remember { mutableStateOf<RecapActions.Action?>(null) }
    var sent by rememberSaveable { mutableStateOf(listOf<String>()) }
    val day = recapDay(recapDate)
    val m = remember(agenda, scope, today) { RecapAgenda.of(agenda, scope, today) }
    val acts = remember { AgendaActions() }
    // Le voci della card aperta (variante A del 10/10): le stesse del menu Azioni, «Stanotte» con un relay 1.49.
    val canNight = LocalAgendaNight.current != null
    val cardItems: (it.pixelbox.cmwatch.contract.AgendaRow) -> List<it.pixelbox.cmwatch.rules.RecapAgenda.Item> = { r -> it.pixelbox.cmwatch.rules.RecapAgenda.menu(r, canWrite, today, canNight) }
    Column(Modifier.fillMaxSize().background(CmColors.bg).then(if (topBar) Modifier.systemBarsPadding() else Modifier)) {
        if (topBar) Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = CmColors.text) }
            Text(stringResource(R.string.home_sec_recap), style = MaterialTheme.typography.titleLarge, color = CmColors.text)
        } else DeskSectionTitle(stringResource(R.string.home_sec_recap))
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = if (topBar) 16.dp else 0.dp, end = if (topBar) 16.dp else 0.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "filters") {
                Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.FilterChip(selected = scope == null, onClick = { scope = null }, label = { Text(stringResource(R.string.recap_filter_all)) })
                    RecapAgenda.SCOPES.forEach { sc ->
                        androidx.compose.material3.FilterChip(selected = scope == sc, onClick = { scope = sc }, label = { Text(scopeLabel(sc)) })
                    }
                }
            }
            if (actions.isNotEmpty()) {
                item(key = "h-actions") { RecapGroup(stringResource(R.string.recap_actions, actions.size), CmColors.text) }
                item(key = "actions") { RecapActionChips(actions, day, sent) { asking = it } }
            }
            when {
                error != null -> item(key = "error") { Text(error, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, modifier = Modifier.padding(8.dp)) }
                agenda == null && loading -> item(key = "loading") {
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(color = CmColors.actionIcon)
                    }
                }
            }
            if (m.you.isNotEmpty()) {
                item(key = "h-you") { RecapGroup(stringResource(R.string.recap_you, m.you.size), CmColors.waiting) }
                items(m.you.size, key = { "you-$it" }) { i -> val r = m.you[i]; AgendaRowCard(r, onOpen = { acts.deepen(r) }, items = cardItems(r)) { x -> acts.pick(r, x) } }
            }
            if (m.claude.isNotEmpty()) {
                item(key = "h-claude") { RecapGroup(stringResource(R.string.recap_claude, m.claude.size), CmColors.actionIcon) }
                items(m.claude.size, key = { "claude-$it" }) { i -> val r = m.claude[i]; AgendaRowCard(r, onOpen = { acts.deepen(r) }, items = cardItems(r)) { x -> acts.pick(r, x) } }
            }
            if (m.other.isNotEmpty()) {
                item(key = "h-other") { RecapGroup(stringResource(R.string.recap_other, m.other.size), CmColors.text2) }
                items(m.other.size, key = { "other-$it" }) { i -> val r = m.other[i]; AgendaRowCard(r, other = true, onOpen = { acts.deepen(r) }, items = cardItems(r)) { x -> acts.pick(r, x) } }
            }
            if (m.rest.isNotEmpty()) {
                item(key = "rest") {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp).handCursor().clickable { restOpen = !restOpen }.padding(horizontal = 4.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.recap_rest, m.rest.size), style = MaterialTheme.typography.bodyLarge, color = CmColors.text2, modifier = Modifier.weight(1f))
                        Icon(if (restOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = CmColors.text2)
                    }
                }
                if (restOpen) items(m.rest.size, key = { "rest-$it" }) { i -> val r = m.rest[i]; AgendaRowCard(r, onOpen = { acts.deepen(r) }, items = cardItems(r)) { x -> acts.pick(r, x) } }
            }
            if (agenda != null && m.isEmpty && actions.isEmpty()) item(key = "empty") {
                Text(stringResource(R.string.recap_empty), style = MaterialTheme.typography.bodyLarge, color = CmColors.text2, modifier = Modifier.padding(8.dp))
            }
        }
    }
    asking?.let { a -> RecapSendSheet(a, day, onDismiss = { asking = null }) { asking = null; sent = sent + sentKey(a); onSend(a) } }
    AgendaActionsLayer(acts, canWrite, today, onSend = onSend, onTalk = onTalk, onEdit = onEdit, onSpeak = onSpeak)
}

@Composable
internal fun scopeLabel(scope: String) = when (scope) {
    "agenzia" -> stringResource(R.string.recap_scope_agency)
    "personale" -> stringResource(R.string.recap_scope_personal)
    else -> stringResource(R.string.recap_scope_desk)
}
