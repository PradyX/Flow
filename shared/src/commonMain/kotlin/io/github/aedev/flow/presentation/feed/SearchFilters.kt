package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.innertube.YouTubeSearchParams

/** What kind of result the search page is narrowed to; [ANY] leaves YouTube's own mix. */
enum class SearchType {
    ANY,
    VIDEO,
    CHANNEL,
    PLAYLIST,
    MOVIE,
    SHORTS,
}

/** YouTube moved these boundaries from four minutes to three when Shorts grew to three minutes. */
enum class SearchDuration {
    ANY,
    UNDER_3_MINUTES,
    THREE_TO_20_MINUTES,
    OVER_20_MINUTES,
}

enum class SearchUploadDate {
    ANY,
    LAST_HOUR,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
}

/** YouTube's own dialog offers exactly these two; recency is an [SearchUploadDate], not a sort. */
enum class SearchSort {
    RELEVANCE,
    VIEW_COUNT,
}

enum class SearchFeature {
    HD,
    FOUR_K,
    HDR,
    SUBTITLES,
    CREATIVE_COMMONS,
    THREE_SIXTY,
    VR180,
    THREE_D,
    LOCATION,
    PURCHASED,
    LIVE,
}

/**
 * Everything the search page can be narrowed by. Every choice encodes into the one `params` token
 * `/youtubei/v1/search` reads, so a filtered page is YouTube's own narrowing rather than a local
 * approximation of it.
 */
@kotlinx.serialization.Serializable
data class SearchFilter(
    val type: SearchType = SearchType.ANY,
    val duration: SearchDuration = SearchDuration.ANY,
    val uploadDate: SearchUploadDate = SearchUploadDate.ANY,
    val sort: SearchSort = SearchSort.RELEVANCE,
    val features: Set<SearchFeature> = emptySet(),
) {
    /** How many narrowing choices are active, for the badge on the filter control. */
    val activeCount: Int
        get() =
            listOf(
                type != SearchType.ANY,
                duration != SearchDuration.ANY,
                uploadDate != SearchUploadDate.ANY,
                sort != SearchSort.RELEVANCE,
            ).count { it } + features.size

    /** Channels and playlists carry no duration, upload date or feature filter, as the app's dialog knows. */
    val videoFiltersVisible: Boolean
        get() = type != SearchType.CHANNEL && type != SearchType.PLAYLIST

    /**
     * Null when nothing is narrowed: an absent token is YouTube's unfiltered relevance. A narrowing
     * the type cannot carry is dropped rather than sent, so switching to a channel or playlist
     * search never narrows it by a duration the dialog no longer shows.
     */
    fun params(): String? =
        YouTubeSearchParams.build(
            sortBy = sort.toParams(),
            contentType = type.toParams(),
            duration = duration.toParams().takeIf { videoFiltersVisible },
            uploadDate = uploadDate.toParams().takeIf { videoFiltersVisible },
            features = if (videoFiltersVisible) features.mapNotNull { it.toParams() }.toSet() else emptySet(),
        )
}

private fun SearchSort.toParams() =
    when (this) {
        SearchSort.RELEVANCE -> YouTubeSearchParams.SortBy.RELEVANCE
        SearchSort.VIEW_COUNT -> YouTubeSearchParams.SortBy.VIEW_COUNT
    }

private fun SearchType.toParams() =
    when (this) {
        SearchType.ANY -> null
        SearchType.VIDEO -> YouTubeSearchParams.ContentType.VIDEO
        SearchType.CHANNEL -> YouTubeSearchParams.ContentType.CHANNEL
        SearchType.PLAYLIST -> YouTubeSearchParams.ContentType.PLAYLIST
        SearchType.MOVIE -> YouTubeSearchParams.ContentType.MOVIE
        SearchType.SHORTS -> YouTubeSearchParams.ContentType.SHORTS
    }

private fun SearchDuration.toParams() =
    when (this) {
        SearchDuration.ANY -> null
        SearchDuration.UNDER_3_MINUTES -> YouTubeSearchParams.Duration.UNDER_3_MINUTES
        SearchDuration.THREE_TO_20_MINUTES -> YouTubeSearchParams.Duration.THREE_TO_20_MINUTES
        SearchDuration.OVER_20_MINUTES -> YouTubeSearchParams.Duration.OVER_20_MINUTES
    }

private fun SearchUploadDate.toParams() =
    when (this) {
        SearchUploadDate.ANY -> null
        SearchUploadDate.LAST_HOUR -> YouTubeSearchParams.UploadDate.LAST_HOUR
        SearchUploadDate.TODAY -> YouTubeSearchParams.UploadDate.TODAY
        SearchUploadDate.THIS_WEEK -> YouTubeSearchParams.UploadDate.THIS_WEEK
        SearchUploadDate.THIS_MONTH -> YouTubeSearchParams.UploadDate.THIS_MONTH
        SearchUploadDate.THIS_YEAR -> YouTubeSearchParams.UploadDate.THIS_YEAR
    }

private fun SearchFeature.toParams() =
    when (this) {
        SearchFeature.HD -> YouTubeSearchParams.Feature.HD
        SearchFeature.FOUR_K -> YouTubeSearchParams.Feature.FOUR_K
        SearchFeature.HDR -> YouTubeSearchParams.Feature.HDR
        SearchFeature.SUBTITLES -> YouTubeSearchParams.Feature.SUBTITLES
        SearchFeature.CREATIVE_COMMONS -> YouTubeSearchParams.Feature.CREATIVE_COMMONS
        SearchFeature.THREE_SIXTY -> YouTubeSearchParams.Feature.THREE_SIXTY
        SearchFeature.VR180 -> YouTubeSearchParams.Feature.VR180
        SearchFeature.THREE_D -> YouTubeSearchParams.Feature.THREE_D
        SearchFeature.LOCATION -> YouTubeSearchParams.Feature.LOCATION
        SearchFeature.PURCHASED -> YouTubeSearchParams.Feature.PURCHASED
        SearchFeature.LIVE -> YouTubeSearchParams.Feature.LIVE
    }
