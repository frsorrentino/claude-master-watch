package it.pixelbox.cmwatch.wear.live

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.google.android.gms.wearable.Wearable
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.rules.LiveCard
import it.pixelbox.cmwatch.rules.LiveTap
import it.pixelbox.cmwatch.rules.LiveWire
import it.pixelbox.cmwatch.wear.haptics.Haptics
import it.pixelbox.cmwatch.wear.push.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * La modalità live sul watch: un telecomando del telefono (specifica live, §3). Riceve la scheda della notizia in corso,
 * vibra secondo il livello (lunga il livello 1, corta il 2, doppio tick a conferma riuscita), tiene la notifica fissa
 * «Live» che apre la scheda, e manda i tocchi al telefono. Nessuna regola qui: la regia sta sul telefono.
 */
class LiveLink(private val ctx: Context, private val scope: CoroutineScope) {
    private val _card = MutableStateFlow<LiveCard?>(null)
    /** La scheda in corso; null = live spenta. */
    val card: StateFlow<LiveCard?> = _card

    fun receive(c: LiveCard) {
        if (c.kind == LiveCard.Kind.OFF) {
            _card.value = null
            NotificationManagerCompat.from(ctx).cancel(ID)
            NotificationManagerCompat.from(ctx).cancel(ALERT_ID)
            return
        }
        val first = _card.value == null
        _card.value = c
        when (c.buzz) {
            LiveCard.Buzz.LONG -> Haptics.play(ctx, Haptics.Kind.GONE)
            LiveCard.Buzz.SHORT -> Haptics.play(ctx, Haptics.Kind.OUTCOME)
            LiveCard.Buzz.DONE -> Haptics.play(ctx, Haptics.Kind.CONFIRMED)
            LiveCard.Buzz.NONE -> Unit
        }
        if (first || c.buzz != LiveCard.Buzz.NONE) note(c)
        if (c.buzz == LiveCard.Buzz.LONG) alert(c)
    }

    /**
     * Livello 1 (domanda, ok, Prossimo con «!»): una notifica in primo piano col testo, che apre la scheda. Quella fissa
     * è silenziosa e restava sotto (prova del 07/10 alle 11:13: la domanda non si vedeva sull'orologio).
     */
    private fun alert(c: LiveCard) {
        val b = NotificationCompat.Builder(ctx, Notifier.CHANNEL_LIVE)
            .setSmallIcon(R.drawable.ic_app_mono).setContentTitle(c.title.ifBlank { ctx.getString(R.string.live_title) })
            .setContentText(c.text).setStyle(NotificationCompat.BigTextStyle().bigText(c.text))
            .setContentIntent(tap()).setAutoCancel(true).setTimeoutAfter(ALERT_MS)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_MESSAGE)
        runCatching { NotificationManagerCompat.from(ctx).notify(ALERT_ID, b.build()) }
    }

    private fun tap() = PendingIntent.getActivity(
        ctx, ID, Intent(ctx, LiveActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun send(t: LiveTap) {
        val bytes = LiveWire.encode(t)
        scope.launch {
            runCatching {
                for (n in Wearable.getNodeClient(ctx).connectedNodes.await()) Wearable.getMessageClient(ctx).sendMessage(n.id, LiveWire.TAP, bytes).await()
            }.onFailure { Haptics.play(ctx, Haptics.Kind.ERROR) }
        }
    }

    /** La notifica fissa con la notizia più recente: il tocco apre la scheda. */
    private fun note(c: LiveCard) {
        val tap = tap()
        val title = ctx.getString(R.string.live_title)
        val text = listOf(c.title, c.text).filter { it.isNotBlank() }.joinToString(": ")
        val b = NotificationCompat.Builder(ctx, Notifier.CHANNEL_FOLLOW)
            .setSmallIcon(R.drawable.ic_app_mono).setContentTitle(title).setContentText(text).setOngoing(true)
            .setContentIntent(tap).setPriority(NotificationCompat.PRIORITY_LOW).setCategory(NotificationCompat.CATEGORY_STATUS)
        OngoingActivity.Builder(ctx, ID, b)
            .setStaticIcon(R.drawable.ic_app_mono)
            .setStatus(Status.Builder().addTemplate(title).build())
            .setTouchIntent(tap)
            .build().apply(ctx)
        runCatching { NotificationManagerCompat.from(ctx).notify(ID, b.build()) }
    }

    companion object {
        const val ID = 7042
        const val ALERT_ID = 7043

        /** La notifica in primo piano sparisce da sola: la notizia resta sulla scheda. */
        const val ALERT_MS = 60_000L
    }
}
