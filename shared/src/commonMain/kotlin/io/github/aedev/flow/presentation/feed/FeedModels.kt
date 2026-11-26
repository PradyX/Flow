package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.data.model.Channel
import io.github.aedev.flow.data.model.Playlist
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.innertube.models.AlbumItem
import io.github.aedev.flow.innertube.models.ArtistItem
import io.github.aedev.flow.innertube.models.PlaylistItem
import io.github.aedev.flow.innertube.models.SongItem
import io.github.aedev.flow.innertube.models.YTItem
import io.github.aedev.flow.innertube.pages.renderer.FeedItem
import io.github.aedev.flow.innertube.pages.search.SearchChip
import io.github.aedev.flow.innertube.pages.search.SearchResultsPage
import io.github.aedev.flow.innertube.pages.search.SearchSection
import io.github.aedev.flow.utils.formatViewCount

/**
 * One row in a feed. [videoId] is null for an entry this UI cannot open yet; [durationSeconds] and
 * [isArtist] are what the cards need to draw the duration pill and the round artist portrait, and
 * [thumbnailUrl] is the artwork the wire response carried.
 */
@kotlinx.serialization.Serializable
data class FeedEntry(
    val id: String,
    val title: String,
    val subtitle: String?,
    val videoId: String?,
    val durationSeconds: Int? = null,
    val isArtist: Boolean = false,
    val thumbnailUrl: String? = null,
    /** Set when the entry is a playlist (or album) card rather than a single video. */
    val playlistId: String? = null,
    /** The thumbnail badge, for a card whose pill is not a duration (a playlist's song count). */
    val overlayText: String? = null,
    /** YouTube marks this track explicit; the row draws its marker beside the byline. */
    val explicit: Boolean = false,
    /** Set when the entry is a channel result, which the search page draws as a row. */
    val channelId: String? = null,
    val channelSubscriberCount: Long? = null,
)

/** What a screen shows while its request is in flight, done or failed. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>

    data class Ready<T>(
        val value: T,
    ) : LoadState<T>

    data class Failed(
        val message: String,
    ) : LoadState<Nothing>
}

internal fun YTItem.toFeedEntry(): FeedEntry =
    when (this) {
        is SongItem -> {
            FeedEntry(
                id = id,
                title = title,
                subtitle = artists.joinToString(", ") { it.name },
                videoId = id,
                durationSeconds = duration,
                thumbnailUrl = thumbnail,
                explicit = explicit,
            )
        }

        is AlbumItem -> {
            FeedEntry(
                id = id,
                title = title,
                subtitle = artists?.joinToString(", ") { it.name },
                videoId = null,
                thumbnailUrl = thumbnail,
                playlistId = playlistId,
            )
        }

        is PlaylistItem -> {
            FeedEntry(
                id = id,
                title = title,
                subtitle = author?.name ?: songCountText,
                videoId = playEndpoint?.videoId,
                thumbnailUrl = thumbnail,
                playlistId = id,
            )
        }

        is ArtistItem -> {
            FeedEntry(
                id = id,
                title = title,
                subtitle = subscriberCountText,
                videoId = null,
                isArtist = true,
                thumbnailUrl = thumbnail,
            )
        }
    }

private fun searchSubtitle(video: Video): String? =
    listOfNotNull(
        video.channelName.takeIf { it.isNotBlank() },
        video.viewCount.takeIf { it > 0 }?.let { "${formatViewCount(it)} views" },
        video.uploadDate.takeIf { it.isNotBlank() },
    ).joinToString(" · ").takeIf { it.isNotBlank() }

/**
 * One page of search: the ranked rows, YouTube's own estimate when it sends one, and the filter
 * chips above the grid. A chip re-runs the search through its continuation token, so the chips are
 * the page's own — not a fixed list this UI invents.
 */
data class SearchFeed(
    val entries: List<FeedEntry>,
    val estimatedResults: Long?,
    val chips: List<SearchChip> = emptyList(),
    val continuation: String? = null,
) {
    /** The next page's results under the ones already shown, with its own token to continue from. */
    fun more(next: SearchFeed): SearchFeed =
        copy(
            entries = (entries + next.entries).distinctBy { it.id },
            continuation = next.continuation,
        )
}

fun SearchResultsPage.toSearchFeed(): SearchFeed =
    SearchFeed(
        entries = toSearchEntries(),
        estimatedResults = estimatedResults,
        chips = header.chips,
        continuation = continuation,
    )

fun SearchResultsPage.toSearchEntries(): List<FeedEntry> = sections.mapNotNull { (it as? SearchSection.Result)?.item?.toSearchEntry() }

/**
 * One ranked result in the grid's card shape. A channel result becomes a row rather than a video
 * card; a playlist keeps its own id so the card opens its page.
 */
private fun FeedItem.toSearchEntry(): FeedEntry? =
    when (this) {
        is FeedItem.VideoItem -> {
            videoCardEntry(video)
        }

        is FeedItem.ShortItem -> {
            videoCardEntry(video)
        }

        is FeedItem.PlaylistItem -> {
            FeedEntry(
                id = playlist.id,
                title = playlist.name,
                subtitle = null,
                videoId = null,
                thumbnailUrl = playlist.thumbnailUrl.takeIf { it.isNotBlank() },
                playlistId = playlist.id,
                overlayText = playlistCountText(playlist.videoCount),
            )
        }

        is FeedItem.RelatedChannelItem -> {
            FeedEntry(
                id = channel.id,
                title = channel.name,
                subtitle = channel.handle.takeIf { it.isNotBlank() },
                videoId = null,
                thumbnailUrl = channel.thumbnailUrl.takeIf { it.isNotBlank() },
                channelId = channel.id,
                channelSubscriberCount = channel.subscriberCount.takeIf { it > 0 },
            )
        }

        else -> {
            null
        }
    }

/** YouTube's own count, drawn on the card the way it prints a duration. */
private fun playlistCountText(count: Int): String? = count.takeIf { it > 0 }?.let { if (it == 1) "1 video" else "$it videos" }

/**
 * A music shelf card: its square artwork, its title and byline, and the page or song a tap opens.
 * An album is reached through the playlist its browse id carries, which is how YouTube Music
 * publishes one; a playlist keeps its own id, not the first song's watch endpoint.
 */
internal fun YTItem.toMusicEntry(): FeedEntry =
    when (this) {
        is SongItem -> {
            FeedEntry(
                id = id,
                title = title,
                subtitle = artists.joinToString(", ") { it.name },
                videoId = id,
                durationSeconds = duration,
                thumbnailUrl = thumbnail,
                explicit = explicit,
            )
        }

        is AlbumItem -> {
            FeedEntry(
                id = browseId,
                title = title,
                subtitle = artists?.joinToString(", ") { it.name },
                videoId = null,
                thumbnailUrl = thumbnail,
                // The album page is browsed by the album's own id; the OLAK5uy_ playlist id does
                // not resolve as a browse id and the playlist page fails on it.
                playlistId = browseId,
            )
        }

        is PlaylistItem -> {
            FeedEntry(
                id = id,
                title = title,
                subtitle = author?.name ?: songCountText,
                videoId = null,
                thumbnailUrl = thumbnail,
                playlistId = id,
            )
        }

        is ArtistItem -> {
            FeedEntry(
                id = id,
                title = title,
                subtitle = subscriberCountText,
                videoId = null,
                isArtist = true,
                thumbnailUrl = thumbnail,
            )
        }
    }

/**
 * A channel tab's grid in the same card shape the feeds use. Home's related-channel shelf is a
 * row of channels, so a channel keeps its id here instead of being dropped; a post is not a card
 * this shape opens, and travels in the shelf's own posts.
 */
fun FeedItem.toFeedEntry(): FeedEntry? =
    when (this) {
        is FeedItem.VideoItem -> videoCardEntry(video)
        is FeedItem.ShortItem -> videoCardEntry(video)
        is FeedItem.PlaylistItem -> playlistCardEntry(playlist)
        is FeedItem.RelatedChannelItem -> channelCardEntry(channel)
        is FeedItem.PostItem -> null
    }

/** A channel card: the avatar, the name, and the handle or audience under it. */
fun channelCardEntry(channel: Channel): FeedEntry =
    FeedEntry(
        id = channel.id,
        title = channel.name,
        subtitle = channel.handle.takeIf { it.isNotBlank() },
        videoId = null,
        thumbnailUrl = channel.thumbnailUrl.takeIf { it.isNotBlank() },
        channelId = channel.id,
        channelSubscriberCount = channel.subscriberCount.takeIf { it > 0 },
    )

/** A playlist as a card: its own id opens the playlist page, and the pill is its item count. */
fun playlistCardEntry(playlist: Playlist): FeedEntry =
    FeedEntry(
        id = playlist.id,
        title = playlist.name,
        subtitle = null,
        videoId = null,
        thumbnailUrl = playlist.thumbnailUrl.takeIf { it.isNotBlank() },
        playlistId = playlist.id,
        overlayText = playlist.videoCount.takeIf { it > 0 }?.let { formatViewCount(it.toLong()) },
    )

/** A video as the card shape the grids draw. */
fun videoCardEntry(video: Video): FeedEntry =
    FeedEntry(
        id = video.id,
        title = video.title,
        subtitle = searchSubtitle(video),
        videoId = video.id,
        durationSeconds = video.duration.takeIf { it > 0 },
        thumbnailUrl = video.thumbnailUrl.takeIf { it.isNotBlank() },
    )
