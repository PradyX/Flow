package io.github.aedev.flow.data.playback

import platform.Foundation.NSUserDefaults

actual fun loadPlaybackSessionJson(): String? = NSUserDefaults.standardUserDefaults.stringForKey(KEY)

actual fun savePlaybackSessionJson(json: String) {
    NSUserDefaults.standardUserDefaults.setObject(json, KEY)
}

actual fun clearPlaybackSessionJson() {
    NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY)
}

private const val KEY = "flow_player_session"
