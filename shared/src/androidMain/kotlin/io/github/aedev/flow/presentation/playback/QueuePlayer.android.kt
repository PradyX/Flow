package io.github.aedev.flow.presentation.playback

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import io.github.aedev.flow.innertube.pages.player.PlayableStream

@Composable
actual fun rememberQueuePlayer(
    onTrackStarted: (videoId: String) -> Unit,
    onEnded: () -> Unit,
    onError: () -> Unit,
    onCheckpoint: (videoId: String, positionMs: Long) -> Unit,
): QueuePlayer {
    val context = LocalContext.current
    // The callbacks belong to the page that is composing now, not the one that built the engine.
    val started by rememberUpdatedState(onTrackStarted)
    val ended by rememberUpdatedState(onEnded)
    val failed by rememberUpdatedState(onError)
    val checkpoint by rememberUpdatedState(onCheckpoint)
    val player =
        remember(context) {
            AndroidQueuePlayer(
                context = context.applicationContext,
                onTrackStarted = { videoId -> started(videoId) },
                onEnded = { ended() },
                onFailure = { failed() },
                onCheckpoint = { videoId, positionMs -> checkpoint(videoId, positionMs) },
            )
        }
    DisposableEffect(player) {
        player.startCheckpoints()
        onDispose { player.stopCheckpoints() }
    }
    return player
}

@Composable
actual fun QueuePlayerSurface(
    player: QueuePlayer,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val engine = (player as? AndroidQueuePlayer)?.engine ?: return
    DisposableEffect(engine) {
        val activity = context.findActivity()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = engine
                // The page draws the controls itself; the platform controller would only add a
                // second, half-drawn bar on top of them.
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        modifier = modifier,
    )
}

/**
 * The engine itself. It exists once per app run and owns one ExoPlayer; [sync] only ever appends to
 * or rebuilds that player's playlist, so consecutive tracks are one continuous timeline.
 */
internal class AndroidQueuePlayer(
    private val context: Context,
    private val onTrackStarted: (videoId: String) -> Unit,
    private val onEnded: () -> Unit,
    private val onFailure: () -> Unit,
    private val onCheckpoint: (videoId: String, positionMs: Long) -> Unit,
) : QueuePlayer,
    PlaybackHost.EngineListener {
    /** The queue entries the engine's playlist currently holds, in order. */
    private var loadedIds: List<String> = emptyList()
    private var lastReportedId: String? = null

    // The app's one engine: the media session publishes this same instance, so the notification
    // and the lock screen control the queue that is actually playing.
    val engine: ExoPlayer = PlaybackHost.player(context)

    init {
        PlaybackHost.watch(this)
    }

    override fun onTransition(
        index: Int,
        reason: Int,
    ) {
        Log.d(
            "FlowPlayback",
            "transition to=$index reason=$reason at=${engine.currentPosition}ms state=${engine.playbackState}",
        )
        // A playlist change (an appended window) is not a track change.
        val videoId = loadedIds.getOrNull(index)
        if (videoId != null && videoId != lastReportedId) {
            lastReportedId = videoId
            onTrackStarted(videoId)
        }
    }

    override fun onStateChanged(state: Int) {
        Log.d("FlowPlayback", "state=$state at item=${engine.currentMediaItemIndex} pos=${engine.currentPosition}")
        if (state == Player.STATE_ENDED) onEnded()
    }

    override fun onError(error: PlaybackException) {
        Log.e("FlowPlayback", "queue playback failed", error)
        onFailure()
    }

    fun startCheckpoints() {
        checkpointHandler.post(checkpointRunnable)
    }

    fun stopCheckpoints() {
        checkpointHandler.removeCallbacks(checkpointRunnable)
    }

    // A killed process runs no code on the way out, so the position is written often enough that
    // whatever is lost is only the time since the last tick.
    private val checkpointHandler = Handler(Looper.getMainLooper())
    private val checkpointRunnable =
        object : Runnable {
            override fun run() {
                val videoId = loadedIds.getOrNull(engine.currentMediaItemIndex)
                if (videoId != null) onCheckpoint(videoId, engine.currentPosition)
                checkpointHandler.postDelayed(this, CHECKPOINT_INTERVAL_MS)
            }
        }

    override fun sync(
        media: List<PlayerMedia>,
        startIndex: Int,
        startPositionMs: Long,
        repeat: RepeatMode,
        playing: Boolean,
        replace: Boolean,
    ) {
        Log.d(
            "FlowPlayback",
            "sync media=${media.map { it.videoId }} start=$startIndex at=${startPositionMs}ms playing=$playing loaded=$loadedIds",
        )
        if (media.isEmpty()) return
        val ids = media.map { it.videoId }
        val index = startIndex.coerceIn(0, media.lastIndex)
        engine.repeatMode =
            when (repeat) {
                RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            }
        // The window always starts at the track that is playing, so it is a tail of what the engine
        // already holds: stepping moves the window forward rather than replacing the playlist.
        val overlap = loadedIds.indexOf(ids.first())
        // A page that is being replaced can still compose once after the engine moved on; its
        // window starts before the track that is playing and must not touch the playlist.
        if (!replace && overlap < 0 && loadedIds.isNotEmpty()) return
        val held = if (overlap >= 0) loadedIds.drop(overlap) else emptyList()
        // The window and the held tail line up when one is a prefix of the other: shorter is a
        // late page still catching up, longer is the pre-resolve fetching further ahead.
        val compatible = overlap >= 0 && (held.take(ids.size) == ids || ids.take(held.size) == held)
        when {
            replace || loadedIds.isEmpty() || !compatible -> {
                engine.setMediaSources(
                    media.map { it.toMediaSource(context) },
                    index,
                    startPositionMs.coerceAtLeast(0),
                )
                loadedIds = ids
                engine.prepare()
            }

            else -> {
                if (ids.size > held.size) {
                    media.drop(held.size).forEach { engine.addMediaSource(it.toMediaSource(context)) }
                    loadedIds = loadedIds + ids.drop(held.size)
                }
            }
        }
        // Everything after the playing entry is filled into the cache now, so by the time the
        // boundary arrives its media is already on disk.
        media.drop((index + 1).coerceAtMost(media.size)).forEach { upcoming ->
            MediaCache.prefetch(context, upcoming.videoId, upcoming.stream)
        }
        engine.playWhenReady = playing
        // Playing brings up the session's service: a foreground media service with a notification,
        // which is what keeps the audio alive once Flow is off screen.
        if (playing) PlaybackHost.startService(context)
    }

    override fun select(
        videoId: String,
        positionMs: Long,
    ) {
        val index = loadedIds.indexOf(videoId)
        if (index < 0) return
        if (currentVideoId() != videoId) {
            Log.d("FlowPlayback", "select $videoId at $index")
            engine.seekTo(index, 0)
        } else if (positionMs > 0) {
            // A note's moment, or where History stopped: the item playing is the same one, so the
            // seek is a position inside it.
            Log.d("FlowPlayback", "select $videoId seek=${positionMs}ms")
            engine.seekTo(positionMs)
        }
    }

    override fun seekTo(positionMs: Long) {
        Log.d("FlowPlayback", "seek to ${positionMs}ms")
        engine.seekTo(positionMs.coerceAtLeast(0))
    }

    override fun resetQueue() {
        // Only the bookkeeping: the engine keeps playing until the next sync swaps its playlist, so
        // a jump between rows of one queue does not gap.
        Log.d("FlowPlayback", "resetQueue was=$loadedIds")
        loadedIds = emptyList()
        lastReportedId = null
    }

    override fun positionMs(): Long = engine.currentPosition

    override fun durationMs(): Long = engine.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0

    override fun currentVideoId(): String? = loadedIds.getOrNull(engine.currentMediaItemIndex)

    override fun isPlaying(): Boolean = engine.playWhenReady

    override fun pause() {
        engine.pause()
    }

    override fun play() {
        // Resuming brings the notification back, as the session's own resume does.
        engine.playWhenReady = true
        PlaybackHost.startService(context)
    }

    override fun stop() {
        Log.d("FlowPlayback", "stop")
        // Clearing the playlist makes the engine report a transition, and a transition to an id this
        // player still holds would read as a track starting. Dropping the record first keeps the
        // stop a stop: nothing is opened, nothing is re-resolved.
        loadedIds = emptyList()
        lastReportedId = null
        engine.stop()
        engine.clearMediaItems()
        stopCheckpoints()
    }
}

/**
 * One entry's media. Adaptive video and audio are two files, merged into one timeline entry; the
 * datasource presents the user agent the URL was minted for, because GVS binds them together. The
 * item carries the track's metadata, which is what the notification and the lock screen draw.
 */
private fun PlayerMedia.toMediaSource(context: Context): MediaSource {
    // A URL reads through the cache; an imported file is opened straight from the device.
    val dataSource: DataSource.Factory =
        if (MediaCache.isLocalStream(stream)) {
            MediaCache.localDataSourceFactory(context)
        } else {
            MediaCache.dataSourceFactory(context, stream)
        }
    val metadata =
        MediaMetadata
            .Builder()
            .setTitle(title.ifBlank { videoId })
            .apply {
                artist?.takeIf { it.isNotBlank() }?.let { setArtist(it) }
                artworkUri(artworkUri)?.let { setArtworkUri(it) }
            }.build()
    val item =
        MediaItem
            .Builder()
            .setUri(stream.videoUrl)
            .setMediaMetadata(metadata)
            .build()
    val video = ProgressiveMediaSource.Factory(dataSource).createMediaSource(item)
    return stream.audioUrl
        ?.let { audioUrl ->
            MergingMediaSource(
                video,
                ProgressiveMediaSource.Factory(dataSource).createMediaSource(MediaItem.fromUri(audioUrl)),
            )
        } ?: video
}

private const val CHECKPOINT_INTERVAL_MS = 100L

/** The window the player lives in, so the screen stays awake while a queue is on it. */
private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
