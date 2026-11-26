package io.github.aedev.flow.presentation.playback

/** iOS plays through its own media stack, which keeps no cache this app can clear. */
actual fun clearPlaybackCache() = Unit
