package it.pixelbox.cmwatch.wear

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
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.rules.Screen
import it.pixelbox.cmwatch.rules.ViewState
import it.pixelbox.cmwatch.settings.Settings
import it.pixelbox.cmwatch.wear.haptics.Haptics
import it.pixelbox.cmwatch.wear.ui.Keyboard
import it.pixelbox.cmwatch.wear.ui.Routes
import it.pixelbox.cmwatch.crypto.KeyVault
import it.pixelbox.cmwatch.transport.FakeTransport
import it.pixelbox.cmwatch.wear.ui.screens.PairingScreen
import it.pixelbox.cmwatch.wear.ui.screens.PairingStatus
import it.pixelbox.cmwatch.wear.ui.screens.QuestionScreen
import it.pixelbox.cmwatch.wear.ui.screens.SettingsScreen
import it.pixelbox.cmwatch.wear.ui.screens.SessionScreen
import it.pixelbox.cmwatch.wear.ui.screens.SessionsScreen
import it.pixelbox.cmwatch.wear.ui.theme.CmTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val deepLink = mutableStateOf<Screen?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLink.value = Routes.fromDeepLink(intent?.data)
        val app = application as CmApp
        setContent { CmTheme { AppScaffold(timeText = { TimeText() }) { App(app) } } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLink.value = Routes.fromDeepLink(intent.data)
    }

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
        // Codice di pairing dalla tastiera di sistema.
        val codeInput = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            val code = Keyboard.result(res.data) ?: return@rememberLauncherForActivityResult
            pairing = PairingStatus.Working
            scope.launch {
                pairing = try {
                    val info = app.transport.pair(code, settings?.deviceName ?: "watch-pixel5")
                    val wrapped = KeyVault.wrap(KeyVault.newSessionKey(), KeyVault.keystoreKek())
                    app.prefs.update { it.copy(paired = true, uid = info.uid, host = info.host, wrappedKey = wrapped) }
                    Haptics.play(this@MainActivity, Haptics.Kind.CONFIRMED)
                    PairingStatus.Done(info.host)
                } catch (e: Exception) {
                    Haptics.play(this@MainActivity, Haptics.Kind.ERROR); PairingStatus.Failed(e.message ?: "")
                }
            }
        }
        // Demo: la fixture scelta nelle impostazioni (solo con il Transport finto).
        LaunchedEffect(settings?.demoFixture) {
            val fx = settings?.demoFixture ?: return@LaunchedEffect
            (app.transport as? FakeTransport)?.useFixture(fx)
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

        SwipeDismissableNavHost(navController = nav, startDestination = Routes.SESSIONS) {
            composable(Routes.SESSIONS) {
                SessionsScreen(snapshot, now, onOpen = { nav.go(Screen.Session(it)) }, onSettings = { nav.go(Screen.Settings) })
            }
            composable(Routes.SESSION) { back ->
                val name = back.arguments?.getString("name").orEmpty()
                SessionScreen(
                    snapshot, name, now,
                    onReply = { nav.go(Screen.Question(name)) },
                    onWrite = { write(name) },
                    onTerminal = { nav.go(Screen.Terminal(name)) },
                    onFollow = { follow -> scope.launch { app.repo.command(if (follow) CmdOp.FOLLOW else CmdOp.UNFOLLOW, name, null) } },
                    onOutcome = { nav.go(Screen.Outcome(name)) },
                    onBackToSessions = { nav.go(Screen.Sessions) },
                )
            }
            composable(Routes.QUESTION) { back ->
                val name = back.arguments?.getString("name").orEmpty()
                val qid = snapshot.state?.sessions?.firstOrNull { it.name == name }?.question?.id
                LaunchedEffect(qid) {
                    if (qid != null && hapticFor != qid) { hapticFor = qid; Haptics.play(this@MainActivity, Haptics.Kind.QUESTION) }
                }
                QuestionScreen(
                    snapshot, name, now, sentId,
                    onAnswer = { n -> if (qid != null) seen = seen + qid; scope.launch { sentId = app.repo.answer(name, n); Haptics.play(this@MainActivity, Haptics.Kind.SENT) } },
                    onFreeText = { if (qid != null) seen = seen + qid; write(name) },
                    onAllowAll = { if (qid != null) seen = seen + qid; scope.launch { sentId = app.repo.command(CmdOp.ALLOW_ALL, name, null) } },
                    onRetry = { id -> scope.launch { app.repo.retry(id) } },
                    onAnsweredElsewhere = { Haptics.play(this@MainActivity, Haptics.Kind.OUTCOME) },
                    onDone = { sentId = null; nav.go(Screen.Sessions) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settings ?: Settings(),
                    onChange = { s -> scope.launch { app.prefs.update { s } } },
                    onRepair = { scope.launch { app.prefs.update { it.copy(paired = false, uid = null, host = null, wrappedKey = null) }; pairing = PairingStatus.Idle } },
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
            composable(Routes.OUTCOME) { SessionsScreen(snapshot, now, onOpen = {}, onSettings = {}) }
            composable(Routes.TERMINAL) { SessionsScreen(snapshot, now, onOpen = {}, onSettings = {}) }
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
