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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Choices
import it.pixelbox.cmwatch.contract.Recurring
import it.pixelbox.cmwatch.rules.NextSteps
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
import it.pixelbox.cmwatch.rules.ChatNews
import it.pixelbox.cmwatch.rules.ChatRules
import it.pixelbox.cmwatch.rules.ModelText
import it.pixelbox.cmwatch.rules.PhonePrimary
import it.pixelbox.cmwatch.rules.QuestionRules
import it.pixelbox.cmwatch.rules.QuotaWarning
import it.pixelbox.cmwatch.rules.Sent
import it.pixelbox.cmwatch.rules.SessionMeters
import it.pixelbox.cmwatch.rules.SessionsText
import it.pixelbox.cmwatch.rules.ToolText
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.Instant
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Contratto 1.24: aprire i file della chat. `local` = dove sta sul telefono un file già scaricato (per percorso sul PC),
 * `loading` = quelli in arrivo. Senza un fornitore i chip restano come prima.
 */
/** Le quattro azioni sotto un file della chat (Franz, 07/10 16:07). */
enum class FileAct { OPEN, DOWNLOAD, COPY, SHARE }

data class FileOpener(
    val open: (TranscriptFile) -> Unit = {},
    val loading: Set<String> = emptySet(),
    val local: Map<String, String> = emptyMap(),
    val act: (TranscriptFile, FileAct) -> Unit = { _, _ -> },
    /** Chiede il file al PC senza aprirlo e senza avvisi se non arriva: l'anteprima che nasce da sola. */
    val preview: (TranscriptFile) -> Unit = {},
    /** Una rete senza contatore, come il Wi-Fi: i video arrivano da soli solo lì (`MediaPreview`). */
    val unmetered: () -> Boolean = { false },
)
val LocalFileOpener = compositionLocalOf { FileOpener() }

/** L'ultima visita di una sessione (secondi) data da fuori, per la riga «Nuovi» negli snapshot, dove le preferenze non ci sono. */
val LocalChatSeen = staticCompositionLocalOf<((String) -> Long?)?> { null }

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
    /** Contratto 1.25: un comando slash consentito, senza «/», con i suoi argomenti o null. */
    val slash: (cmd: String, args: String?) -> Unit = { _, _ -> },
    /** «Fai controllare alla master» dal menu ⋮; null sulla master stessa o senza master aperta. */
    val askMaster: (() -> Unit)? = null,
    /** La lettura a voce da un paragrafo toccato (Franz, 02/10 00:01: «come in orologio»). */
    val speakFrom: (text: String, block: Int) -> Unit = { _, _ -> },
    /** Contratto 1.37: «Handoff, poi /clear» (il /clear parte a turno finito, lo gestisce chi tiene lo stato). */
    val handoff: () -> Unit = {},
    /** Contratto 1.37: una decisione per la memoria della master; `project` null = vale per tutti; null = op non supportata. */
    val decision: ((text: String, project: String?) -> Unit)? = null,
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
    /** Contratto 1.25: i comandi slash consentiti (`state.slash`); null = il relay non li supporta. */
    slash: List<String>? = null,
    /** Contratto 1.22: la conversazione vera; null = relay senza `transcript`, resta la chat dei messaggi mandati. */
    feed: List<ChatFeed.Item>? = null, more: Boolean = false, onOlder: () -> Unit = {},
    /** La conversazione vera sta arrivando: niente chat di ripiego nel frattempo, che poi salterebbe (dal vivo 30/09 23:36). */
    loadingFeed: Boolean = false,
    /** Da quando (ms) una lettura della conversazione aspetta il PC; null se nessuna è in volo (piano prestazioni, Task 7). */
    waitingSince: Long? = null,
    /** L'orologio della riga d'attesa, in ms. */
    clockMs: () -> Long = System::currentTimeMillis,
    /** La finestra di 5 ore dell'account della sessione sta finendo (`QuotaWarning`); null = nessun avviso. */
    quota: QuotaWarning.Warn? = null,
    /** Le frasi rapide del progetto (`QuickPhrases`), come chip sopra la barra. */
    phrases: List<String> = emptyList(),
    /** «Manda stanotte» possibile: il relay ha la notte e la cartella del progetto è nota. */
    canTonight: Boolean = false,
    /** Modello ed effort da mostrare: la scelta fatta dal telefono finché il PC non la riporta (`Tune`). */
    model: it.pixelbox.cmwatch.contract.Model? = s.model, effort: String? = s.effort,
    /** La casa della master: «Per te» e il Quadro sopra la chat; si chiudono quando si scorre indietro per rileggere. */
    top: (@Composable () -> Unit)? = null,
    /** La griglia di puntini dello stile tech dietro la chat (casa della master). */
    grid: Boolean = false,
    /**
     * Casa A (02/10): al posto della chat la pagina della master, con la stessa barra di scrittura. Riceve come mettere un
     * testo nel campo e come mandarlo subito.
     */
    home: (@Composable ColumnScope.(onDraft: (String) -> Unit, onSend: (String) -> Unit) -> Unit)? = null,
    /** L'avviso delle altre sessioni sotto la barra (`Elsewhere`, variante A, Franz 02/10 20:47); null = niente da dire. */
    elsewhere: it.pixelbox.cmwatch.rules.Elsewhere.Alert? = null, onElsewhere: () -> Unit = {}, onElsewhereDismiss: () -> Unit = {},
    /** Riepilogo unico (design 03/10): false = niente intestazione con le pillole di modello ed effort. */
    header: Boolean = true,
    /** Riepilogo unico: il riquadro della master, fermo fra il contenuto e la barra di scrittura. */
    dock: (@Composable () -> Unit)? = null,
    /** La master espansa (Franz, 03/10 16:44): la sua barra in cima, sopra l'intestazione, per ridurla con un tocco. */
    bar: (@Composable () -> Unit)? = null,
    /** Con `home`: true la lista della home con `dock` in basso, false la conversazione con `bar` e l'intestazione. */
    homeOpen: Boolean = true,
    /**
     * Le tre misure della master sul tablet (Franz, 08/10 18:15): `halfStops` le accende, `half` con `homeOpen` false è la
     * misura a metà, con la home sopra e la conversazione sotto. Sul telefono restano due: barra e tutto schermo.
     */
    halfStops: Boolean = false, half: Boolean = false,
    /** La testata della pagina (`PageHeader`), in cima e dentro la parte che vola: scorre e vola con la sessione. */
    appBar: (@Composable () -> Unit)? = null,
    /** Contratto 1.28: nel «+» anche «File», di qualunque formato. */
    canAttachFiles: Boolean = false,
    /** La quota dell'account della sessione: in testata la finestra delle 5 ore (Franz, 03/10 20:31). */
    accountQuota: it.pixelbox.cmwatch.contract.QuotaAccount? = null,
    /** Il tablet (piano 04/10): nome e stato in testa alla riga di modello ed effort, come nel mockup della plancia. */
    headerLead: (@Composable RowScope.() -> Unit)? = null,
    /** La bozza tenuta sopra l'interruttore dei 840 dp: resta quando la finestra passa da telefono a tablet e indietro. */
    draftState: androidx.compose.runtime.MutableState<String>? = null,
    /** Obiettivo e bassa priorità in testata; il tablet con l'ispettore li mostra lì e in testata non si ripetono. */
    notesInHeader: Boolean = true,
) {
    // L'orologio dell'attesa del PC gira solo con una lettura in volo, e si legge solo dentro la lista: il tic di ogni
    // secondo non ricompone la scheda (la digitazione lenta del 05/10 veniva da letture in cima).
    val waitClock = produceState(clockMs(), waitingSince) { while (waitingSince != null) { value = clockMs(); kotlinx.coroutines.delay(1_000) } }
    // Legata anche alla domanda: una domanda nuova non eredita la bozza scritta per quella di prima (revisione 29/09).
    val ownDraft = rememberSaveable(s.id, s.question?.id) { mutableStateOf("") }
    // La bozza si legge solo dove serve (Franz, 05/10 10:45: «la digitazione risulta ancora molto lenta»): letta qui in
    // cima, ogni lettera ricomponeva tutta la scheda e la conversazione. Il campo la legge da sé; qui solo valori derivati,
    // che cambiano quando cambia il risultato.
    val draftHolder = draftState ?: ownDraft
    var draft by draftHolder
    var holdHint by rememberSaveable(s.question?.id) { mutableStateOf(false) }
    // Il box «Prossimi» e il campo (Franz, 04/10 20:21): il primo consiglio dell'ultima risposta fa da suggerimento nel
    // campo vuoto, gli altri restano nel box anche scrivendo; il suggerito del terminale entra solo se è diverso.
    val idle = s.state == SessionState.IDLE && s.question == null
    // La conversazione raggruppata una volta per feed, non a ogni ricomposizione della lista.
    val groupedFeed = remember(feed) { feed?.let { f -> ChatFeed.group(f) } }
    val steps = remember(groupedFeed) {
        groupedFeed?.let { g -> g.lastOrNull { x -> x !is ChatFeed.Item.Tool && x !is ChatFeed.Item.Steps } as? ChatFeed.Item.Claude }
            ?.let { c -> NextSteps.parse(c.entry.text.orEmpty()).steps }.orEmpty()
    }
    // Contratto 1.38: quali Prossimi sbloccano, dal «!» dell'ultima risposta o da `next_steps` della sessione.
    val stepsBlocking = remember(groupedFeed, s.nextSteps) {
        val c = groupedFeed?.lastOrNull { x -> x !is ChatFeed.Item.Tool && x !is ChatFeed.Item.Steps } as? ChatFeed.Item.Claude
        NextSteps.parse(c?.entry?.text.orEmpty()).blocking + s.nextSteps.orEmpty().filter { it.blocking }.map { it.text }
    }
    val stepsBox by remember(steps, s.suggestion, idle, draftHolder) {
        derivedStateOf { if (idle) NextSteps.box(steps, s.suggestion, draftHolder.value) else NextSteps.Box(null, emptyList()) }
    }
    // Le azioni tolte a mano e quelle vecchie (Franz, 08/10 20:50): le tolte non tornano per questa sessione, quelle ripetute
    // in tre risposte di fila vanno in fondo, chiuse in una riga.
    val stepsCtx = androidx.compose.ui.platform.LocalContext.current
    val dismissPrefs = remember { stepsCtx.getSharedPreferences("dismissed_steps", android.content.Context.MODE_PRIVATE) }
    var dismissed by remember(s.name) { mutableStateOf(dismissPrefs.getStringSet(s.name, emptySet()).orEmpty()) }
    val earlierSteps = remember(groupedFeed) {
        groupedFeed.orEmpty().filterIsInstance<ChatFeed.Item.Claude>().dropLast(1).reversed()
            .map { c -> NextSteps.parse(c.entry.text.orEmpty()).steps }.filter { it.isNotEmpty() }
    }
    val stepsSplit = remember(stepsBox.rows, earlierSteps, dismissed) { NextSteps.split(stepsBox.rows, earlierSteps, dismissed) }
    val dismissStep: (String) -> Unit = { t -> dismissed = dismissed + NextSteps.key(t); dismissPrefs.edit().putStringSet(s.name, dismissed).apply() }
    val draftBlank by remember(draftHolder) { derivedStateOf { draftHolder.value.isBlank() } }
    val boxes = LocalPromptBoxes.current
    val then = stringResource(R.string.next_then)
    val fieldFocus = LocalFieldFocus.current
    val primary by remember(s, draftHolder) { derivedStateOf { PhonePrimary.button(s, draftHolder.value) } }
    val list = rememberLazyListState()
    // L'apertura della sessione (Franz, 10/10 16:01, A + B): le ultime voci entrano a cascata, e quelle arrivate dall'ultima
    // visita hanno sopra la riga «Nuovi» e un fondo che sfuma. L'ultima visita si ricorda lasciando la pagina; la cascata
    // scaglionata vale nel primo secondo, dopo una voce che arriva entra da sola.
    val seenPrefs = remember { stepsCtx.getSharedPreferences("chat_seen", android.content.Context.MODE_PRIVATE) }
    // Negli snapshot (immagini ferme) la visita non si legge e non si ricorda: la dà la prova, se vuole.
    val still = LocalStill.current
    val seenFrom = LocalChatSeen.current
    val seenAt = remember(s.name) { seenFrom?.invoke(s.name) ?: if (still) null else seenPrefs.getLong(s.name, -1L).takeIf { it >= 0L } }
    val latestAt by rememberUpdatedState(ChatNews.latest(groupedFeed.orEmpty()))
    DisposableEffect(s.name) { onDispose { if (!still) latestAt?.let { seenPrefs.edit().putLong(s.name, it).apply() } } }
    val openedAt = remember(s.name) { android.os.SystemClock.uptimeMillis() }
    val cascaded = remember(s.name) { mutableSetOf<String>() }
    val tinted = remember(s.name) { mutableSetOf<String>() }
    // Contratto 1.37: «Salva come decisione», dalla risposta di Claude o dal + della master (campo vuoto).
    var decisionDraft by remember { mutableStateOf<String?>(null) }
    val decide: ((String) -> Unit)? = actions.decision?.let { _ -> { t: String -> decisionDraft = it.pixelbox.cmwatch.rules.MasterService.decisionDraft(t) } }
    decisionDraft?.let { d ->
        DecisionSheet(d, project = if (home != null) null else it.pixelbox.cmwatch.rules.MasterService.decisionProject(s), onDismiss = { decisionDraft = null }) { text, project ->
            actions.decision?.invoke(text, project); decisionDraft = null
        }
    }
    val recurring = LocalRecurring.current
    val recurringRows by remember(recurring, home == null, idle, draftHolder) {
        derivedStateOf {
            if (home == null) emptyList()
            else recurring.filterNot { r -> NextSteps.inDraft(draftHolder.value, r.prompt) }.map { r -> PromptRow(r.label, r.prompt, direct = !r.param && idle) }
        }
    }
    // La nicchia del controller (Franz, 10/10 08:19: «una nicchia vuota sotto così non nasconde informazioni»): con un box
    // sopra il campo (Prossimi o Ricorrenti) il controller si posa su uno spazio vuoto suo fra il box e il campo, invece di
    // coprirne le righe; senza box resta sopra la fine della conversazione, che gli lascia posto in fondo.
    val niche = stepsSplit.fresh.isNotEmpty() || stepsSplit.old.isNotEmpty() || (boxes.recurringOpen && recurringRows.isNotEmpty())
    // La chat segue l'ultimo testo finché non la si sposta a mano per rileggere (Franz, 01/10 06:52). «Segui» si decide
    // solo quando lo scorrimento si ferma: letto dopo l'arrivo di un testo nuovo, il fondo era già più giù e la chat
    // restava ferma. Un proprio invio torna a seguire; una pagina di messaggi precedenti non rimbalza in fondo.
    var follow by remember(s.id) { mutableStateOf(true) }
    LaunchedEffect(list) {
        androidx.compose.runtime.snapshotFlow { list.isScrollInProgress }.collect { moving -> if (!moving) follow = !list.canScrollForward }
    }
    // Il dito che risale la chat la stacca dal fondo subito, non quando lo scorrimento si ferma (Franz, 10/10 08:18: «lo
    // scorrimento verso l'alto risulta problematico»): un testo arrivato fra un gesto e l'altro la riportava giù.
    val reading = remember(list) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput && available.y > 0f) follow = false
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }
    val listScope = androidx.compose.runtime.rememberCoroutineScope()
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
    Column(Modifier.fly("card-${s.id}").fillMaxSize().background(CmColors.bg).then(if (grid) Modifier.dotGrid() else Modifier)) {
        // A tutto schermo la master ha una testata sola, la sua (Franz, 08/10 18:15): quella della home rientra mentre il pannello sale.
        if (home == null) appBar?.invoke()
        else appBar?.let { ab ->
            androidx.compose.animation.AnimatedVisibility(
                visible = homeOpen || (halfStops && half),
                enter = androidx.compose.animation.expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + androidx.compose.animation.fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = androidx.compose.animation.shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + androidx.compose.animation.fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
            ) { ab() }
        }
        val ime = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
        // Tutto quello che sta sopra il campo in una sessione: barra, intestazione, avviso, conversazione, consigli.
        val masterVoice = home != null && LocalMasterLook.current.voice
        val chatArea: @Composable ColumnScope.() -> Unit = { androidx.compose.runtime.CompositionLocalProvider(LocalVoiceAccent provides masterVoice, LocalBubble provides (if (home != null) MasterHighest else null)) {
            // Fissa sopra la chat e compatta (Franz, 30/09 22:01: scorreva con la chat ed era troppo grande).
            bar?.invoke()
            // Nella chat della master, mentre rileggi più su, la testata rientra come le barre che si comprimono del Material 3
            // Expressive, e torna quando sei in fondo (Franz, 06/10 22:27).
            if (header) androidx.compose.animation.AnimatedVisibility(
                visible = home == null || follow,
                enter = androidx.compose.animation.expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + androidx.compose.animation.fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = androidx.compose.animation.shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + androidx.compose.animation.fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
            ) {
                SheetHeader(
                    s, now, choices, canTune, actions, showTerminal = feed == null, model = model, effort = effort, bg = if (home != null) MasterHigh else CmColors.bg,
                    canExit = slash?.contains("exit") == true && s.state != SessionState.GONE, quota = accountQuota, lead = headerLead, notesInHeader = notesInHeader,
                )
            }
            elsewhere?.let { ElsewherePill(it, onElsewhere, onElsewhereDismiss) }
            // Nascosto mentre si scrive (con la tastiera la chat e la barra non avrebbero spazio) e mentre si rilegge; mai più
            // alto di 300 dp, con lo scorrimento dentro (revisione finale 02/10).
            top?.let { block ->
                androidx.compose.animation.AnimatedVisibility(visible = follow && !ime) {
                    Box(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) { block() }
                }
            }
            // La conversazione arriva in dissolvenza (osservazioni del 03/10, transizione 5): mentre si carica la rotella, poi la
            // lista entra in 150 ms invece di comparire a pezzi. Con le animazioni spente, subito.
            val feedOff = animationsOff()
            // Chat e box dei Prossimi si dividono quello che resta dopo il campo, che si misura prima e resta intero: prima il
            // box, che se non ci sta scorre dentro, poi la chat (segnalazione del 09/10 20:38: sul tablet, con la tastiera
            // aperta, il box schiacciava il campo e non si vedeva cosa si scriveva).
            Column(Modifier.weight(1f).fillMaxWidth()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
            androidx.compose.animation.AnimatedContent(
                feed == null && loadingFeed, Modifier.fillMaxSize(), label = "feed",
                transitionSpec = {
                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(if (feedOff) 0 else 150)) togetherWith
                        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(if (feedOff) 0 else 100))
                },
            ) { waiting ->
            if (waiting) Box(Modifier.fillMaxSize().padding(vertical = 32.dp), contentAlignment = Alignment.TopCenter) {
                CircularWavyProgressIndicator(color = CmColors.actionIcon)
            } else ZoomedText { LazyColumn(
                Modifier.fillMaxSize().nestedScroll(reading), state = list,
                // Con il controller in vista, in fondo lo spazio per leggere l'ultimo messaggio sopra di lui; con un box sopra il
                // campo il controller ha la sua nicchia sotto il box, e qui basta il margine.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp + if (niche) 0.dp else readingRoom()), verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (feed != null) {
                    if (more) item(key = "older") {
                        TextButton(onClick = { follow = false; onOlder() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.load_older), color = CmColors.actionIcon) }
                    }
                    // I passaggi di fila diventano un gruppo (Franz, 01/10 15:59: «Gruppi + righe ricche»).
                    val grouped = groupedFeed.orEmpty()
                    val newsFrom = ChatNews.firstNew(grouped, seenAt)
                    itemsIndexed(grouped, key = { _, i -> feedKey(i) }) { index, it ->
                        val k = feedKey(it)
                        val opening = android.os.SystemClock.uptimeMillis() - openedAt < 1_000
                        Column(
                            Modifier.fillMaxWidth().cascadeIn(ChatNews.rank(index, grouped.size), opening, cascaded, k)
                                .newsTint(newsFrom != null && index >= newsFrom && it !is ChatFeed.Item.Mine, tinted, k),
                        ) {
                        if (index == newsFrom) NewsDivider()
                        when (it) {
                            // Una voce ancora in coda nel turno (scritta mentre Claude lavora) si dice «in coda».
                            is ChatFeed.Item.Mine -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                MineBubble(it.sent, if (it.entry?.queued == true && it.status != ChatRules.Status.FAILED) ChatRules.Status.QUEUED else it.status, reasons[it.sent.id], actions, onEdit = { t -> draft = t }, onResend = { t -> actions.send(PhonePrimary.Target.PROMPT, t) })
                                // Il pannello che un comando slash ha aperto sul PC (/cost) risponde sotto il comando.
                                it.sent.panel?.let { p -> ClaudeBubble(p, it.sent.sentAt, ttsMinChars, actions.speak) }
                            }
                            is ChatFeed.Item.User -> UserBubble(it.entry, onResend = { t -> actions.send(PhonePrimary.Target.PROMPT, t) })
                            is ChatFeed.Item.Claude -> ClaudeBubble(
                                it.entry.text.orEmpty(), it.entry.at, ttsMinChars, actions.speak, cut = it.entry.cut, turn = it.entry.turn,
                                onSpeakFrom = actions.speakFrom, onDecision = decide,
                            )
                            is ChatFeed.Item.Tool -> ToolLine(it.entry)
                            is ChatFeed.Item.Steps -> StepsCard(it)
                        }
                        }
                    }
                    // A chat già piena la rotella non c'è: una riga sottile dice da quanto la lettura aspetta il PC.
                    ChatFeed.waitingPc(true, waitingSince, waitClock.value)?.let { secs ->
                        item(key = "waiting-pc") {
                            Text(
                                stringResource(R.string.chat_waiting_pc, secs), style = MaterialTheme.typography.bodySmall, color = CmColors.text2,
                                modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                } else if (loadingFeed) {
                    item(key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            CircularWavyProgressIndicator(color = CmColors.actionIcon)
                        }
                    }
                } else {
                    // Una conversazione appena nata (dal vivo 03/10 16:37: claude-master ripartita con «Sessione nuova»): senza una
                    // riga la pagina sembrava rotta.
                    if (chat.isEmpty() && s.outcome == null && s.question == null && s.state != SessionState.BUSY && s.state != SessionState.AWAITING) item(key = "empty") {
                        Text(
                            stringResource(R.string.chat_empty), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
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
            } }
            }
            // «Torna all'ultimo messaggio» (Franz, 10/10 08:18: «avevamo deliberato un tasto per tornare in diretta, ma non l'ho
            // mai visto»): c'è mentre rileggi più su; il tocco scende in fondo e la chat torna a seguire.
            val behind by remember(list) { derivedStateOf { list.canScrollForward } }
            androidx.compose.animation.AnimatedVisibility(
                visible = !follow && behind && feed != null,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 12.dp + if (niche) 0.dp else readingRoom()),
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.8f),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.8f),
            ) {
                androidx.compose.material3.SmallFloatingActionButton(
                    onClick = {
                        follow = true
                        listScope.launch { list.animateScrollToItem((list.layoutInfo.totalItemsCount - 1).coerceAtLeast(0), Int.MAX_VALUE) }
                    },
                    containerColor = CmColors.surfaceHigh, contentColor = CmColors.actionIcon,
                ) { Icon(Icons.Rounded.ArrowDownward, stringResource(R.string.to_latest)) }
            }
            }
            // Il box «Prossimi» (variante A del 03/10 15:20, rivista il 04/10 20:21): sopra la barra con la sessione ferma,
            // anche scrivendo; il tocco porta la riga nel campo, accodata con «e poi» se c'è già testo, ↗ la manda subito a
            // campo vuoto. Si chiude a una riga, e resta chiuso finché non lo si riapre.
            if (stepsSplit.fresh.isNotEmpty() || stepsSplit.old.isNotEmpty()) PromptBox(
                stringResource(R.string.next_steps), stepsSplit.fresh.map { PromptRow(it, it, direct = true, blocking = it in stepsBlocking) },
                old = stepsSplit.old.map { PromptRow(it, it, direct = true, blocking = it in stepsBlocking) }, onDismiss = { r -> dismissStep(r.text) },
                open = boxes.stepsOpen, onOpen = boxes::steps, draftBlank = draftBlank,
                onPick = { r -> draft = NextSteps.append(draft, r.text, then) },
                onSend = { r -> actions.send(PhonePrimary.Target.PROMPT, r.text); follow = true },
            )
            }
        } }
        if (home == null) { chatArea(); dock?.invoke() } else if (halfStops) {
            // Il tablet (Franz, 08/10 18:15): la home resta sotto, ferma; la conversazione è un foglio che cresce dal basso fino
            // a una delle tre misure: la barra, metà della colonna, tutta. Cresce dalla barra, così la sua barra in cima parte
            // dal posto di quella in basso; a metà la home sopra resta da toccare.
            val h = home
            val off = animationsOff()
            var dockPx by remember { mutableIntStateOf(0) }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                val full = constraints.maxHeight.toFloat()
                val target = when {
                    homeOpen -> dockPx.toFloat()
                    half -> full * HALF_SHARE
                    else -> full
                }
                val height = remember { androidx.compose.animation.core.Animatable(if (homeOpen) 0f else target) }
                LaunchedEffect(target, off) {
                    if (height.value < dockPx && !homeOpen) height.snapTo(dockPx.toFloat())
                    if (off) height.snapTo(target) else height.animateTo(target, androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))
                }
                // Il foglio c'è finché non è tornato nella barra: solo allora ricompare la barra della home.
                val sheet = !homeOpen || height.value > dockPx + 1f
                Column(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        h({ draft = it }, { t -> actions.send(PhonePrimary.Target.PROMPT, t) })
                    }
                    if (!sheet) ReadingSlot(s.name, overlay = true)
                    Box(Modifier.onSizeChanged { dockPx = it.height }.graphicsLayer { alpha = if (sheet) 0f else 1f }) { dock?.invoke() }
                }
                if (sheet) {
                    val px = height.value.coerceIn(0f, full)
                    val sh = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    Column(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            .height(with(androidx.compose.ui.platform.LocalDensity.current) { px.toDp() })
                            .clip(sh).masterChat(LocalMasterChatStyle.current, sh),
                    ) { chatArea() }
                }
            }
        } else {
            // Nella home della master (Franz, 03/10 17:10) si anima solo la parte sopra il campo: la lista con la barra in
            // basso lascia il posto, salendo dal basso, a barra in cima, intestazione e conversazione. Il campo resta fermo,
            // con la bozza.
            val h = home
            val off = animationsOff()
            // L'altezza della barra ridotta: il pannello si ferma lì, così la sua barra in cima parte e arriva esattamente
            // al posto di quella in basso (dal vivo 03/10 19:01: scendeva sotto il campo di testo e poi «risaliva»).
            var dockPx by remember { mutableIntStateOf(0) }
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                androidx.compose.animation.AnimatedContent(
                    homeOpen, label = "home",
                    transitionSpec = {
                        // Il pannello sale opaco dalla barra in basso e la lista dietro arretra (dissolvenza e scala 0,96); alla
                        // chiusura scende per la stessa strada e resta sopra la lista che torna. Animazioni ridotte: solo
                        // dissolvenza.
                        val recede = androidx.compose.animation.core.tween<Float>(250, easing = CmMotion.easing)
                        when {
                            off -> androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(150)) togetherWith
                                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150))
                            !targetState -> androidx.compose.animation.slideInVertically(CmMotion.panel) { full -> (full - dockPx).coerceAtLeast(0) } togetherWith
                                (androidx.compose.animation.fadeOut(recede) + androidx.compose.animation.scaleOut(recede, targetScale = 0.96f))
                            else -> ((androidx.compose.animation.fadeIn(recede) + androidx.compose.animation.scaleIn(recede, initialScale = 0.96f)) togetherWith
                                androidx.compose.animation.slideOutVertically(CmMotion.panel) { full -> (full - dockPx).coerceAtLeast(0) }).apply { targetContentZIndex = -1f }
                        }
                    },
                ) { showList ->
                    // Il pannello della conversazione è opaco e ha gli angoli in alto, come un foglio: copre la lista mentre sale.
                    Column(
                        Modifier.fillMaxSize().then(
                            if (showList) Modifier
                            else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp).let { sh -> Modifier.clip(sh).masterChat(LocalMasterChatStyle.current, sh) }
                        ),
                    ) {
                        if (showList) {
                            // Il contenuto della home scorre da sé: il riepilogo è una lista «pigra» (transizione 4).
                            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                h({ draft = it }, { t -> actions.send(PhonePrimary.Target.PROMPT, t) })
                            }
                            // Una barra sola che sale e scende (dal vivo 03/10 17:51: alla riduzione se ne vedevano due, quella del
                            // pannello che scendeva sopra questa). Salendo sparisce subito: la sostituisce quella del pannello, che parte
                            // da qui. Scendendo ricompare solo quando il pannello è arrivato in fondo.
                            val dockAlpha by transition.animateFloat(
                                transitionSpec = {
                                    if (targetState == androidx.compose.animation.EnterExitState.Visible) androidx.compose.animation.core.tween(150, delayMillis = if (off) 0 else 200, easing = CmMotion.easing)
                                    else androidx.compose.animation.core.snap()
                                },
                                label = "dock",
                            ) { if (it == androidx.compose.animation.EnterExitState.Visible) 1f else 0f }
                            // Il controller della lettura si posa sopra la barra, così la barra resta da toccare (09/10 16:50).
                            ReadingSlot(s.name, overlay = true)
                            Box(Modifier.onSizeChanged { dockPx = it.height }.graphicsLayer { alpha = dockAlpha }) { dock?.invoke() }
                        } else chatArea()
                    }
                }
            }
        }
        // Il posto del mini-controller sopra il campo, come il mini-player delle app di musica (Franz, 03/10 23:00); il
        // controller lo disegna `ReadingOverlayHost` sopra tutte le pagine. Con la tastiera aperta no: il campo ha la precedenza.
        val imeOpen = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
        // Le azioni ricorrenti della master (Franz, 04/10 20:24; contratto 1.33). Chiuse non occupano righe: le apre il tasto
        // ⟳ nel campo (Franz, 04/10 23:55), e il pannello sta sopra il campo con gli stessi gesti dei Prossimi; si chiude
        // dall'intestazione o scegliendo un'azione. Una che aspetta un pezzo (`param`) va nel campo col cursore in fondo.
        if (boxes.recurringOpen && recurringRows.isNotEmpty()) PromptBox(
            stringResource(R.string.recurring), recurringRows,
            open = true, onOpen = { boxes.recurring(false) }, draftBlank = draftBlank,
            onPick = { r ->
                draft = NextSteps.append(draft, if (r.direct) r.text else r.text.trimEnd() + " ", then)
                boxes.recurring(false)
                if (!r.direct) fieldFocus?.target = s.name
            },
            onSend = { r -> boxes.recurring(false); actions.send(PhonePrimary.Target.PROMPT, r.text); follow = true },
            modifier = Modifier.padding(top = 6.dp),
        )
        // Contratto 1.37: oltre il 60 % di contesto, a sessione ferma, la proposta «handoff, poi /clear»; chiusa torna a 70 e 80.
        var ctxDismissed by rememberSaveable(s.name) { mutableStateOf<Int?>(null) }
        val nudge = if (home != null && homeOpen) null else it.pixelbox.cmwatch.rules.ContextActions.nudge(s, ctxDismissed)
        if (nudge != null) ContextNudge(s.context ?: nudge, onGo = actions.handoff, onDismiss = { ctxDismissed = nudge })
        // Il controller sta sopra la fine della conversazione, non in una fascia sua (Franz, 08/10 20:31: «elemento
        // sovrapposto, il resto mantiene i colori suoi»): qui solo il segno di dove posarlo, sopra il campo.
        // Con la home aperta il segno sta sopra la barra della master, non qui (Franz, 09/10 16:50: il controller la copriva
        // e la master non si apriva più).
        // Anche con la tastiera aperta: il controller resta e si posa sopra il campo, che sale con lei (Franz, 10/10 16:52:
        // «posiziona bene live quando apro tastiera»; senza il segno andava sul bordo della tastiera, sopra il campo).
        if (niche && !(home != null && homeOpen)) Spacer(Modifier.height(readingRoom()))
        if (!(home != null && homeOpen)) ReadingSlot(s.name, overlay = true)
        if (home != null && LocalMasterLook.current.thread) MasterThread()
        Composer(
            s, draftHolder, onDraft = { draft = it }, ops, canAttach, actions, onSent = { draft = ""; follow = true }, quota, phrases, canTonight, slash,
            toMaster = home != null, canAttachFiles = canAttachFiles, fieldSuggestion = stepsBox.field,
            onRecurring = if (recurringRows.isEmpty()) null else ({ boxes.recurring(!boxes.recurringOpen) }), recurringOpen = boxes.recurringOpen,
            onDecisionNew = if (home != null && decide != null) ({ decisionDraft = "" }) else null,
        )
    }
}

/**
 * La barra di scrittura (design 30/09, parti 1 e 7): sopra la tastiera, «+» per un'immagine, il tasto a destra.
 * L'immagine scelta resta in attesa sopra il campo, con la sua anteprima e la ×, e parte con Invia insieme al testo
 * (Franz, 30/09 22:03: prima partiva subito).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Composer(
    s: Session, draftState: androidx.compose.runtime.State<String>, onDraft: (String) -> Unit, ops: List<String>?, canAttach: Boolean, actions: SheetActions, onSent: () -> Unit,
    quota: QuotaWarning.Warn? = null, phrases: List<String> = emptyList(), canTonight: Boolean = false,
    slash: List<String>? = null,
    /** Il campo del riepilogo, che scrive alla master: «Scrivi alla master». */
    toMaster: Boolean = false,
    /** Contratto 1.28: il relay accetta file di qualunque formato. */
    canAttachFiles: Boolean = false,
    /** Il suggerimento del campo vuoto (`NextSteps.box`): il primo dei Prossimi, o il suggerito del terminale. */
    fieldSuggestion: String? = null,
    /** Il tasto ⟳ delle azioni ricorrenti della master nel campo; null senza azioni. */
    onRecurring: (() -> Unit)? = null, recurringOpen: Boolean = false,
    /** Contratto 1.37: «Salva una decisione» nel + della master. */
    onDecisionNew: (() -> Unit)? = null,
) {
    val draft = draftState.value
    var images by rememberSaveable(s.id) { mutableStateOf(listOf<Uri>()) }
    // clear ed exit svuotano o chiudono la sessione: prima si chiede (Franz, 02/10 11:12).
    var confirm by remember(s.id) { mutableStateOf<it.pixelbox.cmwatch.rules.Slash.Parsed?>(null) }
    val image = images.firstOrNull()
    val base = PhonePrimary.composer(s, draft, ops)
    // Con un'immagine in attesa c'è sempre qualcosa da mandare, anche con il campo vuoto.
    val mode = if (image != null && base != PhonePrimary.Composer.REOPEN && base != PhonePrimary.Composer.SEND && base != PhonePrimary.Composer.SEND_TONAL)
        (if (PhonePrimary.button(s, "x") == PhonePrimary.Button.OPTION) PhonePrimary.Composer.SEND_TONAL else PhonePrimary.Composer.SEND) else base
    // Il campo della master sta sul fondo della master, velato di celeste e piatto (Celeste velato, Franz 06/10 21:23).
    val bar = Modifier.fillMaxWidth().background(if (toMaster) MasterHigh else CmColors.bg)
        .imePadding().padding(horizontal = 12.dp, vertical = 10.dp)
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
            QuotaLine(w, draft.trim(), canDefer = can, canTonight = can && canTonight, actions, onDeferred = onSent, night = canTonight)
        }
        // Contratto 1.23: il prompt suggerito del terminale, solo per una sessione ferma: attenuato nel campo vuoto, come
        // dopo ❯ nel terminale, e «Usa» lo mette nel campo da ritoccare (design 01/10, variante C; prima era una pillola).
        val sug = fieldSuggestion?.takeIf { image == null }
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
            // Contratto 1.25: un testo che comincia con un comando consentito parte come comando, senza domanda aperta.
            val cmd = it.pixelbox.cmwatch.rules.Slash.parse(draft, slash)?.takeIf { images.isEmpty() && s.question == null }
            when {
                cmd != null && it.pixelbox.cmwatch.rules.Slash.confirm(cmd.cmd) -> confirm = cmd
                cmd != null -> { actions.slash(cmd.cmd, cmd.args); onSent() }
                else -> {
                    if (images.isNotEmpty()) images.forEachIndexed { i, uri -> actions.attach(uri, if (i == 0) draft.trim() else "") }
                    else PhonePrimary.target(s, draft)?.let { actions.send(it, draft.trim()) }
                    images = emptyList()
                    onSent()
                }
            }
        }
        // Scrivendo «/»: i comandi consentiti che cominciano così; il tocco li mette nel campo, pronti per gli argomenti.
        val slashHints = it.pixelbox.cmwatch.rules.Slash.suggest(draft, slash).takeIf { s.question == null }.orEmpty()
        if (slashHints.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            slashHints.forEach { c ->
                AssistChip(onClick = { onDraft("/$c ") }, label = { Text("/$c", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) })
            }
        }
        // /exit passa dalla stessa domanda di ogni chiusura, col nome e lo stato (Franz, 06/10 12:25).
        confirm?.takeIf { it.cmd == "exit" }?.let { c -> CloseDialog(s, onDismiss = { confirm = null }, onConfirm = { confirm = null; actions.slash(c.cmd, c.args); onSent() }) }
        confirm?.takeIf { it.cmd != "exit" }?.let { c ->
            AlertDialog(
                onDismissRequest = { confirm = null }, containerColor = CmColors.surface,
                title = { Text(stringResource(R.string.slash_confirm_title, c.cmd, s.name)) },
                text = { Text(stringResource(R.string.slash_confirm_clear)) },
                confirmButton = { TextButton(onClick = { confirm = null; actions.slash(c.cmd, c.args); onSent() }) { Text(stringResource(R.string.slash_confirm_ok), color = CmColors.actionIcon) } },
                dismissButton = { TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.cancel), color = CmColors.text2) } },
            )
        }
        // Allegato e invio dentro il campo, centrati sulla sua altezza (Franz, 30/09 22:13).
        // Il margine destro del tasto si misura sull'altezza della barra a una riga (dal vivo 03/10 20:00: con il testo di
        // sistema più grande la barra cresce, e un margine fisso non era più uguale a quelli sopra e sotto).
        val density = androidx.compose.ui.platform.LocalDensity.current
        var lineH by remember { mutableIntStateOf(0) }
        var fieldW by remember { mutableIntStateOf(0) }
        // In un campo stretto (le colonne del tablet col carattere grande) il suggerimento accanto a «Usa» e all'invio andava
        // in frammenti di una parola: lì sta su una riga sua sopra il campo, intero, e il tocco lo mette nel campo.
        val narrow = fieldW > 0 && with(density) { fieldW.toDp() } < 320.dp
        if (narrow && sug != null) Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).handCursor().clickable { onDraft(sug) }.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(sug, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = CmColors.stale, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.suggestion_use), color = CmColors.actionIcon, style = MaterialTheme.typography.labelLarge)
        }
        val inField = sug.takeIf { !narrow }
        // Scorrendo da una sessione all'altra il cursore segue la pagina che si vede (Franz, 04/10 20:53): chi scorre chiede
        // il fuoco per questa sessione (`FieldFocus.target`), il campo lo prende appena c'è.
        val fieldFocus = LocalFieldFocus.current
        val focusReq = remember { FocusRequester() }
        if (fieldFocus != null) LaunchedEffect(fieldFocus.target) {
            if (fieldFocus.target == s.name) { fieldFocus.target = null; runCatching { focusReq.requestFocus() } }
        }
        // Il testo messo nel campo da fuori («Usa», una riga dei box) porta il cursore in fondo, pronto per continuare.
        // Il campo a stato (TextFieldState): riceve anche immagini e file incollati, dalla tastiera o trascinati da un'altra
        // app (Franz, 05/10 22:29), che vanno fra gli allegati come dal +. La bozza resta sincronizzata nei due versi.
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val fieldState = androidx.compose.foundation.text.input.rememberTextFieldState(draft)
        val currentDraft by androidx.compose.runtime.rememberUpdatedState(draft)
        LaunchedEffect(draft) { if (fieldState.text.toString() != draft) fieldState.setTextAndPlaceCursorAtEnd(draft) }
        LaunchedEffect(fieldState) {
            androidx.compose.runtime.snapshotFlow { fieldState.text.toString() }.collect { t -> if (t != currentDraft) onDraft(t) }
        }
        OutlinedTextField(
            state = fieldState, lineLimits = androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine(maxHeightInLines = 5),
            modifier = Modifier.fillMaxWidth().onSizeChanged { if (lineH == 0 || it.height < lineH) lineH = it.height; fieldW = it.width }
                .contentReceiver { content ->
                    if (!canAttach) return@contentReceiver content
                    content.consume { item ->
                        val u = item.uri ?: return@consume false
                        val mime = ctx.contentResolver.getType(u).orEmpty()
                        if (!mime.startsWith("image/") && !canAttachFiles) return@consume false
                        images = (images + u).distinct().take(MAX_IMAGES)
                        true
                    }
                }
                .focusRequester(focusReq)
                .onFocusChanged { f -> fieldFocus?.let { ff -> if (f.isFocused) ff.owner = s.name else if (ff.owner == s.name) ff.owner = null } }
                // Il tablet (pezzo 6): con la tastiera fisica Invio manda e Maiusc+Invio va a capo; la tastiera dello schermo
                // resta com'è (il suo Invio arriva da un dispositivo virtuale, o come testo).
                .onPreviewKeyEvent { e ->
                    val enter = e.key == androidx.compose.ui.input.key.Key.Enter || e.key == androidx.compose.ui.input.key.Key.NumPadEnter
                    if (!enter || e.nativeKeyEvent.device?.isVirtual != false || e.isShiftPressed || e.isCtrlPressed || e.isAltPressed) return@onPreviewKeyEvent false
                    if (e.type == androidx.compose.ui.input.key.KeyEventType.KeyDown && (mode == PhonePrimary.Composer.SEND || mode == PhonePrimary.Composer.SEND_TONAL)) send()
                    true
                },
            placeholder = {
                if (inField != null) Text(inField, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = CmColors.stale, maxLines = 5)
                // Il destinatario esplicito, «Scrivi a fable-director» (osservazioni del 03/10).
                else Text(when { s.question != null -> stringResource(R.string.answer_free); toMaster -> stringResource(R.string.master_placeholder); else -> stringResource(if (it.pixelbox.cmwatch.rules.Preposition.ad(s.name)) R.string.write_to_ad else R.string.write_to, s.name) }, maxLines = 2)
            },
            shape = MaterialTheme.shapes.extraLarge,
            // Il campo della master sul suo fondo velato di celeste, piatto e senza bordo; scrivendo, il bordo chiaro (Franz, 06/10 21:23).
            colors = if (toMaster)
                androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent, focusedBorderColor = CmColors.text2,
                    unfocusedContainerColor = MasterContainer, focusedContainerColor = MasterContainer, cursorColor = CmColors.modelOpus,
                )
            else androidx.compose.material3.OutlinedTextFieldDefaults.colors(),
            // Le azioni ricorrenti della master stanno nel menu del + (Franz, 05/10 07:27: il ⟳ nel campo stringeva troppo).
            leadingIcon = if (canAttach || onRecurring != null || onDecisionNew != null) ({
                AttachButton(files = canAttachFiles, attach = canAttach, onRecurring = onRecurring, onDecision = onDecisionNew) { picked -> images = (images + picked).distinct().take(MAX_IMAGES) }
            }) else null,
            trailingIcon = { Row(verticalAlignment = Alignment.CenterVertically) {
                if (inField != null && draft.isBlank()) TextButton(onClick = { onDraft(inField) }) { Text(stringResource(R.string.suggestion_use), color = CmColors.actionIcon) }
                // Con una tastiera fisica collegata il campo lo dice, come nel mockup del tablet.
                else if (hardKeyboard() && mode != PhonePrimary.Composer.STOP && with(density) { fieldW.toDp() } >= 420.dp) Text(stringResource(R.string.enter_sends), style = MonoSmall, modifier = Modifier.padding(end = 10.dp))
                val filled = IconButtonDefaults.filledIconButtonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary)
                // Il cerchio concentrico all'estremità della barra (segnalazione 03/10 19:18, come ChatGPT): 40 dp nella barra da 56,
                // quindi 8 dp sopra, sotto e a destra.
                val endPad = with(density) { ((lineH.toDp() - 40.dp) / 2).coerceAtLeast(6.dp) }
                val size = Modifier.padding(end = if (lineH > 0) endPad else 8.dp).size(40.dp).handCursor()
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
            } },
        )
    }
}

/** Una tastiera fisica collegata e aperta (Chromebook, tablet con tastiera). */
@Composable
private fun hardKeyboard(): Boolean {
    val c = androidx.compose.ui.platform.LocalConfiguration.current
    return c.keyboard == android.content.res.Configuration.KEYBOARD_QWERTY && c.hardKeyboardHidden == android.content.res.Configuration.HARDKEYBOARDHIDDEN_NO
}

/**
 * L'avviso della quota sopra la barra: la finestra e quando riparte, e due bottoni testuali che rimandano il testo
 * scritto (alla ripartenza o stanotte). Senza testo nel campo i bottoni restano spenti; tocco sulla riga = Panoramica.
 */
@Composable
private fun QuotaLine(w: QuotaWarning.Warn, draft: String, canDefer: Boolean, canTonight: Boolean, actions: SheetActions, onDeferred: () -> Unit, night: Boolean = false) {
    Column(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(CmColors.briefWarn).handCursor().clickable(onClick = actions.overview).padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 2.dp)) {
        Text(
            stringResource(if (w.projected) R.string.quota_warn_pace else R.string.quota_warn, w.pct, hhmm(w.resetAt)),
            style = MaterialTheme.typography.bodyMedium, color = CmColors.briefWarnInk,
        )
        // Senza testo nel campo, al posto di due bottoni spenti e illeggibili sull'ambra (segnalazione 01/10 21:58) una riga
        // che dice cosa si può fare; scritto il testo, compaiono i bottoni.
        if (draft.isBlank()) Text(
            stringResource(if (night) R.string.quota_write_hint else R.string.quota_write_hint_reset), style = MaterialTheme.typography.bodySmall, color = CmColors.briefWarnInk,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        ) else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (canDefer) TextButton(onClick = { if (actions.sendAtReset(draft)) onDeferred() }) { Text(stringResource(R.string.send_at_reset), color = CmColors.briefWarnInk, fontWeight = FontWeight.SemiBold) }
            if (canTonight) TextButton(onClick = { if (actions.sendTonight(draft)) onDeferred() }) { Text(stringResource(R.string.send_tonight), color = CmColors.briefWarnInk, fontWeight = FontWeight.SemiBold) }
        }
    }
}

/** Una frase rapida: chip tonale su una riga; tocco = manda, pressione lunga = nel campo. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhraseChip(text: String, onSend: () -> Unit, onEdit: () -> Unit) {
    Surface(
        color = CmColors.surfaceHigh, contentColor = CmColors.text, shape = MaterialTheme.shapes.large,
        modifier = Modifier.clip(MaterialTheme.shapes.large).handCursor().combinedClickable(onClick = onSend, onLongClick = onEdit),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
    }
}

/** Una riga di un box sopra il campo: quello che si legge, quello che va nel campo, e se può partire subito. */
/** `blocking`: un Prossimo che sblocca un lavoro fermo (contratto 1.38): riga ambra a sinistra e lucchetto. */
private data class PromptRow(val label: String, val text: String, val direct: Boolean, val blocking: Boolean = false)

/**
 * I box sopra il campo, «Prossimi» e «Ricorrenti» (Franz, 04/10 20:21 e 20:24): un riquadro a tutta larghezza, una riga
 * per voce. Il tocco porta la voce nel campo; a campo vuoto ↗ la manda subito, con del testo nel campo + la accoda.
 * L'intestazione apre e chiude il box; chiuso resta una riga, col numero delle voci.
 */
@Composable
private fun PromptBox(
    title: String, rows: List<PromptRow>, open: Boolean, onOpen: (Boolean) -> Unit, draftBlank: Boolean,
    onPick: (PromptRow) -> Unit, onSend: (PromptRow) -> Unit, modifier: Modifier = Modifier,
    /** Le azioni vecchie, chiuse in «+N vecchie» in fondo; `onDismiss` toglie un'azione trascinandola o tenendola premuta. */
    old: List<PromptRow> = emptyList(), onDismiss: ((PromptRow) -> Unit)? = null,
) {
    var showOld by remember(old) { mutableStateOf(false) }
    Column(
        modifier.padding(horizontal = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CmColors.surfaceLow)
            .border(1.dp, CmColors.line, RoundedCornerShape(18.dp)),
    ) {
        val toggle = stringResource(if (open) R.string.box_close else R.string.box_open, title)
        Row(
            Modifier.fillMaxWidth().handCursor().clickable(onClickLabel = toggle) { onOpen(!open) }.handCursor().padding(start = 14.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.box_title, title.uppercase(), rows.size + old.size), style = MonoSmall, modifier = Modifier.weight(1f))
            // Il box sta sopra il campo e si apre verso l'alto: chiuso la freccia sale, aperto scende.
            Icon(if (open) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess, null, tint = CmColors.text2, modifier = Modifier.size(20.dp))
        }
        // Con poco spazio (una colonna del tablet con la tastiera aperta) le righe scorrono dentro il box.
        if (open) Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            (rows + if (showOld) old else emptyList()).forEach { r ->
                androidx.compose.material3.HorizontalDivider(color = CmColors.line)
                PromptBoxRow(r, draftBlank, onPick, onSend, onDismiss, faded = r in old)
            }
            if (old.isNotEmpty() && !showOld) {
                androidx.compose.material3.HorizontalDivider(color = CmColors.line)
                Text(
                    androidx.compose.ui.res.pluralStringResource(R.plurals.steps_old, old.size, old.size), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2,
                    modifier = Modifier.fillMaxWidth().handCursor().clickable { showOld = true }.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/**
 * Una riga di un box: tocco = nel campo, ↗ = manda. Si toglie trascinandola via o tenendola premuta (Franz, 08/10 20:50).
 * Quelle che sbloccano un lavoro fermo hanno l'etichetta «sblocca» e lo stesso colore delle altre: l'ambra si leggeva come
 * un avviso.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PromptBoxRow(
    r: PromptRow, draftBlank: Boolean, onPick: (PromptRow) -> Unit, onSend: (PromptRow) -> Unit, onDismiss: ((PromptRow) -> Unit)?, faded: Boolean,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val row: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().background(CmColors.surfaceLow).handCursor()
                .combinedClickable(onClick = { onPick(r) }, onLongClick = onDismiss?.let { d -> { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); d(r) } })
                .padding(start = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(r.label, style = MaterialTheme.typography.bodyLarge, color = if (faded) CmColors.text2 else CmColors.text, modifier = Modifier.weight(1f, fill = false).padding(vertical = 10.dp))
                if (r.blocking) Text(
                    stringResource(R.string.steps_unblock), style = MonoSmall.copy(color = CmColors.text2),
                    modifier = Modifier.border(1.dp, CmColors.line, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            if (draftBlank && r.direct) IconButton(onClick = { onSend(r) }, modifier = Modifier.handCursor()) {
                Icon(Icons.Rounded.NorthEast, stringResource(R.string.send), tint = CmColors.actionIcon, modifier = Modifier.size(20.dp))
            } else IconButton(onClick = { onPick(r) }, modifier = Modifier.handCursor()) {
                Icon(Icons.Rounded.Add, stringResource(R.string.box_append), tint = CmColors.actionIcon, modifier = Modifier.size(20.dp))
            }
        }
    }
    if (onDismiss == null) { row(); return }
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { v -> if (v != SwipeToDismissBoxValue.Settled) { onDismiss(r); true } else false })
    SwipeToDismissBox(state, backgroundContent = {
        Box(Modifier.fillMaxSize().background(CmColors.surface).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
            Text(stringResource(R.string.steps_dismiss), style = MaterialTheme.typography.labelLarge, color = CmColors.text2)
        }
    }) { row() }
}

/** I box sopra il campo aperti o chiusi, ricordati dall'app (Franz, 04/10 20:21): Prossimi aperto, Ricorrenti chiuso. */
class PromptBoxes(stepsOpen: Boolean, recurringOpen: Boolean, private val save: (String, Boolean) -> Unit) {
    var stepsOpen by mutableStateOf(stepsOpen)
        private set
    var recurringOpen by mutableStateOf(recurringOpen)
        private set
    fun steps(open: Boolean) { stepsOpen = open; save(STEPS, open) }
    fun recurring(open: Boolean) { recurringOpen = open; save(RECURRING, open) }
    companion object { const val STEPS = "steps_open"; const val RECURRING = "recurring_open" }
}
val LocalPromptBoxes = androidx.compose.runtime.staticCompositionLocalOf { PromptBoxes(true, false) { _, _ -> } }

/**
 * Il fondo della chat della master (Franz, 04/10 23:36: «servirebbe la modifica del fondo della chat»; 23:48: i primi cinque
 * «poco incisivi, magari anche bordo»): nero com'è oggi, cornice lilla su fondo notte, bordo luminoso a sfumatura, viola
 * deciso, aurora, e cornice col campo lilla.
 */
enum class MasterChatStyle { BLACK, FRAME, EDGE, VIOLET, AURORA, FRAME_FIELD, DEPTH }

/**
 * Il segno della master in tutta l'app (Franz, 05/10 11:30: «una soluzione che si integri bene in tutta l'app»), tre
 * direzioni da scegliere: la firma (la sua icona con un anello corallo-lilla dove compare), il filo (una linea corallo-lilla
 * sotto la sua barra e sopra il suo campo), la voce (le sue risposte con una barra lilla a sinistra). Si combinano.
 */
data class MasterLook(val signature: Boolean = false, val thread: Boolean = false, val voice: Boolean = false)
val LocalMasterLook = androidx.compose.runtime.staticCompositionLocalOf { MasterLook() }
/** La sfumatura della master: dal corallo di Claude al lilla. */
val MasterGradient = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(CmColors.modelOpus, androidx.compose.ui.graphics.Color(0xFFCDB8FF)))
/** Il filo della master, 2 dp. */
@Composable
fun MasterThread(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(2.dp).background(MasterGradient))
/** La voce della master: la barra a sinistra delle sue risposte. */
internal val LocalVoiceAccent = androidx.compose.runtime.staticCompositionLocalOf { false }
/** Il fondo delle bolle di Franz: nella chat della master un passo sopra il suo celeste velato, come nella web app (06/10 21:23). */
internal val LocalBubble = androidx.compose.runtime.staticCompositionLocalOf<androidx.compose.ui.graphics.Color?> { null }
val LocalMasterChatStyle = androidx.compose.runtime.staticCompositionLocalOf { MasterChatStyle.DEPTH }

internal val MasterNight = androidx.compose.ui.graphics.Color(0xFF0E0B18)
internal val MasterLilac = androidx.compose.ui.graphics.Color(0xFFCDB8FF)
private val MasterBlue = androidx.compose.ui.graphics.Color(0xFF4C7DFF)

fun Modifier.masterChat(style: MasterChatStyle, shape: androidx.compose.ui.graphics.Shape): Modifier = when (style) {
    MasterChatStyle.BLACK -> background(CmColors.bg)
    // La conversazione della master su un solo tono, il più scuro del suo celeste velato (Franz, 06/10 21:23).
    MasterChatStyle.DEPTH -> background(MasterLow)
    MasterChatStyle.FRAME, MasterChatStyle.FRAME_FIELD -> background(MasterNight).border(2.dp, MasterLilac.copy(alpha = 0.85f), shape)
    MasterChatStyle.EDGE -> background(CmColors.bg)
        .drawBehind {
            drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(0f to MasterLilac.copy(alpha = 0.22f), 0.35f to androidx.compose.ui.graphics.Color.Transparent))
        }
        .border(androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.ui.graphics.Brush.sweepGradient(listOf(MasterLilac, MasterBlue, MasterLilac, MasterBlue, MasterLilac))), shape)
    MasterChatStyle.VIOLET -> background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF2C1F52), androidx.compose.ui.graphics.Color(0xFF120D22))))
    MasterChatStyle.AURORA -> background(CmColors.bg).drawBehind {
        drawRect(androidx.compose.ui.graphics.Brush.radialGradient(listOf(MasterLilac.copy(alpha = 0.38f), androidx.compose.ui.graphics.Color.Transparent), center = androidx.compose.ui.geometry.Offset(0f, 0f), radius = size.width * 1.1f))
        drawRect(androidx.compose.ui.graphics.Brush.radialGradient(listOf(MasterBlue.copy(alpha = 0.28f), androidx.compose.ui.graphics.Color.Transparent), center = androidx.compose.ui.geometry.Offset(size.width, size.height), radius = size.width))
    }
}

/** Contratto 1.33: le azioni ricorrenti della master, dallo stato (`state.recurring`). */
val LocalRecurring = androidx.compose.runtime.compositionLocalOf { emptyList<Recurring>() }

/** Un consiglio sotto la risposta: «↳» e il testo nel colore delle azioni; tocco = nel campo, pressione lunga = invio. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NextStep(text: String, onEdit: () -> Unit, onSend: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).handCursor().combinedClickable(onClick = onEdit, onLongClick = onSend).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("↳", style = MaterialTheme.typography.bodyLarge, color = CmColors.stale)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = CmColors.actionIcon)
    }
}

/** Il prompt suggerito come pillola tonale, a una riga logica; il tocco lo mette nel campo per modificarlo. */
@Composable
private fun SuggestionPill(text: String, onEdit: () -> Unit) {
    Surface(
        color = CmColors.surfaceHigh, contentColor = CmColors.text, shape = MaterialTheme.shapes.large,
        modifier = Modifier.clip(MaterialTheme.shapes.large).handCursor().clickable(onClick = onEdit),
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
    // Un file che non è un'immagine: l'icona e il suo nome (contratto 1.28).
    if (bmp == null) {
        val name = remember(uri) {
            runCatching {
                ctx.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            }.getOrNull() ?: uri.lastPathSegment.orEmpty()
        }
        Column(modifier.background(CmColors.surface).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.Description, null, tint = CmColors.actionIcon, modifier = Modifier.size(22.dp))
            Text(name, style = MaterialTheme.typography.labelSmall, color = CmColors.text2, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Clip, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    } else
        androidx.compose.foundation.Image(bmp, stringResource(R.string.attach_image), contentScale = ContentScale.Crop, modifier = modifier)
}

/**
 * «+» per un'immagine dalla galleria. Il selettore si registra solo dove c'è un'activity che lo ospita: negli snapshot
 * (Paparazzi) non c'è, e il tasto resta disegnato senza selettore.
 */
@Composable
private fun AttachButton(files: Boolean = false, attach: Boolean = true, onRecurring: (() -> Unit)? = null, onDecision: (() -> Unit)? = null, onPicked: (List<Uri>) -> Unit) {
    val icon: @Composable () -> Unit = { Icon(Icons.Rounded.Add, stringResource(R.string.attach_image), tint = CmColors.actionIcon) }
    if (androidx.activity.compose.LocalActivityResultRegistryOwner.current == null) { IconButton(onClick = {}, content = icon); return }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGES)) { uris -> if (uris.isNotEmpty()) onPicked(uris) }
    // Franz, 03/10 17:34: anche una foto scattata ora. La fotocamera di sistema la scrive nella cache dell'app, dietro il
    // FileProvider; poi segue la strada delle immagini (ridotta, caricata, `report`).
    var shot by rememberSaveable { mutableStateOf<String?>(null) }
    // Contratto 1.28: file di qualunque formato, solo con un relay che li accetta (`share.any`); più d'uno insieme, fino
    // al limite delle immagini (Franz, 07/10 19:46).
    val document = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> if (uris.isNotEmpty()) onPicked(uris.take(MAX_IMAGES)) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> shot?.let { u -> if (ok) onPicked(listOf(Uri.parse(u))) }; shot = null }
    Box {
        IconButton(onClick = { menu = true }, modifier = Modifier.handCursor(), content = icon)
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = CmColors.surface) {
            AttachMenuItems(
                attach = attach, files = files, onRecurring = onRecurring?.let { r -> { menu = false; r() } },
                onDecision = onDecision?.let { d -> { menu = false; d() } },
                onGallery = { menu = false; pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onCamera = {
                    menu = false
                    val file = java.io.File(java.io.File(ctx.cacheDir, "camera").apply { mkdirs() }, java.util.UUID.randomUUID().toString() + ".jpg")
                    val uri = androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".files", file)
                    shot = uri.toString()
                    runCatching { camera.launch(uri) }.onFailure { shot = null }
                },
                onFile = { menu = false; runCatching { document.launch(arrayOf("*/*")) } },
            )
        }
    }
}

/**
 * Le voci del menu del +: galleria, fotocamera, file e, nella master, le azioni ricorrenti in fondo (Franz, 05/10 07:27:
 * nel campo il ⟳ stringeva troppo insieme al + e a «Usa»). Fuori dal menu solo per il provino, che i popup non li disegna.
 */
@Composable
internal fun AttachMenuItems(
    attach: Boolean, files: Boolean, onRecurring: (() -> Unit)?, onGallery: () -> Unit, onCamera: () -> Unit, onFile: () -> Unit,
    onDecision: (() -> Unit)? = null,
) {
    if (attach) {
        DropdownMenuItem(text = { Text(stringResource(R.string.attach_gallery)) }, leadingIcon = { Icon(Icons.Rounded.Image, null, tint = CmColors.actionIcon) }, onClick = onGallery)
        DropdownMenuItem(text = { Text(stringResource(R.string.attach_camera)) }, leadingIcon = { Icon(Icons.Rounded.PhotoCamera, null, tint = CmColors.actionIcon) }, onClick = onCamera)
        if (files) DropdownMenuItem(text = { Text(stringResource(R.string.attach_file)) }, leadingIcon = { Icon(Icons.Rounded.AttachFile, null, tint = CmColors.actionIcon) }, onClick = onFile)
    }
    if (onRecurring != null) {
        if (attach) androidx.compose.material3.HorizontalDivider(color = CmColors.line)
        DropdownMenuItem(text = { Text(stringResource(R.string.recurring_open)) }, leadingIcon = { Icon(Icons.Rounded.Autorenew, null, tint = CmColors.actionIcon) }, onClick = onRecurring)
    }
    if (onDecision != null) DropdownMenuItem(text = { Text(stringResource(R.string.decision_new)) }, leadingIcon = { Icon(Icons.Rounded.BookmarkBorder, null, tint = CmColors.actionIcon) }, onClick = onDecision)
}

/** Contratto 1.37: il foglio «Salva come decisione»: il testo da correggere e per quale progetto vale. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DecisionSheet(draft: String, project: String?, onDismiss: () -> Unit, onSave: (String, String?) -> Unit) {
    var text by remember(draft) { mutableStateOf(draft) }
    var all by remember(project) { mutableStateOf(project == null) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CmColors.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.decision_save), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text)
            Text(stringResource(R.string.decision_sub), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
            androidx.compose.material3.OutlinedTextField(
                text, { v -> text = v.take(it.pixelbox.cmwatch.rules.MasterService.DECISION_MAX) }, modifier = Modifier.fillMaxWidth(), minLines = 3,
            )
            if (project != null) {
                Text(stringResource(R.string.decision_for), style = MaterialTheme.typography.labelMedium, color = CmColors.text2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.FilterChip(selected = !all, onClick = { all = false }, label = { Text(project) })
                    androidx.compose.material3.FilterChip(selected = all, onClick = { all = true }, label = { Text(stringResource(R.string.decision_all)) })
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = CmColors.text2) }
                Button(
                    onClick = { onSave(text.trim(), if (all) null else project) }, enabled = text.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
                ) { Text(stringResource(R.string.decision_ok)) }
            }
        }
    }
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
            QuestionOptions(q, firstFilled, holdHint, onHold, actions.answer)
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
        row.sent.panel?.let { ClaudeBubble(it, row.sent.sentAt, ttsMinChars, actions.speak) }
    }
}

/** Un messaggio mandato dal telefono: a destra, con l'anteprima dell'allegato, lo stato, l'ora, Copia e, se non è arrivato, Modifica. */
@Composable
private fun MineBubble(m: Sent, status: ChatRules.Status, reason: String?, actions: SheetActions, onEdit: (String) -> Unit, onResend: (String) -> Unit) {
    val clip = LocalClipboardManager.current
    // Tutta la larghezza meno un margine a sinistra, non una colonna stretta (Franz, 30/09 22:17).
    Column(Modifier.fillMaxWidth().padding(start = 40.dp), horizontalAlignment = Alignment.End) {
        Surface(
            color = LocalBubble.current ?: CmColors.surfaceHigh, shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp),
            modifier = Modifier,
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                m.attachment?.let { AttachmentThumb(it) }
                // Pressione lunga = selezione di una parte del testo, con la barra di sistema per copiarla (Franz, 02/10 20:31).
                if (m.text.isNotBlank()) SelectionContainer { Text(linked(m.text), style = MaterialTheme.typography.bodyLarge, color = CmColors.text) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            StatusMark(status, m.scheduledFor)
            // Un programmato dice solo quando parte: l'ora in cui è stato scritto accanto confondeva (provini 01/10).
            if (status != ChatRules.Status.SCHEDULED) Text(hhmm(m.sentAt), style = MaterialTheme.typography.labelMedium, color = CmColors.text2, modifier = Modifier.padding(end = 4.dp))
            SmallAction(Icons.Rounded.ContentCopy, stringResource(R.string.copy)) { clip.setText(AnnotatedString(m.text)) }
            // La matita solo su un messaggio non arrivato, per correggerlo e rimandarlo (Franz, 03/10 21:11): su uno già
            // elaborato non modificava niente, rimetteva solo il testo nel campo come Copia.
            if (status == ChatRules.Status.FAILED || status == ChatRules.Status.UNCERTAIN) SmallAction(Icons.Rounded.Edit, stringResource(R.string.edit)) { onEdit(m.text) }
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
private fun UserBubble(e: TranscriptEntry, onResend: (String) -> Unit) {
    val clip = LocalClipboardManager.current
    val text = e.text.orEmpty()
    Column(Modifier.fillMaxWidth().padding(start = 40.dp), horizontalAlignment = Alignment.End) {
        Surface(color = LocalBubble.current ?: CmColors.surface, shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)) {
            SelectionContainer { Text(linked(text), style = MaterialTheme.typography.bodyLarge, color = CmColors.text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val from = when (e.origin) {
                "pc" -> R.string.origin_pc
                "watch" -> R.string.origin_watch
                "phone" -> R.string.origin_phone
                "web" -> R.string.origin_web
                else -> null
            }
            from?.let { Text(stringResource(it), style = MaterialTheme.typography.labelMedium, color = CmColors.text2) }
            if (e.queued) Text(" · " + stringResource(R.string.chat_queued), style = MaterialTheme.typography.labelMedium, color = CmColors.stale)
            e.at?.let { Text(hhmm(it), style = MaterialTheme.typography.labelMedium, color = CmColors.text2, modifier = Modifier.padding(horizontal = 4.dp)) }
            SmallAction(Icons.Rounded.ContentCopy, stringResource(R.string.copy)) { clip.setText(AnnotatedString(text)) }
        }
    }
}

/** Una chiamata a uno strumento in una riga compatta: nome, cosa fa, esito; sotto i file prodotti, se ce ne sono. */
@Composable
private fun ToolLine(e: TranscriptEntry, files: Boolean = true) {
    val (main, detail) = ToolText.row(e.tool, e.text, e.note)
    val error = e.error == true
    Column(Modifier.fillMaxWidth().padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Un'icona per tipo di strumento, come nell'app nativa; un passaggio fallito ha la ✗ rossa.
            Icon(if (error) Icons.Rounded.ErrorOutline else toolIcon(ToolText.kind(e.tool)), e.tool,
                tint = if (error) CmColors.gone else CmColors.text2, modifier = Modifier.size(18.dp).padding(top = 1.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(main, style = MaterialTheme.typography.bodyMedium, color = if (error) CmColors.goneDim else CmColors.text)
                // Il comando o la cartella sotto, in monospazio grigio; al massimo tre righe, tagliate senza puntini.
                detail?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        color = CmColors.stale, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Clip)
                }
            }
        }
        if (files) e.files?.takeIf { it.isNotEmpty() }?.let { fs ->
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 28.dp)) {
                fs.forEach { FileChip(it) }
            }
        }
    }
}

private fun toolIcon(k: ToolText.Kind): androidx.compose.ui.graphics.vector.ImageVector = when (k) {
    ToolText.Kind.RUN -> Icons.Rounded.Terminal
    ToolText.Kind.READ -> Icons.Rounded.Description
    ToolText.Kind.EDIT -> Icons.Rounded.Edit
    ToolText.Kind.WRITE -> Icons.Rounded.Save
    ToolText.Kind.SEARCH -> Icons.Rounded.Search
    ToolText.Kind.WEB -> Icons.Rounded.Public
    ToolText.Kind.MESSAGE -> Icons.Rounded.Forum
    ToolText.Kind.DELEGATE -> Icons.Rounded.SmartToy
    ToolText.Kind.PLAN -> Icons.Rounded.Checklist
    // Il pezzo di puzzle per gli strumenti senza tipo (MCP, plugin): la chiave inglese non diceva niente (segnalazione 01/10 18:06).
    ToolText.Kind.OTHER -> Icons.Rounded.Extension
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
    // Contratto 1.24: il tocco chiede il file al PC; un'immagine arrivata si vede sotto il chip, a tutto schermo al tocco.
    val opener = LocalFileOpener.current
    val local = opener.local[f.path]
    // I media nascono già in anteprima (Franz, 10/10 09:54: «vorrei che nascessero già in anteprima, senza doverle
    // cliccare»): un'immagine si chiede al PC appena il suo messaggio è a schermo, fino a 10 MB, in silenzio se non arriva;
    // un video fino a 25 MB, solo in Wi-Fi (15:24).
    val autoPreview = it.pixelbox.cmwatch.rules.MediaPreview.auto(f.mime, f.size, opener.unmetered())
    LaunchedEffect(f.path, autoPreview) { if (autoPreview && opener.local[f.path] == null && f.path !in opener.loading) opener.preview(f) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(color = CmColors.surface, shape = MaterialTheme.shapes.medium, onClick = { opener.open(f) }) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (f.path in opener.loading) CircularProgressIndicator(Modifier.size(18.dp), color = CmColors.actionIcon, strokeWidth = 2.dp)
                else Icon(icon, null, tint = CmColors.actionIcon, modifier = Modifier.size(18.dp))
                // Il nome va a capo nello spazio che resta; il peso sta sempre su una riga (segnalazione 01/10 18:16: con un
                // nome lungo il peso finiva in colonna, una lettera per riga).
                Text(f.path.substringAfterLast('/'), style = MaterialTheme.typography.labelLarge, color = CmColors.text, modifier = Modifier.weight(1f, fill = false))
                f.size?.let { Text(sizeLabel(it), style = MaterialTheme.typography.labelMedium, color = CmColors.text2, softWrap = false) }
            }
        }
        // Sotto il nome i quattro tasti, come Copia e ▶ sotto le risposte (Franz, 07/10 16:07).
        Row {
            for ((act, icon, label) in listOf(
                Triple(FileAct.OPEN, Icons.AutoMirrored.Rounded.OpenInNew, R.string.file_open),
                Triple(FileAct.DOWNLOAD, Icons.Rounded.Download, R.string.file_download),
                Triple(FileAct.COPY, Icons.Rounded.ContentCopy, R.string.file_copy),
                Triple(FileAct.SHARE, Icons.Rounded.Share, R.string.file_share),
            )) {
                val desc = stringResource(label)
                IconButton(onClick = { opener.act(f, act) }, modifier = Modifier.size(40.dp).handCursor()) {
                    Icon(icon, desc, tint = CmColors.text2, modifier = Modifier.size(20.dp))
                }
            }
        }
        if (local != null && f.mime?.startsWith("image/") == true) AttachmentThumb(local)
        if (local != null && it.pixelbox.cmwatch.rules.MediaPreview.isVideo(f.mime)) VideoPreview(local)
    }
}

private fun sizeLabel(b: Long): String = when {
    b >= 1_000_000 -> "%.1f MB".format(b / 1_000_000.0)
    b >= 1_000 -> "${b / 1_000} KB"
    else -> "$b B"
}

/**
 * A (Franz, 10/10 16:01): la voce sale di 12 dp con una dissolvenza in 200 ms; all'apertura le ultime sei una dopo l'altra,
 * dall'alto, `rank` × 30 ms dopo la prima, così la cascata finisce col volo della card. Una volta sola per voce e pagina:
 * tornando a schermo scorrendo resta ferma. Con le animazioni spente nessun movimento.
 */
@Composable
private fun Modifier.cascadeIn(rank: Int?, opening: Boolean, done: MutableSet<String>, key: String): Modifier {
    val off = animationsOff()
    val first = remember(key) { rank != null && !off && done.add(key) }
    if (!first) return this
    val p = remember(key) { androidx.compose.animation.core.Animatable(0f) }
    val wait = if (opening) (rank ?: 0) * ChatNews.STAGGER_MS else 0L
    LaunchedEffect(key) {
        kotlinx.coroutines.delay(wait)
        p.animateTo(1f, androidx.compose.animation.core.tween(200, easing = CmMotion.easing))
    }
    return graphicsLayer { alpha = p.value; translationY = (1f - p.value) * 12.dp.toPx() }
}

/** B: il fondo appena tinto delle voci arrivate dall'ultima visita, che sfuma in 2 s dopo la cascata; una volta sola per voce. */
@Composable
private fun Modifier.newsTint(isNew: Boolean, done: MutableSet<String>, key: String): Modifier {
    val off = animationsOff()
    val first = remember(key) { isNew && !off && done.add(key) }
    if (!first) return this
    val a = remember(key) { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(key) {
        kotlinx.coroutines.delay(600)
        a.animateTo(0f, androidx.compose.animation.core.tween(2_000))
    }
    val tint = CmColors.actionIcon
    return drawBehind {
        if (a.value > 0f) {
            val dx = 6.dp.toPx(); val dy = 4.dp.toPx()
            drawRoundRect(
                tint.copy(alpha = .12f * a.value), topLeft = androidx.compose.ui.geometry.Offset(-dx, -dy),
                size = androidx.compose.ui.geometry.Size(size.width + 2 * dx, size.height + 2 * dy),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
            )
        }
    }
}

/** B: la riga «Nuovi» sopra la prima voce arrivata dall'ultima visita. */
@Composable
private fun NewsDivider() {
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(Modifier.weight(1f), color = CmColors.actionIcon.copy(alpha = .45f))
        Text(stringResource(R.string.chat_news), style = MaterialTheme.typography.labelMedium, color = CmColors.actionIcon)
        HorizontalDivider(Modifier.weight(1f), color = CmColors.actionIcon.copy(alpha = .45f))
    }
}

private fun feedKey(i: ChatFeed.Item): String = when (i) {
    is ChatFeed.Item.Mine -> "m-" + i.sent.id
    is ChatFeed.Item.User -> "u-" + i.entry.id
    is ChatFeed.Item.Claude -> "a-" + i.entry.id
    is ChatFeed.Item.Tool -> "t-" + i.entry.id
    // Dal primo passaggio: il gruppo che cresce in fondo resta lo stesso elemento della lista.
    is ChatFeed.Item.Steps -> "s-" + i.entries.first().id
}

/**
 * Un gruppo di passaggi (Franz, 01/10 15:59), sempre apribile. Chiuso sta in due righe (Franz, 02/10 00:01): il conteggio
 * con lo strumento più usato, e l'ultimo passaggio in chiaro; aperto, tutte le righe. Fondo appena tinto, senza bordo
 * né barretta (Franz, 02/10 07:38: la barretta a sinistra sapeva di IA standard), e un punto grigio per passaggio, rosso
 * se è fallito. I file prodotti restano sempre in vista sotto.
 */
@Composable
private fun StepsCard(g: ChatFeed.Item.Steps) {
    var open by rememberSaveable(g.entries.first().id) { mutableStateOf(false) }
    val failed = g.entries.count { it.error == true }
    val last = g.entries.last()
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(if (failed > 0) CmColors.stepsFailBg else CmColors.stepsBg).smoothSize(),
    ) {
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).handCursor().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
                val total = androidx.compose.ui.res.pluralStringResource(R.plurals.steps_count, g.entries.size, g.entries.size)
                val errors = if (failed > 0) androidx.compose.ui.res.pluralStringResource(R.plurals.steps_failed, failed, failed) else null
                // Una riga sola: il totale, lo strumento più usato e quanti altri tipi ci sono.
                val top = g.counts.firstOrNull()?.let { (tool, n) -> "$n $tool" }
                val others = (g.counts.size - 1).takeIf { it > 0 }?.let { "+$it" }
                // «1 fallito» in rosso, non affogato nel grigio del riepilogo (osservazioni del 03/10).
                Text(
                    androidx.compose.ui.text.buildAnnotatedString {
                        append(listOfNotNull(total, top, others).joinToString(" · "))
                        errors?.let { e ->
                            append(" · ")
                            pushStyle(androidx.compose.ui.text.SpanStyle(color = CmColors.briefAlertRing, fontWeight = FontWeight.SemiBold)); append(e); pop()
                        }
                    },
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                    style = MaterialTheme.typography.labelLarge, color = CmColors.text2, modifier = Modifier.weight(1f),
                )
                StepDots(g.entries.map { it.error == true })
                Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, stringResource(if (open) R.string.steps_close else R.string.steps_open), tint = CmColors.text2)
            }
            if (open) g.entries.forEach { ToolLine(it, files = false) } else {
                val (main, _) = ToolText.row(last.tool, last.text, last.note)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (last.error == true) Icons.Rounded.ErrorOutline else toolIcon(ToolText.kind(last.tool)), last.tool,
                        tint = if (last.error == true) CmColors.gone else CmColors.text2, modifier = Modifier.size(16.dp))
                    Text(main, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip, style = MaterialTheme.typography.bodyMedium,
                        color = if (last.error == true) CmColors.goneDim else CmColors.text)
                }
            }
            g.entries.flatMap { it.files.orEmpty() }.takeIf { it.isNotEmpty() }?.let { files ->
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    files.forEach { FileChip(it) }
                }
            }
        }
    }
}

/** Un punto per passaggio, i primi 12, poi «+N»: quanti e com'è andata, a colpo d'occhio. */
@Composable
private fun StepDots(failed: List<Boolean>) {
    Row(Modifier.padding(start = 8.dp, end = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        failed.take(MAX_DOTS).forEach { bad ->
            Box(Modifier.size(5.dp).clip(CircleShape).background(if (bad) CmColors.briefAlertRing else CmColors.stepsDot))
        }
        if (failed.size > MAX_DOTS) Text("+${failed.size - MAX_DOTS}", style = MaterialTheme.typography.labelSmall, color = CmColors.text2, modifier = Modifier.padding(start = 2.dp))
    }
}

private const val MAX_DOTS = 12

/** L'esito di un turno come fumetto di Claude, con Copia e Ascolta sotto (come nell'app nativa, senza fissa e dirama). */
@Composable
private fun ClaudeBubble(
    raw: String, at: Long?, ttsMinChars: Int, onSpeak: (String) -> Unit, cut: Boolean = false, turn: TranscriptTurn? = null,
    /** Mostra i consigli della riga `Prossimi:` (solo l'ultima risposta); la riga non si vede mai nel testo. */
    withSteps: Boolean = false, onStep: (String) -> Unit = {}, onSendStep: (String) -> Unit = {},
    onSpeakFrom: (String, Int) -> Unit = { _, _ -> },
    /** Contratto 1.37: «Salva come decisione» con il testo della risposta; null = op non supportata. */
    onDecision: ((String) -> Unit)? = null,
) {
    val parsed = it.pixelbox.cmwatch.rules.NextSteps.parse(raw)
    val text = parsed.text
    val clip = LocalClipboardManager.current
    // Claude a tutta larghezza, senza fumetto, come nell'app nativa (Franz, 30/09 22:17: «non limitiamo nei balloon»).
    // Nella chat della master, con la «voce», una barra lilla a sinistra.
    val voice = LocalVoiceAccent.current
    Column(
        Modifier.fillMaxWidth().then(
            if (voice) Modifier.drawBehind { drawRect(androidx.compose.ui.graphics.Color(0xFFCDB8FF).copy(alpha = 0.8f), size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)) }.padding(start = 10.dp)
            else Modifier,
        ),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Durante la lettura a voce il testo si mostra a paragrafi, quello letto in evidenza; il tocco su un paragrafo
                // fa ripartire la lettura da lì (Franz, 02/10 00:01: «come in orologio»).
                if (LocalSpeaking.current == text) {
                    val blocks = LocalBlocksOf.current(text)
                    val cur = LocalSpeakingBlock.current
                    blocks.forEachIndexed { i, b ->
                        Text(
                            linked(b.text), style = MaterialTheme.typography.bodyLarge, color = if (i == cur) CmColors.text else CmColors.text2,
                            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                                .background(if (i == cur) CmColors.surfaceHigh else androidx.compose.ui.graphics.Color.Transparent)
                                .handCursor().clickable { onSpeakFrom(text, i) }.padding(horizontal = 6.dp, vertical = 4.dp),
                        )
                    }
                // Fuori dalla lettura il testo si seleziona a pezzi con la pressione lunga (Franz, 02/10 20:31); durante la lettura
                // il tocco sui paragrafi resta per ripartire da lì.
                } else SelectionContainer {
                    val shown = it.pixelbox.cmwatch.rules.OutcomeLine.forPhone(text, stringResource(R.string.outcome_label))
                    val blocks = remember(shown) { it.pixelbox.cmwatch.rules.MarkdownTable.blocks(shown) }
                    if (blocks.none { b -> b is it.pixelbox.cmwatch.rules.MarkdownTable.Table }) Text(linked(shown), style = MaterialTheme.typography.bodyLarge, color = CmColors.text)
                    // Le tabelle (Franz, 02/10 21:11): griglia se stretta, una scheda per riga se larga.
                    else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        blocks.forEach { b ->
                            when (b) {
                                is it.pixelbox.cmwatch.rules.MarkdownTable.Text -> Text(linked(b.text), style = MaterialTheme.typography.bodyLarge, color = CmColors.text)
                                is it.pixelbox.cmwatch.rules.MarkdownTable.Table -> TableBlock(b)
                            }
                        }
                    }
                }
                // Variante C: «Prossimi» e i consigli in colonna; tocco = nel campo, pressione lunga = invio.
                if (withSteps && parsed.steps.isNotEmpty()) Column(Modifier.padding(top = 2.dp)) {
                    Text(stringResource(R.string.next_steps), style = MaterialTheme.typography.labelMedium, color = CmColors.text2)
                    parsed.steps.forEach { step -> NextStep(step, onEdit = { onStep(step) }, onSend = { onSendStep(step) }) }
                }
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
            if (reading) RatePill()
            // Il ▶ tondo come nel riepilogo e nel riquadro della master (Franz, 03/10 16:13), non l'altoparlante.
            FilledTonalIconButton(onClick = { onSpeak(text) }, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (reading) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, stringResource(if (reading) R.string.stop_reading else R.string.read_aloud),
                    tint = CmColors.actionIcon, modifier = Modifier.size(20.dp),
                )
            }
            onDecision?.let { d -> SmallAction(Icons.Rounded.BookmarkBorder, stringResource(R.string.decision_save)) { d(text) } }
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
private val PASSIVE_OPS = setOf(it.pixelbox.cmwatch.contract.CmdOp.SCREEN, it.pixelbox.cmwatch.contract.CmdOp.LAST, it.pixelbox.cmwatch.contract.CmdOp.TRANSCRIPT, it.pixelbox.cmwatch.contract.CmdOp.FILE)

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
        modifier = Modifier.widthIn(max = 240.dp).heightIn(max = 180.dp).clip(RoundedCornerShape(14.dp)).handCursor().clickable { full = true },
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
/** La misura a metà della master sul tablet: la parte di colonna che prende il foglio della conversazione. */
private const val HALF_SHARE = 0.55f

@Composable
private fun SheetHeader(
    s: Session, now: Long, choices: Choices?, canTune: Boolean, actions: SheetActions, showTerminal: Boolean,
    model: it.pixelbox.cmwatch.contract.Model? = s.model, effort: String? = s.effort,
    /** Il fondo della testata: il foglio per la master, il nero per le altre. */
    bg: androidx.compose.ui.graphics.Color = CmColors.bg,
    /** Contratto 1.25: il PC accetta /exit per questa sessione; il menu offre di chiuderla, dopo una conferma. */
    canExit: Boolean = false,
    quota: it.pixelbox.cmwatch.contract.QuotaAccount? = null,
    lead: (@Composable RowScope.() -> Unit)? = null,
    notesInHeader: Boolean = true,
) {
    var picker by remember { mutableStateOf<String?>(null) }   // "tune": il foglio di modello ed effort
    var menu by remember { mutableStateOf(false) }
    var exitAsk by remember { mutableStateOf(false) }
    var ctxSheet by remember { mutableStateOf(false) }
    val tunable = canTune && choices != null && s.state != SessionState.GONE
    // Contratto 1.37: il consiglio di fable-director, dentro il foglio; il puntino solo se la scelta attuale è diversa.
    val advice = it.pixelbox.cmwatch.rules.MasterService.advice(s, choices, now)
    Column(Modifier.fillMaxWidth().background(bg)) {
        Row(Modifier.fillMaxWidth().padding(start = if (lead != null) 16.dp else 12.dp, end = 0.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Una riga sola: modello, quota, contesto e menu. Lo stato e il tempo vanno nella riga dal vivo in fondo alla chat
            // (Franz, 30/09 23:01: «disordinata», «lavora 5 h potrebbe essere rimosso»).
            // Modello ed effort in una pillola sola, «Opus 5.5 · medium», che apre un foglio con le due scelte (osservazioni del 03/10:
            // troppi comandi in testa).
            val tune = listOfNotNull(ModelText.short(model) ?: stringResource(R.string.model_title), effort).joinToString(" · ")
            // Sul tablet il nome e lo stato a sinistra, nel posto che resta; le pillole a destra (mockup della plancia).
            if (lead != null) Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { lead() }
            // Tre pillole uguali (variante A, scelta da Franz il 09/10 alle 17:50): modello, quota delle 5 ore con l'ora in cui si
            // azzera, contesto; ognuna si tocca. Se la riga non ci sta la quota si accorcia (`MeterPills`).
            MeterPills(
                fill = lead == null,
                tune = { TunePill(tune, tunable, dot = tunable && advice?.dot == true) { picker = "tune" } },
                quota = quota?.h5?.let { pct -> { form -> QuotaPill(pct, quota.resetH5, quota.stale, now, form, onClick = actions.overview) } },
                context = s.context?.let { pct -> { ContextPill(pct, enabled = tunable) { ctxSheet = true } } },
            ) {
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.more), tint = CmColors.text2) }
                // Il menu della sessione come quello dell'app (Franz, 03/10 15:26): pannello, voci spiegate.
                if (menu) MenuPanel(
                    onDismiss = { menu = false },
                    entries = listOfNotNull(
                        MenuEntry(
                            if (s.followed) Icons.Rounded.NotificationsOff else Icons.Rounded.NotificationsActive,
                            stringResource(if (s.followed) R.string.unfollow else R.string.follow),
                            stringResource(if (s.followed) R.string.unfollow_sub else R.string.follow_sub),
                        ) { actions.follow(!s.followed) },
                        actions.askMaster?.let { ask -> MenuEntry(Icons.Rounded.SupervisorAccount, stringResource(R.string.ask_master), stringResource(R.string.ask_master_sub), onClick = ask) },
                        s.link.takeIf { it.isNotBlank() }?.let { MenuEntry(Icons.AutoMirrored.Rounded.OpenInNew, stringResource(R.string.open_in_claude), stringResource(R.string.open_in_claude_sub), onClick = actions.openInClaude) },
                        // Con la conversazione vera il terminale non serve più dal telefono (Franz, 30/09 20:39).
                        if (showTerminal) MenuEntry(Icons.Rounded.Terminal, stringResource(R.string.terminal), stringResource(R.string.terminal_sub), onClick = actions.terminal) else null,
                    ),
                    // Franz, 03/10 16:02: chiudere la sessione dal menu, con la stessa conferma di /exit scritto nel campo; in fondo,
                    // separata e in rosso (osservazioni del 03/10).
                    footer = if (canExit) MenuEntry(Icons.Rounded.PowerSettingsNew, stringResource(R.string.menu_exit), stringResource(R.string.menu_exit_sub), destructive = true) { exitAsk = true } else null,
                ) {
                    SessionBadge(s, 20.dp)
                    Text(
                        s.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text,
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip, modifier = Modifier.weight(1f),
                    )
                }
            }
            }
        }
        val notes = listOfNotNull(
            SessionsText.goalLine(s, stringResource(R.string.goal)).takeIf { notesInHeader },
            SessionsText.priority(s, stringResource(R.string.low_priority), stringResource(R.string.low_priority_offered)).takeIf { notesInHeader },
            SessionsText.window(s, stringResource(R.string.no_window)),
        )
        notes.forEach { Text(it, style = MaterialTheme.typography.labelMedium, color = CmColors.briefLabel, modifier = Modifier.padding(horizontal = 16.dp)) }
        // Senza note sotto le pillole lo stesso margine di sopra, così stanno al centro della barra (Franz, 10/10 14:32: «riduci barra»).
        Spacer(Modifier.height(if (notes.isEmpty()) 2.dp else 8.dp))
        HorizontalDivider(color = CmColors.line)
    }
    if (exitAsk) CloseDialog(s, onDismiss = { exitAsk = false }, onConfirm = { exitAsk = false; actions.slash("exit", null) })
    if (ctxSheet) s.context?.let { pct -> ContextSheet(pct, it.pixelbox.cmwatch.rules.ContextActions.wider(s.copy(model = model), choices), actions) { ctxSheet = false } }
    // Il pannello Modello ed effort, dalla pillola (bozza approvata da Franz il 07/10 alle 00:06).
    if (picker != null && choices != null) TunePanel(choices, model?.id, effort, advice, onModel = actions.setModel, onEffort = actions.setEffort) { picker = null }
}

/** La proposta del contesto pieno sopra il campo (contratto 1.37): «Contesto al 64%», «Fallo», ×. */
@Composable
private fun ContextNudge(pct: Int, onGo: () -> Unit, onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp).clip(RoundedCornerShape(20.dp)).background(CmColors.surfaceHigh).padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.Layers, null, tint = CmColors.briefWarn, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.ctx_nudge, pct), style = MaterialTheme.typography.labelLarge, color = CmColors.text, maxLines = 1)
            Text(stringResource(R.string.ctx_nudge_sub), style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1)
        }
        FilledTonalButton(onClick = onGo) { Text(stringResource(R.string.ctx_nudge_go)) }
        IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = CmColors.text2) }
    }
}

/**
 * Il foglio del contesto: «Scrivi l'handoff e riparti pulita» (pieno dall'80 %, `ContextActions.urgent`), «Compatta» e,
 * se il modello ha una finestra più grande fra le scelte, «Passa alla finestra da 1M». Sono prompt normali alla sessione.
 */
@Composable
private fun ContextSheet(pct: Int, wider: it.pixelbox.cmwatch.contract.Model?, actions: SheetActions, onClose: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    ModalBottomSheet(onDismissRequest = onClose, containerColor = CmColors.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.ctx_title, pct), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text)
            // Contratto 1.37: «Handoff, poi /clear», pieno dal 60 % (mockup approvato il 05/10 21:07); le voci spiegate sotto.
            val handoff = { actions.handoff(); onClose() }
            val two: @Composable (Int, Int) -> Unit = { title, sub ->
                Column(Modifier.fillMaxWidth()) {
                    Text(stringResource(title))
                    Text(stringResource(sub), style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(.8f))
                }
            }
            if (it.pixelbox.cmwatch.rules.ContextActions.band(pct) != null) {
                Button(onClick = handoff, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary)) { two(R.string.ctx_handoff_clear, R.string.ctx_handoff_clear_sub) }
            } else FilledTonalButton(onClick = handoff, modifier = Modifier.fillMaxWidth()) { two(R.string.ctx_handoff_clear, R.string.ctx_handoff_clear_sub) }
            FilledTonalButton(onClick = { actions.send(PhonePrimary.Target.PROMPT, "/compact"); onClose() }, modifier = Modifier.fillMaxWidth()) { two(R.string.ctx_compact, R.string.ctx_compact_sub) }
            wider?.let { m -> FilledTonalButton(onClick = { actions.setModel(m.id); onClose() }, modifier = Modifier.fillMaxWidth()) { two(R.string.ctx_wider, R.string.ctx_wider_sub) } }
        }
    }
}

/**
 * Le opzioni di una domanda, uguali dovunque (chat, fila «Ti aspettano», «Per te»; consulenza del 02/10): la prima piena
 * se è l'azione della schermata, le altre tonali; con il rischio alto la risposta vuole la pressione lunga
 * (`QuestionRules.needsLongPress`), e il tocco breve lo ricorda.
 */
@Composable
internal fun QuestionOptions(
    q: it.pixelbox.cmwatch.contract.Question, firstFilled: Boolean, holdHint: Boolean, onHold: () -> Unit, onAnswer: (Int) -> Unit,
    /** Il fondo delle opzioni non piene; sulle card del riepilogo un velo del primario, che resta visibile. */
    tonal: androidx.compose.ui.graphics.Color = CmColors.surface,
) {
    val long = QuestionRules.needsLongPress(q.tier)
    if (long) Text(
        stringResource(if (holdHint) R.string.question_hold else R.string.question_high_risk),
        style = MaterialTheme.typography.labelLarge, color = if (holdHint) CmColors.waiting else CmColors.briefAlert,
    )
    // Righe B (Franz, 03/10 15:18: tasti sproporzionati): 44 dp; due o tre opzioni brevi in parti uguali su una riga.
    val inline = QuestionRules.inline(q.options)
    val button: @Composable (Int, it.pixelbox.cmwatch.contract.Option, Modifier, androidx.compose.ui.graphics.Shape) -> Unit = { i, o, m, shape ->
        OptionButton(
            QuestionRules.optionLabel(o), filled = i == 0 && firstFilled,
            onClick = { if (long) onHold() else onAnswer(o.n) },
            onLongClick = if (long) ({ onAnswer(o.n) }) else null,
            modifier = m, shape = shape, center = inline, tonal = tonal,
        )
    }
    if (inline) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        val last = q.options.lastIndex
        q.options.forEachIndexed { i, o ->
            val start = if (i == 0) 22.dp else 6.dp
            val end = if (i == last) 22.dp else 6.dp
            button(i, o, Modifier.weight(1f), RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end))
        }
    } else q.options.forEachIndexed { i, o -> button(i, o, Modifier.fillMaxWidth(), CircleShape) }
}

/** Un'opzione della domanda: piena la prima, tonali le altre; con il rischio alto risponde solo la pressione lunga. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OptionButton(
    label: String, filled: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier.fillMaxWidth(), shape: androidx.compose.ui.graphics.Shape = CircleShape, center: Boolean = false,
    tonal: androidx.compose.ui.graphics.Color = CmColors.surface,
) {
    Surface(
        color = if (filled) CmColors.primary else tonal, contentColor = if (filled) CmColors.onPrimary else CmColors.text,
        shape = shape,
        modifier = modifier.heightIn(min = 44.dp).clip(shape).handCursor().combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), contentAlignment = if (center) Alignment.Center else Alignment.CenterStart) {
            Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize), maxLines = if (center) 1 else Int.MAX_VALUE, overflow = androidx.compose.ui.text.style.TextOverflow.Clip)
        }
    }
}

/**
 * Una tabella del testo (Franz, 02/10 21:11): stretta (`MarkdownTable.compact`) resta una griglia con l'intestazione in
 * grassetto; larga diventa una scheda per riga, titolo dalla prima colonna e «intestazione: valore» per le altre.
 */
@Composable
private fun TableBlock(t: it.pixelbox.cmwatch.rules.MarkdownTable.Table) {
    if (it.pixelbox.cmwatch.rules.MarkdownTable.compact(t)) Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            t.header.forEach { h -> Text(linked(h), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text2, modifier = Modifier.weight(1f)) }
        }
        t.rows.forEach { r ->
            HorizontalDivider(color = CmColors.line)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                r.forEach { c -> Text(linked(c), style = MaterialTheme.typography.bodyMedium, color = CmColors.text, modifier = Modifier.weight(1f)) }
            }
        }
    } else Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        it.pixelbox.cmwatch.rules.MarkdownTable.cards(t).forEach { c ->
            Surface(color = CmColors.surface, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (c.title.isNotBlank()) Text(linked(c.title), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text)
                    c.lines.forEach { line ->
                        val head = line.substringBefore(": ")
                        Text(
                            androidx.compose.ui.text.buildAnnotatedString {
                                pushStyle(androidx.compose.ui.text.SpanStyle(color = CmColors.text2)); append("$head: "); pop()
                                append(linked(line.substringAfter(": ")))
                            },
                            style = MaterialTheme.typography.bodyMedium, color = CmColors.text,
                        )
                    }
                }
            }
        }
    }
}

/** I link `http(s)://` del testo toccabili, nel colore delle azioni e sottolineati; il tocco apre il browser (Franz, 01/10 20:05). */
internal fun linked(text: String): AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
    // La formattazione del markdown (Franz, 01/10 20:17: «**prova**» si vedeva con gli asterischi): il testo senza i
    // segni, poi grassetto, corsivo, codice in monospazio e link con testo; infine i link nudi.
    val md = it.pixelbox.cmwatch.rules.Markdown.parse(text)
    val plain = md.text
    append(plain)
    val link = androidx.compose.ui.text.TextLinkStyles(androidx.compose.ui.text.SpanStyle(color = CmColors.actionIcon, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
    val linked = mutableListOf<IntRange>()
    md.spans.forEach { sp ->
        val (a, b) = sp.range.first to sp.range.last + 1
        when (sp.kind) {
            it.pixelbox.cmwatch.rules.Markdown.Kind.BOLD -> addStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.SemiBold), a, b)
            it.pixelbox.cmwatch.rules.Markdown.Kind.ITALIC -> addStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), a, b)
            it.pixelbox.cmwatch.rules.Markdown.Kind.CODE -> addStyle(androidx.compose.ui.text.SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, background = CmColors.surfaceHigh), a, b)
            it.pixelbox.cmwatch.rules.Markdown.Kind.LINK -> { addLink(androidx.compose.ui.text.LinkAnnotation.Url(sp.url!!, link), a, b); linked += sp.range }
        }
    }
    it.pixelbox.cmwatch.rules.Links.find(plain).filter { r -> linked.none { l -> r.first <= l.last && l.first <= r.last } }.forEach { r ->
        addLink(androidx.compose.ui.text.LinkAnnotation.Url(plain.substring(r), link), r.first, r.last + 1)
    }
}

/** Testo con il tasto ▶ accanto (regola di Franz 12/09: testi lunghi, esiti, risposte e domande). */
@Composable
fun Speakable(text: String, speak: Boolean, onSpeak: (String) -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Text(linked(text), style = MaterialTheme.typography.bodyLarge, color = CmColors.text, modifier = Modifier.weight(1f))
        val reading = LocalSpeaking.current == text
        if (speak && reading) RatePill()
        if (speak) IconButton(onClick = { onSpeak(text) }) {
            Icon(if (reading) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, stringResource(if (reading) R.string.stop_reading else R.string.read_aloud), tint = CmColors.actionIcon)
        }
    }
}

/** Una pillola compatta e toccabile per il modello (contratto 1.12); la freccia solo se si può cambiare. */
@Composable
private fun TunePill(label: String, enabled: Boolean, dot: Boolean = false, onClick: () -> Unit) {
    Box {
        Surface(onClick = onClick, enabled = enabled, color = CmColors.surface, shape = CircleShape) {
            Row(Modifier.padding(start = 12.dp, end = if (enabled) 6.dp else 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = PillText, color = CmColors.text, maxLines = 1, softWrap = false)
                if (enabled) Icon(Icons.Rounded.ArrowDropDown, null, tint = CmColors.text2, modifier = Modifier.size(18.dp))
            }
        }
        // Contratto 1.37: c'è un consiglio diverso dalla scelta di adesso (mockup approvato il 05/10 21:07).
        if (dot) Box(Modifier.align(Alignment.TopEnd).size(10.dp).background(CmColors.bg, CircleShape).padding(2.dp).background(CmColors.advice, CircleShape))
    }
}

/** «CONSIGLIATO» accanto al modello che fable-director consiglia (contratto 1.37), nel pannello Modello ed effort. */
@Composable
internal fun AdviceTag() = Text(
    stringResource(R.string.advice_tag).uppercase(), style = MonoSmall.copy(color = CmColors.advice, fontWeight = FontWeight.SemiBold),
    modifier = Modifier.clip(CircleShape).background(CmColors.advice.copy(alpha = .14f)).padding(horizontal = 9.dp, vertical = 3.dp),
)

/** Il testo delle pillole della testata: 13 sp, così le tre stanno su una riga di telefono (variante A, 09/10). */
private val PillText = androidx.compose.ui.text.TextStyle(fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp), fontWeight = FontWeight.Medium, letterSpacing = androidx.compose.ui.unit.TextUnit(0.1f, androidx.compose.ui.unit.TextUnitType.Sp))

/** La forma della pillola della quota, dalla più lunga: «5h 4% · 20:00», «4% · 20:00», «4%». */
internal enum class QuotaForm { FULL, SHORT, TINY }

/**
 * La riga delle tre pillole (variante A, 09/10 17:50): modello, quota, contesto, poi il menu ⋮ in fondo a destra. La quota
 * prende la forma più lunga che ci sta: col carattere grande o su uno schermo stretto si accorcia invece di andare a capo.
 * `fill` = tutta la larghezza (il menu a destra); senza, la larghezza che serve (sul tablet il nome sta a sinistra).
 */
@Composable
private fun MeterPills(
    fill: Boolean, tune: @Composable () -> Unit, quota: (@Composable (QuotaForm) -> Unit)?, context: (@Composable () -> Unit)?,
    more: @Composable () -> Unit,
) {
    androidx.compose.ui.layout.Layout(
        content = {
            Box { tune() }
            QuotaForm.entries.forEach { f -> Box { quota?.invoke(f) } }
            Box { context?.invoke() }
            Box { more() }
        },
    ) { ms, c ->
        val gap = 6.dp.roundToPx()
        val loose = c.copy(minWidth = 0, minHeight = 0)
        val tuneP = ms[0].measure(loose)
        val quotas = (1..3).map { ms[it].measure(loose) }
        val ctxP = ms[4].measure(loose)
        val moreP = ms[5].measure(loose)
        val parts = { q: androidx.compose.ui.layout.Placeable -> listOf(tuneP, q, ctxP).filter { it.width > 0 } }
        val need = { q: androidx.compose.ui.layout.Placeable -> parts(q).sumOf { it.width } + gap * parts(q).size + moreP.width }
        // Sul tablet il nome sta a sinistra e prende il resto: le pillole non oltre due terzi della riga.
        val room = if (fill) c.maxWidth else c.maxWidth * 2 / 3
        val q = quotas.firstOrNull { need(it) <= room } ?: quotas.last()
        val shown = parts(q)
        val h = (shown + moreP).maxOf { it.height }
        val w = if (fill && c.hasBoundedWidth) c.maxWidth else minOf(need(q), c.maxWidth)
        layout(w, h) {
            var x = 0
            shown.forEach { p -> p.placeRelative(x, (h - p.height) / 2); x += p.width + gap }
            moreP.placeRelative(w - moreP.width, (h - moreP.height) / 2)
        }
    }
}

/** La pillola del contesto: anellino nel colore delle soglie (`SessionMeters`) e «ctx 74%»; il tocco apre il suo foglio. */
@Composable
private fun ContextPill(pct: Int, enabled: Boolean, onClick: () -> Unit) {
    val tone = when (SessionMeters.contextTone(pct)) {
        it.pixelbox.cmwatch.rules.BriefCards.Tone.ALERT -> CmColors.briefAlertRing
        it.pixelbox.cmwatch.rules.BriefCards.Tone.WARN -> CmColors.briefWarn
        else -> CmColors.briefRing
    }
    val frac = SessionMeters.contextFraction(pct) ?: 0f
    MeterPill(enabled, onClick) {
        MeterRing(frac, tone)
        // «ctx» davanti alla percentuale (Franz, 04/10 20:43), come «5h» davanti alla quota.
        Text(stringResource(R.string.ctx_short, pct), style = PillText, color = CmColors.text, maxLines = 1, softWrap = false)
    }
}

/** Il fondo comune delle pillole: stessa forma, stessa altezza della pillola del modello. */
@Composable
private fun MeterPill(enabled: Boolean, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Surface(onClick = onClick, enabled = enabled, color = CmColors.surface, shape = CircleShape) {
        Row(Modifier.padding(start = 8.dp, end = 10.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp), content = content)
    }
}

/** L'anello delle misure: il binario e l'arco della frazione nel colore del tono. */
@Composable
private fun MeterRing(frac: Float, tone: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
        val w = 2.5.dp.toPx()
        val inset = w / 2
        val sz = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
        drawArc(CmColors.briefTrack, -90f, 360f, false, topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = sz, style = androidx.compose.ui.graphics.drawscope.Stroke(w))
        drawArc(tone, -90f, 360f * frac, false, topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = sz, style = androidx.compose.ui.graphics.drawscope.Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

/**
 * La quota delle 5 ore in testata (Franz, 04/10 20:43: «manca l'info di data e ora azzeramento»): anellino, «5h 4%» e l'ora in
 * cui si azzera sulla stessa riga, col giorno se non è oggi. Un dato vecchio nel colore dell'attesa. Il tocco porta a Utilizzo.
 */
@Composable
private fun QuotaPill(pct: Int, resetAt: Long?, stale: Boolean, now: Long, form: QuotaForm, onClick: () -> Unit) {
    val tone = if (stale) CmColors.waiting else when (SessionMeters.quotaTone(pct)) {
        it.pixelbox.cmwatch.rules.BriefCards.Tone.ALERT -> CmColors.briefAlertRing
        it.pixelbox.cmwatch.rules.BriefCards.Tone.WARN -> CmColors.briefWarn
        else -> CmColors.briefRing
    }
    val zone = ZoneId.systemDefault()
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val reset = resetAt?.takeIf { it > now }?.let { r ->
        java.time.format.DateTimeFormatter.ofPattern(if (SessionMeters.resetNeedsDay(r, now, zone)) "EEE HH:mm" else "HH:mm", locale)
            .format(Instant.ofEpochSecond(r).atZone(zone))
    }
    val desc = reset?.let { stringResource(R.string.quota_reset_desc, pct, it) } ?: stringResource(R.string.quota_h5_short, pct)
    val label = when {
        form == QuotaForm.FULL && reset != null -> stringResource(R.string.quota_h5_short, pct) + " · " + reset
        form == QuotaForm.FULL -> stringResource(R.string.quota_h5_short, pct)
        form == QuotaForm.SHORT && reset != null -> "$pct% · $reset"
        else -> "$pct%"
    }
    Box(Modifier.semantics(mergeDescendants = true) { contentDescription = desc }) {
        MeterPill(true, onClick) {
            MeterRing((pct / 100f).coerceIn(0f, 1f), tone)
            Text(label, style = PillText, color = if (stale) CmColors.waiting else CmColors.text, maxLines = 1, softWrap = false)
        }
    }
}

/** Quante immagini si possono mandare insieme: ognuna è un `report`, e il relay le esegue una per volta. */
private const val MAX_IMAGES = 5

/** Il testo che la voce sta leggendo: il suo tasto diventa Stop. Fornito da `MainActivity` da `Speech.speaking`. */
val LocalSpeaking = androidx.compose.runtime.staticCompositionLocalOf<String?> { null }

/** Lo zoom del testo della conversazione (`ChatZoom`) e come cambiarlo, da `MainActivity`. */
val LocalChatZoom = androidx.compose.runtime.staticCompositionLocalOf { 1f }
val LocalSetChatZoom = androidx.compose.runtime.staticCompositionLocalOf<(Float) -> Unit> { {} }

/**
 * La conversazione si ingrandisce con due dita (Franz, 03/10 21:16): cambia la grandezza dei caratteri, che vanno a capo da
 * soli. Con un dito solo lo scorrimento resta della lista; con due il gesto è dello zoom.
 */
@Composable
private fun ZoomedText(content: @Composable () -> Unit) {
    val zoom = LocalChatZoom.current
    val current by androidx.compose.runtime.rememberUpdatedState(zoom)
    val setZoom by androidx.compose.runtime.rememberUpdatedState(LocalSetChatZoom.current)
    val d = androidx.compose.ui.platform.LocalDensity.current
    Box(Modifier.fillMaxSize().pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
            do {
                val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                if (event.changes.count { it.pressed } >= 2) {
                    val z = event.calculateZoom()
                    if (z != 1f) setZoom(it.pixelbox.cmwatch.rules.ChatZoom.clamp(current * z))
                    event.changes.forEach { c -> c.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(d.density, d.fontScale * zoom),
        ) { content() }
    }
}

/** La velocità della voce e come cambiarla mentre legge (`Speech.rate`, `Speech.setRateNow`), da `MainActivity`. */
val LocalSpeechRate = androidx.compose.runtime.staticCompositionLocalOf { it.pixelbox.cmwatch.rules.SpeechRate.NORMAL }

/**
 * Il campo che ha il cursore e quello che deve prenderlo (Franz, 04/10 20:53: cambiando sessione con lo swipe il testo
 * finiva nella sessione di prima). Il pager tiene viva la pagina che ha il fuoco anche fuori dallo schermo: chi scorre
 * sposta il fuoco sul campo della pagina che si vede.
 */
class FieldFocus {
    /** La sessione il cui campo ha il cursore adesso. */
    var owner by mutableStateOf<String?>(null)
    /** La sessione il cui campo deve prendere il cursore appena c'è. */
    var target by mutableStateOf<String?>(null)
}
val LocalFieldFocus = androidx.compose.runtime.staticCompositionLocalOf<FieldFocus?> { null }
val LocalSetSpeechRate = androidx.compose.runtime.staticCompositionLocalOf<(Float) -> Unit> { {} }

/**
 * La pillola «1,25×» accanto a ■ durante la lettura (Franz, 03/10 21:16). Dal 08/10 (19:50) il tocco passa alla velocità
 * dopo (`SpeechRate.next`) e la voce la usa subito; tenuta premuta apre lo slider nella barra di lettura.
 */
@Composable
fun RatePill() {
    val overlay = LocalReadingOverlay.current
    val rate = LocalSpeechRate.current
    val setRate = LocalSetSpeechRate.current
    RatePill(rate, onClick = { setRate(it.pixelbox.cmwatch.rules.SpeechRate.next(rate)) }, onLongClick = {
        overlay?.let { it.rateOpen = true; it.rateTouched = System.currentTimeMillis() }
    })
}

/** La stessa pillola con velocità, tocco e pressione lunga espliciti: la barra di lettura sta fuori dal provider della lettura. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RatePill(rate: Float, onClick: () -> Unit, onLongClick: () -> Unit, interactive: Boolean = true) {
    val label = rateLabel(rate)
    val desc = stringResource(R.string.speech_rate_change, label)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Surface(
        color = CmColors.surface, shape = CircleShape,
        modifier = Modifier.clip(CircleShape).handCursor().semantics { contentDescription = desc }
            .then(if (interactive) Modifier.combinedClickable(onClick = onClick, onLongClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); onLongClick() }) else Modifier),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = CmColors.text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

/** Il paragrafo che la voce sta leggendo (`Speech.block`) e come dividere un testo in paragrafi (`Speech.blocksOf`). */
val LocalSpeakingBlock = androidx.compose.runtime.staticCompositionLocalOf<Int?> { null }
val LocalBlocksOf = androidx.compose.runtime.staticCompositionLocalOf<(String) -> List<it.pixelbox.cmwatch.rules.AnswerText.Block>> {
    { t: String -> listOf(it.pixelbox.cmwatch.rules.AnswerText.Block(it.pixelbox.cmwatch.rules.AnswerText.Kind.PARA, t)) }
}

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
    // Lampeggio graduale, non acceso/spento (Franz, 01/10 18:13): l'opacità segue un coseno di 1,2 s calcolato a mano,
    // perché con le scale a zero le animazioni di Compose saltano subito al valore finale.
    val fade by androidx.compose.runtime.produceState(1f, still) {
        if (!still) while (true) {
            val t = (System.currentTimeMillis() % 1_200) / 1_200.0
            value = 0.3f + 0.7f * (0.5f + 0.5f * kotlin.math.cos(2 * Math.PI * t).toFloat())
            kotlinx.coroutines.delay(33)
        }
    }
    // Gira e pulsa mentre la sessione elabora (Franz, 01/10 06:44: «il simbolo accanto alla riga di stato pulsante»).
    val motion = if (off) null else androidx.compose.animation.core.rememberInfiniteTransition(label = "spin")
    // Stati letti solo nel livello dell'asterisco (Franz, 05/10 12:30): letti qui ricomponevano la riga a ogni fotogramma.
    val spinState = motion?.animateFloat(0f, 360f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2400, easing = androidx.compose.animation.core.LinearEasing)), label = "a")
    val pulseState = motion?.animateFloat(0.7f, 1.15f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(700), androidx.compose.animation.core.RepeatMode.Reverse), label = "p")
    val secs = s.turnStarted?.let { (nowS - it).coerceAtLeast(0) }
    val what = s.toolNote?.takeIf { it.isNotBlank() } ?: s.tool?.takeIf { it.isNotBlank() } ?: stringResource(R.string.live_thinking)
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.foundation.Canvas(Modifier.size(18.dp).graphicsLayer { val spin = spinState?.value ?: 0f; val pulse = pulseState?.value ?: 1f; rotationZ = spin; scaleX = pulse; scaleY = pulse; alpha = if (motion == null) fade else 0.55f + 0.45f * ((pulse - 0.7f) / 0.45f) }) {
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

