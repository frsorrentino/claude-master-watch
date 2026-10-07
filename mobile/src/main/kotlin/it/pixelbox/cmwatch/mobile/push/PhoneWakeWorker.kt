package it.pixelbox.cmwatch.mobile.push

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import it.pixelbox.cmwatch.mobile.PhoneApp
import java.util.concurrent.TimeUnit

/**
 * La ripresa della sveglia FCM sul telefono (piano prestazioni, Task 11): se il GET dello stato fallisce dentro
 * onMessageReceived, si riprova con attese crescenti da 10 s, al massimo tre volte. Prima un GET perso in un cambio di rete
 * lasciava lo stato vecchio fino all'evento dopo.
 */
class PhoneWakeWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ok = (applicationContext as PhoneApp).repo.refresh()
        return if (ok || runAttemptCount >= 3) Result.success() else Result.retry()
    }

    companion object {
        fun enqueue(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<PhoneWakeWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS).setInitialDelay(5, TimeUnit.SECONDS).build()
            WorkManager.getInstance(ctx).enqueueUniqueWork("phone-wake", ExistingWorkPolicy.REPLACE, req)
        }
    }
}
