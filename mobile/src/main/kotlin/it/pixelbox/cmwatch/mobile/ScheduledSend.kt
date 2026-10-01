package it.pixelbox.cmwatch.mobile

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import it.pixelbox.cmwatch.data.Repo
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
 * (telefono spento, app aggiornata): `ChatLog.claimDue` li prende una volta sola. Entrambi partono solo con la rete, e
 * il lavoro aspetta l'esito (`Repo.deliver`): un rifiuto si salva sul messaggio, un invio senza risposta torna in attesa
 * e si riprova con lo stesso id (revisione finale 01/10: prima poteva perdersi e risultare consegnato).
 */
class ScheduledSend(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as PhoneApp
        var again = false
        for (m in app.chatLog.claimDue()) {
            when (val d = app.repo.deliver(CmdOp.PROMPT, m.session, m.text, m.id)) {
                is Repo.Delivery.Done -> if (!d.result.ok) app.chatLog.markFailed(m.id, d.result.text.ifBlank { app.getString(R.string.chat_failed) })
                Repo.Delivery.NotSent -> { app.chatLog.unclaim(m.id); again = true }
            }
        }
        return if (again) Result.retry() else Result.success()
    }

    companion object {
        private val NETWORK = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Il lavoro all'ora dell'invio, più il giro periodico. */
        fun schedule(ctx: Context, at: Long) {
            val delay = (at * 1000 - System.currentTimeMillis()).coerceAtLeast(0)
            val req = OneTimeWorkRequestBuilder<ScheduledSend>().setInitialDelay(delay, TimeUnit.MILLISECONDS).setConstraints(NETWORK).build()
            WorkManager.getInstance(ctx).enqueueUniqueWork("scheduled-$at", ExistingWorkPolicy.KEEP, req)
            sweep(ctx)
        }

        fun sweep(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<ScheduledSend>(15, TimeUnit.MINUTES).setConstraints(NETWORK).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("scheduled-sweep", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
