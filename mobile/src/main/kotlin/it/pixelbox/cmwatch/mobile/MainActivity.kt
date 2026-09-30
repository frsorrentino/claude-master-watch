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
import it.pixelbox.cmwatch.pairing.PairingRecord
import it.pixelbox.cmwatch.rules.AppLanguage
import it.pixelbox.cmwatch.rules.PhoneDiary
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.rules.ChatFeed
import it.pixelbox.cmwatch.rules.ChatRules
import it.pixelbox.cmwatch.rules.PhoneBoard
import it.pixelbox.cmwatch.rules.PhoneOverview
import it.pixelbox.cmwatch.rules.Sent
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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    s.paired || s.demoMode -> key(launchId) { Main(s.demoMode, s.ttsMinChars, s.host, s.pairingJson, ::scan) }
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
        // Un comando rifiutato dal PC si dice in chiaro, con il motivo del relay (spec 29/09, «Errori»).
        LaunchedEffect(Unit) {
            app.repo.userResults.collect { r -> if (!r.ok && r.text.isNotBlank()) android.widget.Toast.makeText(this@MainActivity, r.text, android.widget.Toast.LENGTH_LONG).show() }
        }
        val results by app.repo.resultsById.collectAsStateWithLifecycle()
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
        var now by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
        LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() / 1000 } }
        val state = snap.state

        BackHandler(enabled = settingsOpen || terminal != null) {
            if (settingsOpen) settingsOpen = false else { terminal = null; screenId = null }
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
        // Contratto 1.22: la conversazione della scheda aperta, a pagine, letta dal vivo finché la scheda resta aperta.
        val transcriptOk = !demo && state?.ops?.contains("transcript") == true
        var entries by remember { mutableStateOf<List<it.pixelbox.cmwatch.contract.TranscriptEntry>>(emptyList()) }
        var more by remember { mutableStateOf(false) }
        var olderId by remember { mutableStateOf<String?>(null) }
        var unsupported by remember { mutableStateOf(false) }
        val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(open, transcriptOk, unsupported) {
            entries = emptyList(); more = false
            val name = open ?: return@LaunchedEffect
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
                                if (mode == ChatFeed.Page.FRESH) more = page.more
                            }
                            // L'ultima voce non c'è più (conversazione compattata): si riparte dalle ultime.
                            r.text.contains("no entry") -> entries = emptyList()
                            // Relay aggiornato ma servizio ancora vecchio: resta la chat dei messaggi mandati.
                            r.text.contains("not allowed") -> unsupported = true
                        }
                        pendingId = null
                    }
                    val answered = pendingId == null
                    val moved = answered && TerminalLive.next(prev, cur) != null
                    prev = cur
                    val t = System.currentTimeMillis()
                    if (moved || PhoneTerminal.shouldAsk(cur, askedAt, answered, t)) {
                        askedAt = t
                        mode = if (entries.isEmpty()) ChatFeed.Page.FRESH else ChatFeed.Page.AFTER
                        // Una lettura persa non resta fra i comandi in sospeso (revisione 30/09).
                        pendingId?.let { app.repo.forget(it) }
                        pendingId = runCatching { app.repo.command(CmdOp.TRANSCRIPT, name, ChatFeed.arg(entries.lastOrNull()?.id)) }.getOrNull()
                    }
                    delay(1_000)
                }
            }
        }
        // «Carica i messaggi precedenti»: una pagina `before` in testa.
        // Legata alla sessione aperta e con un limite: una pagina persa non blocca il tasto, e non finisce in un'altra
        // sessione (revisione 30/09).
        LaunchedEffect(open) { olderId = null }
        LaunchedEffect(open, olderId) {
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
        val fab: @Composable () -> Unit = {
            // «Aggiungi alla notte» solo con un relay 1.17, come nel Diario: prima il PC la rifiuterebbe.
            if (open == null && tab != StartRoute.Tab.DIARY && state != null) LaunchFab(
                onLaunch = { launching = true }, onNight = if (state.night.items != null) ({ nightAdding = true }) else null,
            )
        }
        AppShell(
            tab, demo, onTab = { tab = it; open = null }, onSettings = { settingsOpen = true }, fab = fab,
            sessions = state?.let { st -> PhoneBoard.sections(st).flatMap { sec -> sec.sessions } }.orEmpty(),
            current = open, onPick = { n -> if (n != null) tab = StartRoute.Tab.SESSIONS; open = n },
        ) {
            if (tab == StartRoute.Tab.OVERVIEW && open == null) {
                state?.let { st ->
                    val model = remember(st, events, samples, now, snap.freshness) {
                        PhoneOverview.build(st, events, samples, now, java.time.ZoneId.systemDefault(), stale = snap.freshness is Freshness.Stale)
                    }
                    OverviewScreen(model, snap.freshness,
                        onQuestion = { model.questions?.let { q -> tab = StartRoute.Tab.SESSIONS; open = q.oldest } },
                        onSession = { n -> tab = StartRoute.Tab.SESSIONS; open = n })
                }
                return@AppShell
            }
            if (tab == StartRoute.Tab.DIARY && open == null) {
                state?.let { st ->
                    DiaryScreen(st, events.filter { it.kind == EventKind.QUOTA }, PhoneDiary.recaps(events), PhoneDiary.lastNightReport(events), ttsMinChars, speech::speak,
                        onAdd = { nightAdding = true },
                        onRemove = { id -> scope.launch { app.repo.command(CmdOp.NIGHT_REMOVE, null, id) } })
                }
                return@AppShell
            }
            SharedTransitionLayout {
                flight.AnimatedContent(transitionSpec = { EnterTransition.None togetherWith ExitTransition.None }) { name ->
                    CompositionLocalProvider(LocalFly provides Fly(this@SharedTransitionLayout, this@AnimatedContent)) {
                        val session = name?.let { n -> state?.sessions?.firstOrNull { it.name == n } }
                        // I messaggi mandati restano nella chat con il loro stato (design 30/09, parte 3).
                        if (session != null) {
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
                            rows.filter { it.status == ChatRules.Status.FAILED && it.sent.failed == null }.forEach { r ->
                                app.chatLog.markFailed(r.sent.id, r.reason ?: getString(R.string.chat_no_answer))
                            }
                        }
                        SessionSheet(session, now, snap.pending, ttsMinChars, SheetActions(
                            answer = { n -> scope.launch { app.repo.answer(session.name, n) } },
                            allowAll = { scope.launch { app.repo.command(CmdOp.ALLOW_ALL, session.name, null) } },
                            send = { target, text -> sendAndLog(target, text) },
                            follow = { on -> scope.launch { app.repo.command(if (on) CmdOp.FOLLOW else CmdOp.UNFOLLOW, session.name, null) } },
                            reopen = { scope.launch { app.repo.command(CmdOp.REOPEN, session.name, null) } },
                            terminal = { terminal = session.name; screenId = null },
                            openInClaude = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(session.link))) },
                            speak = speech::speak,
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
                            setModel = { v -> scope.launch { app.repo.command(CmdOp.MODEL, session.name, v) } },
                            setEffort = { v -> scope.launch { app.repo.command(CmdOp.EFFORT, session.name, v) } },
                            interrupt = { scope.launch { app.repo.command(CmdOp.INTERRUPT, session.name, null) } },
                            attach = { uri, text -> attachImage(session.name, uri, text, state?.share?.maxBytes ?: 0) },
                        ), chat = rows, choices = state?.choices, ops = state?.ops, canTune = !demo, canAttach = state?.share != null,
                            feed = if (transcriptOk && !unsupported && entries.isNotEmpty()) ChatFeed.merge(entries, rows.map { it.sent to it.status }) else null,
                            more = more,
                            onOlder = {
                                val first = entries.firstOrNull()?.id
                                if (first != null && olderId == null) scope.launch { olderId = runCatching { app.repo.command(CmdOp.TRANSCRIPT, session.name, ChatFeed.olderArg(first)) }.getOrNull() }
                            },
                        )
                        } else SessionsScreen(snap, now, onOpen = { id -> open = state?.sessions?.firstOrNull { it.id == id }?.name })
                    }
                }
            }
        }
        if (nightAdding && state != null) {
            ModalBottomSheet(onDismissRequest = { nightAdding = false }) {
                LaunchSheet(state, action = R.string.night_add) { project, prompt ->
                    nightAdding = false
                    if (prompt.isNotBlank()) scope.launch { app.repo.command(CmdOp.NIGHT_ADD, null, project.path, prompt) }
                }
            }
        }
        if (launching && state != null) {
            ModalBottomSheet(onDismissRequest = { launching = false }) {
                LaunchSheet(state) { project, first ->
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

    private companion object {
        const val KEY_ROTATING = "cm.rotating"
        const val KEY_LAUNCH = "cm.launch"
    }
}
