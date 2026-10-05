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
import it.pixelbox.cmwatch.mobile.ui.ModalBottomSheet
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.mobile.pair.Phase
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.rules.PairAddText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
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
    private val readingOverlay = it.pixelbox.cmwatch.mobile.ui.ReadingOverlay()
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
    /** L'invito a zero tocchi dal relay (`cmwatch://pair?q=`): la riga del QR, in attesa della conferma. */
    private var pairInvite by mutableStateOf<String?>(null)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) intent?.let(::route)
        val rotating = savedInstanceState?.getBoolean(KEY_ROTATING) == true
        launchId = savedInstanceState?.getString(KEY_LAUNCH)?.takeIf { rotating } ?: java.util.UUID.randomUUID().toString()
        enableEdgeToEdge()
        // Pixel Tablet in finestra (Android 15+): la barra del titolo trasparente, la disegna la plancia (Franz, 05/10 11:21).
        if (android.os.Build.VERSION.SDK_INT >= 35) window.insetsController?.setSystemBarsAppearance(
            android.view.WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND,
            android.view.WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND,
        )
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
                            androidx.compose.runtime.CompositionLocalProvider(it.pixelbox.cmwatch.mobile.ui.LocalReadingOverlay provides readingOverlay) {
                                Main(s.demoMode, s.ttsMinChars, s.host, s.pairingJson, ::scan)
                            }
                            it.pixelbox.cmwatch.mobile.ui.ReadingOverlayHost(readingOverlay)
                            RefusalBanner(app.repo.userResults, androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.BottomCenter))
                        }
                    }
                    else -> NotPairedScreen(onPair = ::scan, onPaste = { paste = true }, onDemo = { app.setDemo(true) })
                }
                if (paste) PasteDialog(onPair = { t -> paste = false; scope.launch { app.pairing.run(t) } }, onDismiss = { paste = false })
                // Franz, 04/10 15:48 («ok c»): l'invito che il relay apre sul Chromebook. Il link si può aprire anche da una
                // pagina web, quindi mai un accoppiamento silenzioso: si dice a quale PC e si accoppia solo col tocco.
                pairInvite?.let { line ->
                    it.pixelbox.cmwatch.pairing.PairQr.parse(line)?.let { qr ->
                        PairInviteDialog(qr.h, qr.add, onPair = { pairInvite = null; scope.launch { app.pairing.run(line) } }, onDismiss = { pairInvite = null })
                    } ?: run { pairInvite = null }
                }
            }
        }
    }

    /** L'app accoppiata, o in Demo: tre schede (restyling 30/09), scheda sessione, terminale, Lancia, impostazioni. */
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
    @Composable
    private fun Main(demo: Boolean, ttsMinChars: Int, host: String?, pairingJson: String?, onRepair: () -> Unit) {
        val snap by app.repo.snapshot.collectAsStateWithLifecycle()
        // Il mini-controller della lettura (Franz, 03/10 23:00): cosa legge e da dove; su ogni schermata finché legge.
        val readingNow by speech.speaking.collectAsStateWithLifecycle()
        val fieldFocus = remember { it.pixelbox.cmwatch.mobile.ui.FieldFocus() }
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        val readingSource by speech.source.collectAsStateWithLifecycle()
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
        val tabState = rememberSaveable { mutableStateOf(StartRoute.tab(restored = null)) }
        var tab by tabState
        // La master espansa nella home (Franz, 03/10 16:30-16:44): dal basso a tutta pagina, ridotta con un tocco sulla sua barra.
        val masterChatState = rememberSaveable { mutableStateOf(false) }
        var masterChat by masterChatState
        // Nome della sessione aperta; la master non è mai una pagina: aprirla, da qualunque strada, porta la home sulla sua chat.
        val openState = rememberSaveable { mutableStateOf<String?>(null) }
        val route = remember { OpenRoute(openState, tabState, masterChatState) }
        var open by route
        var terminal by rememberSaveable { mutableStateOf<String?>(null) }   // nome della sessione del terminale
        var screenId by rememberSaveable { mutableStateOf<String?>(null) }
        var settingsOpen by rememberSaveable { mutableStateOf(false) }
        var launching by rememberSaveable { mutableStateOf(false) }
        var nightAdding by rememberSaveable { mutableStateOf(false) }
        var queueOpen by rememberSaveable { mutableStateOf(false) }
        // La Panoramica in un foglio dal basso sopra la scheda (Franz, 01/10 12:34): si guarda la quota e si torna.
        var overviewSheet by rememberSaveable { mutableStateOf(false) }
        var searchOpen by rememberSaveable { mutableStateOf(false) }
        // Il tocco sul testo del mini-controller riporta alla sessione da cui legge; la master si apre nella home.
        val readingBar: (@Composable () -> Unit)? = readingNow?.let { text ->
            {
                val src = readingSource
                val live = src != null && snap.state?.sessions?.any { x -> x.name == src && x.state != it.pixelbox.cmwatch.contract.SessionState.GONE } == true
                val rate by speech.rate.collectAsStateWithLifecycle()
                val voices by speech.voices.collectAsStateWithLifecycle()
                val voice by speech.voice.collectAsStateWithLifecycle()
                ReadingPill(
                    src, text,
                    rate = rate, onRate = speech::setRateNow,
                    voice = voice, onVoice = if (voices.isEmpty()) null else ({ speech.setVoiceNow(it.pixelbox.cmwatch.rules.VoiceRules.next(voices, voice)) }),
                    onOpen = if (!live) null else ({
                        settingsOpen = false; searchOpen = false; queueOpen = false; tab = StartRoute.Tab.OVERVIEW
                        if (src == it.pixelbox.cmwatch.rules.ContextActions.MASTER) { open = null; masterChat = true } else open = src
                    }),
                    onStop = { speech.stop() },
                )
            }
        }
        // Nel terminale no, come prima: lì il fondo è del testo dal vivo.
        androidx.compose.runtime.SideEffect { readingOverlay.bar = if (terminal == null) readingBar else null; readingOverlay.source = readingSource }
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
        // Il volo card ↔ sessione segue la sessione aperta solo quando cambia per un tocco, un menu o Indietro, non per lo
        // scorrimento (dal vivo 03/10 20:00: scorrendo dalla home alla prima sessione il cambio di contenuto rifaceva la
        // pagina appena vista, con un lampo). `swipeSet`: l'ultimo cambio di `open` viene dallo scorrimento.
        var flyTarget by rememberSaveable { mutableStateOf(open) }
        var swipeSet by remember { mutableStateOf(false) }
        LaunchedEffect(open) { if (swipeSet) swipeSet = false else flyTarget = open }
        val seek = remember { SeekableTransitionState(flyTarget) }
        // Sempre fino in fondo: dopo un gesto completato seekTo ha già messo il bersaglio a null (revisione 29/09).
        // Senza spec la durata è quella della transizione quando parte, ancora 0 perché il volo registra i bordi al layout:
        // il tocco su una card apriva la sessione in un fotogramma (registrazione dal vivo del 03/10 20:57). Con lo spec la
        // frazione avanza da sola; dopo un gesto indietro (frazione già avanzata) resta il tempo che manca, come prima.
        LaunchedEffect(flyTarget) {
            seek.animateTo(flyTarget, if (seek.fraction == 0f) androidx.compose.animation.core.tween(FLY_MS, easing = androidx.compose.animation.core.LinearEasing) else null)
        }
        PredictiveBackHandler(enabled = open != null && flyTarget != null && !settingsOpen && terminal == null) { progress ->
            try {
                progress.collect { seek.seekTo(it.progress, targetState = null) }
                open = null
            } catch (e: CancellationException) {
                withContext(NonCancellable) { seek.animateTo(flyTarget) }
            }
        }
        // Arrivati a una sessione scorrendo dalla home non c'è un volo da ripercorrere: Indietro riporta la pagina alla home.
        BackHandler(enabled = open != null && flyTarget == null && !settingsOpen && terminal == null) { open = null }
        val flight = rememberTransition(seek, label = "fly")
        // Il tablet (Franz, 04/10 16:36): da 840 dp una vista sola, la home del telefono di lato e da una a quattro colonne di
        // sessioni. Colonne, larghezze in dodicesimi, lato della home e dettagli a destra stanno nelle preferenze, così
        // restano quando la finestra cambia larghezza.
        val widthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
        val wide = it.pixelbox.cmwatch.rules.Tablet.wide(widthDp)
        val tabletPrefs = remember { getSharedPreferences("ui", MODE_PRIVATE) }
        // I box sopra il campo aperti o chiusi (Franz, 04/10 20:21): Prossimi aperto e Ricorrenti chiuso finché non li tocca.
        val promptBoxes = remember {
            it.pixelbox.cmwatch.mobile.ui.PromptBoxes(
                tabletPrefs.getBoolean(it.pixelbox.cmwatch.mobile.ui.PromptBoxes.STEPS, true),
                tabletPrefs.getBoolean(it.pixelbox.cmwatch.mobile.ui.PromptBoxes.RECURRING, false),
            ) { k, v -> tabletPrefs.edit().putBoolean(k, v).apply() }
        }
        var pinned by remember { mutableStateOf(it.pixelbox.cmwatch.rules.Tablet.columnsFromPref(tabletPrefs.getString("tablet_columns", null))) }
        var sharesRaw by remember { mutableStateOf(tabletPrefs.getString("tablet_shares", null)) }
        var homeRight by remember { mutableStateOf(tabletPrefs.getBoolean("tablet_home_right", false)) }
        var tabletDetails by remember { mutableStateOf(tabletPrefs.getBoolean("tablet_details", false)) }
        if (settingsOpen) {
            val r = PairingRecord.fromJson(pairingJson)
            val version = remember { packageManager.getPackageInfo(packageName, 0).versionName.orEmpty() }
            // Per lo schema dei dispositivi (mockup A): l'orologio raggiungibile adesso lo dice il Data Layer, le notifiche il sistema.
            var watchNear by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(r?.watchName) {
                watchNear = if (r?.watchName == null) null else it.pixelbox.cmwatch.mobile.pair.WearWatchLink(this@MainActivity).anyConnected() != null
            }
            val notificationsOn = remember { getSystemService(android.app.NotificationManager::class.java).areNotificationsEnabled() }
            // Contratto 1.31 (Franz, 04/10 13:47: «pairing dal telefono senza PC»): l'invito per un dispositivo in più lo apre
            // il relay e vale 5 minuti; qui il QR da inquadrare col dispositivo nuovo. Chiuso il foglio, la risposta tardiva si ignora.
            var addDevice by remember { mutableStateOf<AddDeviceUi?>(null) }
            val askInvite: () -> Unit = {
                addDevice = AddDeviceUi.Asking
                scope.launch {
                    val id = runCatching { app.repo.command(CmdOp.PAIR_ADD, null, null) }.getOrNull()
                    var r: it.pixelbox.cmwatch.contract.CmdResult? = null
                    if (id != null) {
                        val until = System.currentTimeMillis() + PhoneTerminal.LOST_MS
                        while (r == null && System.currentTimeMillis() < until) { r = app.repo.resultsById.value[id]; if (r == null) delay(500) }
                        app.repo.forget(id)
                    }
                    val got = r
                    if (addDevice != null) addDevice = when {
                        got == null -> AddDeviceUi.Lost
                        got.ok -> runCatching { ContractJson.decodePairAdd(got.text) }.map { o -> AddDeviceUi.Offer(o.qr, o.code, o.exp) }
                            .getOrElse { AddDeviceUi.Refused(PairAddText.Refusal.OTHER, got.text) }
                        else -> AddDeviceUi.Refused(PairAddText.refusal(got.text), PairAddText.reason(got.text))
                    }
                }
            }
            addDevice?.let { ui ->
                ModalBottomSheet(onDismissRequest = { addDevice = null }, containerColor = it.pixelbox.cmwatch.ui.tokens.CmColors.surface) {
                    AddDeviceSheet(ui, onRetry = askInvite, onDone = { addDevice = null })
                }
            }
            SettingsScreen(
                host = host, phoneName = app.phoneName, watchName = r?.watchName, watchPending = r?.watchPending == true, demo = demo,
                version = version,
                devices = it.pixelbox.cmwatch.rules.SettingsDevices.build(
                    host, state, snap.freshness, now, app.phoneName, version, notificationsOn, r?.watchName, r?.watchPending == true, watchNear,
                ),
                onBack = { settingsOpen = false },
                onRepair = onRepair, onDemo = { app.setDemo(it) },
                onNotifications = { startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)) },
                onPrivacy = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_url)))) },
                language = AppLanguage.fromTags(localeManager.applicationLocales.toLanguageTags()),
                // Il sistema ricrea l'activity nella lingua nuova; con «come il telefono» la lista vuota torna a seguirlo.
                onLanguage = { localeManager.applicationLocales = android.os.LocaleList.forLanguageTags(it.tag) },
                voices = speech.voices.collectAsStateWithLifecycle().value, voice = speech.voice.collectAsStateWithLifecycle().value,
                onVoice = speech::setVoice, onTryVoice = { speech.toggle(getString(R.string.voice_sample)) },
                rate = speech.rate.collectAsStateWithLifecycle().value, onRate = speech::setRate,
                onAddDevice = if (!demo && state?.ops?.contains("pair_add") == true) askInvite else null,
                tabletDetails = tabletDetails.takeIf { wide },
                onTabletDetails = { on -> tabletDetails = on; tabletPrefs.edit().putBoolean("tablet_details", on).apply() },
                // Contratto 1.32: lo schema dei collegamenti con i dispositivi veri; «questo» è il primo uid dell'accoppiamento.
                linked = it.pixelbox.cmwatch.rules.SettingsDevices.linked(state, r?.uids?.firstOrNull(), now),
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
            // Contratto 1.27: con un relay che cerca, la ricerca va nelle conversazioni di tutte le sessioni; una richiesta per
            // volta, la precedente dimenticata.
            var searchId by remember { mutableStateOf<String?>(null) }
            val searchResult = searchId?.let { results[it] }
            val searchPage = searchResult?.takeIf { it.ok }?.let { r -> runCatching { ContractJson.decodeSearch(r.text) }.getOrNull() }
            SearchScreen(
                chatLog, events, onOpen = { n -> searchOpen = false; if (n != null) { open = n } else { open = null; tab = StartRoute.Tab.DIARY } },
                remote = state?.ops?.contains("search") == true, page = searchPage, loading = searchId != null && searchResult == null,
                known = state?.sessions?.map { it.name }?.toSet().orEmpty(),
                onQuery = { q ->
                    scope.launch {
                        searchId?.let { app.repo.forget(it) }
                        searchId = runCatching { app.repo.command(CmdOp.SEARCH, null, q) }.getOrNull()
                    }
                },
            )
            return
        }
        if (queueOpen && state != null) {
            // La coda usa solo la card della domanda: risposta, «Parliamone», «Consenti tutto» e la lettura a voce.
            QueueScreen(state, now, actionsFor = { s ->
                SheetActions(
                    answer = { n -> scope.launch { app.repo.answer(s.name, n) } },
                    allowAll = { scope.launch { app.repo.command(CmdOp.ALLOW_ALL, s.name, null) } },
                    send = { _, _ -> }, follow = {}, reopen = {}, terminal = {}, openInClaude = {},
                    speak = { t -> speech.toggle(t, s.name) }, retry = {},
                    chat = { scope.launch { app.repo.chat(s.name) }; queueOpen = false; open = s.name },
                )
            }, onSession = { n -> queueOpen = false; open = n })
            return
        }
        // Contratto 1.22: la conversazione della scheda aperta, a pagine, letta dal vivo finché la scheda resta aperta.
        val transcriptOk = !demo && state?.ops?.contains("transcript") == true
        // La casa della master (design 01/10): sulla prima scheda, senza schede aperte, la chat è quella della master.
        val masterName = it.pixelbox.cmwatch.rules.ContextActions.master(state)?.name
        val liveNames = state?.sessions?.filter { x -> x.state != it.pixelbox.cmwatch.contract.SessionState.GONE }?.map { x -> x.name }.orEmpty()
        // La master sta nella home: non diventa una colonna.
        val tabletCols = it.pixelbox.cmwatch.rules.Tablet.columns(pinned, liveNames.filter { n -> n != it.pixelbox.cmwatch.rules.ContextActions.MASTER })
        val tabletShares = it.pixelbox.cmwatch.rules.Tablet.Shares.fromPref(sharesRaw, tabletCols.size)
        val columnsOn = wide
        val saveCols: (List<String>) -> Unit = { next -> pinned = next; tabletPrefs.edit().putString("tablet_columns", it.pixelbox.cmwatch.rules.Tablet.columnsPref(next)).apply() }
        val saveShares: (List<Int>) -> Unit = { s -> val raw = it.pixelbox.cmwatch.rules.Tablet.Shares.pref(s); sharesRaw = raw; tabletPrefs.edit().putString("tablet_shares", raw).apply() }
        // Sul tablet la conversazione della home è quella della master; le colonne leggono la loro qui sotto.
        val chatName = if (wide) masterName else open ?: masterName?.takeIf { tab == StartRoute.Tab.OVERVIEW }
        // I dettagli a destra (spenti di default, si accendono dalle Impostazioni): della prima colonna, se c'è posto.
        val inspectorOn = wide && tabletDetails && tab != StartRoute.Tab.DIARY && tabletCols.isNotEmpty() && it.pixelbox.cmwatch.rules.Tablet.inspector(widthDp)
        // Sul tablet aprire una sessione, da qualunque strada (scheda della home, ricerca, notifica, menu), la mette in colonna.
        LaunchedEffect(open, wide) {
            val n = open ?: return@LaunchedEffect
            if (wide) { saveCols(it.pixelbox.cmwatch.rules.Tablet.add(tabletCols, n)); open = null }
        }
        // Le bozze del campo sopra l'interruttore dei 840 dp (standard della master, 04/10): restano quando la finestra del
        // Chromebook cambia larghezza, in tutte e due le direzioni.
        val drafts = rememberSaveable(saver = DraftStore.Saver) { DraftStore() }
        // I resoconti della notte già ascoltati e i prossimi passi già avviati: spariscono da «Per te» e non tornano,
        // nemmeno dopo la coda, una rotazione o un riavvio (revisione finale 02/10: in memoria si perdevano).
        val forYouPrefs = remember { getSharedPreferences("for-you", MODE_PRIVATE) }
        // Contratto 1.37, «Handoff, poi /clear»: il /clear parte a turno finito, una volta sola; dopo 3 ore si lascia perdere.
        val clearAfter = remember { getSharedPreferences("clear-after", MODE_PRIVATE) }
        LaunchedEffect(state) {
            val st0 = state ?: return@LaunchedEffect
            val nowS = System.currentTimeMillis() / 1000
            clearAfter.all.forEach { (name, v) ->
                val sentAt = (v as? Long) ?: return@forEach
                val due = it.pixelbox.cmwatch.rules.ContextActions.clearDue(st0.sessions.firstOrNull { x -> x.name == name }, sentAt)
                if (due) runCatching { app.repo.command(CmdOp.SLASH, name, "clear", null) }
                if (due || nowS - sentAt > 3 * 3600) clearAfter.edit().remove(name).apply()
            }
        }
        val readReports = remember { androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(forYouPrefs.getStringSet("read", emptySet()).orEmpty()) } }
        val markRead: (String) -> Unit = { k -> if (k !in readReports) { readReports.add(k); forYouPrefs.edit().putStringSet("read", readReports.toSet()).apply() } }
        var entries by remember { mutableStateOf<List<it.pixelbox.cmwatch.contract.TranscriptEntry>>(emptyList()) }
        // Di quale sessione sono le voci dal vivo: senza, la pagina appena raggiunta mostrava un attimo quelle di prima.
        var entriesOwner by remember { mutableStateOf<String?>(null) }
        // La sessione chiusa dal telefono con «Chiudi la sessione»: appena è chiusa si torna alla home (dal vivo 03/10 19:57:
        // la sua pagina restava nera).
        var leaving by remember { mutableStateOf<String?>(null) }
        // L'ultima conversazione letta di ogni sessione: riaprendo compare subito, poi si aggiorna.
        val feedCache = remember { mutableStateMapOf<String, List<it.pixelbox.cmwatch.contract.TranscriptEntry>>() }
        var more by remember { mutableStateOf(false) }
        var olderId by remember { mutableStateOf<String?>(null) }
        var unsupported by remember { mutableStateOf(false) }
        val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(chatName, transcriptOk, unsupported) {
            entries = chatName?.let { feedCache[it] }.orEmpty(); entriesOwner = chatName; more = false
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
                        is it.pixelbox.cmwatch.data.Repo.Opened.Refused -> {
                            // «too large: <byte>» in chiaro (Franz, 04/10 21:55): la misura del file e, se il PC lo dice, il limite.
                            val size = { b: Long -> android.text.format.Formatter.formatShortFileSize(this@MainActivity, b) }
                            val big = it.pixelbox.cmwatch.rules.FileRefusal.tooLarge(r.reason)
                            val msg = big?.let { b -> b.max?.let { m -> getString(R.string.file_refused_large_max, size(b.bytes), size(m)) } ?: getString(R.string.file_refused_large, size(b.bytes)) }
                                ?: getString(R.string.file_refused, r.reason)
                            android.widget.Toast.makeText(this@MainActivity, msg, android.widget.Toast.LENGTH_LONG).show()
                        }
                        it.pixelbox.cmwatch.data.Repo.Opened.Failed -> android.widget.Toast.makeText(this@MainActivity, getString(R.string.file_failed), android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        val sessionPage: @Composable (it.pixelbox.cmwatch.contract.Session, List<it.pixelbox.cmwatch.contract.TranscriptEntry>, (@Composable ColumnScope.((String) -> Unit, (String) -> Unit) -> Unit)?, Boolean, (@Composable () -> Unit)?, (@Composable () -> Unit)?, Boolean, (@Composable () -> Unit)?, (@Composable RowScope.() -> Unit)?) -> Unit = { session, pageEntries, home, header, dock, bar, homeOpen, appBar, lead ->
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
                            speak = { t -> speech.toggle(t, session.name) },
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
                            slash = { c, a -> if (c == "exit") leaving = session.name; scope.launch {
                                runCatching { app.repo.command(CmdOp.SLASH, session.name, c, a) }.getOrNull()?.let { id ->
                                    app.chatLog.add(Sent(id, session.name, "/$c" + (a?.let { t -> " $t" } ?: ""), System.currentTimeMillis() / 1000))
                                }
                            } },
                            attach = { uri, text -> attachAny(session.name, uri, text, state?.share) },
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
                            speakFrom = { t, i -> speech.speakBlocks(t, i, session.name) },
                            handoff = {
                                state?.let { st0 ->
                                    val clear = it.pixelbox.cmwatch.rules.ContextActions.canClear(st0)
                                    val fallback = getString(if (clear) R.string.ctx_handoff_clear_prompt else R.string.ctx_handoff_prompt)
                                    sendAndLog(PhonePrimary.Target.PROMPT, it.pixelbox.cmwatch.rules.ContextActions.handoffPrompt(st0, session, fallback))
                                    if (clear) clearAfter.edit().putLong(session.name, System.currentTimeMillis() / 1000).apply()
                                }
                            },
                            decision = if (state?.ops?.contains("decision") == true) ({ text, project ->
                                scope.launch {
                                    runCatching { app.repo.command(CmdOp.DECISION, null, project, text) }.onSuccess {
                                        android.widget.Toast.makeText(this@MainActivity, getString(R.string.decision_sent), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) else null,
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
                        ), chat = rows, quota = quotaWarn, accountQuota = state?.quota?.get(session.account), canTonight = nightDir != null,
                            // Niente frasi rapide (Franz, 01/10 23:34: «via» fisso, generico e fuori luogo): i consigli in più
                            // li scriverà la sessione stessa a fine turno.
                            choices = state?.choices, ops = state?.ops, canTune = !demo, canAttach = state?.share != null,
                            slash = state?.slash?.takeIf { state?.ops?.contains("slash") == true },
                            feed = if (transcriptOk && !unsupported && pageEntries.isNotEmpty()) ChatFeed.merge(pageEntries, rows.map { it.sent to it.status }, more) else null,
                            loadingFeed = transcriptOk && !unsupported && ChatFeed.loading(pageEntries, answered = session.name in feedCache),
                            more = more && session.name == chatName,
                            model = tunePicks[session.name + "/model"].let { p -> Tune.model(session, p, p?.let { results[it.cmd] }, now) },
                            effort = tunePicks[session.name + "/effort"].let { p -> Tune.effort(session, p, p?.let { results[it.cmd] }, now) },
                            home = home, grid = home != null && homeOpen, header = header, dock = dock, bar = bar, homeOpen = homeOpen, appBar = appBar,
                            headerLead = lead, draftState = drafts.state(session),
                            // Con l'ispettore obiettivo e bassa priorità stanno lì: in testata non si ripetono (revisione della master, 04/10).
                            notesInHeader = lead == null || !inspectorOn,
                            canAttachFiles = state?.share?.any == true,
                            // Sul riepilogo chi ti aspetta sta già nella lista: niente avviso doppio (ogni sessione una volta).
                            // Nelle colonne del tablet non si avvisa di chi è già in colonna: lo si vede accanto.
                            elsewhere = elsewhere.takeIf { home == null || !homeOpen }?.takeIf { a ->
                                !columnsOn || when (a) {
                                    is it.pixelbox.cmwatch.rules.Elsewhere.Waiting -> a.sessions.any { n -> n !in tabletCols }
                                    is it.pixelbox.cmwatch.rules.Elsewhere.Finished -> a.session !in tabletCols
                                }
                            },
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
        // Lo stesso se la sessione aperta sparisce dallo stato.
        LaunchedEffect(open, state?.sessions, leaving) {
            val o = open ?: return@LaunchedEffect
            val st = state ?: return@LaunchedEffect
            if (!StartRoute.stillOpen(o, st.sessions, leaving)) {
                if (leaving == o) leaving = null
                open = null; tab = StartRoute.Tab.OVERVIEW
            }
        }
        // La testata di ogni pagina della home e delle sessioni (Franz, 03/10 19:19): scorre e vola con la sua pagina.
        val menuActions = MenuActions(
            host, if (snap.freshness is Freshness.Stale) getString(R.string.menu_updated_ago, (snap.freshness as Freshness.Stale).minutes) else getString(R.string.menu_updated_now),
            snap.freshness is Freshness.Stale, onLaunch = { launching = true }, onRegister = { tab = StartRoute.Tab.DIARY; open = null },
            onQuadro = { overviewSheet = true }, onSearch = { searchOpen = true }, onSettings = { settingsOpen = true },
        )
        val pageHeader: @Composable (String?) -> Unit = { page ->
            PageHeader(
                page, state?.let { st -> PhoneBoard.sections(st).flatMap { sec -> sec.sessions } }.orEmpty(), summary?.open ?: 0, now,
                onPick = { n -> open = n; if (n == null) tab = StartRoute.Tab.OVERVIEW }, onClosed = { closedOpen = true }, menu = menuActions,
                quota = state?.let { st -> {
                    val rings = remember(st, events, samples, now, snap.freshness) {
                        PhoneOverview.build(st, events, samples, now, java.time.ZoneId.systemDefault(), stale = snap.freshness is Freshness.Stale).rings
                    }
                    QuotaLine(rings, onOpen = { overviewSheet = true })
                } },
                showQuota = !(masterChat && summary?.master != null),
            )
        }
        // «Ha finito» sparisce quando apri la sessione, da qualunque strada: riga, menu in alto, scorrimento, avviso, ricerca.
        LaunchedEffect(open) {
            val n = open ?: return@LaunchedEffect
            summary?.rows?.firstOrNull { r -> r.session.name == n && r.key != null }?.key?.let(markRead)
        }
        // Il Registro si apre dal menu a tutto schermo; Indietro torna al riepilogo.
        BackHandler(enabled = tab == StartRoute.Tab.DIARY && open == null) { tab = StartRoute.Tab.OVERVIEW }
        // Dalla chat della master Indietro torna alla lista delle sessioni, sempre nella home.
        BackHandler(enabled = masterChat && open == null && tab == StartRoute.Tab.OVERVIEW && !settingsOpen && terminal == null && !queueOpen && !searchOpen) { masterChat = false }
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
            val masterEntries = master?.let { m -> ChatFeed.pageEntries(m.name, chatName, entriesOwner, entries, feedCache) }.orEmpty()
            val list: @Composable () -> Unit = {
                SummaryList(
                    summary,
                    // Aprire una sessione che ha finito la toglie da «Ha finito», come in «Per te».
                    onOpen = { n ->
                        summary.rows.firstOrNull { r -> r.session.name == n && r.key != null }?.key?.let(markRead)
                        // Sul tablet il tocco sulla scheda apre e chiude la sua colonna (Franz, 05/10 11:09); le altre strade
                        // (ricerca, notifica, menu) aprono soltanto.
                        if (wide) saveCols(it.pixelbox.cmwatch.rules.Tablet.toggle(tabletCols, n)) else open = n
                    },
                    onAnswer = { n, k -> scope.launch { runCatching { app.repo.answer(n, k) } } },
                    onStep = sendPrompt, onService = forYouAction, onClosed = { closedOpen = true },
                    // Contratto 1.37: approvazioni in cima e «Chiudi» per le sessioni finite o doppie senza finestra.
                    approvals = state?.approvals.orEmpty(),
                    onApprove = { task, note -> scope.launch {
                        runCatching { app.repo.command(CmdOp.APPROVE, null, task, it.pixelbox.cmwatch.rules.MasterService.approveText(note)) }.onSuccess {
                            android.widget.Toast.makeText(this@MainActivity, getString(R.string.approved), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } },
                    canExit = state?.ops?.contains("slash") == true && state.slash?.contains("exit") == true,
                    onClose = { n -> scope.launch { runCatching { app.repo.command(CmdOp.SLASH, n, "exit", null) } } },
                    now = now,
                    footer = if (wide) ({
                        val rings = remember(st, events, samples, now, snap.freshness) {
                            PhoneOverview.build(st, events, samples, now, java.time.ZoneId.systemDefault(), stale = snap.freshness is Freshness.Stale).rings
                        }
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            rings.forEach { r -> TabletQuotaPanel(r, now, dataStale = snap.freshness is Freshness.Stale) }
                        }
                    }) else null,
                )
            }
            if (master != null) {
                val hero = remember(masterEntries, master.outcome) { it.pixelbox.cmwatch.rules.MasterHome.hero(masterEntries, master) }
                val homeList: @Composable ColumnScope.((String) -> Unit, (String) -> Unit) -> Unit = { _, _ -> list() }
                // Franz, 03/10 16:44: la master si espande dal basso a tutta pagina (la sua conversazione, con modello ed effort)
                // e torna ridotta toccando la sua barra, ora in cima. Ridotta: la lista con la barra sopra il campo. Il campo
                // scrive alla master in tutte e due.
                // Una pagina sola: si anima la parte sopra il campo, il campo resta fermo (`SessionSheet` con `homeOpen`).
                sessionPage(
                    master, masterEntries, homeList, true,
                    { MasterDock(master, hero, onSpeak = { t -> speech.toggle(t, it.pixelbox.cmwatch.rules.ContextActions.MASTER) }, onToggle = { masterChat = true }) },
                    { MasterDock(master, hero, onSpeak = { t -> speech.toggle(t, it.pixelbox.cmwatch.rules.ContextActions.MASTER) }, onToggle = { masterChat = false }, expanded = true) },
                    !masterChat, { pageHeader(null) }, null,
                )
            } else Column(Modifier.fillMaxSize()) {
                pageHeader(null)
                Box(Modifier.weight(1f)) { list() }
                Box(Modifier.padding(16.dp)) {
                    MasterAbsent { scope.launch { runCatching { app.repo.command(CmdOp.REOPEN, it.pixelbox.cmwatch.rules.ContextActions.MASTER, null) } } }
                }
            }
        }
        val speakingBlock by speech.block.collectAsStateWithLifecycle()
        val speechRate by speech.rate.collectAsStateWithLifecycle()
        // Lo zoom del testo della conversazione, uno per tutte le sessioni, salvato quando le dita si fermano.
        val uiPrefs = remember { getSharedPreferences("ui", MODE_PRIVATE) }
        var chatZoom by remember { mutableStateOf(it.pixelbox.cmwatch.rules.ChatZoom.of(uiPrefs.getFloat("chat_zoom", 1f))) }
        LaunchedEffect(chatZoom) { delay(400); uiPrefs.edit().putFloat("chat_zoom", chatZoom).apply() }
        val diaryPage: @Composable () -> Unit = {
            state?.let { st ->
                DiaryScreen(st, events.filter { it.kind == EventKind.QUOTA }, PhoneDiary.recaps(events), PhoneDiary.lastNightReport(events), ttsMinChars, speech::toggle,
                    onAdd = { nightAdding = true },
                    onRemove = { id -> scope.launch { app.repo.command(CmdOp.NIGHT_REMOVE, null, id) } },
                    rings = PhoneOverview.build(st, events, samples, now, java.time.ZoneId.systemDefault(), stale = snap.freshness is Freshness.Stale).rings,
                    onQuadro = { overviewSheet = true },
                    // Una riga del Registro apre la sessione del progetto, se è viva.
                    onSession = { n -> if (st.sessions.any { s -> s.name == n && s.state != it.pixelbox.cmwatch.contract.SessionState.GONE }) { open = n; tab = StartRoute.Tab.OVERVIEW } })
            }
        }
        // Contratto 1.29: la cronologia di oggi della sessione al centro, per l'ispettore del tablet. Una lettura passiva al
        // minuto, solo con l'app in primo piano; cambiando sessione si riparte da capo.
        var timeline by remember { mutableStateOf<it.pixelbox.cmwatch.contract.TimelinePage?>(null) }
        val timelineOk = demo || state?.ops?.contains("timeline") == true
        val inspected = tabletCols.firstOrNull()
        LaunchedEffect(inspectorOn, inspected, timelineOk) {
            timeline = null
            val name = inspected ?: return@LaunchedEffect
            if (!inspectorOn || !timelineOk) return@LaunchedEffect
            lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                while (true) {
                    val zone = java.time.ZoneId.systemDefault()
                    val id = runCatching { app.repo.command(CmdOp.TIMELINE, name, it.pixelbox.cmwatch.rules.Tablet.timelineArg(System.currentTimeMillis() / 1000, zone)) }.getOrNull()
                    if (id != null) {
                        val until = System.currentTimeMillis() + PhoneTerminal.LOST_MS
                        while (System.currentTimeMillis() < until) {
                            val r = app.repo.resultsById.value[id]
                            if (r != null) {
                                if (r.ok) runCatching { ContractJson.decodeTimeline(r.text) }.onSuccess { page -> timeline = page }
                                break
                            }
                            delay(500)
                        }
                        app.repo.forget(id)
                    }
                    delay(60_000)
                }
            }
        }
        // Le altre colonne: ognuna legge la sua conversazione con la cadenza della sessione aperta, nella cache delle
        // conversazioni; quella di `chatName` ha già il giro qui sopra.
        if (columnsOn && transcriptOk && !unsupported) tabletCols.filter { n -> n != chatName }.forEach { name ->
            androidx.compose.runtime.key(name) {
                LaunchedEffect(name) {
                    lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                        var askedAt: Long? = null
                        var pendingId: String? = null
                        var mode = ChatFeed.Page.FRESH
                        var prev = app.repo.snapshot.value.state?.sessions?.firstOrNull { x -> x.name == name }
                        while (true) {
                            val cur = app.repo.snapshot.value.state?.sessions?.firstOrNull { x -> x.name == name }
                            pendingId?.let { id -> app.repo.resultsById.value[id] }?.let { r ->
                                val have = feedCache[name].orEmpty()
                                feedCache[name] = when {
                                    r.ok -> runCatching { ContractJson.decodeTranscript(r.text) }.map { page -> ChatFeed.append(have, page, mode) }.getOrDefault(have)
                                    r.text.contains("no entry") -> emptyList()
                                    else -> have
                                }
                                pendingId = null
                            }
                            val answered = pendingId == null
                            val moved = answered && TerminalLive.next(prev, cur) != null
                            prev = cur
                            val t = System.currentTimeMillis()
                            if (moved || PhoneTerminal.shouldAskChat(cur, askedAt, answered, t)) {
                                askedAt = t
                                val have = feedCache[name].orEmpty()
                                mode = if (have.isEmpty()) ChatFeed.Page.FRESH else ChatFeed.Page.AFTER
                                pendingId?.let { id -> app.repo.forget(id) }
                                pendingId = runCatching { app.repo.command(CmdOp.TRANSCRIPT, name, ChatFeed.arg(ChatFeed.anchor(have))) }.getOrNull()
                            }
                            delay(1_000)
                        }
                    }
                }
            }
        }
        // Sul tablet il Registro sta al posto delle colonne; Indietro le riporta.
        BackHandler(enabled = wide && tab == StartRoute.Tab.DIARY && !settingsOpen && terminal == null && !queueOpen && !searchOpen) { tab = StartRoute.Tab.OVERVIEW }
        val tabletDesk: @Composable (it.pixelbox.cmwatch.contract.State, it.pixelbox.cmwatch.rules.Summary.Model) -> Unit = { st, sm ->
            val zone = java.time.ZoneId.systemDefault()
            val rows = remember(sm) { it.pixelbox.cmwatch.rules.Tablet.groups(sm).flatMap { g -> g.second } }
            val first = inspected?.let { n -> st.sessions.firstOrNull { x -> x.name == n } }
            TabletDesk(
                home = { summaryPage() }, homeRight = homeRight,
                onHomeSide = { homeRight = !homeRight; tabletPrefs.edit().putBoolean("tablet_home_right", homeRight).apply() },
                columns = tabletCols, shares = tabletShares,
                // Lo scambio porta con sé la larghezza della colonna.
                onSwap = { a, b ->
                    saveCols(it.pixelbox.cmwatch.rules.Tablet.swap(tabletCols, a, b))
                    saveShares(tabletShares.toMutableList().also { s -> val x = s[a]; s[a] = s[b]; s[b] = x })
                },
                onShares = saveShares,
                column = { name, drag ->
                    rows.firstOrNull { r -> r.session.name == name }?.let { r ->
                        sessionPage(
                            // La testata della sessione resta anche in colonna (Franz, 05/10 10:13): modello, quota, contesto e il
                            // menu ⋮ con Segui, Terminale e Chiudi la sessione.
                            r.session, ChatFeed.pageEntries(name, chatName, entriesOwner, entries, feedCache), null, true, null, null, true,
                            { TabletColumnHeader(r, now, onClose = { saveCols(it.pixelbox.cmwatch.rules.Tablet.toggle(tabletCols, name)) }, drag) }, null,
                        )
                    }
                },
                empty = { Text(getString(R.string.tablet_desk_empty), color = it.pixelbox.cmwatch.ui.tokens.CmColors.text2, modifier = Modifier.padding(32.dp)) },
                override = if (tab == StartRoute.Tab.DIARY) ({ diaryPage() }) else null,
                inspector = if (inspectorOn && first != null) ({
                    TabletInspector(it.pixelbox.cmwatch.rules.Tablet.inspect(first, timeline, now, zone), st.quota[first.account]?.h5, loading = timelineOk && timeline == null)
                }) else null,
                // La barra del titolo della finestra: le colonne come schede, a destra la quota delle 5 ore per account.
                caption = {
                    tabletCols.forEach { name ->
                        st.sessions.firstOrNull { x -> x.name == name }?.let { ses ->
                            it.pixelbox.cmwatch.mobile.ui.CaptionTab(ses, onFocus = { fieldFocus.target = name }, onClose = { saveCols(it.pixelbox.cmwatch.rules.Tablet.toggle(tabletCols, name)) })
                        }
                    }
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    st.quota.forEach { (acc, q) ->
                        q.h5?.let { h -> Text("$acc · " + getString(R.string.quota_h5_short, h), style = it.pixelbox.cmwatch.mobile.ui.MonoSmall, color = if (q.stale) it.pixelbox.cmwatch.ui.tokens.CmColors.waiting else it.pixelbox.cmwatch.ui.tokens.CmColors.text2) }
                    }
                },
            )
        }
        CompositionLocalProvider(
            LocalSpeaking provides speaking, LocalSpeakingBlock provides speakingBlock, LocalBlocksOf provides speech::blocksOf,
            LocalSpeechRate provides speechRate, LocalSetSpeechRate provides speech::setRateNow,
            it.pixelbox.cmwatch.mobile.ui.LocalFieldFocus provides fieldFocus,
            it.pixelbox.cmwatch.mobile.ui.LocalPromptBoxes provides promptBoxes,
            it.pixelbox.cmwatch.mobile.ui.LocalRecurring provides state?.recurring.orEmpty(),
            LocalChatZoom provides chatZoom, LocalSetChatZoom provides { z: Float -> chatZoom = z },
        ) {
        if (wide && state != null && summary != null) tabletDesk(state, summary) else
        AppShell(
            tab, demo, onTab = { tab = it; open = null }, onSettings = { settingsOpen = true },
            sessions = state?.let { st -> PhoneBoard.sections(st).flatMap { sec -> sec.sessions } }.orEmpty(),
            // La master dal menu in alto apre la sua chat come le altre (design 03/10); «Tutte le sessioni» torna al riepilogo.
            current = open, onPick = { n -> open = n; if (n == null) tab = StartRoute.Tab.OVERVIEW },
            onQuadro = { overviewSheet = true }, onSearch = { searchOpen = true }, onLaunch = { launching = true },
            host = host, stale = snap.freshness is Freshness.Stale,
            updated = when (val f = snap.freshness) { is Freshness.Stale -> getString(R.string.menu_updated_ago, f.minutes); else -> getString(R.string.menu_updated_now) },
            openCount = summary?.open ?: 0,
            // Con la master espansa la quota sotto la barra lascia spazio alla sua conversazione.
            masterChat = masterChat && masterName != null, now = now, onClosed = { closedOpen = true }, pagedHeaders = true,
            quota = state?.let { st -> {
                val rings = remember(st, events, samples, now, snap.freshness) {
                    PhoneOverview.build(st, events, samples, now, java.time.ZoneId.systemDefault(), stale = snap.freshness is Freshness.Stale).rings
                }
                QuotaLine(rings, onOpen = { overviewSheet = true })
            } },
            // Tirare giù chiede lo stato al PC; la rotella resta finché la risposta arriva o la richiesta fallisce.
            onRefresh = if (open == null) ({ refreshing = true; scope.launch { app.repo.refresh(); refreshing = false } }) else null,
            refreshing = refreshing,
        ) { page ->
            if (page == StartRoute.Tab.DIARY && open == null) {
                diaryPage()
                return@AppShell
            }
            SharedTransitionLayout {
                flight.AnimatedContent(transitionSpec = { EnterTransition.None togetherWith ExitTransition.None }, contentKey = { it != null }) { name ->
                    CompositionLocalProvider(LocalFly provides Fly(this@SharedTransitionLayout, this@AnimatedContent)) {
                        // Scorrimento laterale fra riepilogo e sessioni (Franz, 03/10 15:59): il riepilogo è sempre la prima
                        // pagina (`null`), poi le sessioni nell'ordine della regia (Franz, 30/09: «lo scroll laterale tra
                        // sessioni»). La pagina ferma decide la sessione aperta, e il menu in alto la segue.
                        // Solo il contenuto di destinazione segue la pagina: durante l'uscita (gesto indietro, volo verso la
                        // card) quello che se ne va non deve riaprire né spostarsi. Attivo = la sua chiave (sessione aperta sì/no)
                        // è quella di adesso; il nome della sessione può cambiare restando nello stesso contenuto (menu in alto).
                        val active = (name != null) == (flyTarget != null)
                        val target = if (active) open else name
                        val pages = remember(state?.sessions, target) { it.pixelbox.cmwatch.rules.SwipePages.of(state, target) }
                        val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = pages.indexOf(target).coerceAtLeast(0)) { pages.size }
                        val currentPages by rememberUpdatedState(pages)
                        // Solo uno scorrimento cambia la sessione aperta (`SwipePages.afterSettle`): la pagina vista quando il
                        // contenuto si riattiva è quella vecchia e non deve annullare la scelta del menu in alto (dal vivo 03/10 16:40).
                        LaunchedEffect(pager, active) {
                            if (!active) return@LaunchedEffect
                            var initial = true
                            androidx.compose.runtime.snapshotFlow { pager.settledPage }.collect { p ->
                                val move = it.pixelbox.cmwatch.rules.SwipePages.afterSettle(currentPages, p, open, initial)
                                initial = false
                                if (move is it.pixelbox.cmwatch.rules.SwipePages.Move.Open) {
                                    swipeSet = true
                                    open = move.name
                                    // Sul riepilogo si torna anche dal Registro: la prima pagina è sempre il riepilogo.
                                    if (move.name == null) tab = StartRoute.Tab.OVERVIEW
                                }
                                // Il cursore segue la pagina che si vede (Franz, 04/10 20:53): il pager teneva viva quella di prima
                                // e il testo scritto finiva lì. Sul riepilogo, che non ha campo, la tastiera si chiude.
                                val shown = currentPages.getOrNull(p)
                                val owner = fieldFocus.owner
                                if (owner != null && owner != shown) { if (shown != null) fieldFocus.target = shown else focusManager.clearFocus() }
                            }
                        }
                        LaunchedEffect(open, pages, active) {
                            val i = pages.indexOf(open)
                            // Una pagina accanto si raggiunge scorrendo, come col dito; una lontana subito, senza attraversare le altre.
                            if (active && i >= 0 && i != pager.currentPage && !pager.isScrollInProgress) {
                                if (kotlin.math.abs(i - pager.currentPage) == 1) pager.animateScrollToPage(i) else pager.scrollToPage(i)
                            }
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
                            val pageEntries = ChatFeed.pageEntries(session.name, open, entriesOwner, entries, feedCache)
                            sessionPage(session, pageEntries, null, true, null, null, true, { pageHeader(session.name) }, null)
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
                    Text(resources.getQuantityString(R.plurals.summary_closed, summary.closed.size, summary.closed.size), style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = it.pixelbox.cmwatch.ui.tokens.CmColors.text)
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

    /**
     * Un allegato dalla barra di scrittura: le immagini ridotte come sempre; con un relay 1.28 (`share.any`) ogni altro
     * file così com'è, con il suo nome. Oltre il limite si dice subito, senza leggerlo in memoria.
     */
    private fun attachAny(session: String, uri: Uri, text: String, share: it.pixelbox.cmwatch.contract.Share?) {
        val maxBytes = share?.maxBytes ?: 0
        val mime = contentResolver.getType(uri) ?: "application/octet-stream"
        if (mime.startsWith("image/") || share?.any != true) { attachImage(session, uri, text, maxBytes); return }
        val (name, size) = runCatching {
            contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME, android.provider.OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) (c.getString(0) ?: "file") to (if (c.isNull(1)) -1L else c.getLong(1)) else null
            }
        }.getOrNull() ?: ("file" to -1L)
        // La busta cifrata pesa circa 4/3 del file (base64): il file deve stare sotto i 3/4 del limite.
        val limit = maxBytes.toLong() * 3 / 4 - 2048
        if (size > limit) {
            android.widget.Toast.makeText(this, getString(R.string.file_too_big, android.text.format.Formatter.formatShortFileSize(this, limit)), android.widget.Toast.LENGTH_LONG).show()
            return
        }
        app.scope.launch {
            val bytes = runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull() ?: return@launch
            val id = java.util.UUID.randomUUID().toString()
            val shown = listOfNotNull(getString(R.string.attached_file, name), text.takeIf { it.isNotBlank() }).joinToString("\n")
            app.chatLog.add(Sent(id, session, shown, System.currentTimeMillis() / 1000))
            runCatching { app.repo.report(session, text, mime, bytes, maxBytes, id = id, name = name) }
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
        // Un invito di accoppiamento (cmwatch://pair?q=…); senza `q` è «Apri sul telefono» dall'orologio e apre l'app e basta.
        intent.data?.toString()?.let { uri -> it.pixelbox.cmwatch.pairing.PairLink.line(uri) }?.let { line -> pairInvite = line }
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

/**
 * La sessione aperta, con una regola sola per tutte le strade che la cambiano (card, menu in alto, notifiche, ricerca,
 * avvisi, «Fai controllare»): la master non diventa una pagina, apre la home sulla sua chat (`StartRoute.masterAtHome`).
 */
private class OpenRoute(
    private val open: MutableState<String?>, private val tab: MutableState<StartRoute.Tab>, private val masterChat: MutableState<Boolean>,
) {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): String? = open.value?.takeIf { !StartRoute.masterAtHome(it) }

    operator fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: String?) {
        if (StartRoute.masterAtHome(value)) {
            open.value = null; tab.value = StartRoute.Tab.OVERVIEW; masterChat.value = true
        } else open.value = value
    }
}

