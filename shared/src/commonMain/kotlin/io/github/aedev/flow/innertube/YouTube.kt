package io.github.aedev.flow.innertube

import io.github.aedev.flow.data.model.Comment
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.innertube.models.AccountInfo
import io.github.aedev.flow.innertube.models.AlbumItem
import io.github.aedev.flow.innertube.models.Artist
import io.github.aedev.flow.innertube.models.BrowseEndpoint
import io.github.aedev.flow.innertube.models.GridRenderer
import io.github.aedev.flow.innertube.models.MediaInfo
import io.github.aedev.flow.innertube.models.MusicShelfRenderer
import io.github.aedev.flow.innertube.models.PlaylistItem
import io.github.aedev.flow.innertube.models.ReturnYouTubeDislikeResponse
import io.github.aedev.flow.innertube.models.SearchSuggestions
import io.github.aedev.flow.innertube.models.SongItem
import io.github.aedev.flow.innertube.models.WatchEndpoint
import io.github.aedev.flow.innertube.models.YouTubeClient
import io.github.aedev.flow.innertube.models.YouTubeClient.Companion.ANDROID_MUSIC
import io.github.aedev.flow.innertube.models.YouTubeClient.Companion.WEB
import io.github.aedev.flow.innertube.models.YouTubeClient.Companion.WEB_REMIX
import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.github.aedev.flow.innertube.models.getContinuation
import io.github.aedev.flow.innertube.models.getItems
import io.github.aedev.flow.innertube.models.response.AccountMenuResponse
import io.github.aedev.flow.innertube.models.response.BrowseResponse
import io.github.aedev.flow.innertube.models.response.ChannelSearchResponse
import io.github.aedev.flow.innertube.models.response.EditPlaylistResponse
import io.github.aedev.flow.innertube.models.response.GetLiveChatResponse
import io.github.aedev.flow.innertube.models.response.GetQueueResponse
import io.github.aedev.flow.innertube.models.response.GetSearchSuggestionsResponse
import io.github.aedev.flow.innertube.models.response.GetTranscriptResponse
import io.github.aedev.flow.innertube.models.response.LiveChatSeedResponse
import io.github.aedev.flow.innertube.models.response.NextResponse
import io.github.aedev.flow.innertube.models.response.PlayerResponse
import io.github.aedev.flow.innertube.models.response.ResolveUrlResponse
import io.github.aedev.flow.innertube.models.response.SearchResponse
import io.github.aedev.flow.innertube.models.response.WatchMetadataResponse
import io.github.aedev.flow.innertube.models.splitBySeparator
import io.github.aedev.flow.innertube.models.toSearchSuggestions
import io.github.aedev.flow.innertube.pages.AlbumPage
import io.github.aedev.flow.innertube.pages.ArtistPage
import io.github.aedev.flow.innertube.pages.ChartsPage
import io.github.aedev.flow.innertube.pages.HomePage
import io.github.aedev.flow.innertube.pages.LibraryContinuationPage
import io.github.aedev.flow.innertube.pages.LibraryPage
import io.github.aedev.flow.innertube.pages.MoodAndGenres
import io.github.aedev.flow.innertube.pages.NextPage
import io.github.aedev.flow.innertube.pages.NextResult
import io.github.aedev.flow.innertube.pages.PlaylistContinuationPage
import io.github.aedev.flow.innertube.pages.PlaylistPage
import io.github.aedev.flow.innertube.pages.RelatedPage
import io.github.aedev.flow.innertube.pages.SearchPage
import io.github.aedev.flow.innertube.pages.SearchResult
import io.github.aedev.flow.innertube.pages.SearchSuggestionPage
import io.github.aedev.flow.innertube.pages.SearchSummaryPage
import io.github.aedev.flow.innertube.pages.TimedLyricLine
import io.github.aedev.flow.innertube.pages.VideoCommentsPage
import io.github.aedev.flow.innertube.pages.VideoCommentsPanelHeader
import io.github.aedev.flow.innertube.pages.VideoCommentsSection
import io.github.aedev.flow.innertube.pages.VideoPlaylistPage
import io.github.aedev.flow.innertube.pages.channel.CHANNEL_SEARCH_FALLBACK_LANGUAGE
import io.github.aedev.flow.innertube.pages.channel.ChannelHeader
import io.github.aedev.flow.innertube.pages.channel.ChannelPage
import io.github.aedev.flow.innertube.pages.channel.ChannelSortOption
import io.github.aedev.flow.innertube.pages.channel.ChannelTabContent
import io.github.aedev.flow.innertube.pages.channel.ChannelTabKind
import io.github.aedev.flow.innertube.pages.channel.channelAboutContinuation
import io.github.aedev.flow.innertube.pages.channel.mergedWith
import io.github.aedev.flow.innertube.pages.channel.parseChannelSearchResponse
import io.github.aedev.flow.innertube.pages.channel.shouldSearchChannelInEnglish
import io.github.aedev.flow.innertube.pages.channel.toChannelAbout
import io.github.aedev.flow.innertube.pages.channel.toChannelHeader
import io.github.aedev.flow.innertube.pages.channel.toChannelTabContent
import io.github.aedev.flow.innertube.pages.channel.toChannelTabs
import io.github.aedev.flow.innertube.pages.channel.toOwner
import io.github.aedev.flow.innertube.pages.explore.ExploreDestinationPage
import io.github.aedev.flow.innertube.pages.explore.MusicVideoChart
import io.github.aedev.flow.innertube.pages.explore.exploreShelves
import io.github.aedev.flow.innertube.pages.explore.ranked
import io.github.aedev.flow.innertube.pages.explore.toExploreDestinationShell
import io.github.aedev.flow.innertube.pages.explore.trendingVideoChart
import io.github.aedev.flow.innertube.pages.libraryNotice
import io.github.aedev.flow.innertube.pages.liveChatSeedToken
import io.github.aedev.flow.innertube.pages.mixSeedVideoId
import io.github.aedev.flow.innertube.pages.player.PlayableStream
import io.github.aedev.flow.innertube.pages.player.playableStream
import io.github.aedev.flow.innertube.pages.reel.ReelLockup
import io.github.aedev.flow.innertube.pages.reel.ReelOverlay
import io.github.aedev.flow.innertube.pages.reel.ReelParams
import io.github.aedev.flow.innertube.pages.reel.ReelSequencePage
import io.github.aedev.flow.innertube.pages.reel.toReelLockups
import io.github.aedev.flow.innertube.pages.reel.toReelOverlay
import io.github.aedev.flow.innertube.pages.reel.toReelSequencePage
import io.github.aedev.flow.innertube.pages.renderer.CommunityCommentsPage
import io.github.aedev.flow.innertube.pages.renderer.CommunityPostPage
import io.github.aedev.flow.innertube.pages.renderer.FeedItemOwner
import io.github.aedev.flow.innertube.pages.renderer.toCommunityCommentsPage
import io.github.aedev.flow.innertube.pages.renderer.toCommunityPostPage
import io.github.aedev.flow.innertube.pages.search.SearchResultsPage
import io.github.aedev.flow.innertube.pages.search.toSearchResultsPage
import io.github.aedev.flow.innertube.pages.timedLyricLines
import io.github.aedev.flow.innertube.pages.toCommentRepliesPage
import io.github.aedev.flow.innertube.pages.toMixPlaylistPage
import io.github.aedev.flow.innertube.pages.toTimedComments
import io.github.aedev.flow.innertube.pages.toVideoCommentsPage
import io.github.aedev.flow.innertube.pages.toVideoPlaylistPage
import io.github.aedev.flow.innertube.pages.videoCommentsPanelHeader
import io.github.aedev.flow.innertube.pages.videoCommentsSection
import io.github.aedev.flow.utils.platformUtcOffsetMinutes
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * The client the app calls.
 *
 * `InnerTube` is the wire layer — request construction, headers, retries. This is the domain layer
 * on top of it: every read path here turns a response into the pages the UI consumes, and the
 * continuations are the same ones `completed()` walks.
 *
 * The channel, comments, shorts, live-chat, transcript, lyrics-page, podcast and account surfaces
 * are not here yet; they are the remaining parser-side ports, not request-layer gaps, and are named
 * in the step report.
 */
object YouTube {
    /**
     * The client every request here goes through. Production keeps the default one; the request
     * tests swap in a client backed by a mock engine, which is why this is not private.
     */
    internal var innerTube: InnerTube = InnerTube()

    var locale: YouTubeLocale
        get() = innerTube.locale
        set(value) {
            innerTube.locale = value
        }

    var visitorData: String?
        get() = innerTube.visitorData
        set(value) {
            innerTube.visitorData = value
        }

    var onVisitorDataChanged: ((String?) -> Unit)?
        get() = innerTube.onVisitorDataChanged
        set(value) {
            innerTube.onVisitorDataChanged = value
        }

    var dataSyncId: String?
        get() = innerTube.dataSyncId
        set(value) {
            innerTube.dataSyncId = value
        }

    var cookie: String?
        get() = innerTube.cookie
        set(value) {
            innerTube.cookie = value
        }

    data class ChannelVideoSearchResult(
        val continuation: String?,
        /** The answer in the server's order: a playlist row can sit above the videos. */
        val rows: List<io.github.aedev.flow.innertube.pages.channel.ChannelSearchRow> = emptyList(),
        val channelVideoCountText: String? = null,
        /** The tab's Latest/Popular/Oldest bar. Empty on a continuation, which carries no header. */
        val sorts: List<ChannelSortOption> = emptyList(),
    ) {
        /** The video rows alone, which is what the empty-answer language fallback counts. */
        val videos: List<Video>
            get() =
                rows
                    .filterIsInstance<io.github.aedev.flow.innertube.pages.channel.ChannelSearchRow.VideoRow>()
                    .map { it.video }
    }

    /** The search filters the app's dialog sends; each is a protobuf `params` token. */
    @JvmInline
    value class SearchFilter(
        val value: String,
    ) {
        companion object {
            val FILTER_SONG = SearchFilter("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
            val FILTER_VIDEO = SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ALBUM = SearchFilter("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ARTIST = SearchFilter("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_FEATURED_PLAYLIST = SearchFilter("EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ%3D")
            val FILTER_COMMUNITY_PLAYLIST = SearchFilter("EgeKAQQoAEABagoQAxAEEAoQCRAF")
            val FILTER_PODCAST = SearchFilter("EgWKAQJQAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_EPISODE = SearchFilter("EgWKAQJIAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_PROFILE = SearchFilter("EgWKAQJYAWoSEAUQCRADEAQQEBAVEAoQDhAR")
        }
    }

    // ---- browse ----

    suspend fun home(
        continuation: String? = null,
        browseId: String = "FEmusic_home",
        params: String? = null,
        client: YouTubeClient = WEB_REMIX,
    ): Result<HomePage> =
        runCatching {
            if (continuation != null) {
                return@runCatching homeContinuation(continuation).getOrThrow()
            }

            val response = innerTube.browse(WEB_REMIX, browseId = browseId, params = params).body<BrowseResponse>()
            // Every response carries the visitor the server assigned or recognised; sending it back
            // is what makes the next music home the same page rather than a fresh anonymous one.
            if (innerTube.visitorData.isNullOrBlank()) {
                innerTube.visitorData = response.responseContext.visitorData
            }
            val sectionListRender =
                response.contents
                    ?.singleColumnBrowseResultsRenderer
                    ?.tabs
                    ?.firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
            val continuation =
                sectionListRender
                    ?.continuations
                    ?.getContinuation()
            // Every shelf the response sends, whatever its layout: carousels keep their list rows
            // (Quick picks arrives that way), and a grid shelf is read as the same two-row cards.
            val sections =
                sectionListRender
                    ?.contents
                    .orEmpty()
                    .mapNotNull { content ->
                        content.musicCarouselShelfRenderer?.let {
                            HomePage.Section.fromMusicCarouselShelfRenderer(it, chipPage = true)
                        } ?: content.gridRenderer?.let(HomePage.Section::fromGridRenderer)
                    }.toMutableList()
            val chips =
                sectionListRender
                    ?.header
                    ?.chipCloudRenderer
                    ?.chips
                    ?.mapNotNull { HomePage.Chip.fromChipCloudChipRenderer(it) }
            HomePage(chips, sections, continuation)
        }

    private suspend fun homeContinuation(continuation: String): Result<HomePage> =
        runCatching {
            val response =
                innerTube.browse(WEB_REMIX, continuation = continuation).body<BrowseResponse>()
            HomePage(
                null,
                response.continuationContents
                    ?.sectionListContinuation
                    ?.contents
                    ?.mapNotNull { it.musicCarouselShelfRenderer }
                    ?.mapNotNull {
                        HomePage.Section.fromMusicCarouselShelfRenderer(it)
                    }.orEmpty(),
                response.continuationContents
                    ?.sectionListContinuation
                    ?.continuations
                    ?.getContinuation(),
            )
        }

    /** An artist's page: immersive header, description and its own shelves. */
    suspend fun artist(browseId: String): Result<ArtistPage> =
        runCatching {
            val response =
                innerTube
                    .browse(client = WEB_REMIX, browseId = browseId, setLogin = true)
                    .body<BrowseResponse>()
            ArtistPage.fromBrowseResponse(browseId, response)
        }

    suspend fun related(endpoint: BrowseEndpoint): Result<RelatedPage> =
        runCatching {
            RelatedPage.fromBrowseResponse(innerTube.browse(WEB_REMIX, endpoint.browseId).body<BrowseResponse>())
        }

    /**
     * YouTube Music's moods and genres index: one entry per category of mood buttons, each button
     * carrying the browse endpoint that opens the collection behind it.
     */
    suspend fun moodsAndGenres(): Result<List<MoodAndGenres>> =
        runCatching {
            val response =
                innerTube
                    .browse(client = WEB_REMIX, browseId = "FEmusic_moods_and_genres")
                    .body<BrowseResponse>()
            response.contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                .orEmpty()
                .mapNotNull(MoodAndGenres.Companion::fromSectionListRendererContent)
                .filter { it.items.isNotEmpty() }
        }

    // ---- explore ----

    /**
     * One Explore destination's landing page: the title it arrives with, the category tabs it
     * offers, and every shelf it arranged. A destination carries no continuation of its own; each
     * shelf's own token opens its "see all" grid instead.
     */
    suspend fun exploreDestination(
        browseId: String,
        params: String? = null,
    ): Result<ExploreDestinationPage> =
        runCatching {
            val response = channelBrowseJson(browseId = browseId, params = params)
            val shell = response.toExploreDestinationShell()
            shell.copy(shelves = response.exploreShelves(shell.owner).toList())
        }

    /**
     * A playlist read from the WEB `VL<id>` browse, which is where a chart's ranked videos live:
     * the music playlist browse answers with songs, not videos.
     */
    suspend fun videoPlaylistPage(playlistId: String): Result<VideoPlaylistPage> =
        runCatching {
            channelBrowseJson(browseId = "VL$playlistId").toVideoPlaylistPage()
        }

    /**
     * The trending music videos: YouTube Music's charts page names the trending chart playlist, and
     * that playlist's own browse gives its rows.
     */
    suspend fun musicVideoChart(): Result<MusicVideoChart> =
        runCatching {
            val response =
                innerTube
                    .browse(
                        client = WEB_REMIX,
                        browseId = "FEmusic_charts",
                        params = "ggMGCgQIgAQ%3D",
                    ).body<BrowseResponse>()
            val playlist =
                ChartsPage.fromBrowseResponse(response, country = null).trendingVideoChart()
                    ?: return@runCatching MusicVideoChart(title = "")
            val page = videoPlaylistPage(playlist.id).getOrThrow()
            MusicVideoChart(title = playlist.title, entries = page.videos.ranked())
        }

    // ---- search ----

    suspend fun search(
        query: String,
        filter: SearchFilter,
    ): Result<SearchResult> =
        runCatching {
            val response = innerTube.search(WEB_REMIX, query, filter.value).body<SearchResponse>()
            // The results shelf is not always the last section: a "did you mean" or message can follow it.
            val shelf =
                response.contents
                    ?.tabbedSearchResultsRenderer
                    ?.tabs
                    ?.firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
                    ?.contents
                    ?.firstNotNullOfOrNull { it.musicShelfRenderer }
            SearchResult(
                items =
                    shelf
                        ?.contents
                        ?.getItems()
                        ?.mapNotNull(SearchPage::toYTItem)
                        .orEmpty(),
                continuation = shelf?.continuations?.getContinuation(),
            )
        }

    suspend fun searchContinuation(continuation: String): Result<SearchResult> =
        runCatching {
            val response = innerTube.search(WEB_REMIX, continuation = continuation).body<SearchResponse>()
            SearchResult(
                items =
                    response.continuationContents
                        ?.musicShelfContinuation
                        ?.contents
                        ?.mapNotNull {
                            SearchPage.toYTItem(it.musicResponsiveListItemRenderer ?: return@mapNotNull null)
                        }.orEmpty(),
                continuation =
                    response.continuationContents
                        ?.musicShelfContinuation
                        ?.continuations
                        ?.getContinuation(),
            )
        }

    suspend fun searchSummary(query: String): Result<SearchSummaryPage> =
        runCatching {
            SearchSummaryPage.fromSearchResponse(innerTube.search(WEB_REMIX, query).body<SearchResponse>())
        }

    suspend fun searchSuggestions(query: String): Result<SearchSuggestions> =
        runCatching {
            innerTube.getSearchSuggestions(WEB_REMIX, query).body<GetSearchSuggestionsResponse>().toSearchSuggestions()
        }

    // ---- watch ----

    suspend fun next(
        endpoint: WatchEndpoint,
        continuation: String? = null,
    ): Result<NextResult> =
        runCatching {
            val response =
                innerTube
                    .next(
                        WEB_REMIX,
                        endpoint.videoId,
                        endpoint.playlistId,
                        endpoint.playlistSetVideoId,
                        endpoint.index,
                        endpoint.params,
                        continuation,
                    ).body<NextResponse>()
            // YouTube inserts/reorders watch-next tabs (a Comments tab appeared at index 2 in 2026),
            // so lyrics/related must be found by browseId prefix, never by position.
            val watchNextTabs =
                response.contents.singleColumnMusicWatchNextResultsRenderer
                    ?.tabbedRenderer
                    ?.watchNextTabbedResultsRenderer
                    ?.tabs
            val lyricsBrowseEndpoint = response.lyricsBrowseEndpoint()
            val relatedBrowseEndpoint =
                watchNextTabs?.firstNotNullOfOrNull { tab ->
                    tab.tabRenderer.endpoint
                        ?.browseEndpoint
                        ?.takeIf { it.browseId.startsWith("MPTRt") }
                }
            val playlistPanelRenderer =
                response.continuationContents?.playlistPanelContinuation
                    ?: watchNextTabs
                        ?.get(0)
                        ?.tabRenderer
                        ?.content
                        ?.musicQueueRenderer
                        ?.content
                        ?.playlistPanelRenderer
                    ?: return@runCatching NextResult(
                        items = emptyList(),
                        lyricsEndpoint = lyricsBrowseEndpoint,
                        relatedEndpoint = relatedBrowseEndpoint,
                        continuation = null,
                        endpoint = endpoint,
                    )
            val title =
                watchNextTabs
                    ?.get(0)
                    ?.tabRenderer
                    ?.content
                    ?.musicQueueRenderer
                    ?.header
                    ?.musicQueueHeaderRenderer
                    ?.subtitle
                    ?.runs
                    ?.firstOrNull()
                    ?.text
            val items =
                playlistPanelRenderer.contents.mapNotNull { content ->
                    content.playlistPanelVideoRenderer
                        ?.let(NextPage::fromPlaylistPanelVideoRenderer)
                        ?.let { it to content.playlistPanelVideoRenderer.selected }
                }
            val songs = items.map { it.first }
            val currentIndex = items.indexOfFirst { it.second }.takeIf { it != -1 }

            // load automix items
            playlistPanelRenderer.contents
                .lastOrNull()
                ?.automixPreviewVideoRenderer
                ?.content
                ?.automixPlaylistVideoRenderer
                ?.navigationEndpoint
                ?.watchPlaylistEndpoint
                ?.let { watchPlaylistEndpoint ->
                    return@runCatching next(watchPlaylistEndpoint).getOrThrow().let { result ->
                        result.copy(
                            title = title,
                            items = songs + result.items,
                            lyricsEndpoint = lyricsBrowseEndpoint,
                            relatedEndpoint = relatedBrowseEndpoint,
                            currentIndex = currentIndex,
                            endpoint = watchPlaylistEndpoint,
                        )
                    }
                }
            NextResult(
                title = title,
                items = songs,
                currentIndex = currentIndex,
                lyricsEndpoint = lyricsBrowseEndpoint,
                relatedEndpoint = relatedBrowseEndpoint,
                continuation = playlistPanelRenderer.continuations?.getContinuation(),
                endpoint = endpoint,
            )
        }

    private fun NextResponse.lyricsBrowseEndpoint(): BrowseEndpoint? =
        contents.singleColumnMusicWatchNextResultsRenderer
            ?.tabbedRenderer
            ?.watchNextTabbedResultsRenderer
            ?.tabs
            ?.firstNotNullOfOrNull { tab ->
                tab.tabRenderer.endpoint
                    ?.browseEndpoint
                    ?.takeIf { it.browseId.startsWith("MPLYt") }
            }

    /** One `next` and nothing else: [next] follows the automix row, which is a second, slower request. */
    suspend fun lyricsEndpoint(videoId: String): Result<BrowseEndpoint?> =
        runCatching {
            innerTube
                .next(WEB_REMIX, videoId, null, null, null, null)
                .body<NextResponse>()
                .lyricsBrowseEndpoint()
        }

    suspend fun lyrics(endpoint: BrowseEndpoint): Result<String?> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
            response.contents
                ?.sectionListRenderer
                ?.contents
                ?.firstOrNull()
                ?.musicDescriptionShelfRenderer
                ?.description
                ?.runs
                ?.firstOrNull()
                ?.text
        }

    suspend fun timedLyrics(endpoint: BrowseEndpoint): Result<List<TimedLyricLine>> =
        runCatching {
            timedLyricLines(innerTube.browse(ANDROID_MUSIC, endpoint.browseId, endpoint.params).body())
        }

    suspend fun player(
        videoId: String,
        playlistId: String? = null,
        client: YouTubeClient,
        signatureTimestamp: Int? = null,
        poToken: String? = null,
        localeOverride: YouTubeLocale? = null,
        apiUrl: String? = null,
    ): Result<PlayerResponse> =
        runCatching {
            innerTube.player(client, videoId, playlistId, signatureTimestamp, poToken, localeOverride, apiUrl).body<PlayerResponse>()
        }

    suspend fun playerWeb(
        videoId: String,
        signatureTimestamp: Int?,
        poToken: String?,
        visitorData: String?,
        locale: YouTubeLocale,
        cpn: String?,
        reloadToken: String? = null,
        client: YouTubeClient = WEB,
    ): Result<PlayerResponse> =
        runCatching {
            innerTube
                .playerWeb(videoId, signatureTimestamp, poToken, visitorData, locale, cpn, reloadToken, client)
                .body<PlayerResponse>()
        }

    /**
     * The stream the player can hand straight to a media engine, resolved from the direct-URL
     * clients. Null means no client returned a format with a direct URL (a live stream that only
     * offers manifests, or a video every client refuses).
     *
     * Every request goes to the main site (`www.youtube.com`), not the music endpoint: for several
     * of these clients the music host answers LOGIN_REQUIRED or SABR-only formats while the main
     * site serves the very same client a direct file. That difference was measured on
     * `0J2QdDbelmY` (the music host refused ANDROID, so the old ladder fell to an iOS adaptive URL
     * that GVS answered 403 to past ~60 s, while the main site served ANDROID a muxed file).
     */
    suspend fun playableStream(videoId: String): Result<PlayableStream?> =
        runCatching {
            // Clients differ in what they are willing to serve, and GVS binds a URL to the client
            // that asked for it. The first client that yields a direct track wins.
            var stream: PlayableStream? = null
            playbackClients.forEach { client ->
                if (stream != null) return@forEach
                val response =
                    player(
                        videoId,
                        client = client,
                        apiUrl = YouTubeClient.API_URL_YOUTUBE,
                    ).getOrNull() ?: return@forEach
                stream = response.playableStream()?.copy(userAgent = client.userAgent)
            }
            stream
        }

    /**
     * The direct rungs, in the reference app's order. VISIONOS leads because it is the only one of
     * these GVS still serves without a PO token and without an `n` parameter, so its URLs survive
     * the enforcement that cuts the app clients off. The VR builds answer ordinary videos with a
     * muxed progressive file but are refused for most music; the stock Android client still serves
     * album tracks with one; iOS is the last resort, and its adaptive URLs are the ones GVS may
     * refuse mid-playback without attestation.
     */
    private val playbackClients =
        listOf(
            YouTubeClient.VISIONOS,
            YouTubeClient.ANDROID_VR_NO_AUTH,
            YouTubeClient.ANDROID_VR_1_61_48,
            YouTubeClient.ANDROID_VR_1_65_10,
            YouTubeClient.ANDROID_VR_1_43_32,
            YouTubeClient.ANDROID,
            YouTubeClient.IOS,
        )

    /**
     * The YouTube Music library landing. The signed-out page is captured above; its notice exists
     * only in the raw body, so the typed decode is best-effort and a failure still yields an empty
     * feed with YouTube's own notice instead of an error.
     */
    suspend fun library(
        browseId: String,
        tabIndex: Int = 0,
    ): Result<LibraryPage> =
        runCatching {
            val body =
                innerTube
                    .browse(
                        client = WEB_REMIX,
                        browseId = browseId,
                        params = "FEmusic_library_landing",
                        setLogin = true,
                    ).bodyAsText()
            val root = Json.parseToJsonElement(body)
            val notice = root.libraryNotice()
            val response =
                runCatching { Json { ignoreUnknownKeys = true }.decodeFromString<BrowseResponse>(body) }.getOrNull()
            val tabs = response?.contents?.singleColumnBrowseResultsRenderer?.tabs
            val contents =
                tabs
                    ?.getOrNull(tabIndex)
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
                    ?.contents
                    ?.firstOrNull()

            LibraryPage(
                items =
                    when {
                        contents?.gridRenderer != null -> {
                            contents.gridRenderer.items
                                .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                                .mapNotNull { LibraryPage.fromMusicTwoRowItemRenderer(it) }
                        }

                        contents?.musicShelfRenderer != null -> {
                            contents.musicShelfRenderer.contents
                                ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                                ?.mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it) }
                                .orEmpty()
                        }

                        else -> {
                            emptyList()
                        }
                    },
                continuation =
                    contents?.gridRenderer?.continuations?.getContinuation()
                        ?: contents?.musicShelfRenderer?.continuations?.getContinuation(),
                notice = notice?.first,
                noticeSubtext = notice?.second,
            )
        }

    /** The next page of the library, which arrives as a grid or a shelf continuation. */
    suspend fun libraryContinuation(continuation: String): Result<LibraryContinuationPage> =
        runCatching {
            val response =
                innerTube
                    .browse(
                        client = WEB_REMIX,
                        continuation = continuation,
                        setLogin = true,
                    ).body<BrowseResponse>()

            val contents = response.continuationContents

            when {
                contents?.gridContinuation != null -> {
                    LibraryContinuationPage(
                        items =
                            contents.gridContinuation.items
                                .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                                .mapNotNull { LibraryPage.fromMusicTwoRowItemRenderer(it) },
                        continuation = contents.gridContinuation.continuations?.getContinuation(),
                    )
                }

                else -> {
                    LibraryContinuationPage(
                        items =
                            contents
                                ?.musicShelfContinuation
                                ?.contents
                                .orEmpty()
                                .mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                                .mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it) },
                        continuation = contents?.musicShelfContinuation?.continuations?.getContinuation(),
                    )
                }
            }
        }

    /**
     * A YouTube Music playlist. Podcast shows are not ported: they need their own header renderer
     * and episode shaping, and the app's playlists all arrive as the responsive header.
     */
    suspend fun playlist(playlistId: String): Result<PlaylistPage> =
        runCatching {
            // A playlist's page hangs off the VL prefix, an album's off its own browse id; asking
            // for both lets the one route serve the playlist and album cards the app opens.
            var lastError: Throwable? = null
            for (browseId in listOf("VL$playlistId", playlistId)) {
                val response =
                    innerTube
                        .browse(
                            client = WEB_REMIX,
                            browseId = browseId,
                            setLogin = true,
                        ).body<BrowseResponse>()
                runCatching { playlistPage(playlistId, response) }
                    .onSuccess { return@runCatching it }
                    .onFailure { lastError = it }
            }
            throw lastError ?: error("playlist $playlistId has no music header")
        }

    private fun playlistPage(
        playlistId: String,
        response: BrowseResponse,
    ): PlaylistPage {
        val twoColumn = response.contents?.twoColumnBrowseResultsRenderer
        val base =
            twoColumn
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.firstOrNull()
        val header =
            base?.musicResponsiveHeaderRenderer
                ?: base?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer
                ?: error("playlist $playlistId has no music header")
        val subtitleGroups =
            header.subtitle
                ?.runs
                ?.splitBySeparator()
                ?.map { part -> part.joinToString("") { run -> run.text } }
                .orEmpty()
        val lengthGroups =
            header.secondSubtitle
                ?.runs
                ?.splitBySeparator()
                ?.map { part -> part.joinToString("") { run -> run.text } }
                .orEmpty()
        // A playlist's tracks sit in a playlist shelf, an album's in a music shelf; both
        // render the same responsive list rows.
        val secondary = twoColumn?.secondaryContents?.sectionListRenderer?.contents
        val single =
            response.contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
        val playlistShelf =
            secondary?.firstNotNullOfOrNull { it.musicPlaylistShelfRenderer }
                ?: single?.firstNotNullOfOrNull { it.musicPlaylistShelfRenderer }
        val musicShelf =
            secondary?.firstNotNullOfOrNull { it.musicShelfRenderer }
                ?: single?.firstNotNullOfOrNull { it.musicShelfRenderer }
        val shelfItems = playlistShelf?.contents?.getItems() ?: musicShelf?.contents?.getItems().orEmpty()
        val shelfContinuation = playlistShelf?.contents?.getContinuation() ?: musicShelf?.contents?.getContinuation()

        return PlaylistPage(
            playlist =
                PlaylistItem(
                    id = playlistId,
                    title =
                        header.title
                            ?.runs
                            ?.firstOrNull()
                            ?.text
                            .orEmpty(),
                    author =
                        header.straplineTextOne?.runs?.firstOrNull()?.let {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        },
                    songCountText =
                        header.secondSubtitle
                            ?.runs
                            ?.firstOrNull()
                            ?.text,
                    thumbnail =
                        header.thumbnail
                            ?.musicThumbnailRenderer
                            ?.thumbnail
                            ?.thumbnails
                            ?.lastOrNull()
                            ?.url,
                    playEndpoint = null,
                    shuffleEndpoint =
                        header.buttons
                            .lastOrNull()
                            ?.menuRenderer
                            ?.items
                            ?.firstOrNull()
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint,
                    radioEndpoint =
                        header.buttons
                            .getOrNull(2)
                            ?.menuRenderer
                            ?.items
                            ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint,
                    isEditable = base?.musicEditablePlaylistDetailHeaderRenderer != null,
                ),
            // An album's rows carry their video id and artist in different columns than a
            // playlist's, so the album reader is what reads them.
            songs =
                if (playlistShelf == null && musicShelf != null) {
                    // An album's rows lean on the header for the album and its artists; passing
                    // it in is what lets the album reader resolve every row.
                    val album =
                        AlbumItem(
                            browseId = playlistId,
                            playlistId = playlistId,
                            title =
                                header.title
                                    ?.runs
                                    ?.firstOrNull()
                                    ?.text
                                    .orEmpty(),
                            artists =
                                header.straplineTextOne?.runs?.firstOrNull()?.let {
                                    listOf(Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId))
                                },
                            thumbnail =
                                header.thumbnail
                                    ?.musicThumbnailRenderer
                                    ?.thumbnail
                                    ?.thumbnails
                                    ?.lastOrNull()
                                    ?.url
                                    .orEmpty(),
                        )
                    shelfItems.mapNotNull { renderer -> runCatching { AlbumPage.getSong(renderer, album) }.getOrNull() }
                } else {
                    shelfItems.mapNotNull(PlaylistPage::fromMusicResponsiveListItemRenderer)
                },
            songsContinuation = shelfContinuation,
            continuation =
                twoColumn
                    ?.secondaryContents
                    ?.sectionListRenderer
                    ?.continuations
                    ?.getContinuation(),
            trackCount = PlaylistPage.trackCountFrom(header.secondSubtitle),
            releaseType = subtitleGroups.firstOrNull()?.takeIf { it.isNotBlank() },
            details =
                (subtitleGroups.drop(1) + lengthGroups)
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                    .takeIf { it.isNotBlank() },
            explicit = AlbumPage.isExplicit(header),
        )
    }

    suspend fun playlistContinuation(continuation: String): Result<PlaylistContinuationPage> =
        runCatching {
            val response =
                innerTube
                    .browse(
                        client = WEB_REMIX,
                        continuation = continuation,
                        browseId = "",
                        setLogin = true,
                    ).body<BrowseResponse>()

            val shelfContents =
                response.continuationContents
                    ?.sectionListContinuation
                    ?.contents
                    ?.flatMap { it.musicPlaylistShelfRenderer?.contents.orEmpty() }
                    .orEmpty()
            val appended =
                response.onResponseReceivedActions
                    ?.firstOrNull()
                    ?.appendContinuationItemsAction
                    ?.continuationItems
                    .orEmpty()

            PlaylistContinuationPage(
                songs =
                    (shelfContents + appended)
                        .mapNotNull { it.musicResponsiveListItemRenderer }
                        .mapNotNull(PlaylistPage::fromMusicResponsiveListItemRenderer),
                continuation =
                    response.continuationContents
                        ?.sectionListContinuation
                        ?.continuations
                        ?.getContinuation()
                        ?: response.continuationContents
                            ?.musicPlaylistShelfContinuation
                            ?.continuations
                            ?.getContinuation()
                        ?: response.continuationContents
                            ?.musicShelfContinuation
                            ?.continuations
                            ?.getContinuation()
                        ?: response.onResponseReceivedActions
                            ?.firstOrNull()
                            ?.appendContinuationItemsAction
                            ?.continuationItems
                            ?.getContinuation(),
            )
        }

    // ---- mixes, channels, transcript, account ----

    /**
     * A mix, read from the WEB `next` queue. The playlist browse refuses one ("This playlist type
     * is unviewable"), so its watch queue is the only list of it, and that queue has no next page.
     */
    suspend fun nextMix(playlistId: String): Result<VideoPlaylistPage> =
        runCatching {
            val response = innerTube.nextMix(mixSeedVideoId(playlistId), playlistId)
            Json.parseToJsonElement(response.bodyAsText()).toMixPlaylistPage()
        }

    private val visitorDataRegex = Regex("^Cg[t|s]")

    /**
     * A visitor id scraped from the music site's `sw.js_data`.
     *
     * It is the identity YouTube hands out before any request carries one, and the one place a
     * caller can obtain it without a browse response to read it back from.
     */
    suspend fun visitorData(): Result<String> =
        runCatching {
            Json
                .parseToJsonElement(innerTube.getSwJsData().bodyAsText().substring(5))
                .jsonArray[0]
                .jsonArray[2]
                .jsonArray
                .first {
                    (it as? JsonPrimitive)?.contentOrNull?.let(visitorDataRegex::containsMatchIn) ?: false
                }.jsonPrimitive.content
        }

    suspend fun accountMenu(): Result<AccountInfo> =
        runCatching {
            innerTube
                .accountMenu(WEB_REMIX)
                .body<AccountMenuResponse>()
                .actions[0]
                .openPopupAction.popup.multiPageMenuRenderer
                .header
                ?.activeAccountHeaderRenderer
                ?.toAccountInfo()
                ?: error("The account menu has no active account")
        }

    /**
     * The WEB channel browse. The raw JSON is kept because the header, the tabs and a tab's first
     * page are all read from the same response.
     *
     * The client sends the device's UTC offset: channel rows carry server-rendered times, and the
     * WEB client's zero offset would print them in UTC.
     */
    private suspend fun channelBrowseJson(
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
    ): JsonElement {
        val response =
            innerTube.channelBrowse(
                client = WEB.copy(utcOffsetMinutes = platformUtcOffsetMinutes()),
                channelId = browseId,
                params = params,
                continuation = continuation,
            )
        return Json.parseToJsonElement(response.bodyAsText())
    }

    /**
     * A channel's header and tabs, plus whatever the About panel adds.
     *
     * The landing response carries no description, links or join date; those arrive from a second
     * request whose token only the landing response knows. A failed About request leaves the
     * landing header as it is rather than failing the page.
     */
    suspend fun channel(idOrHandle: String): Result<ChannelPage> =
        runCatching {
            val response = channelBrowseJson(browseId = idOrHandle)
            val about =
                response
                    .channelAboutContinuation()
                    ?.let { token -> runCatching { channelBrowseJson(continuation = token).toChannelAbout() }.getOrNull() }
            response.toChannelPage(response.toChannelHeader(idOrHandle).mergedWith(about))
        }

    suspend fun channelTab(
        browseId: String,
        params: String,
        owner: FeedItemOwner = FeedItemOwner(id = browseId),
        kind: ChannelTabKind = ChannelTabKind.Unknown,
    ): Result<ChannelTabContent> =
        runCatching {
            channelBrowseJson(browseId = browseId, params = params).toChannelTabContent(kind, owner)
        }

    /** A tab re-requested through a sort chip or menu token, which arrives as a continuation. */
    suspend fun channelTabContinuation(
        browseId: String,
        continuation: String,
        kind: ChannelTabKind = ChannelTabKind.Unknown,
    ): Result<ChannelTabContent> =
        runCatching {
            channelBrowseJson(continuation = continuation).toChannelTabContent(kind, FeedItemOwner(id = browseId))
        }

    private fun JsonElement.toChannelPage(header: ChannelHeader): ChannelPage {
        val tabs = toChannelTabs()
        return ChannelPage(
            header = header,
            tabs = tabs,
            initialTab =
                tabs
                    .firstOrNull { it.selected }
                    ?.let { tab -> toChannelTabContent(tab.kind, header.toOwner()) },
        )
    }

    /**
     * A channel-scoped video search. An empty answer in any language but English is asked once more
     * in English, which is what YouTube-the-site ends up showing.
     */
    suspend fun channelSearch(
        channelId: String,
        channelName: String,
        channelThumbnailUrl: String,
        query: String,
        continuation: String? = null,
    ): Result<ChannelVideoSearchResult> =
        runCatching {
            suspend fun search(searchLocale: YouTubeLocale): ChannelVideoSearchResult {
                val rawBody =
                    innerTube
                        .channelSearch(
                            client = WEB.copy(utcOffsetMinutes = platformUtcOffsetMinutes()),
                            channelId = channelId,
                            query = query,
                            continuation = continuation,
                            searchLocale = searchLocale,
                        ).bodyAsText()
                val response =
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    }.decodeFromString<ChannelSearchResponse>(rawBody)
                return parseChannelSearchResponse(response, channelId, channelName, channelThumbnailUrl)
            }
            val searchLocale = locale
            val first = search(searchLocale)
            // A next page has no language to fall back from: the term was already resolved on the
            // page it continues.
            if (continuation != null) return@runCatching first
            if (!shouldSearchChannelInEnglish(first.videos.size, searchLocale.hl)) return@runCatching first
            search(searchLocale.copy(hl = CHANNEL_SEARCH_FALLBACK_LANGUAGE))
        }

    /** The transcript with a `[mm:ss.mmm]` stamp per cue, in the WEB_REMIX panel's own order. */
    suspend fun transcript(videoId: String): Result<String> =
        runCatching {
            val response = innerTube.getTranscript(WEB_REMIX, videoId).body<GetTranscriptResponse>()
            response.actions
                ?.firstOrNull()
                ?.updateEngagementPanelAction
                ?.content
                ?.transcriptRenderer
                ?.body
                ?.transcriptBodyRenderer
                ?.cueGroups
                ?.joinToString(separator = "\n") { group ->
                    val time =
                        group.transcriptCueGroupRenderer.cues[0]
                            .transcriptCueRenderer.startOffsetMs
                    val text =
                        group.transcriptCueGroupRenderer.cues[0]
                            .transcriptCueRenderer.cue.simpleText
                            .trim('♪')
                            .trim(' ')
                    "[${pad(time / 60000, 2)}:${pad((time / 1000) % 60, 2)}.${pad(time % 1000, 3)}]$text"
                } ?: error("No transcript for $videoId")
        }

    private fun pad(
        value: Long,
        width: Int,
    ): String = value.toString().padStart(width, '0')

    suspend fun setThumbnailPlaylist(
        playlistId: String,
        blobId: String,
    ): Result<String?> =
        runCatching {
            innerTube
                .setThumbnailPlaylist(WEB_REMIX, playlistId, blobId)
                .body<EditPlaylistResponse>()
                .newHeader
                ?.musicEditablePlaylistDetailHeaderRenderer
                ?.header
                ?.musicResponsiveHeaderRenderer
                ?.thumbnail
                ?.musicThumbnailRenderer
                ?.getThumbnailUrl()
        }

    private val resolvedChannelIds = mutableMapOf<String, String>()

    /**
     * The channel id behind an @handle, `/c/` or `/user/` link, which browse cannot open directly.
     * Kept for the process: a channel page and its Shorts feed both resolve the same link.
     */
    suspend fun resolveChannelId(url: String): Result<String> =
        runCatching {
            resolvedChannelIds[url]?.let { return@runCatching it }
            val channelId =
                innerTube
                    .resolveUrl(WEB, url)
                    .body<ResolveUrlResponse>()
                    .endpoint
                    ?.browseEndpoint
                    ?.browseId
                    ?.takeIf { it.startsWith("UC") }
                    ?: error("No channel behind $url")
            resolvedChannelIds[url] = channelId
            channelId
        }

    // ---- main-site video surfaces ----

    /**
     * One page of video search. Filters and sorting ride in [params]; paging rides in
     * [continuation]. The client is the static WEB one, so `clientVersion` is pinned here rather
     * than read out of base.js; every other header is the ones the app sends.
     */
    suspend fun videoSearch(
        query: String? = null,
        params: String? = null,
        continuation: String? = null,
    ): Result<SearchResultsPage> =
        runCatching {
            innerTube
                .webSearch(
                    client = WEB,
                    query = query.takeIf { continuation == null },
                    params = params?.takeIf { continuation == null },
                    continuation = continuation,
                    anonymous = true,
                    includeVisitorData = true,
                ).body<JsonObject>()
                .toSearchResultsPage()
        }

    /** Reels matching [query], through the Shorts filter the search dialog itself sends. */
    suspend fun searchShorts(query: String): Result<List<ReelLockup>> =
        runCatching {
            innerTube
                .webSearch(
                    client = WEB,
                    query = query,
                    params = ReelParams.SEARCH_SHORTS_FILTER,
                    anonymous = true,
                    includeVisitorData = true,
                ).body<JsonObject>()
                .toReelLockups()
        }

    /**
     * A main-site browse. `InnerTube.webBrowse` is the private body-builder the channel requests
     * share; this is the same request reached through its public `channelBrowse` path, and it
     * returns the raw document because the video surfaces read several views out of one response.
     */
    suspend fun webBrowse(
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
    ): Result<JsonElement> =
        runCatching {
            Json.parseToJsonElement(
                innerTube
                    .channelBrowse(
                        client = WEB,
                        channelId = browseId,
                        params = params,
                        continuation = continuation,
                    ).bodyAsText(),
            )
        }

    /**
     * The WEB watch-next document. Returned as JSON because the comment section, the attributed
     * description and the related lane all read one of these; [watchMetadataLite] is the typed view.
     */
    suspend fun watchNext(
        videoId: String? = null,
        continuation: String? = null,
    ): Result<JsonElement> =
        runCatching {
            Json.parseToJsonElement(
                innerTube.nextWatch(videoId = videoId, continuation = continuation).bodyAsText(),
            )
        }

    suspend fun watchMetadataLite(videoId: String): Result<WatchMetadataResponse> =
        runCatching {
            innerTube.nextWatch(videoId = videoId).body<WatchMetadataResponse>()
        }

    /**
     * The comment section of an already-fetched watch response: the token its first page opens from,
     * or the notice that the uploader turned comments off.
     */
    fun commentsSection(watchNext: JsonElement): VideoCommentsSection? = watchNext.videoCommentsSection()

    /** The sort menu and the total shown in [watchNext]'s comments panel. */
    fun commentsPanelHeader(watchNext: JsonElement): VideoCommentsPanelHeader? = watchNext.videoCommentsPanelHeader()

    /** The first page behind a comment section's token, or the next page of one already open. */
    suspend fun comments(
        continuation: String,
        ownVideoId: String? = null,
    ): Result<VideoCommentsPage> =
        runCatching {
            Json
                .parseToJsonElement(innerTube.nextWatch(continuation = continuation).bodyAsText())
                .toVideoCommentsPage(ownVideoId)
        }

    /** The replies under one comment, from the token its thread's replies renderer carries. */
    suspend fun commentReplies(
        continuation: String,
        ownVideoId: String? = null,
    ): Result<VideoCommentsPage> =
        runCatching {
            Json
                .parseToJsonElement(innerTube.nextWatch(continuation = continuation).bodyAsText())
                .toCommentRepliesPage(ownVideoId)
        }

    /**
     * The live chat token a watch page carries, or null when the video is not a live stream. The
     * player reads it out of the response it already fetched instead of asking `/next` again.
     */
    fun liveChatToken(watchNext: JsonElement): String? = watchNext.liveChatSeedToken()

    /** The next batch of reels after [sequenceParams], which a lockup carries. */
    suspend fun reelSequence(sequenceParams: String): Result<ReelSequencePage> =
        runCatching {
            innerTube
                .reel(client = WEB, sequenceParams = sequenceParams)
                .body<JsonObject>()
                .toReelSequencePage()
        }

    /** One reel's overlay: its action buttons, description and audio track. */
    suspend fun reelOverlay(videoId: String): Result<ReelOverlay?> =
        runCatching {
            innerTube
                .reelItemWatch(client = WEB, videoId = videoId)
                .body<JsonObject>()
                .toReelOverlay()
        }

    // ---- community posts, timed comments and live chat ----

    /**
     * One community post's page, on the main site. [params] is the token the post's own comment
     * button carries (`browseEndpoint.params` for `FEpost_detail`); the post page does not answer
     * to its id alone. The comments themselves arrive on the page's comment-section token.
     */
    suspend fun communityPost(params: String): Result<CommunityPostPage> =
        runCatching {
            Json
                .parseToJsonElement(
                    innerTube
                        .postCommentsBrowse(client = WEB, params = params)
                        .bodyAsText(),
                ).toCommunityPostPage(FeedItemOwner())
        }

    /** The replies under a community post's comment, which page through the same browse endpoint. */
    suspend fun communityPostReplies(continuation: String): Result<CommunityCommentsPage> =
        runCatching {
            Json
                .parseToJsonElement(
                    innerTube
                        .postCommentsBrowse(client = WEB, continuation = continuation)
                        .bodyAsText(),
                ).toCommunityCommentsPage()
        }

    /**
     * The comment threads under one community post, on the main site.
     *
     * `postId` names the post on the first page; paging carries [continuation] instead. The renderers
     * and the `frameworkUpdates` entities are read by the same parser the video comment section uses.
     */
    suspend fun communityPostComments(
        postId: String? = null,
        params: String? = null,
        continuation: String? = null,
    ): Result<CommunityCommentsPage> =
        runCatching {
            Json
                .parseToJsonElement(
                    innerTube
                        .postCommentsBrowse(
                            client = WEB,
                            postId = postId,
                            params = params,
                            continuation = continuation,
                        ).bodyAsText(),
                ).toCommunityCommentsPage()
        }

    /**
     * YouTube's Timed comment order for the section [continuation] opens, in playback order.
     *
     * The iOS client answers this without a login; the web has no Timed order and Android refuses
     * it, which is why this is the one request that does not use WEB.
     */
    suspend fun timedComments(
        continuation: String,
        ownVideoId: String? = null,
    ): Result<List<Comment>> =
        runCatching {
            Json
                .parseToJsonElement(innerTube.timedComments(continuation).bodyAsText())
                .toTimedComments(ownVideoId)
        }

    /** The continuation that starts a live chat, taken from the watch page's conversation bar. */
    suspend fun liveChatContinuation(videoId: String): Result<String?> =
        runCatching {
            innerTube.nextForLiveChat(videoId).body<LiveChatSeedResponse>().seedContinuation()
        }

    /**
     * One poll of a live chat: the messages posted since [continuation], and the token and interval
     * the next poll should use. Call [liveChatFeed] rather than looping over this by hand.
     */
    suspend fun liveChat(
        continuation: String,
        offsetMs: Long? = null,
    ): Result<GetLiveChatResponse> =
        runCatching {
            innerTube.getLiveChat(continuation, offsetMs).body<GetLiveChatResponse>()
        }

    /**
     * A live chat as a cold flow: each emission is one poll's messages.
     *
     * The loop lives here, in common code, and is driven by the server's own `timeoutMs` through
     * [delay] — a suspending wait, not a platform timer and not a thread of our own, so it cancels
     * with the collector and costs nothing while the chat is closed. The caller owns collection and
     * therefore the lifetime: nothing polls until someone collects, and `offsetMs` advances with
     * each poll so YouTube keeps sending the messages between the last poll and this one.
     */
    fun liveChatFeed(
        continuation: String,
        offsetMs: Long? = null,
    ): Flow<List<GetLiveChatResponse.ChatMessageRenderer>> =
        flow {
            var token = continuation
            var offset = offsetMs
            while (true) {
                val response = liveChat(token, offset).getOrNull() ?: break
                val messages = response.chatMessages()
                if (messages.isNotEmpty()) emit(messages)
                val next = response.nextContinuation() ?: break
                val wait = response.pollIntervalMs()
                if (wait != null) delay(wait)
                offset = offset?.let { it + (wait ?: 0L) }
                token = next
            }
        }

    private fun GetLiveChatResponse.chatMessages(): List<GetLiveChatResponse.ChatMessageRenderer> =
        continuationContents
            ?.liveChatContinuation
            ?.actions
            .orEmpty()
            .mapNotNull { action ->
                val item = action.addChatItemAction?.item ?: return@mapNotNull null
                item.liveChatTextMessageRenderer
                    ?: item.liveChatPaidMessageRenderer
                    ?: item.liveChatMembershipItemRenderer
            }

    private fun GetLiveChatResponse.nextContinuation(): String? =
        continuationContents
            ?.liveChatContinuation
            ?.continuations
            .orEmpty()
            .firstNotNullOfOrNull { it.token() }

    private fun GetLiveChatResponse.pollIntervalMs(): Long? =
        continuationContents
            ?.liveChatContinuation
            ?.continuations
            .orEmpty()
            .firstNotNullOfOrNull { it.timeoutMs() }

    // ---- mutations ----

    suspend fun likeVideo(
        videoId: String,
        like: Boolean,
    ) = runCatching {
        if (like) {
            innerTube.likeVideo(WEB_REMIX, videoId)
        } else {
            innerTube.unlikeVideo(WEB_REMIX, videoId)
        }
    }

    suspend fun subscribeChannel(
        channelId: String,
        subscribe: Boolean,
    ) = runCatching {
        if (subscribe) {
            innerTube.subscribeChannel(WEB_REMIX, channelId)
        } else {
            innerTube.unsubscribeChannel(WEB_REMIX, channelId)
        }
    }

    suspend fun likePlaylist(
        playlistId: String,
        like: Boolean,
    ) = runCatching {
        if (like) {
            innerTube.likePlaylist(WEB_REMIX, playlistId)
        } else {
            innerTube.unlikePlaylist(WEB_REMIX, playlistId)
        }
    }

    suspend fun addToPlaylist(
        playlistId: String,
        videoId: String,
    ) = runCatching { innerTube.addToPlaylist(WEB_REMIX, playlistId, videoId) }

    suspend fun addPlaylistToPlaylist(
        playlistId: String,
        addPlaylistId: String,
    ) = runCatching { innerTube.addPlaylistToPlaylist(WEB_REMIX, playlistId, addPlaylistId) }

    suspend fun removeFromPlaylist(
        playlistId: String,
        videoId: String,
        setVideoId: String,
    ) = runCatching { innerTube.removeFromPlaylist(WEB_REMIX, playlistId, videoId, setVideoId) }

    suspend fun moveSongPlaylist(
        playlistId: String,
        setVideoId: String,
        successorSetVideoId: String?,
    ) = runCatching { innerTube.moveSongPlaylist(WEB_REMIX, playlistId, setVideoId, successorSetVideoId) }

    suspend fun createPlaylist(title: String) = runCatching { innerTube.createPlaylist(WEB_REMIX, title) }

    suspend fun renamePlaylist(
        playlistId: String,
        name: String,
    ) = runCatching { innerTube.renamePlaylist(WEB_REMIX, playlistId, name) }

    suspend fun deletePlaylist(playlistId: String) = runCatching { innerTube.deletePlaylist(WEB_REMIX, playlistId) }

    suspend fun removeThumbnailPlaylist(playlistId: String) = runCatching { innerTube.removeThumbnailPlaylist(WEB_REMIX, playlistId) }

    suspend fun uploadCustomThumbnailLink(contentLength: Int): Result<HttpResponse> =
        runCatching { innerTube.getUploadCustomThumbnailLink(WEB_REMIX, contentLength) }

    suspend fun uploadCustomThumbnail(
        uploadId: String,
        image: ByteArray,
    ): Result<HttpResponse> = runCatching { innerTube.uploadCustomThumbnail(WEB_REMIX, uploadId, image) }

    suspend fun paidPromotion(videoId: String) = runCatching { innerTube.paidPromotion(videoId) }

    suspend fun registerPlayback(
        url: String,
        cpn: String,
        playlistId: String?,
    ) = runCatching { innerTube.registerPlayback(url, cpn, playlistId) }

    const val MAX_GET_QUEUE_SIZE = 1000
}
