package io.github.aedev.flow.data.subscriptions

import platform.Foundation.NSUserDefaults

actual fun loadSubscribedArtistIds(): Set<String> =
    NSUserDefaults.standardUserDefaults
        .stringForKey(KEY_SUBSCRIBED_ARTISTS)
        ?.split(",")
        ?.filter { it.isNotBlank() }
        ?.toSet()
        .orEmpty()

actual fun saveSubscribedArtistIds(ids: Set<String>) {
    NSUserDefaults.standardUserDefaults.setObject(ids.joinToString(","), KEY_SUBSCRIBED_ARTISTS)
}

private const val KEY_SUBSCRIBED_ARTISTS = "flow_subscribed_artist_ids"
