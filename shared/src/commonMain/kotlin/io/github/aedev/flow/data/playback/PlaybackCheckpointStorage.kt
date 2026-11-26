package io.github.aedev.flow.data.playback

/**
 * The player's position between runs, kept in its own tiny record because it is written many times
 * a second while the session (queue, index, modes) only changes when the queue does. A checkpoint
 * left from another track is ignored when it is read, so a stale record cannot move the player.
 */
expect fun loadPlaybackCheckpointJson(): String?

expect fun savePlaybackCheckpointJson(json: String)

expect fun clearPlaybackCheckpointJson()
