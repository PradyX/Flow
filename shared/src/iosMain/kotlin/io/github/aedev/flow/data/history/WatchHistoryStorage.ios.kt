package io.github.aedev.flow.data.history

import platform.Foundation.NSUserDefaults

actual fun loadWatchHistoryJson(): String? = NSUserDefaults.standardUserDefaults.stringForKey(KEY)

actual fun saveWatchHistoryJson(json: String) {
    NSUserDefaults.standardUserDefaults.setObject(json, KEY)
}

private const val KEY = "flow_watch_history"
