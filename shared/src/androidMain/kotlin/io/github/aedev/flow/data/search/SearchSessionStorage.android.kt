package io.github.aedev.flow.data.search

import android.content.Context

private var applicationContext: Context? = null

/** Points search-session storage at the app's own preferences; called once from the host. */
fun initializeSearchSessionStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun loadSearchSessionJson(): String? =
    applicationContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.getString(KEY_SEARCH_SESSION, null)

actual fun saveSearchSessionJson(json: String) {
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(KEY_SEARCH_SESSION, json)
        ?.apply()
}

actual fun loadSearchHistoryJson(): String? =
    applicationContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.getString(KEY_SEARCH_HISTORY, null)

actual fun saveSearchHistoryJson(json: String) {
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(KEY_SEARCH_HISTORY, json)
        ?.apply()
}

private const val PREFS = "flow_onboarding"
private const val KEY_SEARCH_SESSION = "search_session"
private const val KEY_SEARCH_HISTORY = "search_history"
