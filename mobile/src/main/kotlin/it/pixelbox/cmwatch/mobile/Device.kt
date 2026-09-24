package it.pixelbox.cmwatch.mobile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.VibratorManager
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.tasks.await

/** Lo scanner dei servizi Google Play: niente permesso fotocamera. Null se l'utente annulla. */
object Scanner {
    suspend fun scan(ctx: Context): String? = runCatching {
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        GmsBarcodeScanning.getClient(ctx, options).startScan().await().rawValue
    }.getOrNull()
}

/** Da capo, con il QR salvato: Firebase si avvia una volta per processo, e il QR è di un altro progetto. */
object Restarter {
    fun restart(activity: Activity) {
        val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}

/** «Fatto» con la stessa vibrazione dell'orologio (`Haptics.Kind.CONFIRMED`). */
object Buzz {
    fun done(ctx: Context) {
        ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
            ?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 20, 60, 20), -1))
    }
}
