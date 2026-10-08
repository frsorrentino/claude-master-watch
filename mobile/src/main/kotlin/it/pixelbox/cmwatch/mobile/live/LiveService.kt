package it.pixelbox.cmwatch.mobile.live

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.MainActivity
import it.pixelbox.cmwatch.mobile.PhoneApp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.mobile.pair.WearWatchLink
import it.pixelbox.cmwatch.rules.LiveCard
import it.pixelbox.cmwatch.rules.LiveDesk
import it.pixelbox.cmwatch.rules.LiveTap
import it.pixelbox.cmwatch.rules.LiveWire
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * La modalità live sul telefono (specifica live, §3): servizio in primo piano con notifica fissa. Riceve stato, eventi e
 * risultati dal `Repo`, i tocchi dal watch e i tasti degli auricolari, li passa alla regia (`LiveDesk`) uno alla volta e
 * ne esegue gli effetti: voce in cuffia (`LiveVoice`), comandi del contratto, scheda per il watch. Pausa senza auricolari
 * o durante una chiamata; si spegne se gli auricolari non tornano entro 10 minuti o con la batteria al 10%.
 */
class LiveService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val inbox = Channel<Input>(Channel.UNLIMITED)
    private lateinit var app: PhoneApp
    private lateinit var voice: LiveVoice
    private lateinit var lang: LiveDesk.Lang
    private lateinit var media: MediaSession
    private var desk = LiveDesk.Desk()
    private var state: State? = null
    private var seen: Set<String> = emptySet()
    // Letta anche dal collettore dei risultati, su un altro thread.
    private val pending = java.util.concurrent.ConcurrentHashMap<String, LiveDesk.Tag>()
    private var headset = true
    private var focus = true
    private var userPaused = false
    private var paused = false
    private var headsetGoneAt = 0L
    private var batteryWarned = false
    private var watchNode: String? = null

    private sealed class Input {
        data class Snap(val state: State, val fresh: Boolean) : Input()
        data class Events(val keys: List<it.pixelbox.cmwatch.contract.Event>) : Input()
        data class Tap(val tap: LiveTap) : Input()
        data object Spoken : Input()
        data object Tick : Input()
        data class Result(val id: String, val ok: Boolean, val text: String) : Input()
        data class Failed(val tag: LiveDesk.Tag, val text: String) : Input()
        data class Pending(val id: String, val tag: LiveDesk.Tag) : Input()
        data object Audio : Input()
        data class Notice(val text: String) : Input()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        app = application as PhoneApp
        lang = LiveLabels.lang(this)
        channel()
        startForeground(NOTE_ID, note(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        voice = LiveVoice(this, onIdle = { inbox.trySend(Input.Spoken) }, onFocus = { on -> focus = on; inbox.trySend(Input.Audio) })
        media = MediaSession(this, "live").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { userPaused = false; inbox.trySend(Input.Audio) }
                override fun onPause() { userPaused = true; inbox.trySend(Input.Audio) }
                override fun onSkipToNext() { inbox.trySend(Input.Tap(LiveTap(LiveTap.Action.SKIP))) }
                override fun onSkipToPrevious() { inbox.trySend(Input.Tap(LiveTap(LiveTap.Action.REPEAT))) }
            })
            setPlaybackState(
                PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0, 1f)
                    .setActions(PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                    .build(),
            )
            isActive = true
        }
        getSystemService(AudioManager::class.java).registerAudioDeviceCallback(devices, null)
        registerReceiver(battery, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        headset = voice.headset()
        _running.value = true
        app.liveStreams(true)
        seen = app.repo.events.value.map { it.key }.toSet()
        scope.launch { for (i in inbox) handle(i) }
        scope.launch { app.repo.snapshot.collect { s -> s.state?.let { inbox.send(Input.Snap(it, s.freshness !is Freshness.Stale)) } } }
        scope.launch { app.repo.events.collect { ev -> inbox.send(Input.Events(ev)) } }
        scope.launch { app.repo.resultsById.collect { m -> m.forEach { (id, r) -> if (pending.containsKey(id)) inbox.send(Input.Result(id, r.ok, r.text)) } } }
        scope.launch { taps.collect { inbox.send(Input.Tap(it)) } }
        scope.launch {
            while (isActive) {
                delay(if (desk.tell != null || desk.ask != null || desk.confirm.phase == it.pixelbox.cmwatch.rules.DoubleConfirm.Phase.ARMED) 250 else 1_000)
                inbox.send(Input.Tick)
            }
        }
        inbox.trySend(Input.Notice(getString(if (headset) R.string.live_started else R.string.live_no_headset)))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_ONLY_BLOCKING -> inbox.trySend(Input.Tap(LiveTap(LiveTap.Action.ONLY_BLOCKING)))
            ACTION_PAUSE -> { userPaused = !userPaused; inbox.trySend(Input.Audio) }
            ACTION_RECAP -> inbox.trySend(Input.Tap(LiveTap(LiveTap.Action.ROUND)))
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        _running.value = false
        _panel.value = Panel()
        runCatching { getSystemService(AudioManager::class.java).unregisterAudioDeviceCallback(devices) }
        runCatching { unregisterReceiver(battery) }
        media.release()
        voice.shutdown()
        app.liveStreams(false)
        // Il watch toglie la scheda e la sua notifica.
        val off = LiveWire.encode(LiveCard(desk.seq + 1, LiveCard.Kind.OFF))
        app.scope.launch { watch()?.let { n -> runCatching { Wearable.getMessageClient(app).sendMessage(n, LiveWire.CARD, off).await() } } }
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun handle(i: Input) {
        val now = System.currentTimeMillis()
        val st = state
        val out = when (i) {
            is Input.Snap -> {
                val linkChanged = i.fresh != desk.linked
                state = i.state
                if (linkChanged) run(LiveDesk.link(desk, i.fresh, now, lang))
                if (st != null && st === i.state) null else LiveDesk.state(desk, i.state, emptyList(), now, lang)
            }
            is Input.Events -> {
                val fresh = i.keys.filter { it.key !in seen }
                seen = seen + fresh.map { it.key }
                if (fresh.isEmpty() || st == null) null else LiveDesk.state(desk, st, fresh, now, lang)
            }
            is Input.Tap -> if (st == null) null else LiveDesk.tap(desk, i.tap, st, now, lang)
            Input.Spoken -> { _panel.value = _panel.value.copy(speaking = false); if (st == null) null else LiveDesk.spoken(desk, st, now, lang) }
            Input.Tick -> {
                // L'intervallo del recap dalle impostazioni, letto a ogni tic: cambia anche a live accesa.
                val every = getSharedPreferences("live", MODE_PRIVATE).getInt("recap_min", LiveDesk.RECAP_DEFAULT_MIN) * 60_000L
                if (every != desk.recapEveryMs) desk = desk.copy(recapEveryMs = every)
                // Fine della chiamata: il focus torna da sé quando il telefono non è più in conversazione.
                if (!focus && getSystemService(AudioManager::class.java).mode == AudioManager.MODE_NORMAL) focus = true
                if (!headset && headsetGoneAt > 0 && now - headsetGoneAt >= HEADSET_WAIT_MS) { stopSelf(); return }
                audio(now)
                if (st == null) null else LiveDesk.tick(desk, st, now, lang)
            }
            is Input.Result -> {
                val tag = pending.remove(i.id) ?: return
                if (st == null) null else LiveDesk.result(desk, tag, i.ok, i.text, st, now, lang)
            }
            is Input.Failed -> if (st == null) null else LiveDesk.result(desk, i.tag, false, i.text, st, now, lang)
            is Input.Pending -> {
                // Il risultato può arrivare prima che il comando torni con il suo id.
                val r = app.repo.resultsById.value[i.id]
                if (r == null) { pending[i.id] = i.tag; null } else if (st == null) null else LiveDesk.result(desk, i.tag, r.ok, r.text, st, now, lang)
            }
            Input.Audio -> { audio(now); null }
            is Input.Notice -> LiveDesk.notice(desk, i.text, now, lang)
        }
        out?.let { run(it) }
    }

    /** Ricalcola la pausa: auricolari, chiamata, tasto play/pausa. */
    private suspend fun audio(now: Long) {
        val want = !headset || !focus || userPaused
        if (want == paused) return
        paused = want
        _panel.value = _panel.value.copy(paused = paused, noHeadset = !headset)
        state?.let { run(LiveDesk.pause(desk, want, it, now, lang)) }
    }

    private suspend fun run(out: LiveDesk.Out) {
        desk = out.desk
        for (e in out.effects) when (e) {
            is LiveDesk.Effect.Say -> { voice.say(e.text); _panel.value = _panel.value.copy(last = e.text, speaking = true) }
            LiveDesk.Effect.Hush -> voice.hush()
            LiveDesk.Effect.Tone -> voice.tone()
            is LiveDesk.Effect.Show -> card(e.card)
            LiveDesk.Effect.Stop -> stopSelf()
            is LiveDesk.Effect.Send -> scope.launch {
                runCatching {
                    app.repo.command(e.op, e.session, e.arg, e.text, voice = e.voice, via = e.via, confirmations = e.confirmations)
                }.onSuccess { id -> inbox.send(Input.Pending(id, e.tag)) }
                    .onFailure { inbox.send(Input.Failed(e.tag, it.message ?: "")) }
            }
        }
        _panel.value = _panel.value.copy(onlyBlocking = desk.onlyBlocking, paused = paused, noHeadset = !headset)
        updateNote()
    }

    private fun card(c: LiveCard) {
        val bytes = LiveWire.encode(c)
        scope.launch {
            val n = watch() ?: return@launch
            if (runCatching { Wearable.getMessageClient(this@LiveService).sendMessage(n, LiveWire.CARD, bytes).await() }.isFailure) watchNode = null
        }
    }

    private suspend fun watch(): String? = watchNode ?: runCatching {
        Wearable.getCapabilityClient(app).getCapability(WearWatchLink.WATCH_CAPABILITY, CapabilityClient.FILTER_REACHABLE).await().nodes.firstOrNull()?.id
    }.getOrNull()?.also { watchNode = it }

    private val devices = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) = changed()
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) = changed()
        private fun changed() {
            val now = voice.headset()
            if (now == headset) return
            headset = now
            headsetGoneAt = if (now) 0 else System.currentTimeMillis()
            // Scollegati: il watch vibra e dice perché la voce tace.
            if (!now) card(LiveCard(desk.seq, LiveCard.Kind.NEWS, text = getString(R.string.live_headset_gone), buzz = LiveCard.Buzz.LONG))
            inbox.trySend(Input.Audio)
        }
    }

    private val battery = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val charging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            if (level < 0 || charging) return
            val pct = level * 100 / scale
            when {
                pct <= 10 -> {
                    inbox.trySend(Input.Notice(getString(R.string.live_battery_off)))
                    scope.launch { delay(5_000); stopSelf() }
                }
                pct <= 20 && !batteryWarned -> { batteryWarned = true; inbox.trySend(Input.Notice(getString(R.string.live_battery_low))) }
            }
        }
    }

    private fun channel() = getSystemService(NotificationManager::class.java)
        .createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.live_channel), NotificationManager.IMPORTANCE_LOW))

    private fun note(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        fun action(a: String) = PendingIntent.getService(this, a.hashCode(), Intent(this, LiveService::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE)
        val text = if (paused && !headset) getString(R.string.live_no_headset) else getString(R.string.live_notification)
        return NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.live_on)).setContentText(text).setOngoing(true).setContentIntent(open)
            .setSilent(true)
            .addAction(0, getString(if (desk.onlyBlocking) R.string.live_all else R.string.live_only_blocking), action(ACTION_ONLY_BLOCKING))
            .addAction(0, getString(R.string.live_stop), action(ACTION_STOP))
            .build()
    }

    private var noted: Pair<Boolean, Boolean>? = null
    private fun updateNote() {
        val key = desk.onlyBlocking to (paused && !headset)
        if (key == noted) return
        noted = key
        getSystemService(NotificationManager::class.java).notify(NOTE_ID, note())
    }

    companion object {
        private const val CHANNEL = "live"
        private const val NOTE_ID = 4242
        private const val ACTION_STOP = "it.pixelbox.cmwatch.live.STOP"
        private const val ACTION_ONLY_BLOCKING = "it.pixelbox.cmwatch.live.ONLY_BLOCKING"
        private const val ACTION_PAUSE = "it.pixelbox.cmwatch.live.PAUSE"
        private const val ACTION_RECAP = "it.pixelbox.cmwatch.live.RECAP"

        /**
         * Quello che il pannello della live nell'app mostra (Franz, 08/10 20:03: «un box di controllo proprio come quello
         * audio»): l'ultima cosa detta, se sta parlando, la pausa e perché, il filtro delle notizie.
         */
        data class Panel(
            val last: String? = null, val speaking: Boolean = false, val paused: Boolean = false,
            val noHeadset: Boolean = false, val onlyBlocking: Boolean = false,
        )
        private val _panel = MutableStateFlow(Panel())
        val panel: StateFlow<Panel> get() = _panel

        /** I tasti del pannello: pausa e ripresa, solo bloccanti o tutte; spegni è `toggle`. */
        fun pause(ctx: Context) { ctx.startService(Intent(ctx, LiveService::class.java).setAction(ACTION_PAUSE)) }
        fun recap(ctx: Context) { ctx.startService(Intent(ctx, LiveService::class.java).setAction(ACTION_RECAP)) }
        fun onlyBlocking(ctx: Context) { ctx.startService(Intent(ctx, LiveService::class.java).setAction(ACTION_ONLY_BLOCKING)) }

        /** Auricolari scollegati: se non tornano entro 10 minuti la live si chiude. */
        const val HEADSET_WAIT_MS = 600_000L

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> get() = _running

        /** I tocchi del watch, da `PhoneListenerService`. */
        val taps = MutableSharedFlow<LiveTap>(extraBufferCapacity = 32)

        fun toggle(ctx: Context) {
            val i = Intent(ctx, LiveService::class.java)
            if (_running.value) ctx.startService(i.setAction(ACTION_STOP)) else ctx.startForegroundService(i)
        }
    }
}
