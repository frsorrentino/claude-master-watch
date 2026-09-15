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
import it.pixelbox.cmwatch.wear.ui.screens.TimelineScreen
import it.pixelbox.cmwatch.wear.ui.screens.LaunchScreen
import it.pixelbox.cmwatch.wear.ui.screens.QuotaScreen
import it.pixelbox.cmwatch.wear.ui.screens.RecapScreen
import it.pixelbox.cmwatch.wear.ui.screens.NightScreen
import it.pixelbox.cmwatch.wear.ui.screens.MenuScreen
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.data.PendingStatus
import it.pixelbox.cmwatch.contract.Night
import it.pixelbox.cmwatch.contract.Recap
import it.pixelbox.cmwatch.wear.ui.screens.SessionScreen
import it.pixelbox.cmwatch.wear.ui.screens.SessionsScreen
import it.pixelbox.cmwatch.wear.ui.theme.CmTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                CmTheme { AppScaffold(timeText = { TimeText() }) { App(app) } }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLink.value = link(intent)
        pairCodeFromIntent.value = intent.getStringExtra(EXTRA_PAIR_CODE)?.takeIf { it.matches(Regex("\\d{6}")) }
    }

    companion object { const val EXTRA_URI = "cmwatch_uri"; const val EXTRA_PAIR_CODE = "pair_code" }

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
        // Tastiera di sistema: il testo libero va alla sessione scelta come «prompt».
        var writeTarget by rememberSaveable { mutableStateOf<String?>(null) }
        val keyboard = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            val text = Keyboard.result(res.data); val target = writeTarget
            if (text != null && target != null) scope.launch { sentId = app.repo.prompt(target, text); Haptics.play(this@MainActivity, Haptics.Kind.SENT) }
        }
        fun write(name: String) {
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
        LaunchedEffect(Unit) {
            app.repo.results.collect { r -> Haptics.play(this@MainActivity, if (r.ok) Haptics.Kind.CONFIRMED else Haptics.Kind.ERROR) }
        }
        val entry by nav.currentBackStackEntryFlow.collectAsStateWithLifecycle<NavBackStackEntry?>(null)
        val current = Routes.parse(entry?.destination?.route, entry?.arguments?.getString("name"))

        // Un solo ViewState: non accoppiato > domanda > schermata scelta.
        LaunchedEffect(snapshot.state, paired, seen, deepLink.value) {
            val chosen = deepLink.value?.also { deepLink.value = null } ?: current
            val target = ViewState.reduce(snapshot, paired = paired, chosen = chosen, seen = seen)
            if (target != current) nav.go(target)
        }

        val ambient = rememberAmbient()
        SwipeDismissableNavHost(navController = nav, startDestination = Routes.SESSIONS) {
            composable(Routes.SESSIONS) {
                SessionsScreen(snapshot, now, onOpen = { nav.go(Screen.Session(it)) }, onSettings = { nav.go(Screen.Settings) }, onMenu = { nav.go(it) }, ambient = ambient)
            }
            composable(Routes.SESSION) { back ->
                val name = back.arguments?.getString("name").orEmpty()
                val speaking by app.speaker.speaking.collectAsStateWithLifecycle()
                val preparing by app.reader.preparing.collectAsStateWithLifecycle()
                SessionScreen(
                    snapshot, name, now,
                    onReply = { nav.go(Screen.Question(name)) },
                    onWrite = { write(name) },
                    onTerminal = { nav.go(Screen.Terminal(name)) },
                    onFollow = { follow -> scope.launch { app.repo.command(if (follow) CmdOp.FOLLOW else CmdOp.UNFOLLOW, name, null) } },
                    onBackToSessions = { nav.go(Screen.Sessions) },
                    speaking = speaking || preparing,
                    onListen = snapshot.state?.sessions?.firstOrNull { it.name == name }?.outcome?.let { o -> { SpeakService.last(this@MainActivity, name, o.full) } },
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
                )
            }
            composable(Routes.PAIRING) {
                PairingScreen(
                    pairing,
                    onEnterCode = {
                        runCatching { codeInput.launch(Keyboard.intent(getString(R.string.pairing_code_label))) }
                            .onFailure { pairing = PairingStatus.Failed("no keyboard") }
                    },
                    onRetry = { pairing = PairingStatus.Idle },
                )
            }
            composable(Routes.TERMINAL) { back ->
                val name = back.arguments?.getString("name").orEmpty()
                var text by remember { mutableStateOf<String?>(null) }
                var error by remember { mutableStateOf<String?>(null) }
                var cmdId by remember { mutableStateOf<String?>(null) }
                var answer by remember { mutableStateOf<String?>(null) }
                var lastId by remember { mutableStateOf<String?>(null) }
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
                    if (r.ok) text = r.text else error = r.text
                }
                LaunchedEffect(lastId, results) {
                    val r = lastId?.let { results[it] } ?: return@LaunchedEffect
                    answer = SpeechText.pick(r, fallback).orEmpty()
                }
                LaunchedEffect(lastId) { if (lastId != null) { kotlinx.coroutines.delay(5_000L); if (answer == null) answer = fallback.orEmpty() } }
                val blocks = remember(answer) { answer?.let { AnswerText.blocks(it) } }
                val block by app.speaker.block.collectAsStateWithLifecycle()
                val failed = snapshot.pending.any { it.cmd.id == cmdId && it.status == PendingStatus.FAILED }
                val speaking by app.speaker.speaking.collectAsStateWithLifecycle()
                val preparing by app.reader.preparing.collectAsStateWithLifecycle()
                TerminalScreen(
                    name, text, loading = text == null && error == null && !failed,
                    error = error ?: if (failed) getString(R.string.question_not_delivered) else null,
                    onRefresh = { ask() },
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
                )
            }
            composable(Routes.TIMELINE) { val events by app.repo.events.collectAsStateWithLifecycle(); TimelineScreen(events) }
            composable(Routes.LAUNCH) {
                LaunchScreen(snapshot.state?.projects.orEmpty(), enabled = snapshot.freshness is Freshness.Fresh) { path -> scope.launch { app.repo.command(CmdOp.LAUNCH, null, path); Haptics.play(this@MainActivity, Haptics.Kind.SENT) }; nav.go(Screen.Sessions) }
            }
            composable(Routes.QUOTA) { QuotaScreen(snapshot.state, snapshot.freshness) }
            composable(Routes.RECAP) {
                val speaking by app.speaker.speaking.collectAsStateWithLifecycle()
                val preparing by app.reader.preparing.collectAsStateWithLifecycle()
                val recap = snapshot.state?.recap ?: Recap()
                RecapScreen(recap, speaking || preparing, onSpeak = { SpeakService.text(this@MainActivity, SpeechText.recap(recap, getString(R.string.tts_recap_next))) })
            }
            composable(Routes.NIGHT) { NightScreen(snapshot.state?.night ?: Night()) }
            composable(Routes.MENU) { MenuScreen(onOpen = { nav.go(it) }) }
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
