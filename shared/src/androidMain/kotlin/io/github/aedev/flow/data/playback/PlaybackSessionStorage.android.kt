package io.github.aedev.flow.data.playback

import android.content.Context

private var applicationContext: Context? = null

/** Points the session store at the app's own preferences; called once from the host. */
fun initializePlaybackSessionStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun loadPlaybackSessionJson(): String? =
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.getString(KEY, null)

actual fun savePlaybackSessionJson(json: String) {
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(KEY, json)
        ?.apply()
}

actual fun clearPlaybackSessionJson() {
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.edit()
        ?.remove(KEY)
        ?.apply()
}

private const val PREFS = "flow_onboarding"
private const val KEY = "player_session"
