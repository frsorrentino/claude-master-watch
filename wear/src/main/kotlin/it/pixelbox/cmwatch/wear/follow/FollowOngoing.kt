package it.pixelbox.cmwatch.wear.follow

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.FollowRules
import it.pixelbox.cmwatch.wear.push.Notifier

/** Segui: «▶ ledger-api · 4 m» sul quadrante mentre la sessione seguita lavora; sparisce quando si ferma. */
class FollowOngoing(private val ctx: Context) {
    private var shownFor: String? = null

    fun update(state: State?, now: Long) {
        val s = state?.let { FollowRules.ongoing(it) }
        if (s == null) { if (shownFor != null) { NotificationManagerCompat.from(ctx).cancel(ID); shownFor = null }; return }
        val tap = PendingIntent.getActivity(ctx, ID, Intent(Intent.ACTION_VIEW, Uri.parse("cmwatch://session/${s.name}")).setPackage(ctx.packageName), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(ctx, Notifier.CHANNEL_FOLLOW)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(FollowRules.status(s, now)).setOngoing(true).setContentIntent(tap)
            .setPriority(NotificationCompat.PRIORITY_LOW).setCategory(NotificationCompat.CATEGORY_STATUS)
        OngoingActivity.Builder(ctx, ID, builder)
            .setStaticIcon(R.drawable.ic_notification)
            .setStatus(Status.Builder().addTemplate(FollowRules.status(s, now)).build())
            .setTouchIntent(tap)
            .build().apply(ctx)
        runCatching { NotificationManagerCompat.from(ctx).notify(ID, builder.build()) }
        shownFor = s.name
    }

    companion object { const val ID = 7001 }
}
