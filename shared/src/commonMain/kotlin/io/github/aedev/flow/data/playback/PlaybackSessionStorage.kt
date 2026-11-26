package io.github.aedev.flow.data.playback

/**
 * Where the player's session is kept between runs: the queue, the track it was on, the position and
 * the modes. It is text so the same session can be written by any host, and a value the codec no
 * longer understands is simply ignored when it is read back.
 */
expect fun loadPlaybackSessionJson(): String?

expect fun savePlaybackSessionJson(json: String)

/** Removes the stored session: the queue the user emptied must not come back next launch. */
expect fun clearPlaybackSessionJson()
