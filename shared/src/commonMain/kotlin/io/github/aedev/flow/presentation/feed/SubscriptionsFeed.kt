package io.github.aedev.flow.presentation.feed

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.aedev.flow.data.notifications.ChannelUploads
import io.github.aedev.flow.data.notifications.InboxUpload
import io.github.aedev.flow.data.subscriptions.Subscription
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** A channel the viewer is subscribed to, as the Subscriptions header draws it. */
data class SubscribedChannel(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
)

/** One upload in the Subscriptions grid, with the channel the card shows above its meta line. */
data class SubscriptionVideo(
    val entry: FeedEntry,
    val channelId: String,
    val channelName: String,
    val channelAvatarUrl: String? = null,
)

/** The subscriptions feed: the channel strip's channels, then every channel's recent uploads. */
data class SubscriptionsFeed(
    val channels: List<SubscribedChannel>,
    val videos: List<SubscriptionVideo>,
    /** Channels whose uploads could not be fetched; they are still listed in the strip. */
    val failedChannelCount: Int = 0,
    /**
     * Each channel's own uploads, newest first, kept per channel instead of merged: the inbox's
     * check asks one channel at a time for the entries published since it was last read.
     */
    val uploads: List<ChannelUploads> = emptyList(),
)

/** The Subscriptions tab's state, owned by the shell so switching tabs does not refetch. */
@Stable
class SubscriptionsFeedState {
    var feed: LoadState<SubscriptionsFeed> by mutableStateOf(LoadState.Loading)
}

private const val VIDEOS_PER_CHANNEL = 8

/**
 * What the Subscriptions tab shows: each subscribed channel's Videos tab, one request pair per
 * channel (the landing names the tab token), merged newest first. The app merges RSS uploads the
 * same way; this port has no RSS client, so the channel's own uploads are what it reads instead.
 *
 * A channel that fails is skipped rather than failing the page — the strip still lists it, and the
 * page only reports failure when every channel failed.
 */
suspend fun loadSubscriptions(channels: List<Subscription>): Result<SubscriptionsFeed> =
    runCatching {
        val subscribed = channels.filter { it.id.isNotBlank() }.distinctBy { it.id }
        val loaded =
            coroutineScope {
                subscribed
                    .map { channel ->
                        async { channel to loadChannel(channel.id).getOrNull() }
                    }.map { it.await() }
            }
        val strip =
            loaded.map { (channel, feed) ->
                SubscribedChannel(
                    id = channel.id,
                    name = feed?.name?.takeIf { it.isNotBlank() } ?: channel.name,
                    avatarUrl = feed?.avatarUrl ?: channel.thumbnailUrl,
                )
            }
        val ranked =
            loaded.flatMapIndexed { channelIndex, (channel, feed) ->
                val name = strip[channelIndex].name
                feed
                    ?.videos
                    .orEmpty()
                    .take(VIDEOS_PER_CHANNEL)
                    .mapIndexedNotNull { position, entry ->
                        val videoId = entry.videoId ?: return@mapIndexedNotNull null
                        RankedUpload(
                            channelIndex = channelIndex,
                            position = position,
                            ageSeconds = publishedAgeSeconds(entry.subtitle),
                            video =
                                SubscriptionVideo(
                                    entry = entry.copy(subtitle = uploadMeta(entry, name)),
                                    channelId = channel.id,
                                    channelName = name,
                                    channelAvatarUrl = strip[channelIndex].avatarUrl,
                                ),
                        )
                    }
            }
        val videos =
            ranked
                .sortedWith(
                    // Smallest age is the newest upload; a line this code cannot read sorts last.
                    compareBy<RankedUpload> { it.ageSeconds ?: Long.MAX_VALUE }
                        .thenBy { it.position }
                        .thenBy { it.channelIndex },
                ).distinctBy { it.video.entry.id }
                .map { it.video }
        val uploads =
            loaded.mapIndexed { index, (channel, feed) ->
                ChannelUploads(
                    channelId = channel.id,
                    channelName = strip[index].name,
                    uploads =
                        feed
                            ?.videos
                            .orEmpty()
                            .take(VIDEOS_PER_CHANNEL)
                            .mapNotNull { entry ->
                                val videoId = entry.videoId ?: return@mapNotNull null
                                InboxUpload(
                                    videoId = videoId,
                                    title = entry.title,
                                    thumbnailUrl = entry.thumbnailUrl,
                                )
                            },
                )
            }
        SubscriptionsFeed(
            channels = strip,
            videos = videos,
            failedChannelCount = loaded.count { (_, feed) -> feed == null },
            uploads = uploads,
        )
    }

private class RankedUpload(
    val channelIndex: Int,
    val position: Int,
    val ageSeconds: Long?,
    val video: SubscriptionVideo,
)

/**
 * A card's meta line without the channel's name: the card draws the channel above it already, the
 * way Home's cards do. The channel name is the subtitle's first part in every feed entry.
 */
private fun uploadMeta(
    entry: FeedEntry,
    channelName: String,
): String? =
    entry.subtitle
        ?.split(" · ")
        ?.filterNot { it == channelName }
        ?.joinToString(" · ")
        ?.takeIf { it.isNotBlank() }

private val RelativeAge = Regex("(\\d+)\\s*(second|minute|hour|day|week|month|year)s?\\s+ago", RegexOption.IGNORE_CASE)

/**
 * How old an upload is, in seconds, from YouTube's relative line ("2 hours ago"). Used only to rank
 * the merged grid, so a line in another locale simply sorts after the ones it can read.
 */
internal fun publishedAgeSeconds(text: String?): Long? {
    val match = RelativeAge.find(text ?: return null) ?: return null
    val value = match.groupValues[1].toLongOrNull() ?: return null
    val unitSeconds =
        when (match.groupValues[2].lowercase()) {
            "second" -> 1L
            "minute" -> 60L
            "hour" -> 60L * 60L
            "day" -> 24L * 60L * 60L
            "week" -> 7L * 24L * 60L * 60L
            "month" -> 30L * 24L * 60L * 60L
            "year" -> 365L * 24L * 60L * 60L
            else -> return null
        }
    return value * unitSeconds
}
