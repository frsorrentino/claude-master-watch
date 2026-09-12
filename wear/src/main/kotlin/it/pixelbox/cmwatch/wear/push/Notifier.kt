package it.pixelbox.cmwatch.wear.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.rules.NotificationTexts
import java.time.ZoneId

/** Notifiche locali da FCM (design, sezione 2): una per sessione, sostituita, mai accumulata; vibrazione per canale. */
class Notifier(private val ctx: Context) {
    private val labels get() = NotificationTexts.Labels(
        open = ctx.getString(R.string.notif_open), reply = ctx.getString(R.string.notif_reply),
        resetWeek = ctx.getString(R.string.notif_reset_week), window5h = ctx.getString(R.string.notif_window_5h),
    )

    fun ensureChannels() {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        fun ch(id: String, name: Int, importance: Int, pattern: LongArray?) = NotificationChannel(id, ctx.getString(name), importance).apply {
            if (pattern != null) { enableVibration(true); vibrationPattern = pattern } else enableVibration(false)
        }
        nm.createNotificationChannel(ch(NotificationTexts.CHANNEL_QUESTIONS, R.string.channel_questions, NotificationManager.IMPORTANCE_HIGH, longArrayOf(0, 60, 80, 60)))
        nm.createNotificationChannel(ch(NotificationTexts.CHANNEL_OUTCOMES, R.string.channel_outcomes, NotificationManager.IMPORTANCE_DEFAULT, longArrayOf(0, 40)))
        nm.createNotificationChannel(ch(NotificationTexts.CHANNEL_GONE, R.string.channel_gone, NotificationManager.IMPORTANCE_DEFAULT, longArrayOf(0, 200)))
        nm.createNotificationChannel(ch(NotificationTexts.CHANNEL_QUOTA, R.string.channel_quota, NotificationManager.IMPORTANCE_DEFAULT, null))
        nm.createNotificationChannel(ch(CHANNEL_FOLLOW, R.string.channel_follow, NotificationManager.IMPORTANCE_LOW, null))
    }

    private fun canPost() = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun open(uri: String, code: Int): PendingIntent =
        PendingIntent.getActivity(ctx, code, Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(ctx.packageName), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun reply(session: String, n: Int?, code: Int, mutable: Boolean): PendingIntent {
        val i = Intent(ctx, ReplyReceiver::class.java).setAction(ReplyReceiver.ACTION).putExtra(ReplyReceiver.SESSION, session)
        if (n != null) i.putExtra(ReplyReceiver.OPTION, n)
        return PendingIntent.getBroadcast(ctx, code, i, (if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE) or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun question(s: Session) {
        val q = s.question ?: return
        val n = NotificationTexts.question(s, labels)
        val id = s.name.hashCode()
        val b = base(n).setContentIntent(open("cmwatch://question/${s.name}", id))
        q.options.take(2).forEachIndexed { i, o -> b.addAction(0, "${o.n} · ${o.label}", reply(s.name, o.n, id * 10 + i, false)) }
        b.addAction(0, labels.open, open("cmwatch://question/${s.name}", id))
        b.addAction(NotificationCompat.Action.Builder(0, labels.reply, reply(s.name, null, id * 10 + 9, true))
            .addRemoteInput(RemoteInput.Builder(ReplyReceiver.TEXT).setLabel(labels.reply).build()).build())
        post(id, b)
    }

    fun outcome(s: Session) {
        val n = NotificationTexts.outcome(s, labels)
        post(s.name.hashCode(), base(n).setContentIntent(open("cmwatch://session/${s.name}", s.name.hashCode())))
    }

    fun gone(name: String) {
        val n = NotificationTexts.gone(name, labels)
        post(name.hashCode(), base(n).setContentIntent(open("cmwatch://sessions", name.hashCode())))
    }

    fun quota(account: String, q: QuotaAccount) {
        val n = NotificationTexts.quota(account, q, labels, ZoneId.systemDefault())
        post(("quota-$account").hashCode(), base(n).setContentIntent(open("cmwatch://quota", account.hashCode())))
    }

    fun cancel(session: String) = NotificationManagerCompat.from(ctx).cancel(session.hashCode())

    private fun base(n: NotificationTexts.Note) = NotificationCompat.Builder(ctx, n.channel)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(n.title)
        .setContentText(n.body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(n.body))
        .setOnlyAlertOnce(false)
        .setAutoCancel(true)
        .setCategory(if (n.channel == NotificationTexts.CHANNEL_QUESTIONS) NotificationCompat.CATEGORY_MESSAGE else NotificationCompat.CATEGORY_STATUS)
        .setPriority(if (n.channel == NotificationTexts.CHANNEL_QUESTIONS) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)

    private fun post(id: Int, b: NotificationCompat.Builder) {
        if (!canPost()) return
        NotificationManagerCompat.from(ctx).notify(id, b.build())
    }

    companion object { const val CHANNEL_FOLLOW = "follow" }
}
