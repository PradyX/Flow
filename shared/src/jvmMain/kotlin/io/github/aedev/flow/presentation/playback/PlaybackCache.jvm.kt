package io.github.aedev.flow.presentation.playback

import io.github.aedev.flow.innertube.pages.player.PlayableStream

/** The desktop target buffers nothing it owns, so there is nothing to free. */
actual fun clearPlaybackCache() = Unit

/** The desktop target keeps no media cache, so there is nothing to warm. */
actual fun preloadPlaybackStream(
    videoId: String,
    stream: PlayableStream,
) = Unit
