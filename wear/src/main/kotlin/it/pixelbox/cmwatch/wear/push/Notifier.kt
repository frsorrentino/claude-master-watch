package it.pixelbox.cmwatch.wear.push

import it.pixelbox.cmwatch.rules.SpeechText
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.NotificationPlan
import it.pixelbox.cmwatch.rules.NotificationPlan.Act

/** Wear OS scarta le azioni senza icona: ogni azione ha la sua (Franz, 13/09 09:32: «non mostra le opzioni come tasti»). */
private fun optionIcon(n: Int) = when (n) {
    1 -> R.drawable.ic_num_1; 2 -> R.drawable.ic_num_2; else -> R.drawable.ic_num_3
}

/**
 * Notifiche native al 100 % (specifica di Franz, 12/09): conversazione per sessione (MessagingStyle + shortcut), chip di
 * risposta (RemoteInput.setChoices, senza risposte generate), azioni dirette per le prime due opzioni, aggiornamento in place
 * («✓ 1 · yes inviato» → «✓ confermato» → chiusa; «✗ non consegnato · Riprova»), cronometro sulla domanda, gruppo con
 * summary, canali solo vibrazione, dismiss = «visto». Una notifica per sessione (id = hash del nome), mai accumulata.
 */
class Notifier(private val ctx: Context) {
    private val history = HashMap<String, MutableList<NotificationPlan.Qa>>()
    private val lastQuestion = HashMap<String, Pair<String, String>>()   // sessione → (id domanda, testo)
    /** Comandi inviati dalla notifica: id comando → sessione, per l'aggiornamento in place al /result. */
    val pendingBySession = HashMap<String, String>()

    /**
     * Sessione la cui notifica ha fatto partire la lettura: lì «Leggi» diventa «Ferma» finché la voce va (Franz, 14/09
     * 16:08). La notifica «Lettura in corso» non serve a questo: Wear OS non mostra le notifiche in corso nell'elenco.
     */
    private var reading: String? = null

    /** Segna (o toglie) la lettura e ridisegna le notifiche toccate, solo se sono ancora visibili: mai resuscitarle. */
    fun reading(session: String?, sessions: List<Session>) {
        val prima = reading
        reading = session
        val attive = runCatching { ctx.getSystemService(NotificationManager::class.java).activeNotifications.toList() }.getOrDefault(emptyList())
        for (name in setOfNotNull(prima, session)) {
            val s = sessions.firstOrNull { it.name == name } ?: continue
            when (attive.firstOrNull { it.id == id(name) }?.notification?.channelId) {
                NotificationPlan.CH_QUESTIONS -> if (s.question != null) question(s)
                NotificationPlan.CH_OUTCOMES -> if (s.outcome != null) outcome(s)
            }
        }
    }

    private fun readIcon(session: String) = if (reading == session) R.drawable.ic_stop else R.drawable.ic_play
    private fun readLabel(session: String) = NotificationPlan.readLabel(reading == session, labels)

    private val labels get() = NotificationPlan.Labels(
        open = ctx.getString(R.string.notif_open), reply = ctx.getString(R.string.notif_reply), retry = ctx.getString(R.string.question_retry),
        read = ctx.getString(R.string.notif_read), stop = ctx.getString(R.string.notif_stop), write = ctx.getString(R.string.card_write), resume = ctx.getString(R.string.notif_resume),
        sent = ctx.getString(R.string.notif_sent), confirmed = ctx.getString(R.string.notif_confirmed), notDelivered = ctx.getString(R.string.question_not_delivered),
        sessions = ctx.getString(R.string.sessions_label), goneText = ctx.getString(R.string.notif_gone_text),
        waiting = ctx.getString(R.string.notif_state_waiting), busy = ctx.getString(R.string.notif_state_busy),
        idle = ctx.getString(R.string.notif_state_idle), gone = ctx.getString(R.string.notif_state_gone),
    )

    fun ensureChannels() {
        cleanShortcuts()
        val nm = ctx.getSystemService(NotificationManager::class.java)
        fun ch(id: String, name: Int, importance: Int, pattern: LongArray?) = NotificationChannel(id, ctx.getString(name), importance).apply {
            setSound(null, null)
            if (pattern != null) { enableVibration(true); vibrationPattern = pattern } else enableVibration(false)
        }
        nm.createNotificationChannel(ch(NotificationPlan.CH_QUESTIONS, R.string.channel_questions, NotificationManager.IMPORTANCE_HIGH, longArrayOf(0, 60, 80, 60)))
        nm.createNotificationChannel(ch(NotificationPlan.CH_OUTCOMES, R.string.channel_outcomes, NotificationManager.IMPORTANCE_DEFAULT, longArrayOf(0, 40)))
        nm.createNotificationChannel(ch(NotificationPlan.CH_GONE, R.string.channel_gone, NotificationManager.IMPORTANCE_DEFAULT, longArrayOf(0, 200)))
        nm.createNotificationChannel(ch(NotificationPlan.CH_QUOTA, R.string.channel_quota, NotificationManager.IMPORTANCE_LOW, null))
        nm.createNotificationChannel(ch(CHANNEL_FOLLOW, R.string.channel_follow, NotificationManager.IMPORTANCE_LOW, null))
    }

    fun enabled(): Boolean = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    private fun canPost() = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED && enabled()

    private fun id(session: String) = session.hashCode()

    private fun open(uri: String, code: Int): PendingIntent =
        PendingIntent.getActivity(ctx, code, Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(ctx.packageName), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun broadcast(action: String, session: String, code: Int, mutable: Boolean = false, extras: Intent.() -> Unit = {}): PendingIntent {
        val i = Intent(ctx, ReplyReceiver::class.java).setAction(action).putExtra(ReplyReceiver.SESSION, session).apply(extras)
        return PendingIntent.getBroadcast(ctx, code, i, (if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE) or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun badge(s: Session) = BadgeBitmap.draw(it.pixelbox.cmwatch.rules.Badge.of(s.account, s.color, s.state, s.icon, s.accountKind))

    private fun person(name: String, s: Session?, state: SessionState?): Person =
        Person.Builder().setName(name).setKey(name).setIcon(IconCompat.createWithBitmap(s?.let { badge(it) } ?: Glyphs.state(ctx, state))).setImportant(true).build()

    /**
     * Le scorciatoie di conversazione (una per sessione) finivano nel carosello «Recenti» di Wear OS come se fossero
     * app: Franz ne vedeva due, «claude-master» con il badge ambra e «Claude Master» con l'icona vera, e sembrava un
     * residuo (13/09 20:16). Verificato con `dumpsys shortcut`: erano scorciatoie dinamiche, non un secondo task.
     * Non se ne registrano più, e quelle vecchie si cancellano all'avvio: lo stile conversazione della notifica resta,
     * si perde solo il raggruppamento fra le conversazioni di sistema, che al polso non si vede.
     */
    private fun cleanShortcuts() {
        runCatching {
            // Le dinamiche si cancellano, ma quelle già usate da una notifica restano in cache («Ic-fStrLiv» nel
            // dump): per quelle serve togliere il vincolo di durata, altrimenti la voce resta nel carosello.
            val ids = ShortcutManagerCompat.getShortcuts(
                ctx, ShortcutManagerCompat.FLAG_MATCH_DYNAMIC or ShortcutManagerCompat.FLAG_MATCH_CACHED,
            ).map { it.id }
            if (ids.isNotEmpty()) ShortcutManagerCompat.removeLongLivedShortcuts(ctx, ids)
            ShortcutManagerCompat.removeAllDynamicShortcuts(ctx)
        }
    }

    private fun base(plan: NotificationPlan.Plan): NotificationCompat.Builder = NotificationCompat.Builder(ctx, plan.channel)
        .setSmallIcon(R.drawable.ic_app_mono)
        .setColor(0xFF4C7DFF.toInt())
        .setContentTitle(plan.title)
        .setSubText(plan.subText)
        .setLargeIcon(Glyphs.state(ctx, plan.accent))
        .setAutoCancel(plan.autoCancel)
        .setOnlyAlertOnce(true)
        .setLocalOnly(true)
        .setGroup(NotificationPlan.GROUP)
        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
        .setSound(null)
        .apply {
            plan.whenS?.let { setWhen(it * 1000).setShowWhen(true) }
            if (plan.chronometer) setUsesChronometer(true)
            plan.timeoutMs?.let { setTimeoutAfter(it) }
            plan.progress?.let { setProgress(100, it, false) }
        }

    /** Domanda: conversazione con storico, prime due opzioni dirette, Rispondi con chip + dettatura, Apri. */
    fun question(s: Session) {
        val q = s.question ?: return
        val plan = NotificationPlan.question(s, labels, history[s.name].orEmpty())
        lastQuestion[s.name] = q.id to q.text
        val me = Person.Builder().setName(ctx.getString(R.string.notif_me)).setKey("me").build()
        val them = person(plan.person ?: s.name, s, SessionState.WAITING)
        val style = NotificationCompat.MessagingStyle(me).setConversationTitle(plan.title)
        val hist = history[s.name].orEmpty()
        hist.forEach { style.addMessage(it.question, it.at * 1000, them).addMessage(it.answer, it.at * 1000 + 1, me) }
        style.addMessage(q.text, q.askedAt * 1000, them)
        val b = base(plan).setLargeIcon(badge(s)).setStyle(style).setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentText(q.text)
            .setContentIntent(open("cmwatch://question/${s.name}", id(s.name)))
            .setDeleteIntent(broadcast(ReplyReceiver.ACTION_SEEN, s.name, id(s.name) * 10 + 8) { putExtra(ReplyReceiver.QUESTION_ID, q.id) })
        for ((i, a) in plan.actions.withIndex()) when (a) {
            is Act.Option -> b.addAction(NotificationCompat.Action.Builder(optionIcon(a.n), a.label, broadcast(ReplyReceiver.ACTION_OPTION, s.name, id(s.name) * 10 + i) { putExtra(ReplyReceiver.OPTION, a.n); putExtra(ReplyReceiver.OPTION_LABEL, a.label) })
                .setShowsUserInterface(false).build())
            Act.Reply -> b.addAction(NotificationCompat.Action.Builder(R.drawable.ic_reply, labels.reply, broadcast(ReplyReceiver.ACTION_REPLY, s.name, id(s.name) * 10 + 7, mutable = true))
                .addRemoteInput(RemoteInput.Builder(ReplyReceiver.TEXT).setLabel(labels.reply).setChoices(plan.choices.toTypedArray()).setAllowFreeFormInput(plan.freeForm).build())
                // Niente risposte generate da Wear OS: su una domanda a opzioni proponeva frasi inventate come «Ok, provo»
                // e Franz non capiva da dove venissero (13/09 20:02). Le scelte sono quelle vere della domanda.
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY).setAllowGeneratedReplies(false).setShowsUserInterface(false).build())
            Act.Open -> b.addAction(NotificationCompat.Action.Builder(R.drawable.ic_open, labels.open, open("cmwatch://question/${s.name}", id(s.name) * 10 + 9))
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ).build())
            else -> Unit
        }
        // «Leggi»: la domanda con le opzioni numerate, senza aprire l'app (Franz, 14/09 12:17).
        b.addAction(readIcon(s.name), readLabel(s.name), PendingIntent.getForegroundService(ctx, id(s.name) * 10 + 4,
            SpeakService.textIntent(ctx, SpeechText.question(q, ctx.getString(R.string.tts_option)), from = s.name), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        post(id(s.name), b)
    }

    /** Dopo il tap: la stessa notifica dice «✓ 1 · yes inviato»; il /result la conferma o la segna non consegnata. */
    fun sent(session: String, n: Int?, label: String?, cmdId: String) {
        pendingBySession[cmdId] = session
        lastQuestion[session]?.let { (_, text) -> if (n != null && label != null) remember(session, text, "$n $label") }
        val line = if (n != null && label != null) NotificationPlan.sentLine(n, label, labels) else "✓ ${labels.sent}"
        post(id(session), simple(session, NotificationPlan.CH_QUESTIONS, line, SessionState.BUSY))
    }

    fun confirmed(cmdId: String) {
        val session = pendingBySession.remove(cmdId) ?: return
        post(id(session), simple(session, NotificationPlan.CH_QUESTIONS, NotificationPlan.confirmedLine(labels), SessionState.BUSY).setTimeoutAfter(5_000))
    }

    fun failed(cmdId: String) {
        val session = pendingBySession[cmdId] ?: return
        val b = simple(session, NotificationPlan.CH_QUESTIONS, NotificationPlan.failedLine(labels), SessionState.GONE)
            .addAction(R.drawable.ic_retry, labels.retry, broadcast(ReplyReceiver.ACTION_RETRY, session, id(session) * 10 + 6) { putExtra(ReplyReceiver.CMD_ID, cmdId) })
        post(id(session), b)
    }

    private fun remember(session: String, question: String, answer: String) {
        val list = history.getOrPut(session) { ArrayList() }
        list += NotificationPlan.Qa(question, answer, System.currentTimeMillis() / 1000)
        while (list.size > 3) list.removeAt(0)
    }

    private fun simple(session: String, channel: String, text: String, accent: SessionState) = NotificationCompat.Builder(ctx, channel)
        .setSmallIcon(R.drawable.ic_app_mono).setColor(0xFF4C7DFF.toInt()).setContentTitle(session).setContentText(text)
        .setLargeIcon(Glyphs.state(ctx, accent)).setOnlyAlertOnce(true).setLocalOnly(true).setGroup(NotificationPlan.GROUP).setSound(null)
        .setContentIntent(open("cmwatch://session/$session", id(session)))

    fun outcome(s: Session) {
        val plan = NotificationPlan.outcome(s, labels)
        val b = base(plan).setLargeIcon(badge(s)).setContentText(plan.messages.first()).setStyle(NotificationCompat.BigTextStyle().bigText(plan.bigText))
            .setContentIntent(open("cmwatch://outcome/${s.name}", id(s.name)))
            .addAction(readIcon(s.name), readLabel(s.name), PendingIntent.getForegroundService(ctx, id(s.name) * 10 + 3, SpeakService.lastIntent(ctx, s.name, plan.bigText, from = s.name), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .addAction(NotificationCompat.Action.Builder(R.drawable.ic_edit, labels.write, broadcast(ReplyReceiver.ACTION_REPLY, s.name, id(s.name) * 10 + 7, mutable = true))
                .addRemoteInput(RemoteInput.Builder(ReplyReceiver.TEXT).setLabel(labels.write).build()).setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY).build())
            .addAction(R.drawable.ic_open, labels.open, open("cmwatch://outcome/${s.name}", id(s.name) * 10 + 9))
        post(id(s.name), b)
    }

    fun gone(name: String, account: String?) {
        val plan = NotificationPlan.gone(name, account, labels)
        // Oltre al titolo con il nome e l'icona di chiusura, la chiusura detta a parole con l'azione per riaprirla (Franz, 15/09 17:27).
        post(id(name), base(plan).setContentText(plan.bigText).setStyle(NotificationCompat.BigTextStyle().bigText(plan.bigText))
            .setContentIntent(open("cmwatch://sessions", id(name)))
            .addAction(R.drawable.ic_play, labels.resume, broadcast(ReplyReceiver.ACTION_RESUME, name, id(name) * 10 + 5)))
    }

    fun quota(account: String, q: QuotaAccount) {
        val plan = NotificationPlan.quota(account, q, labels)
        post(("quota-$account").hashCode(), base(plan).setContentText(ctx.getString(R.string.notif_quota_body)).setContentIntent(open("cmwatch://quota", account.hashCode())))
    }

    /** Summary del gruppo: una riga per sessione. */
    fun summary(state: State) {
        val s = NotificationPlan.summary(state, labels)
        val style = NotificationCompat.InboxStyle().setBigContentTitle(s.title)
        s.rows.forEach { style.addLine(it) }
        post(SUMMARY_ID, NotificationCompat.Builder(ctx, NotificationPlan.CH_OUTCOMES).setSmallIcon(R.drawable.ic_app_mono).setColor(0xFF4C7DFF.toInt())
            .setContentTitle(s.title).setStyle(style).setGroup(NotificationPlan.GROUP).setGroupSummary(true).setLocalOnly(true).setOnlyAlertOnce(true).setSound(null)
            .setContentIntent(open("cmwatch://sessions", SUMMARY_ID)))
    }

    fun cancel(session: String) = NotificationManagerCompat.from(ctx).cancel(id(session))

    /**
     * La domanda è stata risposta altrove: si chiude la sua notifica, ma solo se è ancora quella della domanda. Una
     * sessione ha una notifica sola (stesso id), e un esito arrivato nel frattempo non va cancellato.
     */
    fun closeQuestion(session: String) {
        val attiva = runCatching { ctx.getSystemService(NotificationManager::class.java).activeNotifications.firstOrNull { it.id == id(session) } }.getOrNull()
        if (attiva?.notification?.channelId == NotificationPlan.CH_QUESTIONS) cancel(session)
        lastQuestion.remove(session)
    }

    private fun post(id: Int, b: NotificationCompat.Builder) {
        if (!canPost()) return
        runCatching { NotificationManagerCompat.from(ctx).notify(id, b.build()) }
    }

    companion object { const val CHANNEL_FOLLOW = "follow"; const val SUMMARY_ID = 7000 }
}
