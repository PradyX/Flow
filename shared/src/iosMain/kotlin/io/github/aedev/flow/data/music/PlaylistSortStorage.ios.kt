package io.github.aedev.flow.data.music

import platform.Foundation.NSUserDefaults

actual fun loadPlaylistSortOrder(playlistId: String): String? = NSUserDefaults.standardUserDefaults.stringForKey("$KEY_PREFIX$playlistId")

actual fun savePlaylistSortOrder(
    playlistId: String,
    value: String,
) {
    NSUserDefaults.standardUserDefaults.setObject(value, "$KEY_PREFIX$playlistId")
}

private const val KEY_PREFIX = "flow_playlist_sort_"
