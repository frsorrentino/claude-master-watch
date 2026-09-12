package it.pixelbox.cmwatch.wear

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import it.pixelbox.cmwatch.data.Repo
import it.pixelbox.cmwatch.data.RoomStore
import it.pixelbox.cmwatch.settings.Prefs
import it.pixelbox.cmwatch.transport.FakeTransport
import it.pixelbox.cmwatch.transport.Transport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CmApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var prefs: Prefs
    lateinit var transport: Transport
    lateinit var repo: Repo

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        // Finché il relay reale non c'è: il Transport finto sulle fixture del contratto (assets/contract).
        transport = FakeTransport(load = { assets.open("contract/$it.json").bufferedReader().readText() })
        val store = RoomStore.open(this)
        repo = Repo(store, transport, scope, { System.currentTimeMillis() / 1000 }, ::isOnline, "watch-pixel5")
        repo.start()
    }

    fun isOnline(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
