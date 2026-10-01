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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.core.animateFloat
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
import it.pixelbox.cmwatch.rules.QuotaWarning
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
    /** Avviso quota (piano 30/09, Task 3): il testo scritto parte alla ripartenza della finestra, o va nella notte. */
    /** Vero se il testo è partito (o programmato): solo allora la bozza si svuota (revisione finale 01/10, I5). */
    val sendAtReset: (String) -> Boolean = { false }, val sendTonight: (String) -> Boolean = { false },
    /** Tocco sulla riga dell'avviso: la Panoramica, casa dei dati della quota. */
    val overview: () -> Unit = {},
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
    /** La conversazione vera sta arrivando: niente chat di ripiego nel frattempo, che poi salterebbe (dal vivo 30/09 23:36). */
    loadingFeed: Boolean = false,
    /** La finestra di 5 ore dell'account della sessione sta finendo (`QuotaWarning`); null = nessun avviso. */
    quota: QuotaWarning.Warn? = null,
    /** Le frasi rapide del progetto (`QuickPhrases`), come chip sopra la barra. */
    phrases: List<String> = emptyList(),
    /** «Manda stanotte» possibile: il relay ha la notte e la cartella del progetto è nota. */
    canTonight: Boolean = false,
) {
    // Legata anche alla domanda: una domanda nuova non eredita la bozza scritta per quella di prima (revisione 29/09).
    var draft by rememberSaveable(s.id, s.question?.id) { mutableStateOf("") }
    var holdHint by rememberSaveable(s.question?.id) { mutableStateOf(false) }
    val primary = PhonePrimary.button(s, draft)
    val list = rememberLazyListState()
    // La chat segue l'ultimo testo finché non la si sposta a mano per rileggere (Franz, 01/10 06:52). «Segui» si decide
    // solo quando lo scorrimento si ferma: letto dopo l'arrivo di un testo nuovo, il fondo era già più giù e la chat
    // restava ferma. Un proprio invio torna a seguire; una pagina di messaggi precedenti non rimbalza in fondo.
    var follow by remember(s.id) { mutableStateOf(true) }
    LaunchedEffect(list) {
        androidx.compose.runtime.snapshotFlow { list.isScrollInProgress }.collect { moving -> if (!moving) follow = !list.canScrollForward }
    }
    val reasons = chat.associate { it.sent.id to it.reason }
    // Alla prima apertura, e quando arriva la prima pagina della conversazione vera, la chat parte dal fondo
    // (Franz, 30/09 22:55: «ancorato alla fine»).
    var anchored by remember(s.id, feed != null) { mutableStateOf(false) }
    LaunchedEffect(chat.size, chat.lastOrNull()?.status, feed?.size, feed?.lastOrNull()) {
        if (chat.isEmpty() && feed.isNullOrEmpty()) return@LaunchedEffect
        val count = androidx.compose.runtime.snapshotFlow { list.layoutInfo.totalItemsCount }.first { it > 0 }
        if (!anchored) { list.scrollToItem(count - 1, Int.MAX_VALUE); anchored = true; follow = true }
    }
    // Ogni testo nuovo o più lungo in fondo (risposta, riga dal vivo, domanda): giù fino alla fine dell'ultima voce.
    LaunchedEffect(list, anchored) {
        if (!anchored) return@LaunchedEffect
        androidx.compose.runtime.snapshotFlow { list.layoutInfo.totalItemsCount to list.canScrollForward }.collect { (n, below) ->
            if (follow && below && n > 0 && !list.isScrollInProgress) list.scrollToItem(n - 1, Int.MAX_VALUE)
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
                    TextButton(onClick = { follow = false; onOlder() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.load_older), color = CmColors.actionIcon) }
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
            } else if (loadingFeed) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        CircularWavyProgressIndicator(color = CmColors.actionIcon)
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
            // Che cosa sta facendo, come la riga dell'app nativa: asterisco, tempo del turno, strumento o pensiero.
            if (s.state == SessionState.BUSY || s.state == SessionState.AWAITING) item(key = "live") { ActivityLine(s, now) }
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
        Composer(s, draft, onDraft = { draft = it }, ops, canAttach, actions, onSent = { draft = ""; follow = true }, quota, phrases, canTonight)
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
    quota: QuotaWarning.Warn? = null, phrases: List<String> = emptyList(), canTonight: Boolean = false,
) {
    var images by rememberSaveable(s.id) { mutableStateOf(listOf<Uri>()) }
    val image = images.firstOrNull()
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
        quota?.let { w ->
            val can = draft.isNotBlank() && s.question == null && images.isEmpty()
            QuotaLine(w, draft.trim(), canDefer = can, canTonight = can && canTonight, actions, onDeferred = onSent)
        }
        // Contratto 1.23: il prompt suggerito del terminale, solo per una sessione ferma. Il tocco lo mette nel campo, da
        // ritoccare prima di mandarlo (Franz, 01/10 14:57: prima partiva subito e la pillola restava lì).
        PhonePrimary.suggestion(s, draft)?.takeIf { image == null }?.let { sug -> SuggestionPill(sug, onEdit = { onDraft(sug) }) }
        // Frasi rapide (piano 30/09, Task 6): stesse mosse del suggerito, solo senza una domanda aperta.
        if (phrases.isNotEmpty() && draft.isBlank() && image == null && s.question == null) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                phrases.forEach { p -> PhraseChip(p, onSend = { actions.send(PhonePrimary.Target.PROMPT, p); onSent() }, onEdit = { onDraft(p) }) }
            }
        }
        // Più immagini insieme (Franz, 30/09 23:04: l'ultima sostituiva la precedente), ognuna con la sua ×.
        if (images.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            images.forEach { uri ->
                Box {
                    UriThumb(uri, Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)))
                    SmallAction(Icons.Rounded.Close, stringResource(R.string.remove_image)) { images = images - uri }
                }
            }
        }
        val send = {
            if (images.isNotEmpty()) images.forEachIndexed { i, uri -> actions.attach(uri, if (i == 0) draft.trim() else "") }
            else PhonePrimary.target(s, draft)?.let { actions.send(it, draft.trim()) }
            images = emptyList()
            onSent()
        }
        // Allegato e invio dentro il campo, centrati sulla sua altezza (Franz, 30/09 22:13).
        OutlinedTextField(
            value = draft, onValueChange = onDraft, maxLines = 5, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(if (s.question != null) R.string.answer_free else R.string.write_prompt)) },
            shape = MaterialTheme.shapes.extraLarge,
            leadingIcon = if (canAttach) ({ AttachButton { picked -> images = (images + picked).distinct().take(MAX_IMAGES) } }) else null,
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

/**
 * L'avviso della quota sopra la barra: la finestra e quando riparte, e due bottoni testuali che rimandano il testo
 * scritto (alla ripartenza o stanotte). Senza testo nel campo i bottoni restano spenti; tocco sulla riga = Panoramica.
 */
@Composable
private fun QuotaLine(w: QuotaWarning.Warn, draft: String, canDefer: Boolean, canTonight: Boolean, actions: SheetActions, onDeferred: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(CmColors.briefWarn).clickable(onClick = actions.overview).padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 2.dp)) {
        Text(
            stringResource(if (w.projected) R.string.quota_warn_pace else R.string.quota_warn, w.pct, hhmm(w.resetAt)),
            style = MaterialTheme.typography.bodyMedium, color = CmColors.briefWarnInk,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { if (actions.sendAtReset(draft)) onDeferred() }, enabled = canDefer) { Text(stringResource(R.string.send_at_reset), color = CmColors.briefWarnInk.copy(alpha = if (canDefer) 1f else 0.45f)) }
            TextButton(onClick = { if (actions.sendTonight(draft)) onDeferred() }, enabled = canTonight) { Text(stringResource(R.string.send_tonight), color = CmColors.briefWarnInk.copy(alpha = if (canTonight) 1f else 0.45f)) }
        }
    }
}

/** Una frase rapida: chip tonale su una riga; tocco = manda, pressione lunga = nel campo. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhraseChip(text: String, onSend: () -> Unit, onEdit: () -> Unit) {
    Surface(
        color = CmColors.surfaceHigh, contentColor = CmColors.text, shape = MaterialTheme.shapes.large,
        modifier = Modifier.clip(MaterialTheme.shapes.large).combinedClickable(onClick = onSend, onLongClick = onEdit),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
    }
}

/** Il prompt suggerito come pillola tonale, a una riga logica; il tocco lo mette nel campo per modificarlo. */
@Composable
private fun SuggestionPill(text: String, onEdit: () -> Unit) {
    Surface(
        color = CmColors.surfaceHigh, contentColor = CmColors.text, shape = MaterialTheme.shapes.large,
        modifier = Modifier.clip(MaterialTheme.shapes.large).clickable(onClick = onEdit),
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
private fun AttachButton(onPicked: (List<Uri>) -> Unit) {
    val icon: @Composable () -> Unit = { Icon(Icons.Rounded.Add, stringResource(R.string.attach_image), tint = CmColors.actionIcon) }
    if (androidx.activity.compose.LocalActivityResultRegistryOwner.current == null) { IconButton(onClick = {}, content = icon); return }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGES)) { uris -> if (uris.isNotEmpty()) onPicked(uris) }
    IconButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, content = icon)
}

/** La domanda come sull'orologio: prima opzione piena, pressione lunga per il rischio alto, «Parliamone», «Consenti tutto». */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun QuestionCard(
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
            modifier = Modifier,
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                m.attachment?.let { AttachmentThumb(it) }
                if (m.text.isNotBlank()) Text(m.text, style = MaterialTheme.typography.bodyLarge, color = CmColors.text)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            StatusMark(status, m.scheduledFor)
            // Un programmato dice solo quando parte: l'ora in cui è stato scritto accanto confondeva (provini 01/10).
            if (status != ChatRules.Status.SCHEDULED) Text(hhmm(m.sentAt), style = MaterialTheme.typography.labelMedium, color = CmColors.text2, modifier = Modifier.padding(end = 4.dp))
            SmallAction(Icons.Rounded.ContentCopy, stringResource(R.string.copy)) { clip.setText(AnnotatedString(m.text)) }
            SmallAction(Icons.Rounded.Edit, stringResource(R.string.edit)) { onEdit(m.text) }
        }
        // Il ↻ sembrava «Aggiorna» e ogni tocco mandava un doppione (dal vivo 30/09 23:00): ora solo «Riprova», a parole,
        // e solo dove serve.
        if (status == ChatRules.Status.FAILED || status == ChatRules.Status.UNCERTAIN) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (status == ChatRules.Status.FAILED) (reason ?: stringResource(R.string.chat_no_answer)) else stringResource(R.string.chat_uncertain_hint),
                style = MaterialTheme.typography.labelMedium, color = if (status == ChatRules.Status.FAILED) CmColors.goneDim else CmColors.waiting,
                modifier = Modifier.weight(1f, fill = false),
            )
            TextButton(onClick = { actions.retry(m.id) }) { Text(stringResource(R.string.retry), color = CmColors.actionIcon) }
        }
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
            val reading = LocalSpeaking.current == text
            SmallAction(if (reading) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp, stringResource(if (reading) R.string.stop_reading else R.string.read_aloud)) { onSpeak(text) }
            at?.let { Text(hhmm(it), style = MaterialTheme.typography.labelMedium, color = CmColors.text2, modifier = Modifier.padding(start = 4.dp)) }
        }
    }
}

/**
 * Lo stato preciso di un messaggio, sempre con la parola (Franz, 30/09 22:13): orologio mentre parte, una spunta quando è
 * sul canale, due quando la sessione l'ha ricevuto, azzurre quando Claude l'ha preso in carico e quando ha finito.
 */
@Composable
private fun StatusMark(st: ChatRules.Status, scheduledFor: Long? = null) {
    val (icon, tint, label) = when (st) {
        ChatRules.Status.SCHEDULED -> Triple(Icons.Rounded.Alarm, CmColors.waiting, R.string.chat_scheduled)
        ChatRules.Status.OFFLINE -> Triple(Icons.Rounded.CloudOff, CmColors.stale, R.string.chat_offline)
        ChatRules.Status.UPLOADING -> Triple(Icons.Rounded.CloudUpload, CmColors.text2, R.string.chat_uploading)
        ChatRules.Status.SENDING -> Triple(Icons.Rounded.Schedule, CmColors.text2, R.string.chat_sending)
        ChatRules.Status.SENT -> Triple(Icons.Rounded.Check, CmColors.text2, R.string.chat_sent)
        ChatRules.Status.UNCERTAIN -> Triple(Icons.Rounded.HourglassTop, CmColors.waiting, R.string.chat_uncertain)
        ChatRules.Status.FAILED -> Triple(Icons.Rounded.ErrorOutline, CmColors.gone, R.string.chat_failed)
        ChatRules.Status.DELIVERED -> Triple(Icons.Rounded.DoneAll, CmColors.text2, R.string.chat_delivered)
        ChatRules.Status.QUEUED -> Triple(Icons.Rounded.DoneAll, CmColors.stale, R.string.chat_queued)
        ChatRules.Status.WORKING -> Triple(Icons.Rounded.DoneAll, CmColors.busy, R.string.chat_working)
        ChatRules.Status.DONE -> Triple(Icons.Rounded.DoneAll, CmColors.actionIcon, R.string.chat_done)
    }
    val text = if (st == ChatRules.Status.SCHEDULED && scheduledFor != null) stringResource(label, hhmm(scheduledFor)) else stringResource(label)
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
            // Una riga sola: modello, effort, contesto e menu. Lo stato e il tempo vanno nella riga dal vivo in fondo alla chat
            // (Franz, 30/09 23:01: «disordinata», «lavora 5 h potrebbe essere rimosso»).
            TunePill(ModelText.short(s.model) ?: stringResource(R.string.model_title), tunable) { picker = "model" }
            EffortPill(s.effort, tunable) { picker = "effort" }
            Spacer(Modifier.weight(1f))
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
        val reading = LocalSpeaking.current == text
        if (speak) IconButton(onClick = { onSpeak(text) }) {
            Icon(if (reading) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, stringResource(if (reading) R.string.stop_reading else R.string.read_aloud), tint = CmColors.actionIcon)
        }
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

/** Quante immagini si possono mandare insieme: ognuna è un `report`, e il relay le esegue una per volta. */
private const val MAX_IMAGES = 5

/** Il testo che la voce sta leggendo: il suo tasto diventa Stop. Fornito da `MainActivity` da `Speech.speaking`. */
val LocalSpeaking = androidx.compose.runtime.staticCompositionLocalOf<String?> { null }

/**
 * La riga dal vivo in fondo alla chat, come nell'app nativa (Franz, 30/09 23:06): asterisco che gira nel corallo di
 * Claude, tempo del turno al secondo, e che cosa sta facendo (la descrizione dello strumento, o lo strumento, o «sta
 * pensando»). Ferma con le animazioni spente.
 */
@Composable
private fun ActivityLine(s: Session, now: Long) {
    val off = animationsOff()
    // Il contatore e il lampeggio non sono movimento: vanno anche con le animazioni di sistema a zero, come il cursore
    // di testo (Franz, 01/10 15:57: sul suo telefono le scale sono a 0 e la riga restava ferma). Fermi solo negli
    // snapshot, che partono dall'ora della scheda.
    val still = LocalStill.current
    val nowS by androidx.compose.runtime.produceState(now, s.turnStarted, still) {
        if (!still) while (true) { value = System.currentTimeMillis() / 1000; kotlinx.coroutines.delay(1_000) }
    }
    val blink by androidx.compose.runtime.produceState(true, still) {
        if (!still) while (true) { kotlinx.coroutines.delay(600); value = !value }
    }
    // Gira e pulsa mentre la sessione elabora (Franz, 01/10 06:44: «il simbolo accanto alla riga di stato pulsante»).
    val motion = if (off) null else androidx.compose.animation.core.rememberInfiniteTransition(label = "spin")
    val spin = motion?.let {
        val a by it.animateFloat(0f, 360f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2400, easing = androidx.compose.animation.core.LinearEasing)), label = "a")
        a
    } ?: 0f
    val pulse = motion?.let {
        val p by it.animateFloat(0.7f, 1.15f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(700), androidx.compose.animation.core.RepeatMode.Reverse), label = "p")
        p
    } ?: 1f
    val secs = s.turnStarted?.let { (nowS - it).coerceAtLeast(0) }
    val what = s.toolNote?.takeIf { it.isNotBlank() } ?: s.tool?.takeIf { it.isNotBlank() } ?: stringResource(R.string.live_thinking)
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.foundation.Canvas(Modifier.size(22.dp).graphicsLayer { rotationZ = spin; scaleX = pulse; scaleY = pulse; alpha = if (motion == null) (if (blink) 1f else 0.3f) else 0.55f + 0.45f * ((pulse - 0.7f) / 0.45f) }) {
            val c = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            val r = size.minDimension / 2
            repeat(8) { i ->
                val ang = Math.toRadians(i * 45.0)
                val dx = kotlin.math.cos(ang).toFloat(); val dy = kotlin.math.sin(ang).toFloat()
                drawLine(CmColors.modelOpus, androidx.compose.ui.geometry.Offset(c.x + dx * r * 0.25f, c.y + dy * r * 0.25f), androidx.compose.ui.geometry.Offset(c.x + dx * r, c.y + dy * r), strokeWidth = r * 0.22f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
        Text(
            listOfNotNull(secs?.let { elapsed(it) }, what).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium, color = CmColors.text2,
        )
    }
}

@Composable
private fun elapsed(s: Long): String = when {
    s < 60 -> stringResource(R.string.live_s, s)
    s < 3600 -> stringResource(R.string.live_ms, s / 60, s % 60)
    else -> stringResource(R.string.live_hm, s / 3600, (s % 3600) / 60)
}
