package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.data.model.Playlist
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.pages.channel.ChannelTabContent
import io.github.aedev.flow.innertube.pages.channel.ChannelTabKind
import io.github.aedev.flow.innertube.pages.explore.ExploreDestination
import io.github.aedev.flow.innertube.pages.explore.ExploreSectionKind
import io.github.aedev.flow.innertube.pages.renderer.FeedItem
import io.github.aedev.flow.innertube.pages.renderer.FeedShelf
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.category_fashion
import io.github.aedev.flow.resources.category_gaming
import io.github.aedev.flow.resources.category_learning
import io.github.aedev.flow.resources.category_live
import io.github.aedev.flow.resources.category_music
import io.github.aedev.flow.resources.category_news
import io.github.aedev.flow.resources.category_sports
import org.jetbrains.compose.resources.StringResource

/** One tab of the Explore screen, with the destination it browses and the label it carries. */
data class ExploreCategory(
    val destination: ExploreDestination,
    val label: StringResource,
)

/**
 * The tabs Explore shows, in the order the app ships them.
 *
 * Trending, Podcasts, Movies and Shopping are absent on purpose: each answers 400, 429 or an empty
 * page for an anonymous client, which is why the app's own screen lists only these.
 */
val EXPLORE_CATEGORIES: List<ExploreCategory> =
    listOf(
        ExploreCategory(ExploreDestination.LIVE, Res.string.category_live),
        ExploreCategory(ExploreDestination.GAMING, Res.string.category_gaming),
        ExploreCategory(ExploreDestination.MUSIC, Res.string.category_music),
        ExploreCategory(ExploreDestination.NEWS, Res.string.category_news),
        ExploreCategory(ExploreDestination.SPORTS, Res.string.category_sports),
        ExploreCategory(ExploreDestination.LEARNING, Res.string.category_learning),
        ExploreCategory(ExploreDestination.FASHION, Res.string.category_fashion),
    )

/** One row of an Explore page: a shelf's heading, its cards, and the token its See all opens. */
data class ExploreSection(
    val id: String,
    val title: String? = null,
    val subtitle: String? = null,
    val entries: List<FeedEntry> = emptyList(),
    val moreParams: String? = null,
)

/** An Explore tab as it is drawn: its shelves, or the single list a grid or chart destination has. */
data class ExplorePage(
    val title: String? = null,
    val sections: List<ExploreSection> = emptyList(),
    /** The next page behind a grid destination, which pages in place rather than by shelf. */
    val continuation: String? = null,
)

/** One page of a shelf's See all grid, and the token for the one after it. */
data class ExploreGridPage(
    val entries: List<FeedEntry> = emptyList(),
    val continuation: String? = null,
)

/** A shelf as the feed draws it; a shelf whose items this UI cannot open is dropped. */
internal fun FeedShelf.toExploreSection(): ExploreSection =
    ExploreSection(
        id = id,
        title = title?.takeIf { it.isNotBlank() },
        subtitle = subtitle?.takeIf { it.isNotBlank() },
        entries = items.mapNotNull { item -> item.toExploreEntry() }.distinctBy { it.id },
        moreParams = moreParams,
    )

/** The page's shelves in order, without the ones that carry nothing playable. */
internal fun List<FeedShelf>.toExplorePage(title: String? = null): ExplorePage =
    ExplorePage(
        title = title?.takeIf { it.isNotBlank() },
        sections = map { shelf -> shelf.toExploreSection() }.filter { it.entries.isNotEmpty() },
    )

/** A shelf item as a card: videos and reels play, playlists open their page, the rest is not a card. */
internal fun FeedItem.toExploreEntry(): FeedEntry? =
    when (this) {
        is FeedItem.VideoItem -> videoCardEntry(video)
        is FeedItem.ShortItem -> videoCardEntry(video)
        is FeedItem.PlaylistItem -> playlist.toExploreEntry()
        else -> null
    }

private fun Playlist.toExploreEntry(): FeedEntry =
    FeedEntry(
        id = id,
        title = name,
        subtitle = null,
        videoId = null,
        thumbnailUrl = thumbnailUrl.takeIf { it.isNotBlank() },
        playlistId = id,
        overlayText = videoCount.takeIf { it > 0 }?.toString(),
    )

/**
 * One Explore tab. A shelves destination is its own landing page; the Gaming tab is a paged grid
 * browsed through the token its destination carries; Music is YouTube Music's trending chart.
 */
suspend fun loadExplore(category: ExploreDestination): Result<ExplorePage> =
    when (category.kind) {
        ExploreSectionKind.CHART -> {
            YouTube.musicVideoChart().mapCatching { chart ->
                val entries = chart.entries.map(::videoCardEntry).distinctBy { it.id }
                ExplorePage(
                    sections =
                        listOf(
                            ExploreSection(
                                id = "chart-${category.name}",
                                title = chart.title.takeIf { it.isNotBlank() },
                                entries = entries,
                            ),
                        ),
                )
            }
        }

        ExploreSectionKind.GRID -> {
            loadExploreGrid(category.browseId, category.params.orEmpty()).mapCatching { grid ->
                ExplorePage(
                    sections = listOf(ExploreSection(id = "grid-${category.name}", entries = grid.entries)),
                    continuation = grid.continuation,
                )
            }
        }

        ExploreSectionKind.SHELVES -> {
            YouTube.exploreDestination(category.browseId, category.params).mapCatching { page ->
                page.shelves.toExplorePage(page.title)
            }
        }
    }

/** A shelf's See all, and the next page of a grid destination's own list. */
suspend fun loadExploreGrid(
    browseId: String,
    params: String,
    continuation: String? = null,
): Result<ExploreGridPage> =
    runCatching {
        val page: ChannelTabContent =
            if (continuation == null) {
                YouTube.channelTab(browseId, params, kind = ChannelTabKind.Videos).getOrThrow()
            } else {
                YouTube
                    .channelTabContinuation(browseId, continuation, ChannelTabKind.Videos)
                    .getOrThrow()
            }
        ExploreGridPage(
            entries = page.items.mapNotNull { item -> item.toExploreEntry() }.distinctBy { it.id },
            continuation = page.continuation,
        )
    }
