package io.github.aedev.flow.data.music

import android.content.Context

private var applicationContext: Context? = null

/** Points the sort-order store at the app's own preferences; called once from the host. */
fun initializePlaylistSortStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun loadPlaylistSortOrder(playlistId: String): String? =
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.getString(key(playlistId), null)

actual fun savePlaylistSortOrder(
    playlistId: String,
    value: String,
) {
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(key(playlistId), value)
        ?.apply()
}

private fun key(playlistId: String): String = "$KEY_PREFIX$playlistId"

private const val PREFS = "flow_onboarding"
private const val KEY_PREFIX = "playlist_sort_"
