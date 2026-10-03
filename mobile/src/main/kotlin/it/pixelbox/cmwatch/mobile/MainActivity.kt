package it.pixelbox.cmwatch.mobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.mobile.pair.Phase
import it.pixelbox.cmwatch.mobile.ui.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.pairing.PairingRecord
import it.pixelbox.cmwatch.rules.AppLanguage
import it.pixelbox.cmwatch.rules.PhoneDiary
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.rules.ChatFeed
import it.pixelbox.cmwatch.rules.ChatRules
import it.pixelbox.cmwatch.rules.Tune
import it.pixelbox.cmwatch.rules.PhoneBoard
import it.pixelbox.cmwatch.rules.PhoneOverview
import it.pixelbox.cmwatch.rules.Sent
import it.pixelbox.cmwatch.rules.Slash
import it.pixelbox.cmwatch.rules.PhoneTerminal
import it.pixelbox.cmwatch.rules.TerminalLive
import it.pixelbox.cmwatch.rules.StartRoute
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.rules.PhonePrimary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val app get() = application as PhoneApp
    private val speech by lazy { Speech(this) }
    private val localeManager by lazy { getSystemService(android.app.LocaleManager::class.java) }
    /**
     * Un avvio nuovo cambia l'id, una rotazione lo tiene (restyling 30/09): sotto `key(launchId)` lo stato salvato di
     * schede e scheda aperta si ritrova solo dopo una rotazione, non quando il sistema ricrea l'app uccisa in background.
     */
    private var launchId by mutableStateOf("")
    /** Quando l'app è uscita di scena, non per una rotazione: al ritorno dopo un'assenza lunga si riparte dalla Panoramica. */
    private var stoppedAt: Long? = null
    /** Una notifica con domanda chiede la coda «Ti aspettano» (piano 30/09, Task 1); `Main` la apre e la azzera. */
    private var queueAsked by mutableStateOf(false)
    /** Dal widget: il nome della sessione da aprire, "" per la Panoramica, null per niente. */
    private var sessionAsked by mutableStateOf<String?>(null)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) intent?.let(::route)
        val rotating = savedInstanceState?.getBoolean(KEY_ROTATING) == true
        launchId = savedInstanceState?.getString(KEY_LAUNCH)?.takeIf { rotating } ?: java.util.UUID.randomUUID().toString()
        enableEdgeToEdge()
        setContent {
            CmPhoneTheme {
                val settings by app.prefs.flow.collectAsStateWithLifecycle(initialValue = null)
                val ui by app.pairing.ui.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()
                var paste by remember { mutableStateOf(false) }
                // Dopo un riavvio per un altro progetto Firebase si riprende il QR salvato.
                LaunchedEffect(Unit) { app.pairing.resume() }
                LaunchedEffect(ui.phase) {
                    when (ui.phase) {
                        Phase.RESTART -> Restarter.restart(this@MainActivity)
                        Phase.DONE -> { Buzz.done(this@MainActivity); app.reconfigure() }
                        else -> Unit
                    }
                }
                fun scan() {
                    scope.launch {
                        when (val r = Scanner.scan(this@MainActivity)) {
                            is Scanner.Result.Read -> app.pairing.run(r.text)
                            Scanner.Result.Unavailable -> paste = true
                            Scanner.Result.Cancelled -> Unit
                        }
                    }
                }
                // Indietro da un errore: si torna alla schermata di prima, niente resta a metà.
                BackHandler(enabled = ui.phase == Phase.FAILED) { app.pairing.reset() }
                val s = settings ?: return@CmPhoneTheme
                when {
                    ui.phase != Phase.IDLE -> PairingScreen(
                        ui,
                        onRetry = { scope.launch { app.pairing.retry() } },
                        onRescan = ::scan,
                        onWithoutWatch = { scope.launch { app.pairing.retry(withoutWatch = true) } },
                        onInstallOnWatch = { scope.launch { app.pairing.installOnWatch() } },
                        onDone = { app.pairing.reset() },
                    )
                    s.paired || s.demoMode -> key(launchId) {
                        Box(androidx.compose.ui.Modifier.fillMaxSize()) {
                            Main(s.demoMode, s.ttsMinChars, s.host, s.pairingJson, ::scan)
                            RefusalBanner(app.repo.userResults, androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.BottomCenter))
                        }
                    }
                    else -> NotPairedScreen(onPair = ::scan, onPaste = { paste = true }, onDemo = { app.setDemo(true) })
                }
                if (paste) PasteDialog(onPair = { t -> paste = false; scope.launch { app.pairing.run(t) } }, onDismiss = { paste = false })
            }
        }
    }

    /** L'app accoppiata, o in Demo: tre schede (restyling 30/09), scheda sessione, terminale, Lancia, impostazioni. */
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
    @Composable
    private fun Main(demo: Boolean, ttsMinChars: Int, host: String?, pairingJson: String?, onRepair: () -> Unit) {
        val snap by app.repo.snapshot.collectAsStateWithLifecycle()
        // Il permesso delle notifiche si chiede solo accoppiati, mai in Demo.
        val askNotifications = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(demo) {
            if (!demo && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                askNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val events by app.repo.events.collectAsStateWithLifecycle()
        // Un comando rifiutato dal PC si dice in chiaro, con il motivo intero del relay (spec 29/09, «Errori»): `RefusalBanner`.
        val results by app.repo.resultsById.collectAsStateWithLifecycle()
        // Modello ed effort scelti dal telefono, per sessione: «nome/model», «nome/effort» (`Tune`).
        val tunePicks = remember { androidx.compose.runtime.mutableStateMapOf<String, Tune.Pick>() }
        var refreshing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val samples by app.repo.quotaSamples.collectAsStateWithLifecycle()
        val chatLog by app.chatLog.messages.collectAsStateWithLifecycle()
        val uploads by app.repo.uploads.collectAsStateWithLifecycle()
        var tab by rememberSaveable { mutableStateOf(StartRoute.tab(restored = null)) }
        var open by rememberSaveable { mutableStateOf<String?>(null) }       // nome della sessione aperta
        var terminal by rememberSaveable { mutableStateOf<String?>(null) }   // nome della sessione del terminale
        var screenId by rememberSaveable { mutableStateOf<String?>(null) }
        var settingsOpen by rememberSaveable { mutableStateOf(false) }
        var launching by rememberSaveable { mutableStateOf(false) }
        var nightAdding by rememberSaveable { mutableStateOf(false) }
        var queueOpen by rememberSaveable { mutableStateOf(false) }
        // La Panoramica in un foglio dal basso sopra la scheda (Franz, 01/10 12:34): si guarda la quota e si torna.
        var overviewSheet by rememberSaveable { mutableStateOf(false) }
        var searchOpen by rememberSaveable { mutableStateOf(false) }
        // Gli avvisi delle altre sessioni già visti o chiusi (`Elsewhere`): un turno finito si dice una volta sola.
        val elsewhereSeen = remember { mutableStateListOf<String>() }
        LaunchedEffect(sessionAsked) {
            val n = sessionAsked ?: return@LaunchedEffect
            settingsOpen = false; terminal = null; queueOpen = false; searchOpen = false
            if (n.isEmpty()) { open = null; tab = StartRoute.Tab.OVERVIEW } else { open = n }
            sessionAsked = null
        }
        LaunchedEffect(queueAsked) { if (queueAsked) { queueOpen = true; searchOpen = false; settingsOpen = false; terminal = null; queueAsked = false } }
        var now by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
        LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() / 1000 } }
        val state = snap.state

        BackHandler(enabled = settingsOpen || terminal != null || queueOpen || searchOpen) {
            when {
                searchOpen -> searchOpen = false
                settingsOpen -> settingsOpen = false
                terminal != null -> { terminal = null; screenId = null }
                else -> queueOpen = false
            }
        }
        // Il gesto indietro dalla scheda mostra la regia mentre lo si trascina: la scheda si restringe verso la card.
        val seek = remember { SeekableTransitionState(open) }
        // Sempre fino in fondo: dopo un gesto completato seekTo ha già messo il bersaglio a null (revisione 29/09).
        LaunchedEffect(open) { seek.animateTo(open) }
        PredictiveBackHandler(enabled = open != null && !settingsOpen && terminal == null) { progress ->
            try {
                progress.collect { seek.seekTo(it.progress, targetState = null) }
                open = null
            } catch (e: CancellationException) {
                withContext(NonCancellable) { seek.animateTo(open) }
            }
        }
        val flight = rememberTransition(seek, label = "fly")
        if (settingsOpen) {
            val r = PairingRecord.fromJson(pairingJson)
            SettingsScreen(
                host = host, phoneName = app.phoneName, watchName = r?.watchName, watchPending = r?.watchPending == true, demo = demo,
                version = remember { packageManager.getPackageInfo(packageName, 0).versionName.orEmpty() },
                onRepair = onRepair, onDemo = { app.setDemo(it) },
                onNotifications = { startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)) },
                onPrivacy = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_url)))) },
                language = AppLanguage.fromTags(localeManager.applicationLocales.toLanguageTags()),
                // Il sistema ricrea l'activity nella lingua nuova; con «come il telefono» la lista vuota torna a seguirlo.
                onLanguage = { localeManager.applicationLocales = android.os.LocaleList.forLanguageTags(it.tag) },
                voices = speech.voices.collectAsStateWithLifecycle().value, voice = speech.voice.collectAsStateWithLifecycle().value,
                onVoice = speech::setVoice, onTryVoice = { speech.toggle(getString(R.string.voice_sample)) },
                rate = speech.rate.collectAsStateWithLifecycle().value, onRate = speech::setRate,
            )
            return
        }
        terminal?.let { name ->
            // Dal vivo, come sull'orologio (restyling 30/09): una lettura all'apertura, poi una ogni pochi secondi dopo la
            // risposta alla precedente, e subito quando la sessione cambia. Il ciclo muore con la schermata.
            var shown by remember(name) { mutableStateOf<String?>(null) }
            val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
            // Solo con l'app in primo piano (revisione 30/09): a schermo spento o in background nessuna lettura al PC.
            LaunchedEffect(name) { lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                var askedAt: Long? = null
                var prev = app.repo.snapshot.value.state?.sessions?.firstOrNull { it.name == name }
                while (true) {
                    val cur = app.repo.snapshot.value.state?.sessions?.firstOrNull { it.name == name }
                    val answered = screenId?.let { app.repo.resultsById.value[it] } != null
                    val t = System.currentTimeMillis()
                    val moved = answered && TerminalLive.next(prev, cur) != null
                    prev = cur
                    if (moved || PhoneTerminal.shouldAsk(cur, askedAt, answered, t)) {
                        askedAt = t
                        // Senza rete una lettura passiva non parte: si riprova al giro dopo, senza chiudere l'app.
                        runCatching { app.repo.command(CmdOp.SCREEN, name, null) }.onSuccess { screenId = it }
                    }
                    delay(1_000)
                }
            } }
            val text = screenId?.let { results[it]?.text }
            LaunchedEffect(text) { if (text != null) shown = text }
            TerminalScreen(name, text ?: shown, loading = screenId != null && text == null, onRefresh = {
                scope.launch { runCatching { app.repo.command(CmdOp.SCREEN, name, null) }.onSuccess { screenId = it } }
            })
            return
        }
        if (searchOpen) {
            SearchScreen(chatLog, events, onOpen = { n -> searchOpen = false; if (n != null) { open = n } else { open = null; tab = StartRoute.Tab.DIARY } })
            return
        }
        if (queueOpen && state != null) {
            // La coda usa solo la card della domanda: risposta, «Parliamone», «Consenti tutto» e la lettura a voce.
            QueueScreen(state, now, actionsFor = { s ->
                SheetActions(
                    answer = { n -> scope.launch { app.repo.answer(s.name, n) } },
                    allowAll = { scope.launch { app.repo.command(CmdOp.ALLOW_ALL, s.name, null) } },
                    send = { _, _ -> }, follow = {}, reopen = {}, terminal = {}, openInClaude = {},
                    speak = speech::toggle, retry = {},
                    chat = { scope.launch { app.repo.chat(s.name) }; queueOpen = false; open = s.name },
                )
            }, onSession = { n -> queueOpen = false; open = n })
            return
        }
        // Contratto 1.22: la conversazione della scheda aperta, a pagine, letta dal vivo finché la scheda resta aperta.
        val transcriptOk = !demo && state?.ops?.contains("transcript") == true
        // La casa della master (design 01/10): sulla prima scheda, senza schede aperte, la chat è quella della master.
        val masterName = it.pixelbox.cmwatch.rules.ContextActions.master(state)?.name
        val chatName = open ?: masterName?.takeIf { tab == StartRoute.Tab.OVERVIEW }
        // I resoconti della notte già ascoltati e i prossimi passi già avviati: spariscono da «Per te» e non tornano,
        // nemmeno dopo la coda, una rotazione o un riavvio (revisione finale 02/10: in memoria si perdevano).
        val forYouPrefs = remember { getSharedPreferences("for-you", MODE_PRIVATE) }
        val readReports = remember { androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(forYouPrefs.getStringSet("read", emptySet()).orEmpty()) } }
        val markRead: (String) -> Unit = { k -> if (k !in readReports) { readReports.add(k); forYouPrefs.edit().putStringSet("read", readReports.toSet()).apply() } }
        var entries by remember { mutableStateOf<List<it.pixelbox.cmwatch.contract.TranscriptEntry>>(emptyList()) }
        // L'ultima conversazione letta di ogni sessione: riaprendo compare subito, poi si aggiorna.
        val feedCache = remember { mutableStateMapOf<String, List<it.pixelbox.cmwatch.contract.TranscriptEntry>>() }
        var more by remember { mutableStateOf(false) }
        var olderId by remember { mutableStateOf<String?>(null) }
        var unsupported by remember { mutableStateOf(false) }
        val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(chatName, transcriptOk, unsupported) {
            entries = chatName?.let { feedCache[it] }.orEmpty(); more = false
            val name = chatName ?: return@LaunchedEffect
            if (!transcriptOk || unsupported) return@LaunchedEffect
            lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                var askedAt: Long? = null
                var pendingId: String? = null
                var mode = ChatFeed.Page.FRESH
                var prev = app.repo.snapshot.value.state?.sessions?.firstOrNull { it.name == name }
                while (true) {
                    val cur = app.repo.snapshot.value.state?.sessions?.firstOrNull { it.name == name }
                    pendingId?.let { app.repo.resultsById.value[it] }?.let { r ->
                        when {
                            r.ok -> runCatching { ContractJson.decodeTranscript(r.text) }.onSuccess { page ->
                                entries = ChatFeed.append(entries, page, mode)
                                feedCache[name] = entries
                                if (mode == ChatFeed.Page.FRESH) more = page.more
                            }
                            // L'ultima voce non c'è più (conversazione compattata): si riparte dalle ultime.
                            r.text.contains("no entry") -> entries = emptyList()
                            // Relay aggiornato ma servizio ancora vecchio: resta la chat dei messaggi mandati.
                            r.text.contains("not allowed") -> unsupported = true
                        }
                        // Anche una risposta vuota o un errore chiudono la rotella (segnalazione 02/10).
                        feedCache[name] = entries
                        pendingId = null
                    }
                    val answered = pendingId == null
                    val moved = answered && TerminalLive.next(prev, cur) != null
                    prev = cur
                    val t = System.currentTimeMillis()
                    // Letture diradate: ogni lettura costa al relay 5-8 s (dal vivo 30/09 23:00).
                    if (moved || PhoneTerminal.shouldAskChat(cur, askedAt, answered, t)) {
                        askedAt = t
                        mode = if (entries.isEmpty()) ChatFeed.Page.FRESH else ChatFeed.Page.AFTER
                        // Una lettura persa non resta fra i comandi in sospeso (revisione 30/09).
                        pendingId?.let { app.repo.forget(it) }
                        pendingId = runCatching { app.repo.command(CmdOp.TRANSCRIPT, name, ChatFeed.arg(ChatFeed.anchor(entries))) }.getOrNull()
                    }
                    delay(1_000)
                }
            }
        }
        // «Carica i messaggi precedenti»: una pagina `before` in testa.
        // Legata alla sessione aperta e con un limite: una pagina persa non blocca il tasto, e non finisce in un'altra
        // sessione (revisione 30/09).
        LaunchedEffect(chatName) { olderId = null }
        LaunchedEffect(chatName, olderId) {
            val id = olderId ?: return@LaunchedEffect
            val until = System.currentTimeMillis() + PhoneTerminal.LOST_MS
            while (System.currentTimeMillis() < until) {
                val r = app.repo.resultsById.value[id]
                if (r != null) {
                    if (r.ok) runCatching { ContractJson.decodeTranscript(r.text) }.onSuccess { page ->
                        entries = ChatFeed.append(entries, page, ChatFeed.Page.BEFORE); more = page.more
                    }
                    break
                }
                delay(500)
            }
            app.repo.forget(id)
            olderId = null
        }
        val speaking by speech.speaking.collectAsStateWithLifecycle()
        // Un prompt a una sessione, registrato nella sua chat come quelli scritti a mano.
        val sendPrompt: (String, String) -> Unit = { name, text ->
            scope.launch {
                runCatching { app.repo.prompt(name, text) }.getOrNull()?.let { id -> app.chatLog.add(Sent(id, name, text, System.currentTimeMillis() / 1000)) }
            }
        }
        val forYouAction: (it.pixelbox.cmwatch.rules.MasterHome.Row) -> Unit = { row ->
            when (row.kind) {
                // Variante 3 (02/10 21:11): la riga si apre sul posto; «Apri la conversazione» porta alla sessione.
                it.pixelbox.cmwatch.rules.MasterHome.Kind.QUESTION -> { open = row.session }
                it.pixelbox.cmwatch.rules.MasterHome.Kind.FINISHED -> { row.key?.let(markRead); open = row.session }
                it.pixelbox.cmwatch.rules.MasterHome.Kind.CONTEXT -> row.session?.let { n -> sendPrompt(n, getString(R.string.ctx_handoff_prompt)) }
                it.pixelbox.cmwatch.rules.MasterHome.Kind.NIGHT_REPORT -> { row.detail?.let { speech.toggle(it) }; row.key?.let(markRead) }
                it.pixelbox.cmwatch.rules.MasterHome.Kind.NIGHT -> nightAdding = true
                it.pixelbox.cmwatch.rules.MasterHome.Kind.NEXT_STEP -> {
                    val text = row.detail.orEmpty()
                    val n = row.session
                    markRead(it.pixelbox.cmwatch.rules.MasterHome.nextKey(row.title, text))
                    // Con la coda senza rete piena il comando si rifiuta: niente chiusura dell'app (revisione finale 02/10).
                    if (n != null) sendPrompt(n, text) else row.project?.let { p -> scope.launch { runCatching { app.repo.command(CmdOp.LAUNCH, null, p, text.ifBlank { null }) } } }
                }
                it.pixelbox.cmwatch.rules.MasterHome.Kind.SCHEDULED -> { open = row.session }
            }
        }
        // La pagina di una sessione (scheda e chat), usata dallo scorrimento fra le sessioni e dalla casa della master,
        // che le aggiunge «Per te» e il Quadro in cima (`top`).
        // Contratto 1.24: i file della chat si aprono dal telefono. Scaricati restano nella cache dell'app: un'immagine
        // compare come miniatura sotto il suo chip, gli altri file vanno all'app di sistema (Franz, 02/10 10:48).
        val fileLocal = remember { mutableStateMapOf<String, String>() }
        val fileLoading = remember { androidx.compose.runtime.mutableStateListOf<String>() }
        val openFile: (String, it.pixelbox.cmwatch.contract.TranscriptFile) -> Unit = { name, f ->
            val known = fileLocal[f.path]
            if (known != null) { if (f.mime?.startsWith("image/") != true) viewFile(java.io.File(known), f.mime) }
            else if (f.path !in fileLoading) {
                fileLoading.add(f.path)
                scope.launch {
                    val r = app.repo.openFile(name, f.path)
                    fileLoading.remove(f.path)
                    when (r) {
                        is it.pixelbox.cmwatch.data.Repo.Opened.Ok -> {
                            val out = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                val dir = java.io.File(cacheDir, "files").apply { mkdirs() }
                                java.io.File(dir, Integer.toHexString(f.path.hashCode()) + "-" + f.path.substringAfterLast('/')).apply { writeBytes(r.file.bytes) }
                            }
                            fileLocal[f.path] = out.path
                            if (!r.file.mime.startsWith("image/")) viewFile(out, r.file.mime)
                        }
                        is it.pixelbox.cmwatch.data.Repo.Opened.Refused -> android.widget.Toast.makeText(this@MainActivity, getString(R.string.file_refused, r.reason), android.widget.Toast.LENGTH_LONG).show()
                        it.pixelbox.cmwatch.data.Repo.Opened.Failed -> android.widget.Toast.makeText(this@MainActivity, getString(R.string.file_failed), android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        val sessionPage: @Composable (it.pixelbox.cmwatch.contract.Session, List<it.pixelbox.cmwatch.contract.TranscriptEntry>, (@Composable ColumnScope.((String) -> Unit, (String) -> Unit) -> Unit)?, Boolean, (@Composable () -> Unit)?) -> Unit = { session, pageEntries, home, header, dock ->
                        val nightDir = state?.takeIf { it.night.items != null }?.let { st -> ChatRules.nightDir(st, session) }
                        val quotaWarn = state?.let { st -> it.pixelbox.cmwatch.rules.QuotaWarning.of(st, session, samples[session.account].orEmpty(), now) }
                        // Variante A (Franz, 02/10 20:47): chi ti aspetta altrove, poi un turno finito; questo si chiude da sé in 6 s.
                        val elsewhere = state?.let { st -> it.pixelbox.cmwatch.rules.Elsewhere.alert(st, session.name, now, chatLog.map { m -> m.session }.toSet(), elsewhereSeen.toSet()) }
                        LaunchedEffect(elsewhere?.key) {
                            if (elsewhere is it.pixelbox.cmwatch.rules.Elsewhere.Finished) { delay(6_000); elsewhereSeen += elsewhere.key }
                        }
                        // I messaggi mandati restano nella chat con il loro stato (design 30/09, parte 3).
                        run {
                        fun sendAndLog(target: PhonePrimary.Target, text: String) = scope.launch {
                            val sentAt = System.currentTimeMillis() / 1000
                            val id = runCatching {
                                if (target == PhonePrimary.Target.ANSWER_TEXT) app.repo.answerText(session.name, text) else app.repo.prompt(session.name, text)
                            }.getOrElse {
                                // Coda senza rete piena: il messaggio non si finge partito (revisione 30/09).
                                android.widget.Toast.makeText(this@MainActivity, getString(R.string.queue_full), android.widget.Toast.LENGTH_LONG).show()
                                return@launch
                            }
                            // Una risposta continua il turno della domanda: è presa in carico appena consegnata.
                            app.chatLog.add(Sent(id, session.name, text, sentAt, startedAt = if (target == PhonePrimary.Target.ANSWER_TEXT) sentAt else null))
                        }
                        val rows = chatLog.filter { it.session == session.name }.sortedBy { it.sentAt }.map { m ->
                            val p = snap.pending.firstOrNull { it.cmd.id == m.id }?.status
                            val up = uploads[m.id]
                            ChatRow(m, ChatRules.status(m, p, results[m.id], session, up), ChatRules.reason(p, results[m.id], up, m))
                        }
                        // Un fallimento si salva sul messaggio: non si perde con i risultati in memoria né con un riavvio.
                        LaunchedEffect(rows.map { it.sent.id to it.status }) {
                            // Solo i rifiuti con un motivo: un'attesa senza risposta non è un fallimento (dal vivo 30/09 23:00).
                            rows.filter { it.status == ChatRules.Status.FAILED && it.sent.failed == null && it.reason != null }.forEach { r ->
                                app.chatLog.markFailed(r.sent.id, r.reason!!)
                            }
                            // Il pannello di un comando slash (/cost) arriva col risultato e si salva sul messaggio come il motivo.
                            rows.filter { it.sent.panel == null }.forEach { r ->
                                Slash.panel(r.sent, results[r.sent.id])?.let { p -> app.chatLog.markPanel(r.sent.id, p) }
                            }
                        }
                        CompositionLocalProvider(LocalFileOpener provides FileOpener({ f -> openFile(session.name, f) }, fileLoading.toSet(), fileLocal.toMap())) {
                        SessionSheet(session, now, snap.pending, ttsMinChars, SheetActions(
                            answer = { n -> scope.launch { app.repo.answer(session.name, n) } },
                            allowAll = { scope.launch { app.repo.command(CmdOp.ALLOW_ALL, session.name, null) } },
                            send = { target, text -> sendAndLog(target, text) },
                            follow = { on -> scope.launch { app.repo.command(if (on) CmdOp.FOLLOW else CmdOp.UNFOLLOW, session.name, null) } },
                            reopen = { scope.launch { app.repo.command(CmdOp.REOPEN, session.name, null) } },
                            terminal = { terminal = session.name; screenId = null },
                            openInClaude = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(session.link))) },
                            speak = speech::toggle,
                            // Un comando perso si riprova con lo stesso id; uno rifiutato dal PC si rimanda come nuovo.
                            retry = { id ->
                                scope.launch {
                                    val m = chatLog.firstOrNull { it.id == id }
                                    when {
                                        app.repo.snapshot.value.pending.any { it.cmd.id == id } -> app.repo.retry(id)
                                        // Un'immagine rifiutata si rimanda dalla sua copia locale, con lo stesso testo.
                                        m?.attachment != null -> attachImage(session.name, Uri.fromFile(java.io.File(m.attachment!!)), m.text, state?.share?.maxBytes ?: 0)
                                        m != null -> sendAndLog(PhonePrimary.Target.PROMPT, m.text)
                                    }
                                }
                            },
                            chat = { scope.launch { app.repo.chat(session.name) } },
                            // La scelta si vede subito, finché il PC non la riporta al turno dopo (segnalazione 01/10 20:25).
                            setModel = { v -> scope.launch {
                                val was = session.model?.id
                                runCatching { app.repo.command(CmdOp.MODEL, session.name, v) }.getOrNull()?.let { id ->
                                    tunePicks[session.name + "/model"] = Tune.Pick(id, System.currentTimeMillis() / 1000, model = state?.choices?.models?.firstOrNull { it.id == v }, was = was)
                                }
                            } },
                            setEffort = { v -> scope.launch {
                                val was = session.effort
                                runCatching { app.repo.command(CmdOp.EFFORT, session.name, v) }.getOrNull()?.let { id ->
                                    tunePicks[session.name + "/effort"] = Tune.Pick(id, System.currentTimeMillis() / 1000, effort = v, was = was)
                                }
                            } },
                            interrupt = { scope.launch { app.repo.command(CmdOp.INTERRUPT, session.name, null) } },
                            // Contratto 1.25: il comando resta nella chat come un messaggio, con l'esito del PC.
                            slash = { c, a -> scope.launch {
                                runCatching { app.repo.command(CmdOp.SLASH, session.name, c, a) }.getOrNull()?.let { id ->
                                    app.chatLog.add(Sent(id, session.name, "/$c" + (a?.let { t -> " $t" } ?: ""), System.currentTimeMillis() / 1000))
                                }
                            } },
                            attach = { uri, text -> attachImage(session.name, uri, text, state?.share?.maxBytes ?: 0) },
                            // Avviso quota (piano 30/09, Task 4): il testo resta nella chat come «parte alle …».
                            sendAtReset = { text -> quotaWarn?.let { w ->
                                val t = System.currentTimeMillis() / 1000
                                app.chatLog.add(Sent(java.util.UUID.randomUUID().toString(), session.name, text, t, scheduledFor = w.resetAt))
                                ScheduledSend.schedule(this@MainActivity, w.resetAt)
                                true
                            } ?: false },
                            sendTonight = { text -> nightDir?.let { dir ->
                                // L'avviso dopo il comando; una coda senza rete piena si dice invece di chiudere l'app.
                                scope.launch {
                                    val ok = runCatching { app.repo.command(CmdOp.NIGHT_ADD, null, dir, text) }.isSuccess
                                    android.widget.Toast.makeText(this@MainActivity, getString(if (ok) R.string.night_added else R.string.queue_full), android.widget.Toast.LENGTH_LONG).show()
                                }
                                true
                            } ?: false },
                            overview = { overviewSheet = true },
                            // Proposta approvata (01/10 21:19): la master legge la sessione e risponde nella sua chat.
                            speakFrom = { t, i -> speech.speakBlocks(t, i) },
                            askMaster = it.pixelbox.cmwatch.rules.ContextActions.master(state)?.takeIf { m -> m.name != session.name }?.let { m -> {
                                scope.launch {
                                    val text = getString(R.string.ask_master_prompt, session.name)
                                    runCatching { app.repo.prompt(m.name, text) }.getOrNull()?.let { id ->
                                        app.chatLog.add(Sent(id, m.name, text, System.currentTimeMillis() / 1000))
                                        android.widget.Toast.makeText(this@MainActivity, getString(R.string.ask_master_done), android.widget.Toast.LENGTH_SHORT).show()
                                        // La risposta arriva nella chat della master: ci si va subito (Franz, 03/10 15:25: «non funziona»,
                                        // la master rispondeva ma la risposta restava fuori vista).
                                        open = m.name
                                    }
                                }
                            } },
                        ), chat = rows, quota = quotaWarn, canTonight = nightDir != null,
                            // Niente frasi rapide (Franz, 01/10 23:34: «via» fisso, generico e fuori luogo): i consigli in più
                            // li scriverà la sessione stessa a fine turno.
                            choices = state?.choices, ops = state?.ops, canTune = !demo, canAttach = state?.share != null,
                            slash = state?.slash?.takeIf { state?.ops?.contains("slash") == true },
                            feed = if (transcriptOk && !unsupported && pageEntries.isNotEmpty()) ChatFeed.merge(pageEntries, rows.map { it.sent to it.status }, more) else null,
                            loadingFeed = transcriptOk && !unsupported && ChatFeed.loading(pageEntries, answered = session.name in feedCache),
                            more = more && session.name == chatName,
                            model = tunePicks[session.name + "/model"].let { p -> Tune.model(session, p, p?.let { results[it.cmd] }, now) },
                            effort = tunePicks[session.name + "/effort"].let { p -> Tune.effort(session, p, p?.let { results[it.cmd] }, now) },
                            home = home, grid = home != null, header = header, dock = dock,
                            // Sul riepilogo chi ti aspetta sta già nella lista: niente avviso doppio (ogni sessione una volta).
                            elsewhere = elsewhere.takeIf { dock == null },
                            onElsewhere = {
                                when (val a = elsewhere) {
                                    is it.pixelbox.cmwatch.rules.Elsewhere.Waiting -> if (a.sessions.size > 1) queueOpen = true else { open = a.sessions[0] }
                                    is it.pixelbox.cmwatch.rules.Elsewhere.Finished -> { elsewhereSeen += a.key; open = a.session }
                                    null -> {}
                                }
                            },
                            onElsewhereDismiss = { elsewhere?.let { a -> elsewhereSeen += a.key } },
                            onOlder = {
                                val first = entries.firstOrNull()?.id
                                if (first != null && olderId == null) scope.launch { olderId = runCatching { app.repo.command(CmdOp.TRANSCRIPT, session.name, ChatFeed.olderArg(first)) }.getOrNull() }
                            },
                        )
                        }
                        }
        }
        // Il riepilogo unico (design 03/10): ogni sessione una volta, nell'ordine del bisogno.
        val summary = state?.let { st ->
            remember(st, events, chatLog, now, readReports.toList()) {
                it.pixelbox.cmwatch.rules.Summary.build(st, events, chatLog, now, java.time.ZoneId.systemDefault(), readReports.toSet())
            }
        }
        var closedOpen by rememberSaveable { mutableStateOf(false) }
        // «Ha finito» sparisce quando apri la sessione, da qualunque strada: riga, menu in alto, scorrimento, avviso, ricerca.
        LaunchedEffect(open) {
            val n = open ?: return@LaunchedEffect
            summary?.rows?.firstOrNull { r -> r.session.name == n && r.key != null }?.key?.let(markRead)
        }
        // Il Registro si apre dal menu a tutto schermo; Indietro torna al riepilogo.
        BackHandler(enabled = tab == StartRoute.Tab.DIARY && open == null) { tab = StartRoute.Tab.OVERVIEW }
        // La pagina del riepilogo: lista, master agganciata sopra «Scrivi alla master», o «Riapri la master» se non c'è.
        val summaryPage: @Composable () -> Unit = summaryPage@{
            val st = state
            if (st == null || summary == null) {
                // Prima del primo stato la rotella: mai un riepilogo vuoto (revisione finale 02/10).
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = it.pixelbox.cmwatch.ui.tokens.CmColors.actionIcon)
                }
                return@summaryPage
            }
            val master = summary.master
            val masterEntries = master?.let { m -> if (chatName == m.name) entries else feedCache[m.name].orEmpty() }.orEmpty()
            val list: @Composable () -> Unit = {
                SummaryList(
                    summary, now,
                    // Aprire una sessione che ha finito la toglie da «Ha finito», come in «Per te».
                    onOpen = { n -> summary.rows.firstOrNull { r -> r.session.name == n && r.key != null }?.key?.let(markRead); open = n },
                    onAnswer = { n, k -> scope.launch { runCatching { app.repo.answer(n, k) } } },
                    onStep = sendPrompt, onService = forYouAction, onClosed = { closedOpen = true },
                )
            }
            if (master != null) {
                val hero = remember(masterEntries, master.outcome) { it.pixelbox.cmwatch.rules.MasterHome.hero(masterEntries, master) }
                sessionPage(master, masterEntries, { _, _ -> list() }, false) {
                    MasterDock(master, hero, onSpeak = { hero?.let { h -> speech.toggle(listOf(h.headline, h.body).filter { it.isNotBlank() }.joinToString("\n")) } },
                        onConversation = { open = master.name })
                }
            } else Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)) { list() }
                Box(Modifier.padding(16.dp)) {
                    MasterAbsent { scope.launch { runCatching { app.repo.command(CmdOp.REOPEN, it.pixelbox.cmwatch.rules.ContextActions.MASTER, null) } } }
                }
            }
        }
        val speakingBlock by speech.block.collectAsStateWithLifecycle()
        CompositionLocalProvider(LocalSpeaking provides speaking, LocalSpeakingBlock provides speakingBlock, LocalBlocksOf provides speech::blocksOf) {
        AppShell(
            tab, demo, onTab = { tab = it; open = null }, onSettings = { settingsOpen = true },
            sessions = state?.let { st -> PhoneBoard.sections(st).flatMap { sec -> sec.sessions } }.orEmpty(),
            // La master dal menu in alto apre la sua chat come le altre (design 03/10); «Tutte le sessioni» torna al riepilogo.
            current = open, onPick = { n -> open = n; if (n == null) tab = StartRoute.Tab.OVERVIEW },
            onQuadro = { overviewSheet = true }, onSearch = { searchOpen = true }, onLaunch = { launching = true },
            host = host, stale = snap.freshness is Freshness.Stale,
            updated = when (val f = snap.freshness) { is Freshness.Stale -> getString(R.string.menu_updated_ago, f.minutes); else -> getString(R.string.menu_updated_now) },
            openCount = summary?.open ?: 0,
            quota = state?.let { st -> {
                val rings = remember(st, events, samples, now, snap.freshness) {
                    PhoneOverview.build(st, events, samples, now, java.time.ZoneId.systemDefault(), stale = snap.freshness is Freshness.Stale).rings
                }
                QuotaBars(rings, onOpen = { overviewSheet = true })
            } },
            // Tirare giù chiede lo stato al PC; la rotella resta finché la risposta arriva o la richiesta fallisce.
            onRefresh = if (open == null) ({ refreshing = true; scope.launch { app.repo.refresh(); refreshing = false } }) else null,
            refreshing = refreshing,
        ) { page ->
            if (page == StartRoute.Tab.DIARY && open == null) {
                state?.let { st ->
                    DiaryScreen(st, events.filter { it.kind == EventKind.QUOTA }, PhoneDiary.recaps(events), PhoneDiary.lastNightReport(events), ttsMinChars, speech::toggle,
                        onAdd = { nightAdding = true },
                        onRemove = { id -> scope.launch { app.repo.command(CmdOp.NIGHT_REMOVE, null, id) } },
                        rings = PhoneOverview.build(st, events, samples, now, java.time.ZoneId.systemDefault(), stale = snap.freshness is Freshness.Stale).rings,
                        onQuadro = { overviewSheet = true },
                        // Una riga del Registro apre la sessione del progetto, se è viva.
                        onSession = { n -> if (st.sessions.any { s -> s.name == n && s.state != it.pixelbox.cmwatch.contract.SessionState.GONE }) { open = n } })
                }
                return@AppShell
            }
            SharedTransitionLayout {
                flight.AnimatedContent(transitionSpec = { EnterTransition.None togetherWith ExitTransition.None }, contentKey = { it != null }) { name ->
                    CompositionLocalProvider(LocalFly provides Fly(this@SharedTransitionLayout, this@AnimatedContent)) {
                        // Scorrimento laterale fra riepilogo e sessioni (Franz, 03/10 15:59): il riepilogo è sempre la prima
                        // pagina (`null`), poi le sessioni nell'ordine della regia (Franz, 30/09: «lo scroll laterale tra
                        // sessioni»). La pagina ferma decide la sessione aperta, e il menu in alto la segue.
                        val pages = remember(state?.sessions, name) { it.pixelbox.cmwatch.rules.SwipePages.of(state, name) }
                        val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = pages.indexOf(name).coerceAtLeast(0)) { pages.size }
                        // Solo il contenuto di destinazione segue la pagina: durante l'uscita (gesto indietro, volo verso la
                        // card) quello che se ne va non deve riaprire né spostarsi.
                        val active = name == open
                        LaunchedEffect(pager.settledPage, active) {
                            if (!active) return@LaunchedEffect
                            val n = pages.getOrNull(pager.settledPage) ?: run {
                                // Sul riepilogo si torna anche dal Registro: la prima pagina è sempre il riepilogo.
                                if (open != null) { open = null; tab = StartRoute.Tab.OVERVIEW }
                                return@LaunchedEffect
                            }
                            if (n != open) open = n
                        }
                        LaunchedEffect(open, pages, active) {
                            val i = pages.indexOf(open)
                            if (active && i >= 0 && i != pager.currentPage && !pager.isScrollInProgress) pager.scrollToPage(i)
                        }
                        androidx.compose.foundation.pager.HorizontalPager(pager, key = { pages[it] ?: SUMMARY_PAGE }, beyondViewportPageCount = 0) { page ->
                            val n = pages[page]
                            val session = n?.let { x -> state?.sessions?.firstOrNull { it.name == x } }
                            if (session == null) {
                                // Il riepilogo; sotto la scheda, durante il gesto indietro, è questa pagina.
                                if (n == null) summaryPage()
                                return@HorizontalPager
                            }
                            // La conversazione della pagina: quella dal vivo per la sessione aperta, l'ultima letta per le vicine.
                            val pageEntries = if (session.name == open) entries else feedCache[session.name].orEmpty()
                            sessionPage(session, pageEntries, null, true, null)
                        }
                    }
                }
            }
        }
        }
        // Contratto 1.26 (dal vivo 02/10 17:10: /state porta 5 progetti su 98): all'apertura di Lancia o della notte si chiede
        // al PC l'elenco completo; finché non arriva, o con un relay senza l'op, restano quelli di /state.
        var projectsId by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(launching || nightAdding) {
            if ((launching || nightAdding) && state?.ops?.contains("projects") == true) {
                projectsId = runCatching { app.repo.command(CmdOp.PROJECTS, null, null) }.getOrNull()
            }
        }
        val allProjects = projectsId?.let { results[it] }?.takeIf { it.ok }
            ?.let { r -> runCatching { it.pixelbox.cmwatch.contract.ContractJson.decodeProjects(r.text).projects }.getOrNull() }
        if (nightAdding && state != null) {
            ModalBottomSheet(onDismissRequest = { nightAdding = false }) {
                LaunchSheet(state, action = R.string.night_add, projects = allProjects ?: state.projects) { project, prompt ->
                    nightAdding = false
                    if (prompt.isNotBlank()) scope.launch { app.repo.command(CmdOp.NIGHT_ADD, null, project.path, prompt) }
                }
            }
        }
        // «Chiuse · N» del riepilogo: l'elenco delle chiuse con «Riapri».
        if (closedOpen && summary != null) {
            ModalBottomSheet(onDismissRequest = { closedOpen = false }, containerColor = it.pixelbox.cmwatch.ui.tokens.CmColors.surface) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(getString(R.string.summary_closed, summary.closed.size), style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = it.pixelbox.cmwatch.ui.tokens.CmColors.text)
                    summary.closed.forEach { s ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SessionBadge(s, 20.dp)
                            Text(s.name, color = it.pixelbox.cmwatch.ui.tokens.CmColors.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip)
                            androidx.compose.material3.FilledTonalButton(onClick = { closedOpen = false; scope.launch { runCatching { app.repo.command(CmdOp.REOPEN, s.name, null) } } }) { Text(getString(R.string.reopen)) }
                        }
                    }
                }
            }
        }
        if (overviewSheet && state != null) {
            ModalBottomSheet(onDismissRequest = { overviewSheet = false }, containerColor = it.pixelbox.cmwatch.ui.tokens.CmColors.bg) {
                val model = remember(state, events, samples, now, snap.freshness) {
                    PhoneOverview.build(state, events, samples, now, java.time.ZoneId.systemDefault(), stale = snap.freshness is Freshness.Stale)
                }
                OverviewScreen(model, snap.freshness,
                    onQuestion = { overviewSheet = false; queueOpen = true },
                    onSession = { n -> overviewSheet = false; open = n })
            }
        }
        if (launching && state != null) {
            ModalBottomSheet(onDismissRequest = { launching = false }) {
                LaunchSheet(state, projects = allProjects ?: state.projects, onSession = { name, reopen ->
                    launching = false
                    if (reopen) scope.launch { app.repo.command(CmdOp.REOPEN, name, null) } else { open = name }
                }) { project, first ->
                    launching = false
                    scope.launch { app.repo.command(CmdOp.LAUNCH, null, project.path, first.ifBlank { null }) }
                }
            }
        }
    }

    /**
     * Un'immagine dalla barra di scrittura: ridotta come in «Condividi» (contratto 1.19), mandata con `report`, e una copia
     * locale per l'anteprima nel fumetto della chat.
     */
    /** Un file scaricato dalla chat all'app di sistema che lo apre; senza un'app adatta, lo si dice. */
    private fun viewFile(file: java.io.File, mime: String?) {
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.files", file)
        val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { startActivity(view) }.onFailure {
            android.widget.Toast.makeText(this, getString(R.string.file_no_app), android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun attachImage(session: String, uri: Uri, text: String, maxBytes: Int) {
        app.scope.launch {
            val bytes = it.pixelbox.cmwatch.mobile.share.ImageShrink.jpeg(this@MainActivity, uri) ?: return@launch
            // Il messaggio compare subito nella chat; caricamento, invio e rifiuto sono suoi passaggi (30/09 22:13).
            val id = java.util.UUID.randomUUID().toString()
            val copy = java.io.File(java.io.File(filesDir, "chat").apply { mkdirs() }, "$id.jpg").apply { writeBytes(bytes) }
            app.chatLog.add(Sent(id, session, text, System.currentTimeMillis() / 1000, attachment = copy.path))
            runCatching { app.repo.report(session, text, "image/jpeg", bytes, maxBytes, id = id) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        route(intent)
    }

    /** Notifica con domanda → la coda; widget → la scheda della sessione o la Panoramica. */
    private fun route(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_QUEUE, false)) queueAsked = true
        intent.getStringExtra(EXTRA_SESSION)?.let { sessionAsked = it }
        if (intent.getBooleanExtra(EXTRA_OVERVIEW, false)) sessionAsked = ""
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_ROTATING, isChangingConfigurations)
        outState.putString(KEY_LAUNCH, launchId)
    }

    override fun onStart() {
        super.onStart()
        val away = stoppedAt?.let { System.currentTimeMillis() - it }
        stoppedAt = null
        if (away != null && StartRoute.resetOnReturn(away)) launchId = java.util.UUID.randomUUID().toString()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) stoppedAt = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        app.scope.launch { app.pairing.completePending() }
    }

    override fun onDestroy() {
        speech.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val KEY_ROTATING = "cm.rotating"
        /** La chiave della prima pagina dello scorrimento, il riepilogo: nessun nome di sessione la può avere. */
        private const val SUMMARY_PAGE = "\u0000summary"
        private const val KEY_LAUNCH = "cm.launch"
        /** Extra dell'intent delle notifiche con domanda: apre la coda «Ti aspettano». */
        const val EXTRA_QUEUE = "cm.queue"
        /** Extra del widget: la sessione da aprire, o la Panoramica. */
        const val EXTRA_SESSION = "cm.session"
        const val EXTRA_OVERVIEW = "cm.overview"
    }
}
