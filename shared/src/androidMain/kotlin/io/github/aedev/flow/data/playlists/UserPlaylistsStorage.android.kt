package io.github.aedev.flow.data.playlists

import android.content.Context

private var applicationContext: Context? = null

/** Points the playlists store at the app's own preferences; called once from the host. */
fun initializeUserPlaylistsStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun loadUserPlaylistsJson(): String? =
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.getString(KEY, null)

actual fun saveUserPlaylistsJson(json: String) {
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(KEY, json)
        ?.apply()
}

private const val PREFS = "flow_onboarding"
private const val KEY = "user_playlists"
