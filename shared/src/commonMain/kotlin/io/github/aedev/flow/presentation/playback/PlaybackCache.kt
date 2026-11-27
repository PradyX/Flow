package io.github.aedev.flow.presentation.playback

import io.github.aedev.flow.innertube.pages.player.PlayableStream

/**
 * Frees the buffered media the player keeps on disk. Entries a running player still holds are left
 * alone, so clearing while something plays does not interrupt it.
 */
expect fun clearPlaybackCache()

/**
 * Warms the media of the video a finished queue is about to roll into. The caller keeps the resolved
 * [stream] and hands it to the next page, because the bytes are cached under these exact URLs: a
 * second resolution would mint different ones and start from the network again.
 */
expect fun preloadPlaybackStream(
    videoId: String,
    stream: PlayableStream,
)
