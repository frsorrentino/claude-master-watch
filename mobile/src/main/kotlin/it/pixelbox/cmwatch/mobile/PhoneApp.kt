package it.pixelbox.cmwatch.mobile

import android.app.Application
import it.pixelbox.cmwatch.pairing.FirebaseBoot
import it.pixelbox.cmwatch.pairing.FirebaseConfig
import it.pixelbox.cmwatch.settings.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

class PhoneApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var prefs: Prefs
    val phoneName: String by lazy {
        android.provider.Settings.Global.getString(contentResolver, android.provider.Settings.Global.DEVICE_NAME) ?: android.os.Build.MODEL
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        FirebaseBoot.start(this, FirebaseConfig.fromJson(runBlocking { prefs.current() }.firebaseJson))
    }
}
