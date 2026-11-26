package io.github.aedev.flow.data.playlists

import platform.Foundation.NSUserDefaults

actual fun loadUserPlaylistsJson(): String? = NSUserDefaults.standardUserDefaults.stringForKey(KEY)

actual fun saveUserPlaylistsJson(json: String) {
    NSUserDefaults.standardUserDefaults.setObject(json, KEY)
}

private const val KEY = "flow_user_playlists"
