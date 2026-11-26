package io.github.aedev.flow.presentation.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * The app's one playback engine and the media session that publishes it. Both are process-wide:
 * the screen hands the engine its queue, and the session is what the notification, the lock screen
 * and Bluetooth buttons talk to, so playback keeps running and controllable when Flow is not on
 * screen.
 */
internal object PlaybackHost {
    private var engine: ExoPlayer? = null
    private var session: MediaSession? = null

    /**
     * What the engine reports to. The engine outlives every page, so it holds one listener and
     * forwards to whoever composed last: a page that composed again replaces the one before it
     * instead of adding a second copy of every callback.
     */
    interface EngineListener {
        fun onTransition(
            index: Int,
            reason: Int,
        )

        fun onStateChanged(state: Int)

        fun onError(error: PlaybackException)
    }

    private var activeListener: EngineListener? = null

    fun watch(listener: EngineListener) {
        synchronized(this) { activeListener = listener }
    }

    fun player(context: Context): ExoPlayer =
        synchronized(this) {
            engine
                ?: ExoPlayer
                    .Builder(context.applicationContext)
                    // Enough read-ahead that the following track is already buffered when the
                    // current one ends, and a stall mid-track does not reach the screen.
                    .setLoadControl(
                        DefaultLoadControl
                            .Builder()
                            .setBufferDurationsMs(
                                DEFAULT_MIN_BUFFER_MS,
                                DEFAULT_MAX_BUFFER_MS,
                                DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                                DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                            ).build(),
                    )
                    // Music content with audio focus: a call or another player takes the audio, and
                    // headphones being pulled out pauses instead of playing to the room.
                    .setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .build(),
                        true,
                    ).setHandleAudioBecomingNoisy(true)
                    // Background audio is streamed off the network, which is what lets the engine
                    // keep its buffers while the screen is off.
                    .setWakeMode(C.WAKE_MODE_NETWORK)
                    .build()
                    .apply {
                        addListener(
                            object : Player.Listener {
                                override fun onMediaItemTransition(
                                    mediaItem: MediaItem?,
                                    reason: Int,
                                ) = activeListener?.onTransition(currentMediaItemIndex, reason) ?: Unit

                                override fun onPlaybackStateChanged(playbackState: Int) =
                                    activeListener?.onStateChanged(playbackState) ?: Unit

                                override fun onPlayerError(error: PlaybackException) = activeListener?.onError(error) ?: Unit
                            },
                        )
                        playWhenReady = false
                        // The next entry of the queue is kept loaded ahead of the boundary, so the
                        // handoff starts on prepared media instead of fetching its first bytes then.
                        setPreloadConfiguration(ExoPlayer.PreloadConfiguration(PRELOAD_TARGET_US))
                    }.also { engine = it }
        }

    fun session(context: Context): MediaSession =
        synchronized(this) {
            session
                ?: MediaSession
                    .Builder(context.applicationContext, player(context))
                    .apply {
                        // Tapping the notification opens Flow on the page the queue belongs to.
                        launchIntent(context)?.let { setSessionActivity(it) }
                    }.build()
                    .also { session = it }
        }

    /** The session the service publishes; null until something asked for one. */
    fun existingSession(): MediaSession? = synchronized(this) { session }

    /**
     * Brings up the playback service, which is what turns a playing queue into a foreground
     * service with a notification instead of a foreground app that is about to be reclaimed.
     */
    fun startService(context: Context) {
        runCatching {
            context.applicationContext.startService(Intent(context.applicationContext, FlowPlaybackService::class.java))
        }.onFailure { error -> Log.w("FlowPlayback", "playback service did not start", error) }
    }
}

/** The app's single launch intent, as a PendingIntent the session can carry. */
private fun launchIntent(context: Context): PendingIntent? {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
    return PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/**
 * Hosts the media session so the system treats Flow as a media app: the notification, the lock
 * screen, the media keys and the audio focus all go through it, and playback survives the app being
 * backgrounded or its task being swiped away.
 */
class FlowPlaybackService : MediaSessionService() {
    override fun onCreate() {
        super.onCreate()
        // The notification is Media3's own media notification, drawn from the session's metadata;
        // the small icon is the platform's play glyph rather than a second set of brand assets.
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply { setSmallIcon(android.R.drawable.ic_media_play) },
        )
        val session = PlaybackHost.session(this)
        if (!isSessionAdded(session)) addSession(session)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        PlaybackHost.existingSession() ?: PlaybackHost.session(this)

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping the task away leaves the service (and its notification) only while something is
        // actually playing; an idle queue takes the service down with the task.
        if (!isPlaybackOngoing()) stopSelf()
    }
}

/** The artwork a notification may draw, kept as a URI so the session never holds a bitmap. */
internal fun artworkUri(url: String?): Uri? = url?.takeIf { it.isNotBlank() }?.let(Uri::parse)

/** How much of the next queue entry is kept buffered ahead of the boundary. */
private const val PRELOAD_TARGET_US = 20_000_000L
private const val DEFAULT_MIN_BUFFER_MS = 30_000
private const val DEFAULT_MAX_BUFFER_MS = 120_000
private const val DEFAULT_BUFFER_FOR_PLAYBACK_MS = 2_500
private const val DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5_000
