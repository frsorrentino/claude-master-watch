package it.pixelbox.cmwatch.wear

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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CmApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var prefs: Prefs
    lateinit var transport: SwitchableTransport
    lateinit var repo: Repo
    lateinit var notifier: Notifier
    lateinit var follow: FollowOngoing
    val speaker: Speaker by lazy { Speaker(this) }
    val fake: FakeTransport by lazy { FakeTransport(load = { assets.open("contract/$it.json").bufferedReader().readText() }) }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        notifier = Notifier(this).also { it.ensureChannels() }
        val settings = runBlocking { prefs.current() }
        transport = SwitchableTransport(choose(settings))
        val store = RoomStore.open(this)
        repo = Repo(store, transport, scope, { System.currentTimeMillis() / 1000 }, ::isOnline, settings.deviceName)
        repo.start()
        follow = FollowOngoing(this)
        scope.launch { repo.snapshot.collect { follow.update(it.state, System.currentTimeMillis() / 1000) } }
        // Aggiornamento in place delle notifiche: /result → «confermato»; comando fallito → «non consegnato · Riprova».
        scope.launch { repo.results.collect { r -> if (r.ok) notifier.confirmed(r.id) else notifier.failed(r.id) } }
        scope.launch {
            repo.snapshot.collect { snap ->
                snap.pending.filter { p -> p.status == PendingStatus.FAILED && p.cmd.id in notifier.pendingBySession }
                    .forEach { notifier.failed(it.cmd.id) }
            }
        }
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

    /** Sveglia (FCM): un GET, poi ciò che è cambiato → notifiche, tile, complication. */
    suspend fun onWake(notify: Boolean) {
        val prev: State? = repo.snapshot.value.state
        if (!repo.refresh()) return
        val cur = repo.snapshot.value.state ?: return
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
