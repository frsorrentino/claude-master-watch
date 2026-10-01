package it.pixelbox.cmwatch.mobile.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.mobile.MainActivity
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.NotificationPlan
import it.pixelbox.cmwatch.rules.PhoneAlert

/** Le notifiche del telefono (design 29/09): due canali, suono o silenzio secondo PhoneAlert. */
class PhoneNotifier(private val ctx: Context) {
    private val nm = NotificationManagerCompat.from(ctx)

    fun ensureChannels() {
        val m = ctx.getSystemService(NotificationManager::class.java)
        m.createNotificationChannel(NotificationChannel(CH_SOUND, ctx.getString(R.string.channel_questions), NotificationManager.IMPORTANCE_HIGH))
        m.createNotificationChannel(NotificationChannel(CH_QUIET, ctx.getString(R.string.channel_quiet), NotificationManager.IMPORTANCE_LOW))
    }

    private fun channel(mode: PhoneAlert.Mode) = if (mode == PhoneAlert.Mode.SOUND) CH_SOUND else CH_QUIET
    private fun id(session: String) = session.hashCode()

    private val labels get() = NotificationPlan.Labels(
        open = "", reply = ctx.getString(R.string.answer_free), retry = ctx.getString(R.string.retry), read = ctx.getString(R.string.read_aloud),
        stop = "", write = ctx.getString(R.string.write_prompt), resume = ctx.getString(R.string.reopen), sent = "", confirmed = "",
        notDelivered = ctx.getString(R.string.not_delivered), sessions = ctx.getString(R.string.tab_sessions),
    )

    /** `queue`: il tocco apre la coda «Ti aspettano» invece dell'app dov'era (piano 30/09, Task 1). */
    private fun open(code: Int, queue: Boolean = false) = PendingIntent.getActivity(
        ctx, code, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(MainActivity.EXTRA_QUEUE, queue),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun broadcast(action: String, session: String, n: Int, code: Int, mutable: Boolean = false) = PendingIntent.getBroadcast(
        ctx, code, Intent(ctx, ReplyReceiver::class.java).setAction(action).putExtra(ReplyRoute.SESSION, session).putExtra(ReplyRoute.N, n),
        (if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE) or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun post(id: Int, b: NotificationCompat.Builder) = runCatching { nm.notify(id, b.build()) }

    fun question(s: Session, mode: PhoneAlert.Mode) {
        val q = s.question ?: return
        val plan = NotificationPlan.question(s, labels, emptyList())
        val b = NotificationCompat.Builder(ctx, channel(mode)).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(plan.title).setContentText(q.text).setStyle(NotificationCompat.BigTextStyle().bigText(q.text))
            .setSubText(plan.subText).setContentIntent(open(id(s.name), queue = true)).setOnlyAlertOnce(true)
        q.options.take(3).forEach { o ->
            b.addAction(0, it.pixelbox.cmwatch.rules.QuestionRules.optionLabel(o), broadcast(ReplyRoute.OPTION, s.name, o.n, id(s.name) * 10 + o.n))
        }
        b.addAction(
            NotificationCompat.Action.Builder(0, labels.reply, broadcast(ReplyRoute.REPLY, s.name, 0, id(s.name) * 10 + 9, mutable = true))
                .addRemoteInput(RemoteInput.Builder(ReplyRoute.TEXT).setLabel(labels.reply).build()).build(),
        )
        post(id(s.name), b)
    }

    fun outcome(s: Session, mode: PhoneAlert.Mode) {
        val o = s.outcome ?: return
        post(id(s.name), NotificationCompat.Builder(ctx, channel(mode)).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(s.name).setContentText(o.short).setStyle(NotificationCompat.BigTextStyle().bigText(o.full))
            .setSubText(s.account).setContentIntent(open(id(s.name))).setAutoCancel(true))
    }

    fun quota(account: String, title: String) {
        post(("quota-$account").hashCode(), NotificationCompat.Builder(ctx, CH_QUIET).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title).setContentIntent(open(account.hashCode())).setAutoCancel(true))
    }

    /** Diario o resoconto pronti: una notifica silenziosa che apre l'app (il testo sta nella scheda Diario). */
    fun diary(title: String) {
        if (!ctx.getSystemService(android.app.NotificationManager::class.java).areNotificationsEnabled()) return
        val prefs = kotlinx.coroutines.runBlocking { (ctx.applicationContext as it.pixelbox.cmwatch.mobile.PhoneApp).prefs.current() }
        if (prefs.demoMode || !prefs.paired) return
        post(title.hashCode(), NotificationCompat.Builder(ctx, CH_QUIET).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title).setContentIntent(open(title.hashCode())).setAutoCancel(true))
    }

    fun waitingNetwork(session: String) {
        post(id(session), NotificationCompat.Builder(ctx, CH_QUIET).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(session).setContentText(ctx.getString(R.string.waiting_network)).setOnlyAlertOnce(true))
    }

    fun closeQuestion(session: String) = nm.cancel(id(session))

    companion object {
        const val CH_SOUND = "phone_questions"
        const val CH_QUIET = "phone_quiet"
    }
}
