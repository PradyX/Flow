package io.github.aedev.flow.data.subscriptions

import io.github.aedev.flow.data.taste.TasteChannel
import io.github.aedev.flow.data.taste.UserTaste
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A channel the viewer is subscribed to: the id every feed asks by, and what a strip draws. */
@Serializable
data class Subscription(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
)

/**
 * The one subscription store. Setup seeds it from its channel picks, and the channel page's
 * subscribe control adds and removes channels; the Subscriptions tab reads only this.
 */
@Serializable
data class SubscriptionBook(
    val channels: List<Subscription> = emptyList(),
) {
    fun isSubscribed(channelId: String): Boolean = channels.any { it.id == channelId }

    fun channel(channelId: String): Subscription? = channels.firstOrNull { it.id == channelId }

    /** Subscribing keeps the channel's newest details and moves it to the end of the strip. */
    fun with(channel: Subscription): SubscriptionBook = copy(channels = channels.filterNot { it.id == channel.id } + channel)

    fun without(channelId: String): SubscriptionBook = copy(channels = channels.filterNot { it.id == channelId })

    /** Puts an unsubscribed channel back at [index], so Undo restores the strip's order too. */
    fun restoring(
        channel: Subscription,
        index: Int,
    ): SubscriptionBook {
        val remaining = channels.filterNot { it.id == channel.id }
        val at = index.coerceIn(0, remaining.size)
        return copy(channels = remaining.take(at) + channel + remaining.drop(at))
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String?): SubscriptionBook? =
            text?.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString<SubscriptionBook>(it) }.getOrNull() }

        fun toJson(book: SubscriptionBook): String = json.encodeToString(book)
    }
}

/** Setup's picks become the initial subscription set, so the tab has one source of truth. */
fun UserTaste.seedSubscriptions(): SubscriptionBook = SubscriptionBook(channels = channels.map { it.toSubscription() })

internal fun TasteChannel.toSubscription(): Subscription = Subscription(id = id, name = name, thumbnailUrl = thumbnailUrl)
