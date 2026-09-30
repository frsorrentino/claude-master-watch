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
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.transformable
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
import kotlinx.coroutines.flow.first
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

/** Un messaggio della chat con il suo stato e, se non è stato consegnato, il motivo (`ChatRules.status`/`reason`). */
data class ChatRow(val sent: Sent, val status: ChatRules.Status, val reason: String? = null)

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
    // Si scende da soli solo se si era già in fondo o dopo un proprio invio: chi rilegge più su non viene tirato giù, e
    // una pagina di messaggi precedenti non rimbalza in fondo (revisione 30/09).
    val atBottom by remember { derivedStateOf { !list.canScrollForward } }
    var justSent by remember { mutableStateOf(false) }
    val reasons = chat.associate { it.sent.id to it.reason }
    // Alla prima apertura, e quando arriva la prima pagina della conversazione vera, la chat parte dal fondo
    // (Franz, 30/09 22:55: «ancorato alla fine»).
    var anchored by remember(s.id, feed != null) { mutableStateOf(false) }
    LaunchedEffect(chat.size, chat.lastOrNull()?.status, feed?.size, feed?.lastOrNull()) {
        if (chat.isEmpty() && feed.isNullOrEmpty()) return@LaunchedEffect
        val count = androidx.compose.runtime.snapshotFlow { list.layoutInfo.totalItemsCount }.first { it > 0 }
        when {
            !anchored -> { list.scrollToItem(count - 1); anchored = true }
            atBottom || justSent -> { list.animateScrollToItem(count - 1); justSent = false }
        }
    }
    Column(Modifier.fly("card-${s.id}").fillMaxSize().background(CmColors.bg)) {
        // Fissa sopra la chat e compatta (Franz, 30/09 22:01: scorreva con la chat ed era troppo grande).
        SheetHeader(s, now, choices, canTune, actions, showTerminal = feed == null)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(), state = list,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (feed != null) {
                if (more) item(key = "older") {
                    TextButton(onClick = onOlder, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.load_older), color = CmColors.actionIcon) }
                }
                items(feed, key = { feedKey(it) }) { it ->
                    when (it) {
                        // Una voce ancora in coda nel turno (scritta mentre Claude lavora) si dice «in coda».
                        is ChatFeed.Item.Mine -> MineBubble(it.sent, if (it.entry?.queued == true && it.status != ChatRules.Status.FAILED) ChatRules.Status.QUEUED else it.status, reasons[it.sent.id], actions, onEdit = { t -> draft = t }, onResend = { t -> actions.send(PhonePrimary.Target.PROMPT, t) })
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
            // La domanda dopo la chat, sopra la barra: il suo bottone pieno resta in vista (revisione 30/09).
            s.question?.let { q ->
                item(key = "q-" + q.id) {
                    QuestionCard(q, primary == PhonePrimary.Button.OPTION, holdHint, onHold = { holdHint = true }, actions)
                }
            }
            // Solo i comandi dell'utente: le letture delle schermate non diventano righe «non consegnato».
            pending.filter { it.cmd.session == s.id || it.cmd.session == s.name }
                .filter { it.cmd.op !in PASSIVE_OPS }
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
        Composer(s, draft, onDraft = { draft = it }, ops, canAttach, actions, onSent = { draft = ""; justSent = true })
    }
}

/**
 * La barra di scrittura (design 30/09, parti 1 e 7): sopra la tastiera, «+» per un'immagine, il tasto a destra.
 * L'immagine scelta resta in attesa sopra il campo, con la sua anteprima e la ×, e parte con Invia insieme al testo
 * (Franz, 30/09 22:03: prima partiva subito).
 */
@Composable
private fun Composer(
    s: Session, draft: String, onDraft: (String) -> Unit, ops: List<String>?, canAttach: Boolean, actions: SheetActions, onSent: () -> Unit,
) {
    var image by rememberSaveable(s.id) { mutableStateOf<Uri?>(null) }
    val base = PhonePrimary.composer(s, draft, ops)
    // Con un'immagine in attesa c'è sempre qualcosa da mandare, anche con il campo vuoto.
    val mode = if (image != null && base != PhonePrimary.Composer.REOPEN && base != PhonePrimary.Composer.SEND && base != PhonePrimary.Composer.SEND_TONAL)
        (if (PhonePrimary.button(s, "x") == PhonePrimary.Button.OPTION) PhonePrimary.Composer.SEND_TONAL else PhonePrimary.Composer.SEND) else base
    val bar = Modifier.fillMaxWidth().background(CmColors.bg).imePadding().padding(horizontal = 12.dp, vertical = 10.dp)
    if (mode == PhonePrimary.Composer.REOPEN) {
        Button(
            onClick = actions.reopen, colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
            modifier = bar.height(56.dp),
        ) { Text(stringResource(R.string.reopen)) }
        return
    }
    Column(bar, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Contratto 1.23: il prompt suggerito del terminale come chip; tocco = manda, pressione lunga = nel campo.
        s.suggestion?.takeIf { draft.isBlank() && image == null }?.let { sug -> SuggestionPill(sug, onSend = { actions.send(PhonePrimary.Target.PROMPT, sug); onSent() }, onEdit = { onDraft(sug) }) }
        image?.let { uri ->
            Box {
                UriThumb(uri, Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)))
                SmallAction(Icons.Rounded.Close, stringResource(R.string.remove_image)) { image = null }
            }
        }
        val send = {
            val pending = image
            if (pending != null) actions.attach(pending, draft.trim())
            else PhonePrimary.target(s, draft)?.let { actions.send(it, draft.trim()) }
            image = null
            onSent()
        }
        // Allegato e invio dentro il campo, centrati sulla sua altezza (Franz, 30/09 22:13).
        OutlinedTextField(
            value = draft, onValueChange = onDraft, maxLines = 5, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(if (s.question != null) R.string.answer_free else R.string.write_prompt)) },
            shape = MaterialTheme.shapes.extraLarge,
            leadingIcon = if (canAttach) ({ AttachButton { uri -> image = uri } }) else null,
            trailingIcon = {
                val filled = IconButtonDefaults.filledIconButtonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary)
                val size = Modifier.padding(end = 4.dp).size(44.dp)
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
            },
        )
    }
}

/** Il prompt suggerito come pillola tonale, a una riga logica; pressione lunga = nel campo per modificarlo. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuggestionPill(text: String, onSend: () -> Unit, onEdit: () -> Unit) {
    Surface(
        color = CmColors.surfaceHigh, contentColor = CmColors.text, shape = MaterialTheme.shapes.large,
        modifier = Modifier.clip(MaterialTheme.shapes.large).combinedClickable(onClick = onSend, onLongClick = onEdit),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.AutoAwesome, stringResource(R.string.suggested), tint = CmColors.actionIcon, modifier = Modifier.size(16.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** L'anteprima di un'immagine scelta dalla galleria, ridotta mentre si legge (una foto intera occuperebbe troppa memoria). */
@Composable
private fun UriThumb(uri: Uri, modifier: Modifier) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val bmp = remember(uri) {
        runCatching {
            val o = android.graphics.BitmapFactory.Options().apply { inSampleSize = 8 }
            ctx.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, o) }?.asImageBitmap()
        }.getOrNull()
    }
    if (bmp == null) Box(modifier.background(CmColors.surface)) else
        androidx.compose.foundation.Image(bmp, stringResource(R.string.attach_image), contentScale = ContentScale.Crop, modifier = modifier)
}

/**
 * «+» per un'immagine dalla galleria. Il selettore si registra solo dove c'è un'activity che lo ospita: negli snapshot
 * (Paparazzi) non c'è, e il tasto resta disegnato senza selettore.
 */
@Composable
private fun AttachButton(onPicked: (Uri) -> Unit) {
    val icon: @Composable () -> Unit = { Icon(Icons.Rounded.Add, stringResource(R.string.attach_image), tint = CmColors.actionIcon) }
    if (androidx.activity.compose.LocalActivityResultRegistryOwner.current == null) { IconButton(onClick = {}, content = icon); return }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) onPicked(uri) }
    IconButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, content = icon)
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
        MineBubble(row.sent, row.status, row.reason, actions, onEdit, onResend)
        row.sent.outcomeFull?.let { ClaudeBubble(it, row.sent.doneAt, ttsMinChars, actions.speak) }
    }
}

/** Un messaggio mandato dal telefono: a destra, con l'anteprima dell'allegato, lo stato, l'ora e Copia, Modifica, Reinvia. */
@Composable
private fun MineBubble(m: Sent, status: ChatRules.Status, reason: String?, actions: SheetActions, onEdit: (String) -> Unit, onResend: (String) -> Unit) {
    val clip = LocalClipboardManager.current
    // Tutta la larghezza meno un margine a sinistra, non una colonna stretta (Franz, 30/09 22:17).
    Column(Modifier.fillMaxWidth().padding(start = 40.dp), horizontalAlignment = Alignment.End) {
        Surface(
            color = CmColors.surfaceHigh, shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp),
            modifier = if (status == ChatRules.Status.FAILED) Modifier.clickable { actions.retry(m.id) } else Modifier,
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
        if (status == ChatRules.Status.FAILED) Text(
            stringResource(R.string.chat_failed_why, reason ?: stringResource(R.string.chat_no_answer)),
            style = MaterialTheme.typography.labelMedium, color = CmColors.goneDim,
        )
    }
}

/** Un messaggio scritto altrove (al PC, dall'orologio): a destra come i propri, su una superficie più bassa, con da dove. */
@Composable
private fun UserBubble(e: TranscriptEntry, onEdit: (String) -> Unit, onResend: (String) -> Unit) {
    val clip = LocalClipboardManager.current
    val text = e.text.orEmpty()
    Column(Modifier.fillMaxWidth().padding(start = 40.dp), horizontalAlignment = Alignment.End) {
        Surface(color = CmColors.surface, shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)) {
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
            if (e.queued) Text(" · " + stringResource(R.string.chat_queued), style = MaterialTheme.typography.labelMedium, color = CmColors.stale)
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
        else -> Icons.AutoMirrored.Rounded.InsertDriveFile
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
    // Claude a tutta larghezza, senza fumetto, come nell'app nativa (Franz, 30/09 22:17: «non limitiamo nei balloon»).
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text, style = MaterialTheme.typography.bodyLarge, color = CmColors.text)
                if (cut) Text(stringResource(R.string.text_cut), style = MaterialTheme.typography.labelMedium, color = CmColors.text2)
                // Il costo del turno sull'ultima voce: durata e token scritti (quelli letti comprendono la cache).
                turn?.let { t ->
                    val secs = (t.ended ?: 0) - (t.started ?: 0)
                    val parts = listOfNotNull(
                        // Sotto il minuto in secondi: «0 m» per un turno di 45 s diceva niente (provini 30/09).
                        secs.takeIf { t.started != null && t.ended != null && it > 0 }?.let { d -> if (d < 60) "$d s" else Durations.since(0, d) },
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

/**
 * Lo stato preciso di un messaggio, sempre con la parola (Franz, 30/09 22:13): orologio mentre parte, una spunta quando è
 * sul canale, due quando la sessione l'ha ricevuto, azzurre quando Claude l'ha preso in carico e quando ha finito.
 */
@Composable
private fun StatusMark(st: ChatRules.Status) {
    val (icon, tint, label) = when (st) {
        ChatRules.Status.OFFLINE -> Triple(Icons.Rounded.CloudOff, CmColors.stale, R.string.chat_offline)
        ChatRules.Status.UPLOADING -> Triple(Icons.Rounded.CloudUpload, CmColors.text2, R.string.chat_uploading)
        ChatRules.Status.SENDING -> Triple(Icons.Rounded.Schedule, CmColors.text2, R.string.chat_sending)
        ChatRules.Status.SENT -> Triple(Icons.Rounded.Check, CmColors.text2, R.string.chat_sent)
        ChatRules.Status.FAILED -> Triple(Icons.Rounded.ErrorOutline, CmColors.gone, R.string.chat_failed)
        ChatRules.Status.DELIVERED -> Triple(Icons.Rounded.DoneAll, CmColors.text2, R.string.chat_delivered)
        ChatRules.Status.QUEUED -> Triple(Icons.Rounded.DoneAll, CmColors.stale, R.string.chat_queued)
        ChatRules.Status.WORKING -> Triple(Icons.Rounded.DoneAll, CmColors.busy, R.string.chat_working)
        ChatRules.Status.DONE -> Triple(Icons.Rounded.DoneAll, CmColors.actionIcon, R.string.chat_done)
    }
    val text = stringResource(label)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

/** Le letture che le schermate fanno da sole: non sono comandi dell'utente e non diventano righe «non consegnato». */
private val PASSIVE_OPS = setOf(it.pixelbox.cmwatch.contract.CmdOp.SCREEN, it.pixelbox.cmwatch.contract.CmdOp.LAST, it.pixelbox.cmwatch.contract.CmdOp.TRANSCRIPT)

@Composable
private fun SmallAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) =
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) { Icon(icon, label, tint = CmColors.text2, modifier = Modifier.size(18.dp)) }

/**
 * L'anteprima dell'immagine allegata, piccola nel fumetto e letta ridotta (con l'immagine intera il fumetto restava vuoto);
 * tocco = visore a tutto schermo con lo zoom. Niente se il file non c'è più.
 */
@Composable
private fun AttachmentThumb(path: String) {
    var full by remember { mutableStateOf(false) }
    val bmp = remember(path) { decodeScaled(path, 720) } ?: return
    androidx.compose.foundation.Image(
        bmp, stringResource(R.string.attach_image), contentScale = ContentScale.Crop,
        modifier = Modifier.widthIn(max = 240.dp).heightIn(max = 180.dp).clip(RoundedCornerShape(14.dp)).clickable { full = true },
    )
    if (full) ImageViewer(path) { full = false }
}

/** Decodifica un file immagine riducendolo fino a stare entro `max` pixel sul lato lungo. */
private fun decodeScaled(path: String, max: Int): androidx.compose.ui.graphics.ImageBitmap? = runCatching {
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > max) sample *= 2
    android.graphics.BitmapFactory.decodeFile(path, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}.getOrNull()

/** L'immagine a tutto schermo su fondo nero, con zoom e trascinamento; × per chiudere. */
@Composable
private fun ImageViewer(path: String, onClose: () -> Unit) {
    val bmp = remember(path) { decodeScaled(path, 2048) } ?: return
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val zoom = androidx.compose.foundation.gestures.rememberTransformableState { z, pan, _ ->
        scale = (scale * z).coerceIn(1f, 6f); offset += pan
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(CmColors.bg)) {
            androidx.compose.foundation.Image(
                bmp, stringResource(R.string.attach_image), contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y }
                    .transformable(zoom),
            )
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).systemBarsPadding().padding(8.dp)) {
                Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = CmColors.text)
            }
        }
    }
}

/**
 * Testata fissa e compatta (Franz, 30/09 22:01: «grafica premium»): stato ed età, contesto ad anello, ⋮ con Segui, Apri
 * in Claude e Terminale; sotto modello ed effort toccabili (contratto 1.12) e, se ci sono, obiettivo e bassa priorità. Il
 * nome e la forma dell'account stanno nel menu delle sessioni in alto.
 */
@Composable
private fun SheetHeader(s: Session, now: Long, choices: Choices?, canTune: Boolean, actions: SheetActions, showTerminal: Boolean) {
    var picker by remember { mutableStateOf<String?>(null) }   // "model" | "effort"
    var menu by remember { mutableStateOf(false) }
    val tunable = canTune && choices != null && s.state != SessionState.GONE
    Column(Modifier.fillMaxWidth().background(CmColors.bg)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Le pillole vanno a capo invece di spingere fuori anello e menu (provini 30/09 22:37).
            FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatePill(s.state, Durations.since(s.since, now))
                TunePill(ModelText.short(s.model) ?: stringResource(R.string.model_title), tunable) { picker = "model" }
                EffortPill(s.effort, tunable) { picker = "effort" }
            }
            s.context?.let { ContextRing(it) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.more), tint = CmColors.text2) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = CmColors.surface) {
                    DropdownMenuItem(
                        text = { Text(stringResource(if (s.followed) R.string.unfollow else R.string.follow)) },
                        leadingIcon = { Icon(if (s.followed) Icons.Rounded.NotificationsOff else Icons.Rounded.NotificationsActive, null, tint = CmColors.actionIcon) },
                        onClick = { menu = false; actions.follow(!s.followed) },
                    )
                    if (s.link.isNotBlank()) DropdownMenuItem(
                        text = { Text(stringResource(R.string.open_in_claude)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = CmColors.actionIcon) },
                        onClick = { menu = false; actions.openInClaude() },
                    )
                    // Con la conversazione vera il terminale non serve più dal telefono (Franz, 30/09 20:39).
                    if (showTerminal) DropdownMenuItem(
                        text = { Text(stringResource(R.string.terminal)) },
                        leadingIcon = { Icon(Icons.Rounded.Terminal, null, tint = CmColors.actionIcon) },
                        onClick = { menu = false; actions.terminal() },
                    )
                }
            }
        }
        val notes = listOfNotNull(
            SessionsText.goalLine(s, stringResource(R.string.goal)),
            SessionsText.priority(s, stringResource(R.string.low_priority), stringResource(R.string.low_priority_offered)),
        )
        notes.forEach { Text(it, style = MaterialTheme.typography.labelMedium, color = CmColors.briefLabel, modifier = Modifier.padding(horizontal = 16.dp)) }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = CmColors.line)
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

/** Testo con il tasto ▶ accanto (regola di Franz 12/09: testi lunghi, esiti, risposte e domande). */
@Composable
fun Speakable(text: String, speak: Boolean, onSpeak: (String) -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = CmColors.text, modifier = Modifier.weight(1f))
        if (speak) IconButton(onClick = { onSpeak(text) }) { Icon(Icons.Rounded.PlayArrow, stringResource(R.string.read_aloud), tint = CmColors.actionIcon) }
    }
}

/** Una pillola compatta e toccabile per il modello (contratto 1.12); la freccia solo se si può cambiare. */
@Composable
private fun TunePill(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, color = CmColors.surface, shape = CircleShape) {
        Row(Modifier.padding(start = 12.dp, end = if (enabled) 6.dp else 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = CmColors.text)
            if (enabled) Icon(Icons.Rounded.ArrowDropDown, null, tint = CmColors.text2, modifier = Modifier.size(18.dp))
        }
    }
}

/** L'effort come parola e tacche: xhigh e max accendono tutte le tacche (dal vivo 30/09 21:57). */
@Composable
private fun EffortPill(effort: String?, enabled: Boolean, onClick: () -> Unit) {
    val step = SessionMeters.effortStep(effort)
    Surface(onClick = onClick, enabled = enabled, color = CmColors.surface, shape = CircleShape) {
        Row(
            Modifier.padding(start = 12.dp, end = if (enabled) 6.dp else 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(effort?.trim()?.lowercase() ?: stringResource(R.string.effort), style = MaterialTheme.typography.labelLarge, color = CmColors.text2)
            Spacer(Modifier.width(2.dp))
            repeat(SessionMeters.EFFORT_STEPS) { i ->
                Box(Modifier.size(width = 8.dp, height = 5.dp).background(if (step != null && i < step) CmColors.briefRing else CmColors.briefTrack, CircleShape))
            }
            if (enabled) Icon(Icons.Rounded.ArrowDropDown, null, tint = CmColors.text2, modifier = Modifier.size(18.dp))
        }
    }
}

/** Il contesto come anellino con la percentuale accanto, nel colore delle soglie (`SessionMeters`). */
@Composable
private fun ContextRing(pct: Int) {
    val tone = when (SessionMeters.contextTone(pct)) {
        it.pixelbox.cmwatch.rules.BriefCards.Tone.ALERT -> CmColors.briefAlertRing
        it.pixelbox.cmwatch.rules.BriefCards.Tone.WARN -> CmColors.briefWarn
        else -> CmColors.briefRing
    }
    val frac = SessionMeters.contextFraction(pct) ?: 0f
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        androidx.compose.foundation.Canvas(Modifier.size(20.dp)) {
            val w = 3.dp.toPx()
            val inset = w / 2
            val sz = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
            drawArc(CmColors.briefTrack, -90f, 360f, false, topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = sz, style = androidx.compose.ui.graphics.drawscope.Stroke(w))
            drawArc(tone, -90f, 360f * frac, false, topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = sz, style = androidx.compose.ui.graphics.drawscope.Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        Text("$pct%", style = MaterialTheme.typography.labelLarge, color = CmColors.text2)
    }
}
