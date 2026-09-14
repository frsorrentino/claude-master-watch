package it.pixelbox.cmwatch.wear

import it.pixelbox.cmwatch.wear.tts.Reader
import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.messaging.FirebaseMessaging
import it.pixelbox.cmwatch.BuildConfig
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.crypto.KeyVault
import it.pixelbox.cmwatch.crypto.Pairing
import it.pixelbox.cmwatch.data.PendingStatus
import it.pixelbox.cmwatch.data.Repo
import it.pixelbox.cmwatch.data.RoomStore
import it.pixelbox.cmwatch.rules.TransportChoice
import it.pixelbox.cmwatch.rules.Wake
import it.pixelbox.cmwatch.settings.Prefs
import it.pixelbox.cmwatch.settings.Settings
import it.pixelbox.cmwatch.transport.FakeTransport
import it.pixelbox.cmwatch.transport.FirebaseAuthToken
import it.pixelbox.cmwatch.transport.FirebaseTransport
import it.pixelbox.cmwatch.transport.Rtdb
import it.pixelbox.cmwatch.transport.SwitchableTransport
import it.pixelbox.cmwatch.transport.Transport
import it.pixelbox.cmwatch.wear.complication.CmComplicationService
import it.pixelbox.cmwatch.wear.follow.FollowOngoing
import it.pixelbox.cmwatch.wear.tts.Speaker
import it.pixelbox.cmwatch.wear.push.Notifier
import it.pixelbox.cmwatch.wear.tile.CmTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.runBlocking

class CmApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var prefs: Prefs
    lateinit var transport: SwitchableTransport
    lateinit var repo: Repo
    lateinit var notifier: Notifier
    lateinit var follow: FollowOngoing
    val speaker: Speaker by lazy { Speaker(this) }
    val reader: Reader by lazy { Reader(this) }
    val fake: FakeTransport by lazy { FakeTransport(load = { assets.open("contract/$it.json").bufferedReader().readText() }) }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        notifier = Notifier(this).also { it.ensureChannels() }
        val settings = runBlocking { prefs.current() }
        transport = SwitchableTransport(choose(settings))
        val store = RoomStore.open(this)
        repo = Repo(store, transport, scope, { System.currentTimeMillis() / 1000 }, ::isOnline, settings.deviceName)
        // Batteria (Franz, 14/09 17:18): gli stream RTDB solo con l'app in primo piano. Aperti ad app chiusa costavano
        // 40 mAh in 15 ore e, senza Wi-Fi, riprovavano ogni 30 s; chiusa, la sveglia è FCM e la tile rilegge da sé.
        // Si parte spenti: il processo può nascere in background (FCM, tile) e allora non riceverebbe mai onStop.
        repo.start(live = false)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = repo.live(true)
            override fun onStop(owner: LifecycleOwner) = repo.live(false)
        })
        follow = FollowOngoing(this)
        scope.launch { repo.snapshot.collect { follow.update(it.state, System.currentTimeMillis() / 1000) } }
        // Il diff che decide le notifiche gira su OGNI nuovo /state (stream o risveglio FCM): con il processo vivo lo stream
        // arriva prima del worker e il risveglio da solo non vedrebbe nulla di nuovo.
        scope.launch {
            repo.snapshot.map { it.state }.filterNotNull().distinctUntilChanged().collect { cur ->
                val prev = lastState; lastState = cur
                if (prev != null) react(prev, cur)
            }
        }
        // Aggiornamento in place delle notifiche: /result → «confermato»; comando fallito → «non consegnato · Riprova».
        scope.launch { repo.results.collect { r -> if (r.ok) notifier.confirmed(r.id) else notifier.failed(r.id) } }
        scope.launch {
            repo.snapshot.collect { snap ->
                snap.pending.filter { p -> p.status == PendingStatus.FAILED && p.cmd.id in notifier.pendingBySession }
                    .forEach { notifier.failed(it.cmd.id) }
            }
        }
        scope.launch { prefs.flow.map { it.ttsVoice }.distinctUntilChanged().collect { speaker.setVoice(it) } }
        subscribeTopic()
    }

    /** Firebase solo se accoppiato, con la chiave nel vault e google-services.json presente; altrimenti il finto sulle fixture. */
    fun choose(settings: Settings): Transport {
        val key = settings.wrappedKey?.let { runCatching { KeyVault.unwrap(it, KeyVault.keystoreKek()) }.getOrNull() }
        val kind = TransportChoice.pick(settings.paired, key != null, BuildConfig.FIREBASE && FirebaseAuthToken.databaseUrl() != null)
        return when (kind) {
            TransportChoice.Kind.FAKE -> fake
            TransportChoice.Kind.FIREBASE -> FirebaseTransport(
                rtdb = Rtdb(FirebaseAuthToken.databaseUrl()!!.removeSuffix("/"), token = { FirebaseAuthToken.token() }),
                key = { key }, uid = { FirebaseAuthToken.uid() }, deviceKeyPair = { Pairing.newKeyPair() },
                now = { System.currentTimeMillis() / 1000 },
            )
        }
    }

    /** Il pairing va SEMPRE sul bus reale quando Firebase c'è (il finto accetta qualsiasi codice). */
    fun pairingTransport(): Transport {
        val url = if (BuildConfig.FIREBASE) FirebaseAuthToken.databaseUrl() else null
        return if (url == null) fake else FirebaseTransport(
            rtdb = Rtdb(url.removeSuffix("/"), token = { FirebaseAuthToken.token() }),
            key = { null }, uid = { FirebaseAuthToken.uid() }, deviceKeyPair = { Pairing.newKeyPair() },
            now = { System.currentTimeMillis() / 1000 },
        )
    }

    /** Dopo il pairing o un nuovo pairing: il Transport cambia a caldo. */
    fun reconfigure() { scope.launch { transport.switchTo(choose(prefs.current())) } }

    @Volatile private var lastState: State? = null

    private fun foreground(): Boolean =
        runCatching { ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }.getOrDefault(false)

    /** Sveglia (FCM): un GET; le notifiche partono dal diff sullo stream (react). */
    suspend fun onWake(notify: Boolean) { repo.refresh() }

    /** Ciò che è cambiato fra due /state → notifiche (solo con l'app non in primo piano), tile, complication. */
    suspend fun react(prev: State?, cur: State) {
        val notify = !foreground()
        val s = prefs.current()
        var notified = false
        for (a in Wake.plan(prev, cur)) when (a) {
            is Wake.Action.Notify -> if (notify) when (a.kind) {
                Wake.NotifyKind.QUESTION -> cur.sessions.firstOrNull { it.name == a.session && it.question?.id !in s.seenQuestions }?.let { notifier.question(it); notified = true }
                Wake.NotifyKind.OUTCOME -> cur.sessions.firstOrNull { it.name == a.session }?.let { notifier.outcome(it); notified = true }
                Wake.NotifyKind.GONE -> a.session?.let { notifier.gone(it, prev?.sessions?.firstOrNull { p -> p.name == it }?.account); notified = true }
                Wake.NotifyKind.QUOTA -> a.session?.let { acc -> cur.quota[acc]?.let { notifier.quota(acc, it); notified = true } }
            }
            Wake.Action.RefreshTile -> runCatching { CmTileService.requestUpdate(this) }
            Wake.Action.RefreshComplications -> runCatching { CmComplicationService.requestUpdate(this) }
        }
        if (notified) notifier.summary(cur)
    }

    fun subscribeTopic() {
        if (!BuildConfig.FIREBASE) return
        runCatching {
            val fm = FirebaseMessaging.getInstance()
            fm.token.addOnCompleteListener { t -> android.util.Log.i("cmwatch", "fcm token: " + (if (t.isSuccessful) "ok (${t.result?.take(12)}…)" else "failed ${t.exception?.message}")) }
            fm.subscribeToTopic(FCM_TOPIC).addOnCompleteListener { t -> android.util.Log.i("cmwatch", "fcm topic $FCM_TOPIC: " + (if (t.isSuccessful) "subscribed" else "failed ${t.exception?.message}")) }
        }.onFailure { android.util.Log.w("cmwatch", "fcm: ${it.message}") }
    }

    fun isOnline(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object { const val FCM_TOPIC = "watch" }
}
