package io.github.aedev.flow.innertube.pages.channel

import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.models.response.ChannelSearchResponse
import io.github.aedev.flow.innertube.pages.parseYouTubeViewCount
import io.github.aedev.flow.utils.avatarImageIdentityKey

internal const val CHANNEL_SEARCH_FALLBACK_LANGUAGE = "en"

/**
 * YouTube answers some channel searches with nothing in one language and finds them in English
 * (#2057: "xlov" on a channel has no videos with `hl=es`, 28 with `hl=en`, youtube.com included).
 * So an empty answer in any other language is asked once more in English.
 */
internal fun shouldSearchChannelInEnglish(
    videoCount: Int,
    hl: String,
): Boolean = videoCount == 0 && hl.substringBefore('-').lowercase() != CHANNEL_SEARCH_FALLBACK_LANGUAGE

/** One ranked row of a channel search: the playlist block YouTube puts above the videos, or a video. */
sealed interface ChannelSearchRow {
    data class VideoRow(
        val video: io.github.aedev.flow.data.model.Video,
    ) : ChannelSearchRow

    data class PlaylistRow(
        val playlist: io.github.aedev.flow.data.model.Playlist,
    ) : ChannelSearchRow
}

/**
 * A channel search page. Rows keep the server's order, so the playlist row stays above the videos
 * it was ranked above; the page appends through [response]'s continuation envelope, whichever of the
 * two shapes it arrives in (`onResponseReceivedActions` or the newer `onResponseReceivedEndpoints`).
 */
internal fun parseChannelSearchResponse(
    response: io.github.aedev.flow.innertube.models.response.ChannelSearchResponse,
    channelId: String,
    channelName: String,
    channelThumbnailUrl: String,
): YouTube.ChannelVideoSearchResult {
    val tabs =
        response.contents
            ?.twoColumnBrowseResultsRenderer
            ?.tabs
            .orEmpty()
    val tabContent =
        tabs
            .firstOrNull {
                it.tabRenderer?.selected == true && it.tabRenderer.endpoint
                    ?.commandMetadata
                    ?.webCommandMetadata
                    ?.url
                    ?.contains("/search") == true
            }?.tabRenderer
            ?.content
            ?: tabs.firstOrNull { it.expandableTabRenderer?.content != null }?.expandableTabRenderer?.content
            ?: tabs.firstOrNull { it.tabRenderer?.selected == true }?.tabRenderer?.content

    val rows = mutableListOf<ChannelSearchRow>()
    var continuation: String? = null

    fun addItem(item: io.github.aedev.flow.innertube.models.response.ChannelSearchResponse.Item?) {
        item ?: return
        item.videoRenderer
            ?.let { parseVideoRenderer(it, channelId, channelName, channelThumbnailUrl) }
            ?.let { rows += ChannelSearchRow.VideoRow(it) }
        item.playlistRenderer
            ?.toPlaylistRow()
            ?.let { rows += it }
    }

    fun keepToken(
        item: io.github.aedev.flow.innertube.models.response.ChannelSearchResponse.SectionContent
            .ContinuationItemRenderer?,
    ) {
        item
            ?.continuationEndpoint
            ?.continuationCommand
            ?.token
            ?.takeIf { it.isNotBlank() }
            ?.let { continuation = it }
    }

    fun addSection(section: io.github.aedev.flow.innertube.models.response.ChannelSearchResponse.SectionContent?) {
        section?.itemSectionRenderer?.contents?.forEach(::addItem)
        keepToken(section?.continuationItemRenderer)
    }

    tabContent?.richGridRenderer?.contents?.forEach { richItem ->
        richItem.itemSectionRenderer?.contents?.forEach(::addItem)
        richItem.richItemRenderer
            ?.content
            ?.videoRenderer
            ?.let { parseVideoRenderer(it, channelId, channelName, channelThumbnailUrl) }
            ?.let { rows += ChannelSearchRow.VideoRow(it) }
        keepToken(richItem.continuationItemRenderer)
    }

    tabContent?.sectionListRenderer?.contents?.forEach(::addSection)

    // A later page arrives as appended continuation items rather than as a tab payload, and its
    // rows sit one itemSectionRenderer deeper than the first page's.
    (
        response.onResponseReceivedActions.orEmpty() +
            response.onResponseReceivedEndpoints.orEmpty()
    ).mapNotNull { it.appendContinuationItemsAction }
        .flatMap { it.continuationItems.orEmpty() }
        .forEach { item ->
            item.itemSectionRenderer?.contents?.forEach(::addItem)
            keepToken(item.continuationItemRenderer)
        }

    return YouTube.ChannelVideoSearchResult(continuation = continuation, rows = rows)
}

private fun ChannelSearchResponse.PlaylistRenderer.toPlaylistRow(): ChannelSearchRow.PlaylistRow? {
    val id = playlistId?.takeIf { it.isNotBlank() } ?: return null
    val name =
        title
            ?.simpleText
            ?.takeIf { it.isNotBlank() }
            ?: title?.runs?.joinToString("") { it.text.orEmpty() }?.takeIf { it.isNotBlank() }
            ?: return null
    val cover =
        thumbnails
            .orEmpty()
            .flatMap { group -> group.thumbnails.orEmpty() }
            .maxByOrNull { maxOf(it.width ?: 0, it.height ?: 0) }
            ?.url
    return ChannelSearchRow.PlaylistRow(
        io.github.aedev.flow.data.model.Playlist(
            id = id,
            name = name,
            thumbnailUrl = cover.orEmpty(),
            videoCount = videoCount ?: 0,
        ),
    )
}

private fun parseVideoRenderer(
    r: io.github.aedev.flow.innertube.models.response.ChannelSearchResponse.VideoRenderer,
    channelId: String,
    channelName: String,
    channelThumbnailUrl: String,
): io.github.aedev.flow.data.model.Video? {
    val videoId = r.videoId ?: return null
    val title =
        r.title
            ?.runs
            ?.joinToString("") { it.text ?: "" }
            ?.takeIf { it.isNotBlank() } ?: return null
    val thumbnail =
        r.thumbnail
            ?.thumbnails
            ?.maxByOrNull { it.width ?: 0 }
            ?.url
            ?: "https://i.ytimg.com/vi/$videoId/hq720.jpg"
    val duration = parseLengthText(r.lengthText?.simpleText)
    val viewCount = parseYouTubeViewCount(r.viewCountText?.simpleText)
    val avatarUrls = r.channelAvatarUrls(channelThumbnailUrl)
    return io.github.aedev.flow.data.model.Video(
        id = videoId,
        title = title,
        channelName = channelName,
        channelId = channelId,
        thumbnailUrl = thumbnail,
        duration = duration,
        viewCount = viewCount,
        uploadDate = r.publishedTimeText?.simpleText ?: "",
        channelThumbnailUrl = avatarUrls.firstOrNull().orEmpty(),
        channelThumbnailUrls = avatarUrls,
    )
}

private fun io.github.aedev.flow.innertube.models.response.ChannelSearchResponse.VideoRenderer.channelAvatarUrls(
    fallback: String,
): List<String> {
    val supported = channelThumbnailSupportedRenderers
    val stackAvatars =
        listOfNotNull(
            avatarStackViewModel,
            supported?.avatarStackViewModel,
            supported?.channelThumbnailWithLinkRenderer?.avatarStack?.avatarStackViewModel,
        ).flatMap { stack ->
            stack.avatars.orEmpty().mapNotNull { avatar ->
                avatar.avatarViewModel
                    ?.image
                    ?.sources
                    ?.maxByOrNull { maxOf(it.width ?: 0, it.height ?: 0) }
                    ?.url
            }
        }
    val linkedAvatar =
        supported
            ?.channelThumbnailWithLinkRenderer
            ?.thumbnail
            ?.thumbnails
            ?.maxByOrNull { maxOf(it.width ?: 0, it.height ?: 0) }
            ?.url

    return (stackAvatars + linkedAvatar + fallback)
        .filter { !it.isNullOrBlank() }
        .distinctBy { it.avatarImageIdentityKey() }
        .take(2)
        .filterNotNull()
}

internal fun parseLengthText(text: String?): Int {
    if (text.isNullOrBlank()) return 0
    val parts = text.split(":").map { it.trim().toIntOrNull() ?: 0 }
    return when (parts.size) {
        3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        2 -> parts[0] * 60 + parts[1]
        else -> 0
    }
}
