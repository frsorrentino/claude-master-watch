package it.pixelbox.cmwatch.wear

import it.pixelbox.cmwatch.rules.SpeechText
import it.pixelbox.cmwatch.rules.AnswerText
import it.pixelbox.cmwatch.wear.push.SpeakService
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavBackStackEntry
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import it.pixelbox.cmwatch.BuildConfig
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.rules.LaunchRules
import it.pixelbox.cmwatch.rules.Screen
import it.pixelbox.cmwatch.rules.ViewState
import it.pixelbox.cmwatch.settings.Settings
import it.pixelbox.cmwatch.wear.haptics.Haptics
import it.pixelbox.cmwatch.pairing.FirebaseBoot
import it.pixelbox.cmwatch.wear.pair.PhoneLauncher
import androidx.compose.material.icons.rounded.PhoneAndroid
import it.pixelbox.cmwatch.wear.ui.Keyboard
import it.pixelbox.cmwatch.wear.ui.Routes
import it.pixelbox.cmwatch.wear.ui.ambient.LocalAmbient
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.foundation.rememberAmbientModeManager
import it.pixelbox.cmwatch.wear.ui.ambient.rememberAmbient
import it.pixelbox.cmwatch.crypto.KeyVault
import it.pixelbox.cmwatch.transport.FakeTransport
import it.pixelbox.cmwatch.wear.ui.screens.PairingScreen
import it.pixelbox.cmwatch.wear.ui.screens.PairingStatus
import it.pixelbox.cmwatch.wear.ui.screens.QuestionScreen
import it.pixelbox.cmwatch.wear.ui.screens.SettingsScreen
import it.pixelbox.cmwatch.wear.ui.screens.TerminalScreen
import it.pixelbox.cmwatch.wear.ui.screens.LaunchScreen
import it.pixelbox.cmwatch.wear.ui.screens.QuotaScreen
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.data.PendingStatus
import it.pixelbox.cmwatch.contract.Night
import it.pixelbox.cmwatch.contract.Recap
import it.pixelbox.cmwatch.wear.ui.screens.SessionScreen
import it.pixelbox.cmwatch.wear.ui.screens.SessionsScreen
import it.pixelbox.cmwatch.wear.ui.theme.CmTheme
import it.pixelbox.cmwatch.rules.TerminalLive
import it.pixelbox.cmwatch.wear.ui.components.CmTimeText
import it.pixelbox.cmwatch.wear.ui.components.CmConfirm
import it.pixelbox.cmwatch.wear.ui.components.CmConfirmState
import it.pixelbox.cmwatch.ui.tokens.CmColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.wear.compose.material3.confirmationDialogCurvedText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    private val deepLink = mutableStateOf<Screen?>(null)
    private val pairCodeFromIntent = mutableStateOf<String?>(null)
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private fun link(intent: Intent?): Screen? =
        Routes.fromDeepLink(intent?.data ?: intent?.getStringExtra(EXTRA_URI)?.let(android.net.Uri::parse))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLink.value = link(intent)
        pairCodeFromIntent.value = intent?.getStringExtra(EXTRA_PAIR_CODE)?.takeIf { it.matches(Regex("\\d{6}")) }
        val app = application as CmApp
        demoExtra(intent, app)
        askNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        // Solo debug: `adb shell am start … --ez demo_paired true` salta il pairing dove non c'è la tastiera Wear (ARC).
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("demo_paired", false) == true) {
            app.scope.launch { app.prefs.update { it.copy(paired = true, host = "demo") } }
        }
        setContent {
            // Ambient dal gestore di Wear Compose (handoff 14/09): il nostro `AmbientLifecycleObserver`, anche registrato
            // in onCreate, non riceveva eventi e Wear OS si limitava a scurire l'app («not eligible for ambient lite»).
            val ambientManager = rememberAmbientModeManager()
            androidx.compose.runtime.CompositionLocalProvider(LocalAmbient provides (ambientManager.currentAmbientMode is AmbientMode.Ambient)) {
                CmTheme {
                    AppScaffold(timeText = {
                        // Accanto all'ora, curvo, quante sessioni aspettano una risposta (B10, 15/09 23:40).
                        val snap by app.repo.snapshot.collectAsStateWithLifecycle()
                        CmTimeText(waiting = if (snap.freshness is Freshness.Fresh) snap.state?.sessions?.count { it.question != null } ?: 0 else 0)
                    }) { App(app) }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLink.value = link(intent)
        pairCodeFromIntent.value = intent.getStringExtra(EXTRA_PAIR_CODE)?.takeIf { it.matches(Regex("\\d{6}")) }
        demoExtra(intent, application as CmApp)
    }

    /** L'extra `demo` (solo via adb): accende o spegne la demo per i video. Senza extra non cambia niente. */
    private fun demoExtra(intent: Intent?, app: CmApp) {
        if (intent?.hasExtra(EXTRA_DEMO) == true) app.setDemo(intent.getBooleanExtra(EXTRA_DEMO, false))
        // Scorrimento guidato per i video: `--ei scroll_px 520 --ei scroll_ms 2200`, eseguito solo con la demo accesa.
        if (intent?.hasExtra(EXTRA_SCROLL_PX) == true) {
            val px = intent.getIntExtra(EXTRA_SCROLL_PX, 0); val ms = intent.getIntExtra(EXTRA_SCROLL_MS, 2000)
            app.scope.launch { if (app.prefs.current().demoMode) it.pixelbox.cmwatch.wear.ui.components.DemoScrollBus.requests.emit(px to ms) }
        }
        // Storia dei video (piano 16/09): la scena `--es demo_step question` e la dettatura simulata `--es demo_dictation "…"`.
        intent?.getStringExtra(EXTRA_DEMO_STEP)?.let { app.setDemoStep(it, intent.getIntExtra(EXTRA_DEMO_DELAY_MS, 0).toLong()) }
        intent?.getStringExtra(EXTRA_DEMO_DICTATION)?.let { app.demoDictation = it }
    }

    companion object { const val EXTRA_URI = "cmwatch_uri"; const val EXTRA_PAIR_CODE = "pair_code"; const val EXTRA_DEMO = "demo"; const val EXTRA_SCROLL_PX = "scroll_px"; const val EXTRA_SCROLL_MS = "scroll_ms"; const val EXTRA_DEMO_STEP = "demo_step"; const val EXTRA_DEMO_DICTATION = "demo_dictation"; const val EXTRA_DEMO_DELAY_MS = "demo_delay_ms" }

    @Composable
    private fun App(app: CmApp) {
        val nav = rememberSwipeDismissableNavController()
        val scope = rememberCoroutineScope()
        val snapshot by app.repo.snapshot.collectAsStateWithLifecycle()
        val settings by app.prefs.flow.collectAsStateWithLifecycle(null)
        val paired = settings?.paired ?: true   // finché le preferenze non sono lette, nessun salto al pairing
        var pairing by remember { mutableStateOf<PairingStatus>(PairingStatus.Idle) }
        val now by produceState(System.currentTimeMillis() / 1000) {
            while (true) { delay(30_000); value = System.currentTimeMillis() / 1000 }
        }
        // Domande già viste (chiuse senza rispondere): non si riaprono da sole.
        var seen by rememberSaveable { mutableStateOf(setOf<String>()) }
        var sentId by rememberSaveable { mutableStateOf<String?>(null) }
        var hapticFor by rememberSaveable { mutableStateOf<String?>(null) }
        // Dopo «Parliamone» la tastiera scrive un messaggio, anche se la domanda risulta ancora aperta per un attimo.
        var forcePrompt by remember { mutableStateOf(false) }
        // Sessione → id del `reopen` mandato: «Avvio in corso» finché il PC risponde e la sessione torna (15/09 19:14).
        var reopening by remember { mutableStateOf(mapOf<String, String>()) }
        val allResults by app.repo.resultsById.collectAsStateWithLifecycle()
        fun reopenStatus(n: String): it.pixelbox.cmwatch.rules.ReopenText.Status? = reopening[n]?.let { id ->
            it.pixelbox.cmwatch.rules.ReopenText.status(
                allResults[id],
                gone = snapshot.state?.sessions?.firstOrNull { it.name == n }?.state == it.pixelbox.cmwatch.contract.SessionState.GONE,
                notDelivered = snapshot.pending.any { it.cmd.id == id && it.status == PendingStatus.FAILED },
            )
        }
        fun reopen(n: String) = scope.launch {
            reopening = reopening + (n to app.repo.command(CmdOp.REOPEN, n, null))
            Haptics.play(this@MainActivity, Haptics.Kind.SENT)
        }
        // Tastiera di sistema: il testo libero va alla sessione scelta come «prompt».
        var writeTarget by rememberSaveable { mutableStateOf<String?>(null) }
        val keyboard = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            val text = Keyboard.result(res.data); val target = writeTarget
            // Con la domanda aperta il testo va a «Type something.» (contratto 1.10); dopo «Parliamone» è un messaggio.
            val aperta = !forcePrompt && snapshot.state?.sessions?.firstOrNull { it.name == target }?.question != null
            forcePrompt = false
            if (text != null && target != null) scope.launch {
                sentId = if (aperta) app.repo.answerText(target, text) else app.repo.prompt(target, text)
                Haptics.play(this@MainActivity, Haptics.Kind.SENT)
            }
        }
        // «Nuova sessione» (contratto 1.13): il primo messaggio dalla tastiera, poi il launch; il `/result` dice come si
        // chiama la sessione nata, e appena compare nello stato si apre la sua Scheda.
        var launchPath by rememberSaveable { mutableStateOf<String?>(null) }
        var launchId by rememberSaveable { mutableStateOf<String?>(null) }
        fun launch(path: String, text: String?) {
            scope.launch {
                launchId = app.repo.command(CmdOp.LAUNCH, null, path, text?.takeIf { it.isNotBlank() })
                Haptics.play(this@MainActivity, Haptics.Kind.SENT)
            }
        }
        val launchKeyboard = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            val text = Keyboard.result(res.data); val path = launchPath
            // Tastiera chiusa senza testo: non si lancia niente, si resta sulla scelta del progetto.
            if (text != null && path != null) launch(path, text)
            launchPath = null
        }
        fun write(name: String) {
            // Demo per i video: la «dettatura» preparata via adb va come se l'avesse restituita la tastiera.
            val dettato = app.demoDictation
            if (settings?.demoMode == true && dettato != null) {
                app.demoDictation = null
                val aperta = !forcePrompt && snapshot.state?.sessions?.firstOrNull { it.name == name }?.question != null
                forcePrompt = false
                scope.launch {
                    sentId = if (aperta) app.repo.answerText(name, dettato) else app.repo.prompt(name, dettato)
                    Haptics.play(this@MainActivity, Haptics.Kind.SENT)
                }
                return
            }
            writeTarget = name
            runCatching { keyboard.launch(Keyboard.intent(getString(R.string.write_hint, name))) }
                .onFailure { Haptics.play(this@MainActivity, Haptics.Kind.ERROR) }   // nessuna tastiera Wear (es. ARC)
        }
        // Codice di pairing: dalla tastiera di sistema o dall'extra d'intent `pair_code` (adb / automazioni).
        fun startPairing(code: String) {
            android.util.Log.i("cmwatch", "pairing with code of ${code.length} digits")
            pairing = PairingStatus.Working
            scope.launch {
                pairing = try {
                    val info = app.pairingTransport().pair(code, settings?.deviceName ?: "watch-pixel5")
                    val wrapped = KeyVault.wrap(info.key ?: KeyVault.newSessionKey(), KeyVault.keystoreKek())
                    app.prefs.update { it.copy(paired = true, uid = info.uid, host = info.host, wrappedKey = wrapped) }
                    app.reconfigure()
                    Haptics.play(this@MainActivity, Haptics.Kind.CONFIRMED)
                    PairingStatus.Done(info.host)
                } catch (e: Exception) {
                    android.util.Log.w("cmwatch", "pairing failed", e)
                    Haptics.play(this@MainActivity, Haptics.Kind.ERROR); PairingStatus.Failed(e.message ?: "")
                }
            }
        }
        val codeInput = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            android.util.Log.i("cmwatch", "remote input result code=${res.resultCode} extras=${res.data?.extras?.keySet()?.joinToString()}")
            val code = Keyboard.result(res.data)
            if (code == null) { android.util.Log.w("cmwatch", "remote input: no text"); return@rememberLauncherForActivityResult }
            startPairing(code)
        }
        // Attenzione: startPairing PRIMA di azzerare la chiave, altrimenti l'effetto viene cancellato a metà.
        LaunchedEffect(pairCodeFromIntent.value) {
            val code = pairCodeFromIntent.value ?: return@LaunchedEffect
            startPairing(code)
            pairCodeFromIntent.value = null
        }
        // Demo: la fixture scelta nelle impostazioni (solo con il Transport finto).
        LaunchedEffect(settings?.demoFixture) {
            val fx = settings?.demoFixture ?: return@LaunchedEffect
            (app.transport.active as? FakeTransport)?.useFixture(fx)
        }
        // Conferma animata delle azioni dell'utente (A6, 15/09 23:40): quando il PC risponde, la spunta che si disegna o la
        // croce, insieme alla vibrazione. Solo le azioni dell'utente: le catture del Terminale dal vivo vibravano a ogni giro.
        // La conferma porta con sé la sua frase (Franz, 16/09 01:58): dopo una pressione lunga «Ti avviso quando finisce»,
        // non «Fatto». Le azioni che non hanno una frase propria mostrano «Fatto» quando il PC risponde.
        var conferma by remember { mutableStateOf<CmConfirmState?>(null) }
        fun conferma(icon: ImageVector, tint: Color, text: String) { conferma = CmConfirmState(icon, tint, text) }
        // Dopo «Nuova sessione»: appena il PC dice il nome della sessione nata e quella compare nello stato, si apre la
        // sua Scheda. Rifiutato senza sessione: il motivo del PC nella conferma. Nata ma messaggio non consegnato: si apre
        // lo stesso, così le si può riscrivere (contratto 1.13).
        LaunchedEffect(launchId, allResults[launchId], snapshot.state) {
            val r = launchId?.let { allResults[it] } ?: return@LaunchedEffect
            val nata = r.session
            when {
                nata != null && snapshot.state?.sessions?.any { it.name == nata } == true -> { launchId = null; nav.go(Screen.Session(nata)) }
                nata == null && !r.ok -> { launchId = null; conferma(Icons.Rounded.Close, CmColors.gone, r.text) }
                nata == null -> launchId = null
            }
        }
        LaunchedEffect(Unit) {
            app.repo.userResults.collect { r ->
                Haptics.play(this@MainActivity, if (r.ok) Haptics.Kind.CONFIRMED else Haptics.Kind.ERROR)
                when {
                    !r.ok -> conferma(Icons.Rounded.Close, CmColors.gone, getString(R.string.confirm_failed))
                    // La frase e il segno li ha già messi l'azione: non li sovrascrivo.
                    conferma == null -> conferma(Icons.Rounded.Check, CmColors.idle, getString(R.string.confirm_done))
                }
            }
        }
        // La nostra conferma al posto di quella di sistema (Franz, 16/09 03:08): cerchio, segno, frase sotto, niente
        // testo curvo né forma ruotata. Si chiude da sola dopo poco più di un secondo, o al tocco.
        CmConfirm(conferma) { conferma = null }
        // Accoppiato dal telefono (design 24/09): le preferenze passano a paired, qui si dice con chi.
        var wasPaired by remember { mutableStateOf<Boolean?>(null) }
        LaunchedEffect(settings?.paired, settings?.host) {
            val now = settings?.paired ?: return@LaunchedEffect
            if (wasPaired == false && now && pairing !is PairingStatus.Done) {
                conferma(Icons.Rounded.Check, CmColors.briefGood, getString(R.string.pairing_done, settings?.host.orEmpty()))
            }
            wasPaired = now
        }
        val entry by nav.currentBackStackEntryFlow.collectAsStateWithLifecycle<NavBackStackEntry?>(null)
        val current = Routes.parse(entry?.destination?.route, entry?.arguments?.getString("name"))

        // Un solo ViewState: non accoppiato > domanda > schermata scelta.
        val demo = settings?.demoMode == true
        LaunchedEffect(snapshot.state, paired, demo, seen, deepLink.value) {
            val chosen = deepLink.value?.also { deepLink.value = null } ?: current
            val target = ViewState.reduce(snapshot, paired = paired, chosen = chosen, seen = seen, demo = demo)
            if (target != current) nav.go(target)
        }

        val ambient = rememberAmbient()
        SwipeDismissableNavHost(navController = nav, startDestination = Routes.SESSIONS) {
            composable(Routes.SESSIONS) {
                SessionsScreen(
                    snapshot, now, onOpen = { nav.go(Screen.Session(it)) }, onSettings = { nav.go(Screen.Settings) }, onMenu = { nav.go(it) }, ambient = ambient,
                    // Contratto 1.9: una chiusa si riprende dalla sua riga.
                    onReopen = { n -> reopen(n) },
                    reopenStatus = { n -> reopenStatus(n) },
                    // Pressione lunga sulla riga: segui / smetti, con la vibrazione come conferma (Franz, 15/09 19:04).
                    onFollow = { n, follow ->
                        // Gli interruttori hanno un segno proprio: acceso sale, spento scende (16/09 03:00).
                        Haptics.play(this@MainActivity, if (follow) Haptics.Kind.TOGGLE_ON else Haptics.Kind.TOGGLE_OFF)
                        // Il segno e la frase li mette la pressione lunga, subito: campanella accesa o barrata.
                        conferma(
                            if (follow) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff,
                            if (follow) CmColors.followed else CmColors.text2,
                            getString(if (follow) R.string.confirm_follow else R.string.confirm_unfollow),
                        )
                        scope.launch { app.repo.command(if (follow) CmdOp.FOLLOW else CmdOp.UNFOLLOW, n, null) }
                    },
                )
            }
            composable(Routes.SESSION) { back ->
                val name = back.arguments?.getString("name").orEmpty()
                val speaking by app.speaker.speaking.collectAsStateWithLifecycle()
                val preparing by app.reader.preparing.collectAsStateWithLifecycle()
                // Chi lavora senza uno strumento in vista: si chiede al PC la coda del terminale e se ne prende l'ultimo
                // blocco, al posto di «turno in corso» (Franz, 15/09 19:01).
                val sess = snapshot.state?.sessions?.firstOrNull { it.name == name }
                val senzaStrumento = sess != null && sess.question == null && sess.tool.isNullOrBlank() &&
                    (sess.state == it.pixelbox.cmwatch.contract.SessionState.BUSY || sess.state == it.pixelbox.cmwatch.contract.SessionState.AWAITING)
                var live by remember(name) { mutableStateOf<String?>(null) }
                var liveId by remember(name) { mutableStateOf<String?>(null) }
                LaunchedEffect(name, senzaStrumento) { if (senzaStrumento) liveId = app.repo.command(CmdOp.SCREEN, name, null) }
                val liveResults by app.repo.resultsById.collectAsStateWithLifecycle()
                LaunchedEffect(liveId, liveResults) {
                    val r = liveId?.let { liveResults[it] } ?: return@LaunchedEffect
                    if (r.ok) live = SpeechText.terminal(r.text, blocks = 1).takeIf { it.isNotBlank() }
                }
                SessionScreen(
                    snapshot, name, now,
                    onReply = { nav.go(Screen.Question(name)) },
                    onWrite = { write(name) },
                    onTerminal = { nav.go(Screen.Terminal(name)) },
                    onFollow = { follow ->
                        Haptics.play(this@MainActivity, if (follow) Haptics.Kind.TOGGLE_ON else Haptics.Kind.TOGGLE_OFF)
                        conferma(
                            if (follow) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff,
                            if (follow) CmColors.followed else CmColors.text2,
                            getString(if (follow) R.string.confirm_follow else R.string.confirm_unfollow),
                        )
                        scope.launch { app.repo.command(if (follow) CmdOp.FOLLOW else CmdOp.UNFOLLOW, name, null) }
                    },
                    live = live,
                    onBackToSessions = { nav.go(Screen.Sessions) },
                    speaking = speaking || preparing,
                    onListen = snapshot.state?.sessions?.firstOrNull { it.name == name }?.outcome?.let { o -> { SpeakService.last(this@MainActivity, name, o.full) } },
                    onReopen = { reopen(name); Unit },
                    reopen = reopenStatus(name),
                    onRelaunch = snapshot.state?.let { st ->
                        st.sessions.firstOrNull { it.name == name }
                            ?.let { LaunchRules.pathFor(it, st.projects) }
                            ?.let { path -> { scope.launch { app.repo.command(CmdOp.LAUNCH, null, path); Haptics.play(this@MainActivity, Haptics.Kind.SENT) }; Unit } }
                    },
                )
            }
            composable(Routes.QUESTION) { back ->
                val name = back.arguments?.getString("name").orEmpty()
                val qid = snapshot.state?.sessions?.firstOrNull { it.name == name }?.question?.id
                LaunchedEffect(qid) {
                    if (qid != null && hapticFor != qid) { hapticFor = qid; Haptics.play(this@MainActivity, Haptics.Kind.QUESTION) }
                }
                // Uscire dalla Domanda senza rispondere (swipe/back) la segna come vista: non si riapre da sola.
                androidx.compose.runtime.DisposableEffect(qid) { onDispose { if (qid != null) seen = seen + qid } }
                val speaking by app.speaker.speaking.collectAsStateWithLifecycle()
                val preparing by app.reader.preparing.collectAsStateWithLifecycle()
                QuestionScreen(
                    snapshot, name, now, sentId,
                    onAnswer = { n -> if (qid != null) seen = seen + qid; scope.launch { sentId = app.repo.answer(name, n); Haptics.play(this@MainActivity, Haptics.Kind.SENT) } },
                    onFreeText = { if (qid != null) seen = seen + qid; write(name) },
                    // «Chat about this» (contratto 1.10): la domanda si chiude e la sessione aspetta un messaggio,
                    // quindi la tastiera si apre subito per scriverlo (consiglio del relay, 15/09 19:27).
                    onChat = {
                        if (qid != null) seen = seen + qid
                        scope.launch { sentId = app.repo.chat(name); Haptics.play(this@MainActivity, Haptics.Kind.SENT) }
                        forcePrompt = true; write(name)
                    },
                    onAllowAll = { if (qid != null) seen = seen + qid; scope.launch { sentId = app.repo.command(CmdOp.ALLOW_ALL, name, null) } },
                    onRetry = { id -> scope.launch { app.repo.retry(id) } },
                    onAnsweredElsewhere = { Haptics.play(this@MainActivity, Haptics.Kind.OUTCOME) },
                    onDone = { sentId = null; nav.go(Screen.Sessions) },
                    speaking = speaking || preparing,
                    onSpeak = { snapshot.state?.sessions?.firstOrNull { it.name == name }?.question?.let { q -> SpeakService.text(this@MainActivity, SpeechText.question(q, getString(R.string.tts_option))) } },
                )
            }
            composable(Routes.SETTINGS) {
                val voices by app.speaker.voices.collectAsStateWithLifecycle()
                SettingsScreen(
                    settings ?: Settings(),
                    onChange = { s -> scope.launch { app.prefs.update { s } } },
                    onRepair = { scope.launch { app.prefs.update { it.copy(paired = false, uid = null, host = null, wrappedKey = null) }; pairing = PairingStatus.Idle; app.reconfigure() } },
                    notificationsEnabled = app.notifier.enabled(),
                    onNotificationSettings = { startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)) },
                    voices = voices,
                    accounts = snapshot.state?.quota?.keys?.sorted().orEmpty(),
                    onVoice = { v -> scope.launch { app.prefs.update { it.copy(ttsVoice = v) } }; app.speaker.setVoice(v); app.speaker.speak(getString(R.string.tts_voice_sample)) },
                    onDemo = { on -> app.setDemo(on) },
                )
            }
            composable(Routes.PAIRING) {
                val withCode = remember { FirebaseBoot.bundled(this@MainActivity) != null }
                PairingScreen(
                    pairing,
                    withCode = withCode,
                    onOpenPhone = {
                        scope.launch {
                            if (PhoneLauncher.open(this@MainActivity)) conferma(Icons.Rounded.PhoneAndroid, CmColors.primary, getString(R.string.pairing_continue_phone))
                            else Haptics.play(this@MainActivity, Haptics.Kind.ERROR)
                        }
                    },
                    onEnterCode = {
                        runCatching { codeInput.launch(Keyboard.intent(getString(R.string.pairing_code_label))) }
                            .onFailure { pairing = PairingStatus.Failed("no keyboard") }
                    },
                    onRetry = { pairing = PairingStatus.Idle },
                    onDemo = { app.setDemo(true) },
                )
            }
            composable(Routes.TERMINAL) { back ->
                val name = back.arguments?.getString("name").orEmpty()
                var text by remember { mutableStateOf<String?>(null) }
                var error by remember { mutableStateOf<String?>(null) }
                var cmdId by remember { mutableStateOf<String?>(null) }
                var answer by remember { mutableStateOf<String?>(null) }
                var lastId by remember { mutableStateOf<String?>(null) }
                var capturedAt by remember { mutableStateOf<Long?>(null) }
                val fallback = snapshot.state?.sessions?.firstOrNull { it.name == name }?.outcome?.full
                // Due richieste al PC: le righe del terminale e la risposta intera (contratto 1.4, `last`), che in cima si
                // legge a paragrafi (Franz, 15/09 17:19). Se il PC tace per 5 s resta la coda che l'orologio ha già.
                fun ask() {
                    text = null; error = null; answer = null
                    scope.launch { cmdId = app.repo.command(CmdOp.SCREEN, name, null); lastId = app.repo.command(CmdOp.LAST, name, null) }
                }
                LaunchedEffect(name) { ask() }
                val results by app.repo.resultsById.collectAsStateWithLifecycle()
                LaunchedEffect(cmdId, results) {
                    val r = cmdId?.let { results[it] } ?: return@LaunchedEffect
                    // Una cattura dal vivo che fallisce non cancella quella che si vede: l'errore conta solo senza testo.
                    if (r.ok) { text = r.text; capturedAt = System.currentTimeMillis() } else if (text == null) error = r.text
                }
                LaunchedEffect(lastId, results) {
                    val r = lastId?.let { results[it] } ?: return@LaunchedEffect
                    answer = SpeechText.pick(r, fallback).orEmpty()
                }
                LaunchedEffect(lastId) { if (lastId != null) { kotlinx.coroutines.delay(5_000L); if (answer == null) answer = fallback.orEmpty() } }
                // Dal vivo (design 15/09): a ogni cambio della sessione nello stato, che arriva già in streaming, si chiede
                // di nuovo la cattura senza cancellare quella che si vede; una richiesta alla volta, almeno 3 s fra due, e a
                // fine turno anche la risposta intera. Aggiorna a mano passa di qui, con la risposta.
                val session = snapshot.state?.sessions?.firstOrNull { it.name == name }
                var prev by remember { mutableStateOf(session) }
                var alsoLast by remember { mutableStateOf(false) }
                val wake = remember { Channel<Unit>(Channel.CONFLATED) }
                LaunchedEffect(session) {
                    when (TerminalLive.next(prev, session)) {
                        TerminalLive.Ask.SCREEN -> wake.trySend(Unit)
                        TerminalLive.Ask.SCREEN_AND_LAST -> { alsoLast = true; wake.trySend(Unit) }
                        null -> Unit
                    }
                    prev = session
                }
                LaunchedEffect(name) {
                    var lastAt = 0L
                    while (true) {
                        wake.receive()
                        val wait = 3_000L - (System.currentTimeMillis() - lastAt)
                        if (wait > 0) delay(wait)
                        val withLast = alsoLast
                        alsoLast = false
                        val id = app.repo.command(CmdOp.SCREEN, name, null)
                        cmdId = id
                        if (withLast) lastId = app.repo.command(CmdOp.LAST, name, null)
                        lastAt = System.currentTimeMillis()
                        withTimeoutOrNull(10_000L) { app.repo.resultsById.first { it.containsKey(id) } }
                    }
                }
                val blocks = remember(answer) { answer?.let { AnswerText.blocks(it) } }
                val block by app.speaker.block.collectAsStateWithLifecycle()
                val failed = snapshot.pending.any { it.cmd.id == cmdId && it.status == PendingStatus.FAILED }
                val speaking by app.speaker.speaking.collectAsStateWithLifecycle()
                val preparing by app.reader.preparing.collectAsStateWithLifecycle()
                TerminalScreen(
                    name, text, loading = text == null && error == null && !failed,
                    error = if (text != null) null else error ?: if (failed) getString(R.string.question_not_delivered) else null,
                    capturedAt = capturedAt, session = session,
                    // Il filo delle tue righe nel colore del badge della sessione (proposta 38, fase 1).
                    railColor = session?.let { ses ->
                        androidx.compose.ui.graphics.Color(it.pixelbox.cmwatch.rules.Badge.of(ses.account, ses.color, ses.state, ses.icon, ses.accountKind).fill)
                    } ?: it.pixelbox.cmwatch.ui.tokens.CmColors.accent,
                    answer = blocks,
                    current = if (speaking) block else null,
                    speaking = speaking || preparing,
                    // Il ▶ in testata legge la risposta a paragrafi; senza risposta, le ultime righe del terminale.
                    onSpeakAll = {
                        val b = blocks.orEmpty()
                        if (b.isNotEmpty()) SpeakService.blocks(this@MainActivity, b.map { it.text }, 0, all = true)
                        else text?.let { t -> SpeakService.text(this@MainActivity, SpeechText.terminal(t)) }
                    },
                    onBlock = { i -> blocks?.let { b -> SpeakService.blocks(this@MainActivity, b.map { it.text }, i, all = false) } },
                    onWrite = { write(name) },
                )
            }
            composable(Routes.LAUNCH) {
                LaunchScreen(
                    snapshot.state?.projects.orEmpty(), enabled = snapshot.freshness is Freshness.Fresh,
                    onLaunch = { path -> launch(path, null); nav.go(Screen.Sessions) },
                    onWrite = { p ->
                        val dettato = app.demoDictation
                        if (settings?.demoMode == true && dettato != null) {
                            app.demoDictation = null
                            launch(p.path, dettato)
                            return@LaunchScreen
                        }
                        launchPath = p.path
                        runCatching { launchKeyboard.launch(Keyboard.intent(getString(R.string.launch_hint, p.name))) }
                            .onFailure { Haptics.play(this@MainActivity, Haptics.Kind.ERROR) }
                    },
                )
            }
            composable(Routes.QUOTA) {
                // Eventi per «Oggi» e campioni della quota per il ritmo della finestra (Franz, 16/09 13:00).
                val eventiVeri by app.repo.events.collectAsStateWithLifecycle()
                val eventiDemo by app.fake.events.collectAsStateWithLifecycle(emptyList())
                // In demo «Oggi» conta solo gli eventi della demo: con quelli veri salvati su Room diceva 336 (16/09).
                val events = if (settings?.demoMode == true) eventiDemo else eventiVeri
                val samples by app.repo.quotaSamples.collectAsStateWithLifecycle()
                QuotaScreen(
                    snapshot.state, snapshot.freshness, events = events, samples = samples,
                    onOpenQuestion = { nav.go(Screen.Question(it)) }, onOpenSessions = { nav.go(Screen.Sessions) },
                )
            }
        }
    }
}

private fun NavHostController.go(screen: Screen) {
    val route = Routes.of(screen)
    if (currentDestination?.route == route) return
    navigate(route) {
        launchSingleTop = true
        if (screen == Screen.Sessions) popUpTo(Routes.SESSIONS) { inclusive = false }
    }
}
