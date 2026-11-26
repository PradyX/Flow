package io.github.aedev.flow.data.music

/**
 * The order the last visit gave a collection's rows, kept in the app's own storage so the chip reads
 * the same order after navigating away or relaunching. The seam carries text: the page decides what
 * a stored value means when the enum no longer has it.
 */
expect fun loadPlaylistSortOrder(playlistId: String): String?

expect fun savePlaylistSortOrder(
    playlistId: String,
    value: String,
)
