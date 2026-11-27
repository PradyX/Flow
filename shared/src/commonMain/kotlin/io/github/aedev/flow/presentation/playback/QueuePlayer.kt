package io.github.aedev.flow.presentation.playback

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.aedev.flow.innertube.pages.player.PlayableStream

/** A resolved track on its way to the player, tagged with the queue entry it belongs to. */
data class PlayerMedia(
    val videoId: String,
    val stream: PlayableStream,
    /** What the notification and the lock screen print for this track. */
    val title: String = "",
    val artist: String? = null,
    val artworkUri: String? = null,
)

/**
 * One engine for a whole queue. Tracks are handed to it as the pre-resolve window fills, and it
 * keeps them in its own playlist: a track change is the engine advancing to the next entry, not a
 * new engine being built for the next URL. That is what removes the reload between tracks.
 *
 * [sync] is idempotent. Calling it with a window the engine already holds only re-aligns the
 * current entry and the modes; a longer window appends instead of reloading; a window that starts
 * somewhere else is a new queue and replaces the playlist.
 */
interface QueuePlayer {
    fun sync(
        media: List<PlayerMedia>,
        /** The entry of [media] that is playing: the queue's own index for the window. */
        startIndex: Int,
        /** Where that entry resumes; only used when the playlist is (re)built. */
        startPositionMs: Long,
        repeat: RepeatMode,
        playing: Boolean,
        /** True when this window belongs to another queue than the one the engine holds. */
        replace: Boolean = false,
    )

    /**
     * Moves playback to [videoId] inside the playlist, at [positionMs] when it is given. This is the
     * only way the engine's own position changes other than it advancing by itself: a page following
     * the engine never seeks.
     */
    fun select(
        videoId: String,
        positionMs: Long = 0,
    )

    /**
     * Moves the engine inside the entry that is playing now. A seek never changes the track: the
     * page's bar is the one caller, so dragging it is the position moving, not the queue stepping.
     */
    fun seekTo(positionMs: Long)

    /**
     * Forgets the playlist the engine holds, without touching what is playing, so the next [sync]
     * installs its window even when that window begins before the engine's own tail. A jump to
     * another row of the same queue is the caller that needs this.
     */
    fun resetQueue()

    fun positionMs(): Long

    /** How long the entry playing now runs, or 0 while the engine does not know yet. */
    fun durationMs(): Long

    fun currentVideoId(): String?

    /** True while the engine is actually producing sound, for a surface that mirrors it. */
    fun isPlaying(): Boolean

    fun pause()

    fun play()

    /** Stops the engine and drops its playlist, so the session has nothing left to publish. */
    fun stop()
}

/** The player on a target without an engine: it holds nothing and plays nothing. */
internal object NoQueuePlayer : QueuePlayer {
    override fun sync(
        media: List<PlayerMedia>,
        startIndex: Int,
        startPositionMs: Long,
        repeat: RepeatMode,
        playing: Boolean,
        replace: Boolean,
    ) = Unit

    override fun select(
        videoId: String,
        positionMs: Long,
    ) = Unit

    override fun seekTo(positionMs: Long) = Unit

    override fun resetQueue() = Unit

    override fun positionMs(): Long = 0

    override fun durationMs(): Long = 0

    override fun currentVideoId(): String? = null

    override fun isPlaying(): Boolean = false

    override fun pause() = Unit

    override fun play() = Unit

    override fun stop() = Unit
}

/**
 * The queue engine for this target, remembered for as long as the caller is composed. [player] is
 * owned by the shell, so the engine outlives one track's page: the page for the next track binds
 * the same engine and playback simply continues.
 */
@Composable
expect fun rememberQueuePlayer(
    onTrackStarted: (videoId: String) -> Unit,
    onEnded: () -> Unit,
    onError: () -> Unit,
    /**
     * The engine's position, reported several times a second while it runs, so the host can keep a
     * record that survives a process kill. [videoId] is the entry the position belongs to.
     */
    onCheckpoint: (videoId: String, positionMs: Long) -> Unit = { _, _ -> },
): QueuePlayer

/** The surface the engine draws into; the same player can be bound again after a recomposition. */
@Composable
expect fun QueuePlayerSurface(
    player: QueuePlayer,
    modifier: Modifier,
)
