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

/**
 * Segui: «▶ ledger-api · 4 m» sul quadrante mentre la sessione seguita lavora; sparisce quando si ferma.
 * Su Android 16 (API 36+) la notifica è un «live update» promosso con `ProgressStyle` indeterminata, se la piattaforma
 * espone i metodi; altrimenti resta l'`OngoingActivity` (specifica di Franz, 12/09 15:26, punto 7).
 */
class FollowOngoing(private val ctx: Context) {
    private var shownFor: String? = null

    fun update(state: State?, now: Long) {
        val s = state?.let { FollowRules.ongoing(it) }
        if (s == null) { if (shownFor != null) { NotificationManagerCompat.from(ctx).cancel(ID); shownFor = null }; return }
        if (promoted(s.name, FollowRules.status(s, now))) { shownFor = s.name; return }
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

    /** Live update promosso (API 36+), via reflection: se un pezzo manca si torna all'OngoingActivity. */
    private fun promoted(session: String, status: String): Boolean {
        if (android.os.Build.VERSION.SDK_INT < 36) return false
        return runCatching {
            val tap = PendingIntent.getActivity(ctx, ID, Intent(Intent.ACTION_VIEW, Uri.parse("cmwatch://session/$session")).setPackage(ctx.packageName), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val b = android.app.Notification.Builder(ctx, Notifier.CHANNEL_FOLLOW)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle(status).setOngoing(true).setContentIntent(tap)
            val styleClass = Class.forName("android.app.Notification\$ProgressStyle")
            val style = styleClass.getDeclaredConstructor().newInstance()
            styleClass.getMethod("setProgressIndeterminate", Boolean::class.javaPrimitiveType).invoke(style, true)
            android.app.Notification.Builder::class.java.getMethod("setStyle", android.app.Notification.Style::class.java).invoke(b, style)
            android.app.Notification.Builder::class.java.getMethod("requestPromotedOngoing", Boolean::class.javaPrimitiveType).invoke(b, true)
            val nm = ctx.getSystemService(android.app.NotificationManager::class.java)
            nm.notify(ID, b.build())
            true
        }.getOrElse { android.util.Log.i("cmwatch", "live update non disponibile: ${it.message}"); false }
    }

    companion object { const val ID = 7001 }
}
