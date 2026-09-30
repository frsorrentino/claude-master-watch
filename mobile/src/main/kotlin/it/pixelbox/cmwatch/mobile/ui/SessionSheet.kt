@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package it.pixelbox.cmwatch.mobile.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Choices
import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.Tier
import it.pixelbox.cmwatch.data.Pending
import it.pixelbox.cmwatch.data.PendingStatus
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.contract.TranscriptEntry
import it.pixelbox.cmwatch.contract.TranscriptFile
import it.pixelbox.cmwatch.contract.TranscriptTurn
import it.pixelbox.cmwatch.rules.ChatFeed
import it.pixelbox.cmwatch.rules.ChatRules
import it.pixelbox.cmwatch.rules.ModelText
import it.pixelbox.cmwatch.rules.PhonePrimary
import it.pixelbox.cmwatch.rules.QuestionRules
import it.pixelbox.cmwatch.rules.Sent
import it.pixelbox.cmwatch.rules.SessionMeters
import it.pixelbox.cmwatch.rules.SessionsText
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SheetActions(
    val answer: (Int) -> Unit, val allowAll: () -> Unit, val send: (PhonePrimary.Target, String) -> Unit,
    val follow: (Boolean) -> Unit, val reopen: () -> Unit, val terminal: () -> Unit, val openInClaude: () -> Unit,
    val speak: (String) -> Unit, val retry: (cmdId: String) -> Unit,
    /** «Chat about this» (contratto 1.10), come sull'orologio: la domanda resta gestibile anche senza opzioni. */
    val chat: () -> Unit = {},
    /** Contratto 1.12: modello ed effort di questa sola sessione. */
    val setModel: (id: String) -> Unit = {}, val setEffort: (level: String) -> Unit = {},
    /** Contratto 1.21: il tasto Stop. */
    val interrupt: () -> Unit = {},
    /** Un'immagine dalla galleria, con il testo scritto accanto (contratto 1.19, come «Condividi»). */
    val attach: (Uri, String) -> Unit = { _, _ -> },
)

/** Un messaggio della chat con il suo stato, calcolato in `MainActivity` da `ChatRules.status`. */
data class ChatRow(val sent: Sent, val status: ChatRules.Status)

/**
 * La scheda sessione (design 29/09; restyling e chat 30/09): testata con badge e contatori toccabili (modello, effort),
 * la domanda come sull'orologio, la chat dei messaggi mandati con il loro stato e l'esito di ogni turno, e in fondo la
 * barra di scrittura sopra la tastiera. Un solo bottone pieno: la prima opzione se la domanda ne ha, altrimenti il tasto
 * della barra (Invia o Stop) o «Riapri».
 */
@Composable
fun SessionSheet(
    s: Session, now: Long, pending: List<Pending>, ttsMinChars: Int, actions: SheetActions,
    chat: List<ChatRow> = emptyList(), choices: Choices? = null, ops: List<String>? = null,
    canTune: Boolean = true, canAttach: Boolean = false,
    /** Contratto 1.22: la conversazione vera; null = relay senza `transcript`, resta la chat dei messaggi mandati. */
    feed: List<ChatFeed.Item>? = null, more: Boolean = false, onOlder: () -> Unit = {},
) {
    // Legata anche alla domanda: una domanda nuova non eredita la bozza scritta per quella di prima (revisione 29/09).
    var draft by rememberSaveable(s.id, s.question?.id) { mutableStateOf("") }
    var holdHint by rememberSaveable(s.question?.id) { mutableStateOf(false) }
    val primary = PhonePrimary.button(s, draft)
    val list = rememberLazyListState()
    // Il messaggio appena mandato e l'esito appena arrivato si vedono senza scorrere.
    LaunchedEffect(chat.size, chat.lastOrNull()?.status, feed?.size) {
        if (chat.isNotEmpty() || !feed.isNullOrEmpty()) list.animateScrollToItem((list.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
    }
    Column(Modifier.fly("card-${s.id}").fillMaxSize().background(CmColors.bg)) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(), state = list,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "head") { SheetHeader(s, now, choices, canTune, actions) }
            item(key = "actions") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Con la conversazione vera il terminale non serve più dal telefono (Franz, 30/09 20:39).
                    if (feed == null) ActionChip(stringResource(R.string.terminal), Icons.Rounded.Terminal, actions.terminal)
                    ActionChip(stringResource(if (s.followed) R.string.unfollow else R.string.follow), if (s.followed) Icons.Rounded.NotificationsOff else Icons.Rounded.NotificationsActive) { actions.follow(!s.followed) }
                    if (s.link.isNotBlank()) ActionChip(stringResource(R.string.open_in_claude), Icons.AutoMirrored.Rounded.OpenInNew, actions.openInClaude)
                }
            }
            s.question?.let { q ->
                item(key = "q-" + q.id) {
                    QuestionCard(q, primary == PhonePrimary.Button.OPTION, holdHint, onHold = { holdHint = true }, actions)
                }
            }
            if (feed != null) {
                if (more) item(key = "older") {
                    TextButton(onClick = onOlder, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.load_older), color = CmColors.actionIcon) }
                }
                items(feed, key = { feedKey(it) }) { it ->
                    when (it) {
                        is ChatFeed.Item.Mine -> MineBubble(it.sent, it.status, actions, onEdit = { t -> draft = t }, onResend = { t -> actions.send(PhonePrimary.Target.PROMPT, t) })
                        is ChatFeed.Item.User -> UserBubble(it.entry, onEdit = { t -> draft = t }, onResend = { t -> actions.send(PhonePrimary.Target.PROMPT, t) })
                        is ChatFeed.Item.Claude -> ClaudeBubble(it.entry.text.orEmpty(), it.entry.at, ttsMinChars, actions.speak, cut = it.entry.cut, turn = it.entry.turn)
                        is ChatFeed.Item.Tool -> ToolLine(it.entry)
                    }
                }
            } else {
                items(chat, key = { "c-" + it.sent.id }) { row ->
                    ChatTurn(row, ttsMinChars, actions, onEdit = { draft = it }, onResend = { actions.send(PhonePrimary.Target.PROMPT, it) })
                }
                // L'esito di un turno partito dal PC, che nessun messaggio del telefono ha agganciato.
                s.outcome?.takeIf { o -> chat.none { it.sent.outcomeFull == o.full } }?.let { o ->
                    item(key = "outcome") { ClaudeBubble(o.full, o.at, ttsMinChars, actions.speak) }
                }
            }
            pending.filter { it.cmd.session == s.id || it.cmd.session == s.name }
                .filter { it.status == PendingStatus.FAILED && chat.none { c -> c.sent.id == it.cmd.id } }
                .forEach { p ->
                    item(key = "p-" + p.cmd.id) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.not_delivered), color = CmColors.goneDim, modifier = Modifier.weight(1f))
                            TextButton(onClick = { actions.retry(p.cmd.id) }) { Text(stringResource(R.string.retry), color = CmColors.actionIcon) }
                        }
                    }
                }
        }
        Composer(s, draft, onDraft = { draft = it }, ops, canAttach, actions, onSent = { draft = "" })
    }
}

/** La barra di scrittura (design 30/09, parti 1 e 7): sopra la tastiera, «+» per un'immagine, il tasto a destra. */
@Composable
private fun Composer(
    s: Session, draft: String, onDraft: (String) -> Unit, ops: List<String>?, canAttach: Boolean, actions: SheetActions, onSent: () -> Unit,
) {
    val mode = PhonePrimary.composer(s, draft, ops)
    val bar = Modifier.fillMaxWidth().background(CmColors.bg).imePadding().padding(horizontal = 12.dp, vertical = 10.dp)
    if (mode == PhonePrimary.Composer.REOPEN) {
        Button(
            onClick = actions.reopen, colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
            modifier = bar.height(56.dp),
        ) { Text(stringResource(R.string.reopen)) }
        return
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { actions.attach(uri, draft.trim()); onSent() }
    }
    Row(bar, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (canAttach) IconButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
            Icon(Icons.Rounded.Add, stringResource(R.string.attach_image), tint = CmColors.actionIcon)
        }
        OutlinedTextField(
            value = draft, onValueChange = onDraft, maxLines = 5, modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(if (s.question != null) R.string.answer_free else R.string.write_prompt)) },
            shape = MaterialTheme.shapes.extraLarge,
        )
        val send = {
            PhonePrimary.target(s, draft)?.let { actions.send(it, draft.trim()) }
            onSent()
        }
        val filled = IconButtonDefaults.filledIconButtonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary)
        val size = Modifier.size(52.dp)
        when (mode) {
            PhonePrimary.Composer.SEND -> FilledIconButton(onClick = send, colors = filled, modifier = size) {
                Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.send))
            }
            PhonePrimary.Composer.SEND_TONAL -> FilledTonalIconButton(onClick = send, modifier = size) {
                Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.send))
            }
            PhonePrimary.Composer.STOP -> FilledIconButton(onClick = actions.interrupt, colors = filled, modifier = size) {
                Icon(Icons.Rounded.Stop, stringResource(R.string.stop))
            }
            else -> FilledTonalIconButton(onClick = {}, enabled = false, modifier = size) {
                Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.send))
            }
        }
    }
}

/** La domanda come sull'orologio: prima opzione piena, pressione lunga per il rischio alto, «Parliamone», «Consenti tutto». */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuestionCard(
    q: it.pixelbox.cmwatch.contract.Question, firstFilled: Boolean, holdHint: Boolean, onHold: () -> Unit, actions: SheetActions,
) {
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
                    QuestionRules.optionLabel(o), filled = i == 0 && firstFilled,
                    onClick = { if (long) onHold() else actions.answer(o.n) },
                    onLongClick = if (long) ({ actions.answer(o.n) }) else null,
                )
            }
            val allowAll = QuestionRules.allowAllVisible(q)
            ButtonGroup(overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) }, modifier = Modifier.fillMaxWidth()) {
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

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
private fun hhmm(epoch: Long) = HHMM.format(Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()))

/** Un messaggio mandato (a destra, con lo stato e le azioni) e, se il suo turno è finito con un esito, Claude a sinistra. */
@Composable
private fun ChatTurn(row: ChatRow, ttsMinChars: Int, actions: SheetActions, onEdit: (String) -> Unit, onResend: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MineBubble(row.sent, row.status, actions, onEdit, onResend)
        row.sent.outcomeFull?.let { ClaudeBubble(it, row.sent.doneAt, ttsMinChars, actions.speak) }
    }
}

/** Un messaggio mandato dal telefono: a destra, con l'anteprima dell'allegato, lo stato, l'ora e Copia, Modifica, Reinvia. */
@Composable
private fun MineBubble(m: Sent, status: ChatRules.Status, actions: SheetActions, onEdit: (String) -> Unit, onResend: (String) -> Unit) {
    val clip = LocalClipboardManager.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Surface(
            color = CmColors.surfaceHigh, shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp),
            modifier = Modifier.widthIn(max = 320.dp).then(if (status == ChatRules.Status.FAILED) Modifier.clickable { actions.retry(m.id) } else Modifier),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                m.attachment?.let { AttachmentThumb(it) }
                if (m.text.isNotBlank()) Text(m.text, style = MaterialTheme.typography.bodyLarge, color = CmColors.text)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            StatusMark(status)
            Text(hhmm(m.sentAt), style = MaterialTheme.typography.labelMedium, color = CmColors.text2, modifier = Modifier.padding(end = 4.dp))
            SmallAction(Icons.Rounded.ContentCopy, stringResource(R.string.copy)) { clip.setText(AnnotatedString(m.text)) }
            SmallAction(Icons.Rounded.Edit, stringResource(R.string.edit)) { onEdit(m.text) }
            SmallAction(Icons.Rounded.Replay, stringResource(R.string.resend)) { onResend(m.text) }
        }
    }
}

/** Un messaggio scritto altrove (al PC, dall'orologio): a destra come i propri, su una superficie più bassa, con da dove. */
@Composable
private fun UserBubble(e: TranscriptEntry, onEdit: (String) -> Unit, onResend: (String) -> Unit) {
    val clip = LocalClipboardManager.current
    val text = e.text.orEmpty()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Surface(color = CmColors.surface, shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp), modifier = Modifier.widthIn(max = 320.dp)) {
            Text(text, style = MaterialTheme.typography.bodyLarge, color = CmColors.text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val from = when (e.origin) {
                "pc" -> R.string.origin_pc
                "watch" -> R.string.origin_watch
                "phone" -> R.string.origin_phone
                else -> null
            }
            from?.let { Text(stringResource(it), style = MaterialTheme.typography.labelMedium, color = CmColors.text2) }
            e.at?.let { Text(hhmm(it), style = MaterialTheme.typography.labelMedium, color = CmColors.text2, modifier = Modifier.padding(horizontal = 4.dp)) }
            SmallAction(Icons.Rounded.ContentCopy, stringResource(R.string.copy)) { clip.setText(AnnotatedString(text)) }
            SmallAction(Icons.Rounded.Edit, stringResource(R.string.edit)) { onEdit(text) }
            SmallAction(Icons.Rounded.Replay, stringResource(R.string.resend)) { onResend(text) }
        }
    }
}

/** Una chiamata a uno strumento in una riga compatta: nome, cosa fa, esito; sotto i file prodotti, se ce ne sono. */
@Composable
private fun ToolLine(e: TranscriptEntry) {
    Column(Modifier.fillMaxWidth().padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                if (e.error == true) Icons.Rounded.ErrorOutline else Icons.Rounded.Build, null,
                tint = if (e.error == true) CmColors.gone else CmColors.text2, modifier = Modifier.size(16.dp).padding(top = 2.dp),
            )
            Text(
                listOfNotNull(e.tool, e.note ?: e.text).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium, color = if (e.error == true) CmColors.goneDim else CmColors.text2,
            )
        }
        e.files?.takeIf { it.isNotEmpty() }?.let { files ->
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 24.dp)) {
                files.forEach { FileChip(it) }
            }
        }
    }
}

/** Un file prodotto da Claude: icona per tipo, nome e peso. L'anteprima e l'apertura arrivano con il trasferimento dei file. */
@Composable
private fun FileChip(f: TranscriptFile) {
    val icon = when {
        f.mime?.startsWith("image/") == true -> Icons.Rounded.Image
        f.mime == "application/pdf" -> Icons.Rounded.PictureAsPdf
        f.mime?.startsWith("video/") == true -> Icons.Rounded.Movie
        f.mime?.startsWith("audio/") == true -> Icons.Rounded.AudioFile
        else -> Icons.Rounded.InsertDriveFile
    }
    Surface(color = CmColors.surface, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = CmColors.actionIcon, modifier = Modifier.size(18.dp))
            Text(f.path.substringAfterLast('/'), style = MaterialTheme.typography.labelLarge, color = CmColors.text)
            f.size?.let { Text(sizeLabel(it), style = MaterialTheme.typography.labelMedium, color = CmColors.text2) }
        }
    }
}

private fun sizeLabel(b: Long): String = when {
    b >= 1_000_000 -> "%.1f MB".format(b / 1_000_000.0)
    b >= 1_000 -> "${b / 1_000} KB"
    else -> "$b B"
}

private fun feedKey(i: ChatFeed.Item): String = when (i) {
    is ChatFeed.Item.Mine -> "m-" + i.sent.id
    is ChatFeed.Item.User -> "u-" + i.entry.id
    is ChatFeed.Item.Claude -> "a-" + i.entry.id
    is ChatFeed.Item.Tool -> "t-" + i.entry.id
}

/** L'esito di un turno come fumetto di Claude, con Copia e Ascolta sotto (come nell'app nativa, senza fissa e dirama). */
@Composable
private fun ClaudeBubble(
    text: String, at: Long?, ttsMinChars: Int, onSpeak: (String) -> Unit, cut: Boolean = false, turn: TranscriptTurn? = null,
) {
    val clip = LocalClipboardManager.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Surface(color = CmColors.briefCard, shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp), modifier = Modifier.widthIn(max = 340.dp)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text, style = MaterialTheme.typography.bodyLarge, color = CmColors.text)
                if (cut) Text(stringResource(R.string.text_cut), style = MaterialTheme.typography.labelMedium, color = CmColors.text2)
                // Il costo del turno sull'ultima voce: durata e token scritti (quelli letti comprendono la cache).
                turn?.let { t ->
                    val secs = (t.ended ?: 0) - (t.started ?: 0)
                    val parts = listOfNotNull(
                        secs.takeIf { t.started != null && t.ended != null && it > 0 }?.let { d -> Durations.since(0, d) },
                        t.out?.let { o -> stringResource(R.string.turn_tokens, o) },
                    )
                    if (parts.isNotEmpty()) Text(parts.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = CmColors.briefSecondary)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            SmallAction(Icons.Rounded.ContentCopy, stringResource(R.string.copy)) { clip.setText(AnnotatedString(text)) }
            // Esiti sempre leggibili a voce (regola del ▶: esiti, risposte e domande, oltre alla soglia dei 120 caratteri).
            SmallAction(Icons.AutoMirrored.Rounded.VolumeUp, stringResource(R.string.read_aloud)) { onSpeak(text) }
            at?.let { Text(hhmm(it), style = MaterialTheme.typography.labelMedium, color = CmColors.text2, modifier = Modifier.padding(start = 4.dp)) }
        }
    }
}

/** Lo stato di un messaggio come icona, con la parola solo per chi deve fare qualcosa (non consegnato). */
@Composable
private fun StatusMark(st: ChatRules.Status) {
    val (icon, tint, label) = when (st) {
        ChatRules.Status.SENDING -> Triple(Icons.Rounded.Schedule, CmColors.text2, R.string.chat_sending)
        ChatRules.Status.FAILED -> Triple(Icons.Rounded.ErrorOutline, CmColors.gone, R.string.chat_failed)
        ChatRules.Status.DELIVERED -> Triple(Icons.Rounded.Check, CmColors.actionIcon, R.string.chat_delivered)
        ChatRules.Status.QUEUED -> Triple(Icons.Rounded.Check, CmColors.stale, R.string.chat_queued)
        ChatRules.Status.WORKING -> Triple(Icons.Rounded.PlayArrow, CmColors.busy, R.string.chat_working)
        ChatRules.Status.DONE -> Triple(Icons.Rounded.DoneAll, CmColors.actionIcon, R.string.chat_done)
    }
    val text = stringResource(label)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, text, tint = tint, modifier = Modifier.size(16.dp))
        if (st == ChatRules.Status.FAILED || st == ChatRules.Status.QUEUED || st == ChatRules.Status.WORKING) {
            Text(text, style = MaterialTheme.typography.labelMedium, color = tint)
        }
    }
}

@Composable
private fun SmallAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) =
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) { Icon(icon, label, tint = CmColors.text2, modifier = Modifier.size(18.dp)) }

/** L'anteprima dell'immagine allegata, dalla copia locale; niente se il file non c'è più. */
@Composable
private fun AttachmentThumb(path: String) {
    val bmp = remember(path) { runCatching { android.graphics.BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() } ?: return
    androidx.compose.foundation.Image(
        bmp, stringResource(R.string.attach_image), contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(14.dp)),
    )
}

/**
 * Testata: account e progetto, stato come pillola, modello ed effort toccabili (contratto 1.12) con il foglio delle
 * scelte, contesto, obiettivo e bassa priorità. Il nome sta nel menu delle sessioni in alto.
 */
@Composable
private fun SheetHeader(s: Session, now: Long, choices: Choices?, canTune: Boolean, actions: SheetActions) {
    var picker by remember { mutableStateOf<String?>(null) }   // "model" | "effort"
    val tunable = canTune && choices != null && s.state != SessionState.GONE
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SessionBadge(s, size = 18.dp)
            Text("${s.account} · ${s.project}", style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
        }
        StatePill(s.state, Durations.since(s.since, now))
        val model = ModelText.short(s.model)
        val effort = SessionMeters.effortStep(s.effort)
        if (model != null || effort != null || tunable) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { picker = "model" }, enabled = tunable,
                label = { Text(model ?: stringResource(R.string.model_title)) },
                trailingIcon = if (tunable) ({ Icon(Icons.Rounded.ArrowDropDown, null) }) else null,
                shape = CircleShape, border = null,
                colors = AssistChipDefaults.assistChipColors(containerColor = CmColors.surface, labelColor = CmColors.text, disabledContainerColor = CmColors.surface, disabledLabelColor = CmColors.text),
            )
            AssistChip(
                onClick = { picker = "effort" }, enabled = tunable,
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(stringResource(R.string.effort))
                        repeat(SessionMeters.EFFORT_STEPS) { i ->
                            Box(Modifier.size(width = 10.dp, height = 6.dp).background(if (effort != null && i < effort) CmColors.briefRing else CmColors.briefTrack, CircleShape))
                        }
                    }
                },
                trailingIcon = if (tunable) ({ Icon(Icons.Rounded.ArrowDropDown, null) }) else null,
                shape = CircleShape, border = null,
                colors = AssistChipDefaults.assistChipColors(containerColor = CmColors.surface, labelColor = CmColors.text2, disabledContainerColor = CmColors.surface, disabledLabelColor = CmColors.text2),
            )
        }
        s.context?.let { ContextBar(it, null) }
        SessionsText.goalLine(s, stringResource(R.string.goal))?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = CmColors.briefLabel) }
        SessionsText.priority(s, stringResource(R.string.low_priority), stringResource(R.string.low_priority_offered))?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = CmColors.waiting)
        }
    }
    if (picker != null && choices != null) {
        ModalBottomSheet(onDismissRequest = { picker = null }, containerColor = CmColors.surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(if (picker == "model") R.string.model_title else R.string.effort_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text,
                )
                Text(stringResource(R.string.choice_this_session), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
                Spacer(Modifier.height(8.dp))
                val rows: List<Pair<String, String>> = if (picker == "model") choices.models.map { it.id to (it.label ?: ModelText.short(it) ?: it.id) }
                    else choices.efforts.map { it to it }
                val current = if (picker == "model") s.model?.id else s.effort
                rows.forEach { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable {
                            if (picker == "model") actions.setModel(value) else actions.setEffort(value)
                            picker = null
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = value == current, onClick = null)
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = CmColors.text)
                    }
                }
            }
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
