package io.github.aedev.flow.presentation.playback

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One queued track as the player writes it down between runs. */
@Serializable
data class PlaybackSessionTrack(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String? = null,
)

/**
 * The player as it should come back after the process dies: the queue in its own order, the track it
 * was on, the position inside that track and the two modes that decide what plays next. It is plain
 * data with an encoder, so restoring is testable without a player or a database.
 */
@Serializable
data class PlaybackSession(
    val tracks: List<PlaybackSessionTrack>,
    val index: Int,
    val shuffled: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
    val positionMs: Long = 0,
) {
    /** The queue this session describes, or null when it holds nothing playable. */
    fun toQueue(): PlaybackQueue? {
        val queue = tracks.filter { it.videoId.isNotBlank() }.map { it.toQueueTrack() }
        if (queue.isEmpty()) return null
        return PlaybackQueue(
            tracks = queue,
            index = index.coerceIn(0, queue.lastIndex),
            shuffled = shuffled,
            repeat = repeat,
        )
    }

    /** The saved position, kept inside the track it belongs to and never negative. */
    fun position(): Long = positionMs.coerceAtLeast(0)

    fun encode(): String = SessionJson.encodeToString(serializer(), this)

    companion object {
        /** [queue] at [positionMs], ready to be written down. */
        fun of(
            queue: PlaybackQueue,
            positionMs: Long,
        ): PlaybackSession =
            PlaybackSession(
                tracks = queue.tracks.map { PlaybackSessionTrack(it.videoId, it.title, it.thumbnailUrl) },
                index = queue.index,
                shuffled = queue.shuffled,
                repeat = queue.repeat,
                positionMs = positionMs.coerceAtLeast(0),
            )

        /**
         * The session [json] holds, or null when it is absent, unreadable, or no longer describes
         * anything playable (the queue was emptied, or every entry went stale).
         */
        fun decode(json: String?): PlaybackSession? {
            val text = json?.takeIf { it.isNotBlank() } ?: return null
            val parsed =
                runCatching { SessionJson.decodeFromString(serializer(), text) }.getOrNull()
                    ?: return null
            return parsed.takeIf { it.toQueue() != null }
        }
    }
}

private fun PlaybackSessionTrack.toQueueTrack(): QueueTrack =
    QueueTrack(videoId = videoId, title = title.ifBlank { videoId }, thumbnailUrl = thumbnailUrl)

private val SessionJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
