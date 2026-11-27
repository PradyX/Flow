package io.github.aedev.flow.presentation.playback

import kotlin.random.Random

/** What plays when the last track ends. Manual next and previous ignore it, as they do in the app. */
enum class RepeatMode {
    OFF,
    ALL,
    ONE,
    ;

    /** The mode the repeat control moves on to, in the order it cycles through them. */
    fun next(): RepeatMode =
        when (this) {
            OFF -> ALL
            ALL -> ONE
            ONE -> OFF
        }
}

/** One entry of a collection being played: what the player needs to open the track and label it. */
data class QueueTrack(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String? = null,
    /** Set when a music shelf started the track, so listening history stays music-only. */
    val music: Boolean = false,
)

/**
 * A collection's tracks in the order they will play, with the track the player is on. The queue is
 * plain data, so stepping and building it are testable without a screen or a player: [next] and
 * [previous] return the queue moved by one track, and return null at either end instead of wrapping,
 * which is how playback stops cleanly after the last track.
 */
data class PlaybackQueue(
    val tracks: List<QueueTrack>,
    val index: Int,
    val shuffled: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
) {
    val current: QueueTrack?
        get() = tracks.getOrNull(index)

    val hasNext: Boolean
        get() = index + 1 < tracks.size

    val hasPrevious: Boolean
        get() = index > 0

    val size: Int
        get() = tracks.size

    fun next(): PlaybackQueue? = if (hasNext) copy(index = index + 1) else null

    fun previous(): PlaybackQueue? = if (hasPrevious) copy(index = index - 1) else null

    fun withRepeat(mode: RepeatMode): PlaybackQueue = copy(repeat = mode)

    /**
     * The queue after the playing track ends, which is where repeat applies: the one track plays
     * again, the list wraps to its start, or playback stops. Manual stepping does not come here.
     */
    fun afterEnded(): PlaybackQueue? =
        when {
            tracks.isEmpty() -> null
            repeat == RepeatMode.ONE -> this
            hasNext -> copy(index = index + 1)
            repeat == RepeatMode.ALL -> copy(index = 0)
            else -> null
        }

    /**
     * The tracks the player should be holding ready: this one and the next [ahead], in queue order.
     * With repeat-all the window wraps at the end of the list, which is how the last track still
     * hands over to the first without a reload.
     */
    fun preResolveWindow(ahead: Int = 2): List<Int> {
        if (tracks.isEmpty() || index !in tracks.indices) return emptyList()
        val wanted = ahead.coerceAtLeast(0) + 1
        val forward = (index..minOf(tracks.lastIndex, index + wanted - 1)).toList()
        if (repeat != RepeatMode.ALL || forward.size >= wanted) return forward
        return forward + (0..minOf(tracks.lastIndex, wanted - forward.size - 1)).filterNot { it in forward }
    }

    /**
     * [tracks] inserted directly after the current one, the reference's "Play next": what is playing
     * keeps playing, the new tracks follow it, and whatever was already queued moves behind them.
     */
    fun withNext(tracks: List<QueueTrack>): PlaybackQueue =
        if (tracks.isEmpty()) {
            this
        } else {
            copy(tracks = this.tracks.take(index + 1) + tracks + this.tracks.drop(index + 1))
        }

    /** [tracks] added after the last track, the reference's "Add to queue". */
    fun withLast(tracks: List<QueueTrack>): PlaybackQueue = if (tracks.isEmpty()) this else copy(tracks = this.tracks + tracks)

    companion object {
        /**
         * [tracks] as a queue starting on its first entry, shuffled when [shuffled]; null for an
         * empty collection, which has nothing to play. The shuffle is a parameter so a test can pin
         * the order it gets.
         */
        fun of(
            tracks: List<QueueTrack>,
            shuffled: Boolean = false,
            random: Random = Random.Default,
        ): PlaybackQueue? {
            if (tracks.isEmpty()) return null
            return PlaybackQueue(
                tracks = if (shuffled) tracks.shuffled(random) else tracks,
                index = 0,
                shuffled = shuffled,
            )
        }
    }
}
