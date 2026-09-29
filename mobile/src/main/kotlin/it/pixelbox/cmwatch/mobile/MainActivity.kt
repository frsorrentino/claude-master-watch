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
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.mobile.pair.Phase
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.pairing.PairingRecord
import it.pixelbox.cmwatch.rules.PhonePrimary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val app get() = application as PhoneApp
    private val speech by lazy { Speech(this) }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    s.paired || s.demoMode -> Main(s.demoMode, s.ttsMinChars, s.host, s.pairingJson, ::scan)
                    else -> NotPairedScreen(onPair = ::scan, onPaste = { paste = true }, onDemo = { app.setDemo(true) })
                }
                if (paste) PasteDialog(onPair = { t -> paste = false; scope.launch { app.pairing.run(t) } }, onDismiss = { paste = false })
            }
        }
    }

    /** L'app accoppiata, o in Demo (design 29/09): due schede, scheda sessione, terminale, Lancia, impostazioni. */
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
        val results by app.repo.resultsById.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        var tab by rememberSaveable { mutableStateOf(Tab.SESSIONS) }
        var open by rememberSaveable { mutableStateOf<String?>(null) }       // nome della sessione aperta
        var terminal by rememberSaveable { mutableStateOf<String?>(null) }   // nome della sessione del terminale
        var screenId by rememberSaveable { mutableStateOf<String?>(null) }
        var settingsOpen by rememberSaveable { mutableStateOf(false) }
        var launching by rememberSaveable { mutableStateOf(false) }
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
                version = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty(),
                onRepair = onRepair, onDemo = { app.setDemo(it) },
                onNotifications = { startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)) },
                onPrivacy = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_url)))) },
            )
            return
        }
        terminal?.let { name ->
            val text = screenId?.let { results[it]?.text }
            TerminalScreen(name, text, loading = screenId != null && text == null, onRefresh = {
                scope.launch { screenId = app.repo.command(CmdOp.SCREEN, name, null) }
            })
            return
        }
        AppShell(tab, demo, onTab = { tab = it; open = null }, onSettings = { settingsOpen = true }) {
            if (tab == Tab.DIARY && open == null) {
                state?.let { st -> DiaryScreen(st, events.filter { it.kind == EventKind.QUOTA }, ttsMinChars, speech::speak) }
                return@AppShell
            }
            SharedTransitionLayout {
                flight.AnimatedContent(transitionSpec = { EnterTransition.None togetherWith ExitTransition.None }) { name ->
                    CompositionLocalProvider(LocalFly provides Fly(this@SharedTransitionLayout, this@AnimatedContent)) {
                        val session = name?.let { n -> state?.sessions?.firstOrNull { it.name == n } }
                        if (session != null) SessionSheet(session, now, snap.pending, ttsMinChars, SheetActions(
                            answer = { n -> scope.launch { app.repo.answer(session.name, n) } },
                            allowAll = { scope.launch { app.repo.command(CmdOp.ALLOW_ALL, session.name, null) } },
                            send = { target, text ->
                                scope.launch {
                                    if (target == PhonePrimary.Target.ANSWER_TEXT) app.repo.answerText(session.name, text) else app.repo.prompt(session.name, text)
                                }
                            },
                            follow = { on -> scope.launch { app.repo.command(if (on) CmdOp.FOLLOW else CmdOp.UNFOLLOW, session.name, null) } },
                            reopen = { scope.launch { app.repo.command(CmdOp.REOPEN, session.name, null) } },
                            terminal = { terminal = session.name; screenId = null },
                            openInClaude = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(session.link))) },
                            speak = speech::speak,
                            retry = { id -> scope.launch { app.repo.retry(id) } },
                        ))
                        else SessionsScreen(snap, now, onOpen = { id -> open = state?.sessions?.firstOrNull { it.id == id }?.name }, onLaunch = { launching = true })
                    }
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

    override fun onResume() {
        super.onResume()
        app.scope.launch { app.pairing.completePending() }
    }

    override fun onDestroy() {
        speech.stop()
        super.onDestroy()
    }
}
