package it.pixelbox.cmwatch.wear

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.rules.Screen
import it.pixelbox.cmwatch.rules.ViewState
import it.pixelbox.cmwatch.settings.Settings
import it.pixelbox.cmwatch.wear.ui.Routes
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
        val settings by app.prefs.flow.collectAsStateWithLifecycle(Settings(paired = true))
        val now by produceState(System.currentTimeMillis() / 1000) {
            while (true) { delay(30_000); value = System.currentTimeMillis() / 1000 }
        }
        // Domande già viste (chiuse senza rispondere): non si riaprono da sole.
        var seen by rememberSaveable { mutableStateOf(setOf<String>()) }
        val entry by nav.currentBackStackEntryFlow.collectAsStateWithLifecycle<NavBackStackEntry?>(null)
        val current = Routes.parse(entry?.destination?.route, entry?.arguments?.getString("name"))

        // Un solo ViewState: non accoppiato > domanda > schermata scelta.
        LaunchedEffect(snapshot.state, settings.paired, seen, deepLink.value) {
            val chosen = deepLink.value?.also { deepLink.value = null } ?: current
            val target = ViewState.reduce(snapshot, paired = true, chosen = chosen, seen = seen)
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
                    onWrite = { },
                    onTerminal = { nav.go(Screen.Terminal(name)) },
                    onFollow = { follow -> scope.launch { app.repo.command(if (follow) CmdOp.FOLLOW else CmdOp.UNFOLLOW, name, null) } },
                    onOutcome = { nav.go(Screen.Outcome(name)) },
                    onBackToSessions = { nav.go(Screen.Sessions) },
                )
            }
            composable(Routes.QUESTION) { back ->
                val name = back.arguments?.getString("name").orEmpty()
                SessionScreen(snapshot, name, now, onReply = {}, onWrite = {}, onTerminal = {}, onFollow = {}, onOutcome = {}, onBackToSessions = { nav.go(Screen.Sessions) })
            }
            composable(Routes.SETTINGS) { SessionsScreen(snapshot, now, onOpen = {}, onSettings = {}) }
            composable(Routes.PAIRING) { SessionsScreen(snapshot, now, onOpen = {}, onSettings = {}) }
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
