package io.github.aedev.flow.data.subscriptions

import android.content.Context

private var applicationContext: Context? = null

/** Points the artist-subscription store at the app's own preferences; called once from the host. */
fun initializeArtistSubscriptionStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun loadSubscribedArtistIds(): Set<String> =
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.getString(KEY_SUBSCRIBED_ARTISTS, null)
        ?.split(SEPARATOR)
        ?.filter { it.isNotBlank() }
        ?.toSet()
        .orEmpty()

actual fun saveSubscribedArtistIds(ids: Set<String>) {
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(KEY_SUBSCRIBED_ARTISTS, ids.joinToString(SEPARATOR))
        ?.apply()
}

private const val PREFS = "flow_onboarding"
private const val KEY_SUBSCRIBED_ARTISTS = "subscribed_artist_ids"
private const val SEPARATOR = ","
