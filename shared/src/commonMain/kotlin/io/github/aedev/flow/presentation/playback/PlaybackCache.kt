package io.github.aedev.flow.presentation.playback

/**
 * Frees the buffered media the player keeps on disk. Entries a running player still holds are left
 * alone, so clearing while something plays does not interrupt it.
 */
expect fun clearPlaybackCache()
