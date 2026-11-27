package io.github.aedev.flow.presentation.playback

import io.github.aedev.flow.innertube.pages.player.PlayableStream

/** iOS plays through its own media stack, which keeps no cache this app can clear. */
actual fun clearPlaybackCache() = Unit

/** iOS keeps no media cache of this app's, so there is nothing to warm. */
actual fun preloadPlaybackStream(
    videoId: String,
    stream: PlayableStream,
) = Unit
