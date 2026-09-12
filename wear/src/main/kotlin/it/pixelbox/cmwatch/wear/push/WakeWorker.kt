package it.pixelbox.cmwatch.wear.push

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import it.pixelbox.cmwatch.wear.CmApp

/** Sveglia FCM → lavoro expedited: un GET di /state, diff con Room, notifiche solo per il nuovo, tile e complication. */
class WakeWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as CmApp
        val foreground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        app.onWake(notify = !foreground)
        return Result.success()
    }

    companion object {
        fun enqueue(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<WakeWorker>().setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()
            WorkManager.getInstance(ctx).enqueueUniqueWork("wake", ExistingWorkPolicy.APPEND_OR_REPLACE, req)
        }
    }
}
