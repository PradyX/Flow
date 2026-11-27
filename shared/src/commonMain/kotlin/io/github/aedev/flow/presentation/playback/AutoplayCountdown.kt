package io.github.aedev.flow.presentation.playback

/**
 * The reference's countdown card while a finished queue waits for its next video: what will play,
 * whose it is, and how many of the setting's seconds are left. The shell owns it, so the card a page
 * draws is only a view of the wait the engine's end started.
 */
data class AutoplayCountdown(
    val track: QueueTrack,
    /** The next video's channel, which the card prints under its title. */
    val channel: String?,
    val secondsRemaining: Int,
    val totalSeconds: Int,
)
