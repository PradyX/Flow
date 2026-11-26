package io.github.aedev.flow.presentation.playback

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Where the player is right now: the track and the millisecond inside it. It is written far more
 * often than the session (the queue, index and modes), because a killed process may not get to run
 * any code on the way out and this is the only part that keeps moving.
 */
@Serializable
data class PlaybackCheckpoint(
    val videoId: String,
    val positionMs: Long,
) {
    fun encode(): String = CheckpointJson.encodeToString(serializer(), this)

    companion object {
        /** The checkpoint [json] holds, or null when it is absent, unreadable or has no track. */
        fun decode(json: String?): PlaybackCheckpoint? {
            val text = json?.takeIf { it.isNotBlank() } ?: return null
            val parsed = runCatching { CheckpointJson.decodeFromString(serializer(), text) }.getOrNull() ?: return null
            return parsed.takeIf { it.videoId.isNotBlank() }
        }
    }
}

/**
 * [this] session resumed at [checkpoint], when the checkpoint belongs to the session's current
 * track. A checkpoint for another track is stale after a step or a queue change and is ignored, so
 * the queue never comes back pointing inside a track it is not on.
 */
fun PlaybackSession.withCheckpoint(checkpoint: PlaybackCheckpoint?): PlaybackSession {
    val current = toQueue()?.current?.videoId ?: return this
    if (checkpoint == null || checkpoint.videoId != current) return this
    return copy(positionMs = checkpoint.positionMs.coerceAtLeast(0))
}

private val CheckpointJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
