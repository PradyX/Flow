package io.github.aedev.flow.data.search

import platform.Foundation.NSUserDefaults

actual fun loadSearchSessionJson(): String? = NSUserDefaults.standardUserDefaults.stringForKey(KEY_SEARCH_SESSION)

actual fun saveSearchSessionJson(json: String) {
    NSUserDefaults.standardUserDefaults.setObject(json, KEY_SEARCH_SESSION)
}

actual fun loadSearchHistoryJson(): String? = NSUserDefaults.standardUserDefaults.stringForKey(KEY_SEARCH_HISTORY)

actual fun saveSearchHistoryJson(json: String) {
    NSUserDefaults.standardUserDefaults.setObject(json, KEY_SEARCH_HISTORY)
}

private const val KEY_SEARCH_SESSION = "flow_search_session"
private const val KEY_SEARCH_HISTORY = "flow_search_history"
