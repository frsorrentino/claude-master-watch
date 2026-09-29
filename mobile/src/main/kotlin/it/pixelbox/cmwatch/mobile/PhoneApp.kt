package it.pixelbox.cmwatch.mobile

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import it.pixelbox.cmwatch.crypto.Pairing
import it.pixelbox.cmwatch.data.Repo
import it.pixelbox.cmwatch.data.RoomStore
import it.pixelbox.cmwatch.mobile.pair.KeystoreKeyWrap
import it.pixelbox.cmwatch.mobile.pair.PairingController
import it.pixelbox.cmwatch.mobile.pair.PcPairer
import it.pixelbox.cmwatch.mobile.pair.SdkPhoneFirebase
import it.pixelbox.cmwatch.mobile.pair.WearWatchLink
import it.pixelbox.cmwatch.pairing.FirebaseBoot
import it.pixelbox.cmwatch.pairing.FirebaseConfig
import it.pixelbox.cmwatch.pairing.PhonePairer
import it.pixelbox.cmwatch.rules.TransportChoice
import it.pixelbox.cmwatch.settings.Prefs
import it.pixelbox.cmwatch.settings.Settings
import it.pixelbox.cmwatch.transport.DemoText
import it.pixelbox.cmwatch.transport.FakeTransport
import it.pixelbox.cmwatch.transport.FirebaseAuthToken
import it.pixelbox.cmwatch.transport.FirebaseTransport
import it.pixelbox.cmwatch.transport.Rtdb
import it.pixelbox.cmwatch.transport.SwitchableTransport
import it.pixelbox.cmwatch.transport.Transport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PhoneApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var prefs: Prefs
    lateinit var pairing: PairingController
    lateinit var transport: SwitchableTransport
    lateinit var repo: Repo
    val phoneName: String by lazy {
        android.provider.Settings.Global.getString(contentResolver, android.provider.Settings.Global.DEVICE_NAME) ?: android.os.Build.MODEL
    }

    /** La Demo (design 29/09): le fixture del contratto con i testi della demo, come sull'orologio. */
    val fake: FakeTransport by lazy { FakeTransport(load = { DemoText.dress(assets.open("contract/$it.json").bufferedReader().readText()) }) }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        val settings = runBlocking { prefs.current() }
        FirebaseBoot.start(this, FirebaseConfig.fromJson(settings.firebaseJson))
        pairing = PairingController(
            store = prefs,
            firebase = SdkPhoneFirebase(this),
            link = WearWatchLink(this),
            keys = KeystoreKeyWrap,
            pairer = { fb -> PcPairer { qr, uid, name, w -> PhonePairer(fb.rtdb()).pair(qr, uid, name, w) } },
            phoneName = phoneName,
        )
        transport = SwitchableTransport(choose(settings))
        repo = Repo(RoomStore.open(this), transport, scope, { System.currentTimeMillis() / 1000 }, ::isOnline, phoneName)
        // Come sull'orologio: lo stream RTDB solo con l'app in primo piano; chiusa, la sveglia è FCM.
        repo.start(live = false)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = repo.live(true)
            override fun onStop(owner: LifecycleOwner) = repo.live(false)
        })
    }

    /** Firebase solo se accoppiato, con la chiave nel vault e Firebase avviato; altrimenti la Demo. */
    fun choose(settings: Settings): Transport {
        val key = settings.wrappedKey?.let { runCatching { KeystoreKeyWrap.unwrap(it) }.getOrNull() }
        val fb = FirebaseBoot.active
        return when (TransportChoice.pick(settings.paired, key != null, fb != null, demo = settings.demoMode)) {
            TransportChoice.Kind.FAKE -> fake
            TransportChoice.Kind.FIREBASE -> FirebaseTransport(
                rtdb = Rtdb(fb!!.databaseUrl.removeSuffix("/"), token = { FirebaseAuthToken.token() }),
                key = { key }, uid = { FirebaseAuthToken.uid() }, deviceKeyPair = { Pairing.newKeyPair() },
                now = { System.currentTimeMillis() / 1000 },
            )
        }
    }

    fun setDemo(on: Boolean) {
        scope.launch {
            prefs.update { it.copy(demoMode = on) }
            transport.switchTo(choose(prefs.current()))
            if (on) repo.seedQuotaSamples(fake.demoQuotaSamples()) else repo.refresh()
        }
    }

    /** Dopo l'accoppiamento o il suo rifacimento: il Transport cambia a caldo. */
    fun reconfigure() { scope.launch { transport.switchTo(choose(prefs.current())) } }

    fun isOnline(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
