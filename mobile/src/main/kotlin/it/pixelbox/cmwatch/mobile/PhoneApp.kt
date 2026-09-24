package it.pixelbox.cmwatch.mobile

import android.app.Application
import it.pixelbox.cmwatch.mobile.pair.KeystoreKeyWrap
import it.pixelbox.cmwatch.mobile.pair.PairingController
import it.pixelbox.cmwatch.mobile.pair.PcPairer
import it.pixelbox.cmwatch.mobile.pair.SdkPhoneFirebase
import it.pixelbox.cmwatch.mobile.pair.WearWatchLink
import it.pixelbox.cmwatch.pairing.FirebaseBoot
import it.pixelbox.cmwatch.pairing.FirebaseConfig
import it.pixelbox.cmwatch.pairing.PhonePairer
import it.pixelbox.cmwatch.settings.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

class PhoneApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var prefs: Prefs
    lateinit var pairing: PairingController
    val phoneName: String by lazy {
        android.provider.Settings.Global.getString(contentResolver, android.provider.Settings.Global.DEVICE_NAME) ?: android.os.Build.MODEL
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        FirebaseBoot.start(this, FirebaseConfig.fromJson(runBlocking { prefs.current() }.firebaseJson))
        pairing = PairingController(
            store = prefs,
            firebase = SdkPhoneFirebase(this),
            link = WearWatchLink(this),
            keys = KeystoreKeyWrap,
            pairer = { fb -> PcPairer { qr, uid, name, w -> PhonePairer(fb.rtdb()).pair(qr, uid, name, w) } },
            phoneName = phoneName,
        )
    }
}
