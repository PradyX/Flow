package io.github.aedev.flow.data.history

import android.content.Context

private var applicationContext: Context? = null

/** Points the history store at the app's own preferences; called once from the host. */
fun initializeWatchHistoryStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun loadWatchHistoryJson(): String? =
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.getString(KEY, null)

actual fun saveWatchHistoryJson(json: String) {
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(KEY, json)
        ?.apply()
}

private const val PREFS = "flow_onboarding"
private const val KEY = "watch_history"
