package io.github.aedev.flow.data.subscriptions

/**
 * The artists whose subscribe control is on, by channel id. YouTube persists a subscription on the
 * account; an anonymous visitor has none, so the app keeps its own answer here to keep the pill
 * reading the same after a relaunch.
 */
expect fun loadSubscribedArtistIds(): Set<String>

expect fun saveSubscribedArtistIds(ids: Set<String>)

/** The stored ids as a mutable set, ready for a toggle. */
internal fun subscribedArtistIds(): MutableSet<String> = loadSubscribedArtistIds().toMutableSet()

/** Records [subscribed] for [channelId] and writes the set back. */
internal fun rememberArtistSubscription(
    channelId: String,
    subscribed: Boolean,
) {
    val ids = subscribedArtistIds()
    if (subscribed) ids += channelId else ids -= channelId
    saveSubscribedArtistIds(ids)
}
