package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.core.common.currentTimeMillis
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.data.recommendation.HomeMixState
import io.github.aedev.flow.data.recommendation.NeuroTopicCatalog
import io.github.aedev.flow.data.recommendation.rankHomeVideos
import io.github.aedev.flow.data.taste.UserTaste
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.YouTubeSearchParams
import io.github.aedev.flow.innertube.pages.arrayOrNull
import io.github.aedev.flow.innertube.pages.objectOrNull
import io.github.aedev.flow.innertube.pages.renderer.FeedItem
import io.github.aedev.flow.innertube.pages.renderer.FeedItemOwner
import io.github.aedev.flow.innertube.pages.renderer.toFeedItems
import io.github.aedev.flow.innertube.pages.search.resultVideos
import io.github.aedev.flow.innertube.pages.youtubeText
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * The chips above Home, in the app's order: All, then the viewer's own picks from setup, then the
 * standing filters. [key] is what the selected chip is remembered by.
 */
sealed interface HomeChip {
    val key: String

    data object All : HomeChip {
        override val key = "all"
    }

    /** A topic setup recorded: the reference's taste-derived chip, carrying the viewer's own label. */
    data class Interest(
        val topic: String,
    ) : HomeChip {
        override val key: String = "interest:$topic"
    }

    data object NewToYou : HomeChip {
        override val key = "new"
    }

    data object RecentlyUploaded : HomeChip {
        override val key = "recent"
    }

    data object Live : HomeChip {
        override val key = "live"
    }

    companion object {
        /** How many taste picks sit beside All before the standing filters. */
        const val INTEREST_CHIPS = 2

        fun forTaste(
            taste: UserTaste,
            maxInterests: Int = INTEREST_CHIPS,
        ): List<HomeChip> =
            listOf(All) +
                taste.topics
                    .distinct()
                    .filter { it.isNotBlank() }
                    .take(maxInterests)
                    .map(::Interest) +
                listOf(NewToYou, RecentlyUploaded, Live)
    }
}

/** What a chip keeps, matching the app's chip limits. */
internal object HomeChipParams {
    const val MAX_VIDEOS = 40
    const val QUERIES_PER_INTEREST = 3
    const val RECENT_WINDOW_MS = 7L * 24L * 60L * 60L * 1000L
}

private const val HOME_BROWSE_ID = "FEwhat_to_watch"

/**
 * Home's All chip: the main-site home browse, in the service's own recommendation order. Only
 * long-form videos are cards here; a Short belongs to the Shorts shelf and an upcoming row is not
 * playable yet.
 */
suspend fun loadVideoHome(taste: UserTaste = UserTaste()): Result<List<Video>> =
    YouTube
        .webBrowse(browseId = HOME_BROWSE_ID)
        .mapCatching { it.toVideoHomeItems() }
        .mapCatching { videos -> if (videos.isEmpty()) startingFeed(taste) else videos }

/**
 * What All shows when YouTube has no recommendations to give. A signed-out browse answers with the
 * "try searching to get started" notice and no cards, so the starting feed is built from the same
 * searches the other chips use: every interest in turn, one video per channel, the queries mixed
 * so the grid opens on several topics instead of one.
 */
private suspend fun startingFeed(taste: UserTaste): List<Video> {
    val queries = taste.interestQueries() + taste.explorationQueries()
    val results =
        coroutineScope {
            queries
                .map { query ->
                    async {
                        runCatching { YouTube.videoSearch(query = query).getOrThrow().resultVideos() }.getOrDefault(emptyList())
                    }
                }.map { it.await() }
        }
    return interleave(results).filterNot { it.isShort }.distinctBy { it.id }.take(HomeChipParams.MAX_VIDEOS)
}

/** Round-robin across the sources: one from each in turn, so the top of the grid is mixed. */
internal fun interleave(sources: List<List<Video>>): List<Video> {
    val out = mutableListOf<Video>()
    val longest = sources.maxOfOrNull { it.size } ?: 0
    for (index in 0 until longest) {
        sources.forEach { source -> source.getOrNull(index)?.let { out += it } }
    }
    return out
}

/**
 * The chip's feed. Each chip is a real request built the way the app builds it: All browses Home,
 * New to you and Recently uploaded search the viewer's interests, Live asks YouTube for live
 * broadcasts only.
 */
suspend fun loadHomeChip(
    chip: HomeChip,
    mixState: HomeMixState = HomeMixState(),
    taste: UserTaste = UserTaste(),
): Result<List<Video>> =
    when (chip) {
        HomeChip.All -> {
            loadVideoHome(taste).map { videos -> videos.rankForHome(mixState) }
        }

        HomeChip.NewToYou -> {
            searchVideos(taste.explorationQueries()).map { videos ->
                fromUnknownChannels(videos, mixState.userSubs).rankForHome(mixState)
            }
        }

        HomeChip.RecentlyUploaded -> {
            searchVideos(taste.interestQueries(), thisWeekParams()).map { videos ->
                val now = currentTimeMillis()
                val window = uploadedThisWeek(videos, now)
                val ranked = window.rankForHome(mixState)
                if (ranked.isEmpty()) window else freshnessOrder(ranked, now)
            }
        }

        is HomeChip.Interest -> {
            searchVideos(listOf(chip.topic, "${chip.topic} new")).map { videos ->
                videos.filterNot { it.isShort }.rankForHome(mixState)
            }
        }

        HomeChip.Live -> {
            searchVideos(taste.liveQueries(), liveParams()).map { videos ->
                videos.filter { it.isLive && !it.isUpcoming }.rankForHome(mixState)
            }
        }
    }.map { videos -> videos.distinctBy { it.id }.take(HomeChipParams.MAX_VIDEOS) }
        .map { videos -> videos.withChannelAvatars() }

/**
 * The chip's videos through the mixer. Feedback can suppress every row a chip has: rather than
 * leave Home blank, that chip falls back to the same ranking with the suppressions dropped, so the
 * grid always has something deterministic to show and the viewer can still mark more.
 */
internal fun List<Video>.rankForHome(mixState: HomeMixState): List<Video> {
    val ranked = rankHomeVideos(this, mixState)
    if (ranked.isNotEmpty() || isEmpty()) return ranked
    return rankHomeVideos(this, mixState.copy(suppressedVideoIds = emptySet(), suppressedChannels = emptySet()))
}

/** Reads the home browse document's rich grid into the videos it lists, keeping its order. */
internal fun JsonElement.toVideoHomeItems(): List<Video> {
    val contents =
        homeTabContent()
            ?.get("richGridRenderer")
            ?.objectOrNull()
            ?.get("contents")
            ?: homeTabContent()
                ?.get("sectionListRenderer")
                ?.objectOrNull()
                ?.get("contents")
                ?.arrayOrNull()
                ?.firstOrNull()
                ?.objectOrNull()
                ?.get("itemSectionRenderer")
                ?.objectOrNull()
                ?.get("contents")
    return contents
        .toFeedItems(FeedItemOwner())
        .mapNotNull { item ->
            when (item) {
                is FeedItem.VideoItem -> item.video
                is FeedItem.ShortItem -> null
                else -> null
            }
        }.filter { it.id.isNotBlank() && it.title.isNotBlank() && !it.isUpcoming }
}

/** The browse document's own filter labels, as the All feed offers them above its grid. */
internal fun JsonElement.toVideoHomeChipLabels(): List<String> =
    homeTabContent()
        ?.get("richGridRenderer")
        ?.objectOrNull()
        ?.get("header")
        ?.objectOrNull()
        ?.get("feedFilterChipBarRenderer")
        ?.objectOrNull()
        ?.get("contents")
        ?.arrayOrNull()
        .orEmpty()
        .mapNotNull { chip ->
            chip
                .objectOrNull()
                ?.get("chipCloudChipRenderer")
                ?.objectOrNull()
                ?.get("text")
                ?.youtubeText()
        }

/** The home tab's content node, whichever of the two column shapes the response uses. */
private fun JsonElement.homeTabContent(): JsonObject? {
    val contents = objectOrNull()?.get("contents").objectOrNull() ?: return null
    val results =
        contents["twoColumnBrowseResultsRenderer"].objectOrNull()
            ?: contents["singleColumnBrowseResultsRenderer"].objectOrNull()
    return results
        ?.get("tabs")
        ?.arrayOrNull()
        ?.firstOrNull()
        ?.objectOrNull()
        ?.get("tabRenderer")
        ?.objectOrNull()
        ?.get("content")
        ?.objectOrNull()
}

/** One search per query, in parallel, flattened in query order. */
private suspend fun searchVideos(
    queries: List<String>,
    params: String? = null,
): Result<List<Video>> =
    runCatching {
        coroutineScope {
            queries
                .map { query ->
                    async {
                        runCatching { YouTube.videoSearch(query = query, params = params).getOrThrow().resultVideos() }
                            .getOrDefault(emptyList())
                    }
                }.map { it.await() }
                .flatten()
        }
    }

/**
 * The searches a chip runs: the viewer's own picks from setup, and the ported topic catalog's
 * leading categories when setup was skipped.
 */
internal fun UserTaste.interestQueries(): List<String> = chipQueries().interests.ifEmpty { catalogInterestQueries() }

internal fun UserTaste.explorationQueries(): List<String> = chipQueries().exploration.ifEmpty { catalogExplorationQueries() }

internal fun UserTaste.liveQueries(): List<String> = chipQueries().live.ifEmpty { listOf("live") }

private fun catalogInterestQueries(): List<String> =
    NeuroTopicCatalog.TOPIC_CATEGORIES
        .take(2)
        .flatMap { category -> category.topics.take(2) }
        .distinct()
        .take(HomeChipParams.QUERIES_PER_INTEREST)

/** Discovery searches for channels the viewer has not met yet. */
private fun catalogExplorationQueries(): List<String> =
    NeuroTopicCatalog.TOPIC_CATEGORIES
        .take(2)
        .map { category -> category.topics.first() }
        .distinct()
        .map { topic -> "$topic new" }

/** Videos only, uploaded this week: the app's Recently uploaded filter. */
internal fun thisWeekParams(): String? =
    YouTubeSearchParams.build(
        contentType = YouTubeSearchParams.ContentType.VIDEO,
        uploadDate = YouTubeSearchParams.UploadDate.THIS_WEEK,
    )

/** Live broadcasts only: the app's Live filter. */
internal fun liveParams(): String? =
    YouTubeSearchParams.build(
        contentType = YouTubeSearchParams.ContentType.VIDEO,
        features = setOf(YouTubeSearchParams.Feature.LIVE),
    )
