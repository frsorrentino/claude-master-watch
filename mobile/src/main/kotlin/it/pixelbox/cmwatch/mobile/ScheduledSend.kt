package it.pixelbox.cmwatch.mobile

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import it.pixelbox.cmwatch.contract.CmdOp
import java.util.concurrent.TimeUnit

/**
 * L'invio programmato (piano 30/09, Task 4): manda i messaggi della chat la cui ora è passata, con il loro id, così la
 * chat ne segue i passaggi come per gli altri. Un lavoro all'ora dell'invio e un giro ogni 15 minuti per chi l'ha perso
 * (telefono spento, app aggiornata): `ChatLog.claimDue` li prende una volta sola. Senza rete il comando resta nella coda
 * offline del Repo e parte quando torna.
 */
class ScheduledSend(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as PhoneApp
        for (m in app.chatLog.claimDue()) {
            runCatching { app.repo.command(CmdOp.PROMPT, m.session, m.text, id = m.id) }
                .onFailure { app.chatLog.markFailed(m.id, app.getString(R.string.queue_full)) }
        }
        return Result.success()
    }

    companion object {
        /** Il lavoro all'ora dell'invio, più il giro periodico. */
        fun schedule(ctx: Context, at: Long) {
            val delay = (at * 1000 - System.currentTimeMillis()).coerceAtLeast(0)
            val req = OneTimeWorkRequestBuilder<ScheduledSend>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
            WorkManager.getInstance(ctx).enqueueUniqueWork("scheduled-$at", ExistingWorkPolicy.KEEP, req)
            sweep(ctx)
        }

        fun sweep(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<ScheduledSend>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("scheduled-sweep", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
