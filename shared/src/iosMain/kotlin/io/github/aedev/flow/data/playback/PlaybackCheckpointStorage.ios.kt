package io.github.aedev.flow.data.playback

import platform.Foundation.NSUserDefaults

actual fun loadPlaybackCheckpointJson(): String? = NSUserDefaults.standardUserDefaults.stringForKey(KEY)

actual fun savePlaybackCheckpointJson(json: String) {
    NSUserDefaults.standardUserDefaults.setObject(json, KEY)
}

actual fun clearPlaybackCheckpointJson() {
    NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY)
}

private const val KEY = "flow_player_checkpoint"
