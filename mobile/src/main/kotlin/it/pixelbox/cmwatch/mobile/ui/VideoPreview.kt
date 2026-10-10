package it.pixelbox.cmwatch.mobile.ui

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import it.pixelbox.cmwatch.mobile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Un video della chat già scaricato (Franz, 10/10 15:24: «vorrei che sia immagini che video apparissero già come anteprima,
 * nel caso di video con miniplayer»): il primo fotogramma con ▶ e la durata; il tocco lo fa partire nella card, con play e
 * pausa, la barra, l'audio e lo schermo intero.
 */
@Composable
internal fun VideoPreview(path: String) {
    var playing by remember(path) { mutableStateOf(false) }
    val meta by produceState<VideoMeta?>(null, path) { value = withContext(Dispatchers.IO) { videoMeta(path) } }
    val ratio = meta?.ratio ?: (16f / 9f)
    if (!playing) VideoPoster(meta?.poster, meta?.durationMs, ratio) { playing = true }
    else VideoPlayer(path, ratio)
}

internal class VideoMeta(val poster: ImageBitmap?, val durationMs: Long?, val ratio: Float?)

/** Il primo fotogramma ridotto a 720 px, la durata e le proporzioni (con la rotazione dei video girati in verticale). */
private fun videoMeta(path: String): VideoMeta? = runCatching {
    val r = MediaMetadataRetriever()
    try {
        r.setDataSource(path)
        val num = { k: Int -> r.extractMetadata(k)?.toIntOrNull() }
        val w = num(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: 0
        val h = num(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: 0
        val turned = (num(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION) ?: 0) % 180 != 0
        val ratio = if (w > 0 && h > 0) (if (turned) h.toFloat() / w else w.toFloat() / h) else null
        val frame = r.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 720, 720)
        VideoMeta(frame?.asImageBitmap(), r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(), ratio)
    } finally {
        r.release()
    }
}.getOrNull()

/** La misura del riquadro nella chat: orizzontale largo 280 dp, verticale alto 320 dp. */
private fun frameSize(ratio: Float): Pair<Dp, Dp> {
    val r = ratio.coerceIn(0.4f, 2.5f)
    return if (r >= 1f) 280.dp to (280f / r).dp else (320f * r).dp to 320.dp
}

private val frameShape = RoundedCornerShape(14.dp)

/** «0:24», «12:05», «1:02:33». */
internal fun clockOf(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}

/** Il riquadro fermo: il primo fotogramma (nero finché non c'è), ▶ al centro e la durata in basso a destra. */
@Composable
internal fun VideoPoster(poster: ImageBitmap?, durationMs: Long?, ratio: Float, onPlay: () -> Unit) {
    val (w, h) = frameSize(ratio)
    Box(
        Modifier.size(w, h).clip(frameShape).background(Color.Black).handCursor().clickable(onClick = onPlay),
        contentAlignment = Alignment.Center,
    ) {
        poster?.let { Image(it, stringResource(R.string.video_preview), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Box(Modifier.size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = .55f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PlayArrow, stringResource(R.string.video_play), tint = Color.White, modifier = Modifier.size(34.dp))
        }
        durationMs?.let {
            Text(
                clockOf(it), style = MaterialTheme.typography.labelMedium, color = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = .55f)).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/**
 * Il lettore nella card, che parte subito. Lo schermo intero è un dialogo sopra la chat con lo stesso lettore: il video
 * passa da un riquadro all'altro senza ricominciare. Si ferma quando l'app va in secondo piano e si chiude con la card.
 */
@Composable
private fun VideoPlayer(path: String, ratio: Float) {
    val context = LocalContext.current
    val player = remember(path) {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
            setMediaItem(MediaItem.fromUri(Uri.fromFile(java.io.File(path))))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, player) {
        val pause = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_PAUSE) player.pause() }
        lifecycle.addObserver(pause)
        onDispose { lifecycle.removeObserver(pause) }
    }
    var full by remember { mutableStateOf(false) }
    val (w, h) = frameSize(ratio)
    PlayerFrame(player, Modifier.size(w, h).clip(frameShape), attached = !full, isFull = false) { full = true }
    if (full) Dialog(onDismissRequest = { full = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        PlayerFrame(player, Modifier.fillMaxSize(), attached = true, isFull = true) { full = false }
    }
}

/** Il video con i comandi sopra: un tocco li mostra o li nasconde; mentre il video va spariscono da soli dopo 3 s. */
@Composable
private fun PlayerFrame(player: ExoPlayer, modifier: Modifier, attached: Boolean, isFull: Boolean, onFull: () -> Unit) {
    var controls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var muted by remember { mutableStateOf(player.volume == 0f) }
    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(p: Boolean) { isPlaying = p }
            override fun onPlaybackStateChanged(s: Int) { if (s == Player.STATE_ENDED) controls = true }
        }
        player.addListener(l)
        onDispose { player.removeListener(l) }
    }
    LaunchedEffect(player) {
        while (true) {
            pos = player.currentPosition
            dur = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
            delay(250)
        }
    }
    LaunchedEffect(controls, isPlaying) { if (controls && isPlaying) { delay(3_000); controls = false } }
    Box(modifier.background(Color.Black).clickable(interactionSource = null, indication = null) { controls = !controls }) {
        AndroidView(
            factory = { c -> PlayerView(c).apply { useController = false } },
            update = { v -> v.player = if (attached) player else null },
            onRelease = { v -> v.player = null },
            modifier = Modifier.fillMaxSize(),
        )
        AnimatedVisibility(controls, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .35f))) {
                IconButton(
                    onClick = {
                        if (player.isPlaying) player.pause()
                        else { if (player.playbackState == Player.STATE_ENDED) player.seekTo(0); player.play() }
                    },
                    modifier = Modifier.align(Alignment.Center).size(56.dp),
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        stringResource(if (isPlaying) R.string.video_pause else R.string.video_play), tint = Color.White, modifier = Modifier.size(40.dp),
                    )
                }
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 10.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(clockOf(pos), style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Slider(
                        value = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f,
                        onValueChange = { f -> pos = (f * dur).toLong(); player.seekTo(pos) },
                        enabled = dur > 0,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = .3f)),
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                    )
                    Text(clockOf(dur), style = MaterialTheme.typography.labelSmall, color = Color.White)
                    IconButton(onClick = { muted = !muted; player.volume = if (muted) 0f else 1f }) {
                        Icon(
                            if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            stringResource(if (muted) R.string.video_unmute else R.string.video_mute), tint = Color.White,
                        )
                    }
                    IconButton(onClick = onFull) {
                        Icon(
                            if (isFull) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                            stringResource(if (isFull) R.string.video_fullscreen_exit else R.string.video_fullscreen), tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}
