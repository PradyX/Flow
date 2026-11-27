package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.data.model.LiveChatMessage
import io.github.aedev.flow.data.model.LiveChatMessageType
import io.github.aedev.flow.data.model.LiveChatSegment
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.models.AlbumItem
import io.github.aedev.flow.innertube.models.ArtistItem
import io.github.aedev.flow.innertube.models.PlaylistItem
import io.github.aedev.flow.innertube.models.SongItem
import io.github.aedev.flow.innertube.models.response.GetLiveChatResponse
import io.github.aedev.flow.innertube.models.response.WatchMetadataResponse
import io.github.aedev.flow.innertube.models.response.WatchMetadataResponse.CompactVideo
import io.github.aedev.flow.innertube.network.countryDisplayName
import io.github.aedev.flow.innertube.pages.ArtistSectionKind
import io.github.aedev.flow.innertube.pages.HomePage
import io.github.aedev.flow.innertube.pages.VideoCommentsSection
import io.github.aedev.flow.innertube.pages.channel.ChannelFilterGroup
import io.github.aedev.flow.innertube.pages.channel.ChannelLink
import io.github.aedev.flow.innertube.pages.channel.ChannelTabKind
import io.github.aedev.flow.innertube.pages.player.PlayableStream
import io.github.aedev.flow.innertube.pages.reel.ReelParams
import io.github.aedev.flow.innertube.pages.reel.reelPosterUrl
import io.github.aedev.flow.innertube.pages.renderer.CommunityPost
import io.github.aedev.flow.innertube.pages.renderer.FeedItem
import io.github.aedev.flow.innertube.pages.renderer.FeedShelf
import io.github.aedev.flow.innertube.pages.renderer.FeedShelfStyle
import io.github.aedev.flow.innertube.utils.parseTime
import io.github.aedev.flow.utils.formatViewCount
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.random.Random

/**
 * A comment the watch page shows. [likes] is YouTube's own label ("73K"), so a large count reads
 * the way it does in the app; [isUploader] marks the channel that owns the video.
 */
data class FeedComment(
    val id: String,
    val author: String,
    val authorThumbnailUrl: String?,
    val text: String,
    val likes: String?,
    val replyCount: Int,
    val published: String?,
    val isPinned: Boolean = false,
    /** YouTube's own badge line on a pinned comment, such as "Pinned by @Queen". */
    val pinnedBy: String? = null,
    val isUploader: Boolean = false,
    /** The token that loads this comment's replies; null when YouTube offers no reply thread. */
    val replyToken: String? = null,
)

/**
 * The player's metadata, already flattened out of the watch response. [related] is the rail under
 * the description, in YouTube's own order and never containing the video being watched;
 * [comments] is the first page under that rail, in YouTube's own order.
 */
data class VideoDetail(
    val videoId: String,
    val title: String,
    val channel: String?,
    val channelId: String?,
    val channelAvatarUrl: String?,
    /** The owner's subscriber label ("1.2M subscribers"), drawn under the channel name. */
    val subscribers: String?,
    val views: String?,
    val likes: String?,
    val published: String?,
    val description: String?,
    val related: List<FeedEntry> = emptyList(),
    val comments: List<FeedComment> = emptyList(),
    /** The figure beside the comments heading, as YouTube prints it ("2.4M"). */
    val commentCountText: String? = null,
    /** The live stream's chat token, read from the same watch page; null for a normal video. */
    val liveChatToken: String? = null,
)

fun WatchMetadataResponse.toVideoDetail(videoId: String): VideoDetail =
    VideoDetail(
        videoId = videoId,
        title = title().orEmpty(),
        channel = channelName(),
        channelId = channelId(),
        channelAvatarUrl = channelAvatarUrl(),
        subscribers = subscriberCountText(),
        views = viewCountText(),
        likes = likeLabel(likeCountText()),
        published = relativeUploadDate() ?: uploadDate(),
        description = description(),
        related = relatedVideos().mapNotNull { it.toRelatedEntry() }.filterNot { it.videoId == videoId },
    )

/**
 * A related card in the same [FeedEntry] shape the feeds use, so one card composable draws both.
 * The byline, view count and age are three separate parts of a related lockup, not one string, so
 * they are joined here the way the app's cards join them.
 */
private fun CompactVideo.toRelatedEntry(): FeedEntry? {
    val id = videoId?.takeIf { it.isNotBlank() } ?: return null
    val entryTitle = title?.text()?.takeIf { it.isNotBlank() } ?: return null
    return FeedEntry(
        id = id,
        title = entryTitle,
        subtitle =
            listOfNotNull(
                longBylineText?.text()?.takeIf { it.isNotBlank() },
                viewCountText?.text()?.takeIf { it.isNotBlank() },
                publishedTimeText?.text()?.takeIf { it.isNotBlank() },
            ).joinToString(" · ").takeIf { it.isNotBlank() },
        videoId = id,
        durationSeconds = lengthText?.text()?.parseTime()?.takeIf { it > 0 },
        thumbnailUrl = thumbnail?.bestUrl(),
    )
}

/**
 * A channel page: its header, the tabs YouTube published, and the first page of the Videos tab.
 * The About panel's fields come from the landing fetch's second request, which the header merges.
 */
data class ChannelFeed(
    val channelId: String,
    val name: String,
    val handle: String?,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val subscribers: String?,
    val description: String?,
    val videos: List<FeedEntry> = emptyList(),
    /** The Videos tab's next-page token, so the tab the landing opened can page without a re-request. */
    val videosContinuation: String? = null,
    val tabs: List<ChannelTabSpec> = emptyList(),
    val filters: List<ChannelFilterGroup> = emptyList(),
    val videoCount: String? = null,
    val viewCount: String? = null,
    val joinedDate: String? = null,
    val country: String? = null,
    val canonicalUrl: String? = null,
    val links: List<ChannelLink> = emptyList(),
)

/** One shelf of a channel tab: a release's heading and the songs under it. */
data class ChannelShelf(
    val title: String,
    val entries: List<FeedEntry>,
    /** The release's playlist, which its heading opens; null when the shelf is not a playlist. */
    val playlistId: String? = null,
    /** The channel's own posts: the Home "Posts" carousel is post renderers, not cards. */
    val posts: List<CommunityPost> = emptyList(),
    /** Home's featured video, which sits above the shelves instead of in a row of its own. */
    val isFeatured: Boolean = false,
)

/**
 * One page of one channel tab: its cards, the filter bar above them, and any next-page token. A
 * shelf tab fills [sections], the Posts tab fills [posts], and a grid tab fills [entries].
 */
data class ChannelTabPage(
    val entries: List<FeedEntry> = emptyList(),
    val posts: List<CommunityPost> = emptyList(),
    val sections: List<ChannelShelf> = emptyList(),
    val filters: List<ChannelFilterGroup> = emptyList(),
    val continuation: String? = null,
) {
    /** The next page of the same tab, appended under what is already shown. */
    fun more(next: ChannelTabPage): ChannelTabPage =
        ChannelTabPage(
            entries = (entries + next.entries).distinctBy { it.id },
            posts = (posts + next.posts).distinctBy { it.id },
            sections = sections + next.sections,
            filters = filters,
            continuation = next.continuation,
        )
}

/** A shelf page's carousels as the feed's shelves, dropping any the parser read no cards from. */
internal fun List<FeedShelf>.toChannelShelves(): List<ChannelShelf> =
    mapNotNull { shelf ->
        val entries = shelf.items.mapNotNull { it.toFeedEntry() }.distinctBy { it.id }
        val posts =
            shelf.items
                .filterIsInstance<FeedItem.PostItem>()
                .map { it.post }
                .distinctBy { it.id }
        if (entries.isEmpty() && posts.isEmpty()) {
            null
        } else {
            ChannelShelf(
                title = shelf.title.orEmpty(),
                entries = entries,
                // A shelf heading is a link only when it names a playlist; a video shelf's endpoint
                // is the channel's own tab, and an album's audio playlist (`OLAK5uy_…`) has no
                // WEB_REMIX page (both `VL<id>` and `<id>` answer with empty `contents`), so
                // neither is offered as one.
                playlistId = shelf.morePlaylistId.toPlaylistId(),
                posts = posts,
                isFeatured = shelf.style == FeedShelfStyle.Trailer,
            )
        }
    }

/** A shelf endpoint's browse id as a playlist id, or null when it is not a playlist. */
private fun String?.toPlaylistId(): String? {
    val id = this?.removePrefix("VL") ?: return null
    return id.takeIf { it.startsWith("PL") || it.startsWith("RD") }
}

/** A playlist page: its header and the first page of songs, as the player lists open them. */
data class PlaylistFeed(
    val playlistId: String,
    val title: String,
    val subtitle: String?,
    val thumbnailUrl: String?,
    val entries: List<FeedEntry>,
    /** "Album", "Single", "EP" — the page's own label for what it is, when it has one. */
    val releaseType: String? = null,
    /** The page's year, song count and length line, e.g. "2026 · 12 songs · 35 minutes". */
    val details: String? = null,
    /** True when YouTube marks the release explicit. */
    val explicit: Boolean = false,
    /** The artist's browse id, which its header line opens. */
    val authorId: String? = null,
    /** The token the next page of songs opens from; null when the whole playlist was served. */
    val continuation: String? = null,
)

/** One shelf of an artist page: its heading and cards; a ranked song shelf is [isList]. */
data class ArtistShelf(
    val title: String,
    val entries: List<FeedEntry>,
    val isList: Boolean = false,
)

/** An artist page: its header and its shelves. */
data class ArtistFeed(
    val artistId: String,
    val name: String,
    val thumbnailUrl: String?,
    val subscriberText: String?,
    val monthlyListenersText: String?,
    val description: String?,
    val shelves: List<ArtistShelf>,
    /** The artist's channel id: the share link's id and the subscribe target. */
    val channelId: String?,
    val isSubscribed: Boolean,
)

/**
 * The artist page, requested the way the app requests it: a WEB_REMIX browse of the artist's id,
 * whose immersive header carries the photo, name, description and audience counts.
 */
suspend fun loadArtist(browseId: String): Result<ArtistFeed> =
    YouTube.artist(browseId).mapCatching { page ->
        ArtistFeed(
            artistId = page.artist.id.ifBlank { browseId },
            name = page.artist.title,
            thumbnailUrl = page.artist.thumbnail,
            subscriberText = page.subscriberCountText,
            monthlyListenersText = page.monthlyListenersText,
            description = page.description,
            channelId = page.channelId,
            isSubscribed = page.isSubscribed,
            shelves =
                page.sections.mapNotNull { section ->
                    section.items
                        .map { it.toMusicEntry() }
                        .takeIf { it.isNotEmpty() }
                        ?.let { entries ->
                            ArtistShelf(
                                title = section.title,
                                entries = entries,
                                isList =
                                    section.kind == ArtistSectionKind.TOP_SONGS ||
                                        section.kind == ArtistSectionKind.VIDEOS,
                            )
                        }
                },
        )
    }

/**
 * The playlist, requested as the reference app requests it: a signed-in WEB_REMIX browse of
 * `VL<playlistId>`. Songs keep their own ids as video ids, so an entry opens the same player a
 * search result does.
 */
suspend fun loadPlaylist(playlistId: String): Result<PlaylistFeed> =
    YouTube.playlist(playlistId).mapCatching { page ->
        PlaylistFeed(
            playlistId = playlistId,
            title = page.playlist.title,
            subtitle =
                page.playlist.author
                    ?.name
                    ?.takeIf { it.isNotBlank() },
            releaseType = page.releaseType,
            details = page.details,
            explicit = page.explicit,
            authorId = page.playlist.author?.id,
            thumbnailUrl = page.playlist.thumbnail,
            entries = page.songs.map(::songEntry),
            continuation = page.songsContinuation ?: page.continuation,
        )
    }

/** The next page of a playlist's songs, and the token the page after it opens from. */
data class PlaylistSongsPage(
    val entries: List<FeedEntry>,
    val continuation: String?,
)

/** One page of a playlist's songs from the continuation the page was served with. */
suspend fun loadPlaylistSongs(continuation: String): Result<PlaylistSongsPage> =
    YouTube.playlistContinuation(continuation).mapCatching { page ->
        PlaylistSongsPage(entries = page.songs.map(::songEntry), continuation = page.continuation)
    }

private fun songEntry(song: SongItem): FeedEntry =
    FeedEntry(
        id = song.id,
        title = song.title,
        subtitle = song.artists.joinToString(", ") { it.name }.takeIf { it.isNotBlank() },
        videoId = song.id,
        durationSeconds = song.duration,
        thumbnailUrl = song.thumbnail,
        explicit = song.explicit,
    )

/** The library's landing page: its items, the token the next page opens from, and YouTube's own
 * sign-in notice when the request carried no account. */
data class LibraryFeed(
    val entries: List<FeedEntry>,
    val continuation: String? = null,
    val notice: String? = null,
    val noticeSubtext: String? = null,
)

/** The browse id the reference app's library request uses. */
private const val LIBRARY_BROWSE_ID = "FEmusic_library_landing"

/**
 * The library, requested exactly as the reference app requests it: a WEB_REMIX browse of the
 * library landing id with the signed-in cookies attached. Signed out, YouTube answers with its
 * sign-in message instead of a grid, which lands in [LibraryFeed.notice]; the parsed items are then
 * empty and nothing throws.
 */
suspend fun loadLibrary(): Result<LibraryFeed> =
    YouTube.library(LIBRARY_BROWSE_ID).mapCatching { page ->
        LibraryFeed(
            entries = page.items.map { it.toFeedEntry() },
            continuation = page.continuation,
            notice = page.notice,
            noticeSubtext = page.noticeSubtext,
        )
    }

/**
 * The body a music shelf takes. It follows what the shelf is made of, not its heading: a shelf of
 * mixes reads as tiles, a shelf of portraits as portraits, and anything a response supplies as
 * cards — which is also how a chip's own page draws the list rows it arrives with.
 */
enum class MusicSectionShape {
    /** Dense tiles with a square artwork and two lines, three rows deep, scrolled sideways. */
    DenseTiles,

    /** Portraits in the artist shape, a centred name under each. */
    ArtistPortraits,

    /** Square cards, two lines under the artwork. */
    CollectionCards,

    /** Mood and genre buttons: the shelf of destinations the page carries beside its carousels. */
    MoodButtons,
}

/** One shelf of the music tab: its heading, its entries and the body they are drawn in. */
data class MusicSection(
    val title: String,
    val entries: List<FeedEntry> = emptyList(),
    val shape: MusicSectionShape = MusicSectionShape.CollectionCards,
    /** The buttons a [MusicSectionShape.MoodButtons] shelf draws, in the index's own order. */
    val moods: List<MoodGenre> = emptyList(),
)

/** One of the music page's own filter chips; choosing it asks for the page it names. */
data class MusicChip(
    val title: String,
    val browseId: String?,
    val params: String?,
)

/** The music tab: YouTube Music's own chips and shelves, in the order its response sends them. */
data class MusicHomeFeed(
    val chips: List<MusicChip> = emptyList(),
    val sections: List<MusicSection>,
    val continuation: String? = null,
) {
    /** The next page's shelves under the ones already shown, with its own token to continue from. */
    fun more(next: MusicHomeFeed): MusicHomeFeed = copy(sections = sections + next.sections, continuation = next.continuation)
}

/** A response's carousels as shelves, dropping any the parser cannot read rather than showing it empty. */
internal fun HomePage.toMusicSections(): List<MusicSection> = sections.mapNotNull { it.toMusicSection() }

private fun HomePage.Section.toMusicSection(): MusicSection? {
    val entries = items.map { it.toMusicEntry() }
    if (entries.isEmpty()) return null
    val shape =
        when {
            items.all { it is ArtistItem } -> MusicSectionShape.ArtistPortraits
            items.all { it is AlbumItem } || listRows -> MusicSectionShape.CollectionCards
            else -> MusicSectionShape.DenseTiles
        }
    return MusicSection(title = title, entries = entries, shape = shape)
}

/**
 * A browse page's carousels as shelves, the way the app's own browse page draws them: a lane of
 * square collection cards, whose artist entries take the artist shape with a centred name.
 *
 * The app reads a browse response through `YouTube.browse`, which keeps only the two-row entries of
 * each carousel (app/src/main/java/io/github/aedev/flow/innertube/YouTube.kt:1883-1886) and drops
 * every responsive list item, so a songs carousel reaches its page as an empty shelf and is left
 * out (app/src/main/java/io/github/aedev/flow/ui/screens/music/YouTubeBrowseViewModel.kt:49-56).
 * This keeps the same entries, so a mood shows the shelves the app shows and nothing extra.
 */
internal fun HomePage.toBrowseMusicSections(): List<MusicSection> = sections.mapNotNull { it.toBrowseMusicSection() }

private fun HomePage.Section.toBrowseMusicSection(): MusicSection? {
    val entries =
        items
            .filterNot { it is SongItem }
            .map { it.toMusicEntry() }
            .distinctBy { it.id }
    if (entries.isEmpty()) return null
    return MusicSection(title = title, entries = entries, shape = MusicSectionShape.CollectionCards)
}

/** The heading the app gives its mood and genre shelf. */
private const val MOODS_SHELF_TITLE = "Mood and Genres"

/** The three shelves the app names itself, in the headings it gives them. */
private const val NEW_RELEASES_TITLE = "New releases"
private const val COMMUNITY_PLAYLISTS_TITLE = "Trending community playlists"
private const val ARTISTS_FOR_YOU_TITLE = "Artists for you"

/**
 * The slots the app's music page plans, in the order it lists them before a session moves them
 * (`HomeSectionType` in the app, whose declaration order is this order).
 */
internal enum class MusicHomeSlot {
    DAILY_DISCOVER,
    QUICK_PICKS,
    FROM_COMMUNITY,
    RECOMMENDED,
    SIMILAR_TO,
    LIVE_PERFORMANCES,
    MUSIC_VIDEOS_FOR_YOU,
    MUSIC_VIDEOS,
    GENRES,
    DYNAMIC_HOME,
    TOP_ALBUMS,
    FAVORITE_ARTIST_ALBUMS,
    NEW_RELEASES,
    CHARTS,
    POPULAR_ARTISTS,
    MIXED_FOR_YOU,
    MOODS_AND_GENRES,
}

/**
 * How much the app's recommendation brain knows about the listener. This port keeps no listening
 * model at all, so its brain is always at the state the app starts from, and its plan always takes
 * the `cold_start` anchors: charts, then the mood shelf, then new releases.
 */
internal const val MUSIC_BRAIN_COLD_START = "cold_start"

/**
 * The app's per-session plan: three slots anchored by how much its brain knows about the listener,
 * then every other slot in its listed order, shuffled with the session's own seed. A seed and a
 * maturity always give the same plan, which is what lets two pages of one session agree.
 */
internal fun musicSectionPlan(
    seed: Long,
    maturity: String? = null,
): List<MusicHomeSlot> {
    val anchored =
        when (maturity) {
            "cold_start" -> {
                listOf(MusicHomeSlot.CHARTS, MusicHomeSlot.MOODS_AND_GENRES, MusicHomeSlot.NEW_RELEASES)
            }

            "mature" -> {
                listOf(MusicHomeSlot.QUICK_PICKS, MusicHomeSlot.SIMILAR_TO, MusicHomeSlot.FROM_COMMUNITY)
            }

            else -> {
                listOf(MusicHomeSlot.QUICK_PICKS, MusicHomeSlot.FROM_COMMUNITY, MusicHomeSlot.DAILY_DISCOVER)
            }
        }
    val pool = MusicHomeSlot.entries - anchored.toSet()
    return anchored + pool.shuffled(Random(seed))
}

/**
 * The app's mood and genre shelf: every button its index offers, once each, in the index's order.
 * The app draws it as one of the page's shelves and opens the full index from its heading.
 */
private fun moodsShelf(moods: List<MoodGenre>): List<MusicSection> =
    if (moods.isEmpty()) {
        emptyList()
    } else {
        listOf(MusicSection(title = MOODS_SHELF_TITLE, shape = MusicSectionShape.MoodButtons, moods = moods))
    }

/**
 * The page's shelves in the app's planned order. The app draws its own shelves per slot and the
 * response's shelves together inside one slot, so the response's shelves stay in its own order and
 * the plan decides whether they come before or after the new releases, the community playlists and
 * the mood shelf. A slot the port has no shelves for draws nothing, and the artists shelf the app
 * always draws ahead of the plan stays ahead of it.
 */
internal fun plannedMusicSections(
    response: List<MusicSection>,
    newReleases: MusicSection?,
    communityPlaylists: MusicSection?,
    artistsForYou: MusicSection?,
    moods: List<MusicSection>,
    seed: Long,
    maturity: String?,
    charts: List<MusicSection> = emptyList(),
): List<MusicSection> {
    val dynamic =
        response.filterNot { section ->
            section.title.equals(NEW_RELEASES_TITLE, ignoreCase = true) ||
                section.title.equals(COMMUNITY_PLAYLISTS_TITLE, ignoreCase = true) ||
                section.title.equals(ARTISTS_FOR_YOU_TITLE, ignoreCase = true)
        }
    val planned =
        musicSectionPlan(seed, maturity).flatMap { slot ->
            when (slot) {
                MusicHomeSlot.DYNAMIC_HOME -> dynamic
                MusicHomeSlot.NEW_RELEASES -> listOfNotNull(newReleases)
                MusicHomeSlot.FROM_COMMUNITY -> listOfNotNull(communityPlaylists)
                MusicHomeSlot.MOODS_AND_GENRES -> moods
                MusicHomeSlot.CHARTS -> charts
                else -> emptyList()
            }
        }
    return listOfNotNull(artistsForYou) + planned
}

/**
 * The Charts slot: the ranked playlists and the ranked artists YouTube Music's chart page holds
 * for this region, under the headings the app gives them. The page's own song lane is a ranked
 * list the music tab has no body for, so only its cards are carried.
 */
internal suspend fun loadMusicCharts(): List<MusicSection> =
    YouTube
        .charts(country = YouTube.locale.gl)
        .mapCatching { page ->
            val items = page.sections.flatMap { it.items }
            val playlists =
                items
                    .filterIsInstance<PlaylistItem>()
                    .map { it.toMusicEntry() }
                    .distinctBy { it.id }
            val artists =
                items
                    .filterIsInstance<ArtistItem>()
                    .map { it.toMusicEntry() }
                    .distinctBy { it.id }
            listOfNotNull(
                playlists
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        MusicSection(
                            title = chartsTitle(page.countryCode),
                            entries = it,
                        )
                    },
                artists
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        MusicSection(
                            title = topChartArtistsTitle(page.countryCode),
                            entries = it,
                            shape = MusicSectionShape.ArtistPortraits,
                        )
                    },
            )
        }.getOrElse { emptyList() }

/** "Charts in <region>", or the worldwide heading when the page charts no region of its own. */
internal fun chartsTitle(countryCode: String?): String =
    countryCode
        ?.takeIf { it.isNotBlank() }
        ?.let { "Charts in ${countryDisplayName(it)}" }
        ?: "Global charts"

/** "Top artists in <region>", or the worldwide heading when the page charts no region. */
internal fun topChartArtistsTitle(countryCode: String?): String =
    countryCode
        ?.takeIf { it.isNotBlank() }
        ?.let { "Top artists in ${countryDisplayName(it)}" }
        ?: "Top artists worldwide"

/** A shelf the response did not carry, read from YouTube's own music search for it. */
private suspend fun seededMusicShelf(
    heading: String,
    query: String,
    filter: YouTube.SearchFilter,
    shape: MusicSectionShape,
): MusicSection? =
    YouTube
        .search(query, filter)
        .getOrNull()
        ?.items
        ?.map { it.toMusicEntry() }
        ?.takeIf { it.isNotEmpty() }
        ?.let { MusicSection(title = heading, entries = it, shape = shape) }

/**
 * The music tab: YouTube Music's home shelves in the app's planned order for this session, plus the
 * next page when the response carries a token for it. The plan is the app's own (see
 * [musicSectionPlan]); a shelf the app names itself and the live page omits is asked for by name
 * through the music search, and anything else is left out rather than invented.
 */
suspend fun loadMusicHome(
    chip: MusicChip? = null,
    continuation: String? = null,
    seed: Long = System.currentTimeMillis(),
    maturity: String? = MUSIC_BRAIN_COLD_START,
): Result<MusicHomeFeed> =
    runCatching {
        if (continuation != null) {
            val page = YouTube.home(continuation = continuation).getOrThrow()
            return@runCatching MusicHomeFeed(sections = page.toMusicSections(), continuation = page.continuation)
        }

        // A chosen chip is a browse of the page it names, so the shelves under it are that page's.
        val chipBrowseId = chip?.browseId
        if (chipBrowseId != null) {
            val page = YouTube.home(browseId = chipBrowseId, params = chip.params).getOrThrow()
            return@runCatching MusicHomeFeed(
                chips = page.chips.orEmpty().mapNotNull { it.toMusicChip() },
                sections = page.toMusicSections(),
                continuation = page.continuation,
            )
        }

        val page = YouTube.home().getOrThrow()
        val sections = page.toMusicSections()
        val carried = { heading: String -> sections.firstOrNull { it.title.equals(heading, ignoreCase = true) } }
        val newReleases =
            carried(NEW_RELEASES_TITLE) ?: seededMusicShelf(
                heading = NEW_RELEASES_TITLE,
                query = NEW_RELEASES_TITLE.lowercase(),
                filter = YouTube.SearchFilter.FILTER_ALBUM,
                shape = MusicSectionShape.CollectionCards,
            )
        val communityPlaylists =
            carried(COMMUNITY_PLAYLISTS_TITLE) ?: seededMusicShelf(
                heading = COMMUNITY_PLAYLISTS_TITLE,
                query = COMMUNITY_PLAYLISTS_TITLE.lowercase(),
                filter = YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST,
                shape = MusicSectionShape.DenseTiles,
            )
        val artistsForYou =
            carried(ARTISTS_FOR_YOU_TITLE) ?: seededMusicShelf(
                heading = ARTISTS_FOR_YOU_TITLE,
                query = "popular artists",
                filter = YouTube.SearchFilter.FILTER_ARTIST,
                shape = MusicSectionShape.ArtistPortraits,
            )
        val moods =
            loadMoodGroups()
                .getOrNull()
                .orEmpty()
                .flatMap { group -> group.genres }
                .distinctBy { it.id }
        val charts =
            if (MusicHomeSlot.CHARTS in musicSectionPlan(seed, maturity)) {
                loadMusicCharts()
            } else {
                emptyList()
            }
        MusicHomeFeed(
            chips = page.chips.orEmpty().mapNotNull { it.toMusicChip() },
            sections =
                plannedMusicSections(
                    response = sections,
                    newReleases = newReleases,
                    communityPlaylists = communityPlaylists,
                    artistsForYou = artistsForYou,
                    moods = moodsShelf(moods),
                    seed = seed,
                    maturity = maturity,
                    charts = charts,
                ),
            continuation = page.continuation,
        )
    }

/** A browse chip as the page renders it; one that names no page is a label, so it is dropped. */
internal fun HomePage.Chip.toMusicChip(): MusicChip? {
    val browseId = endpoint?.browseId ?: return null
    return MusicChip(title = title, browseId = browseId, params = endpoint.params)
}

/**
 * A channel from its landing response, then the first page of its Videos tab. The landing response
 * names the tab by token; the tab's own request is what carries the grid.
 */
suspend fun loadChannel(
    channelId: String,
    aboutTitle: String = "About",
): Result<ChannelFeed> =
    YouTube.channel(channelId).mapCatching { page ->
        val header = page.header
        val videosTab =
            page.tabs.firstOrNull { it.kind == ChannelTabKind.Videos }
                ?: page.tabs.firstOrNull { it.title == "Videos" }
        val videosPage =
            videosTab
                ?.params
                ?.let { params -> YouTube.channelTab(channelId, params, kind = ChannelTabKind.Videos).getOrNull() }
        ChannelFeed(
            channelId = header.id.ifBlank { channelId },
            name = header.title,
            handle = header.handle,
            avatarUrl = header.avatarUrl.takeIf { it.isNotBlank() },
            bannerUrl = header.bannerUrl,
            subscribers = header.subscriberCountText,
            description = header.description,
            videos = videosPage?.items.orEmpty().mapNotNull { it.toFeedEntry() },
            videosContinuation = videosPage?.continuation,
            tabs = channelTabs(page.tabs, header, aboutTitle),
            filters = videosPage?.filters.orEmpty(),
            videoCount = header.videoCountText,
            viewCount = header.viewCountText,
            joinedDate = header.joinedDateText,
            country = header.countryText,
            canonicalUrl = header.canonicalUrl,
            links = header.links,
        )
    }

/**
 * One channel tab's page. With no pick the tab is requested by its own params; a picked sort or
 * filter re-requests the tab through the token that option carries. A sort switch is answered with
 * the same filter bar, so a response that omits it keeps the one already on screen.
 */
suspend fun loadChannelTab(
    channelId: String,
    params: String?,
    kind: ChannelTabKind,
    filters: List<ChannelFilterGroup> = emptyList(),
    picks: List<Int> = emptyList(),
): Result<ChannelTabPage> =
    runCatching {
        val request = channelTabFilterRequest(filters, picks)
        val content =
            when (request) {
                is ChannelFilterRequest.Params -> {
                    YouTube.channelTab(channelId, request.token, kind = kind).getOrThrow()
                }

                is ChannelFilterRequest.Continuation -> {
                    YouTube.channelTabContinuation(channelId, request.token, kind).getOrThrow()
                }

                null -> {
                    YouTube
                        .channelTab(
                            channelId,
                            params ?: kind.defaultParams ?: error("The tab carried no token"),
                            kind = kind,
                        ).getOrThrow()
                }
            }
        ChannelTabPage(
            entries = content.items.mapNotNull { it.toFeedEntry() }.distinctBy { it.id },
            posts = content.posts,
            sections = content.sections.toChannelShelves(),
            filters = content.filters.ifEmpty { filters },
            continuation = content.continuation,
        )
    }

/** The next page of a channel tab: the same grid, read through the token the last page ended on. */
suspend fun loadChannelTabMore(
    channelId: String,
    kind: ChannelTabKind,
    continuation: String,
): Result<ChannelTabPage> =
    runCatching {
        val content = YouTube.channelTabContinuation(channelId, continuation, kind).getOrThrow()
        ChannelTabPage(
            entries = content.items.mapNotNull { it.toFeedEntry() }.distinctBy { it.id },
            posts = content.posts,
            sections = content.sections.toChannelShelves(),
            continuation = content.continuation,
        )
    }

/**
 * One short as the reel feed shows it. The sequence names the reel and its still; the overlay is
 * what carries the title, the channel and the counts, because no sequence entry holds them.
 */
data class Short(
    val videoId: String,
    val title: String,
    val channelName: String?,
    val channelId: String?,
    val channelAvatarUrl: String? = null,
    val stillUrl: String,
    val likeCountText: String? = null,
    val commentCountText: String? = null,
    /** The watch page's own view count, "24M views". */
    val viewCountText: String? = null,
    /** "1 month ago", the age the watch page prints beside the views. */
    val publishedTimeText: String? = null,
    /** The track the reel plays, as its overlay names it. */
    val soundTitle: String? = null,
    val params: String? = null,
)

/** The reels to swipe through, plus the token the next page of the sequence opens from. */
data class ShortsFeed(
    val shorts: List<Short>,
    val continuation: String? = null,
)

/**
 * The Shorts feed: the seedless reel sequence's first page (one entry and the continuation of the
 * rest), then that continuation, then one overlay per reel for the title and channel. The same two
 * requests the app makes, in the same order; [limit] only bounds how many overlays are fetched.
 */
suspend fun loadShorts(limit: Int = 3): Result<ShortsFeed> =
    runCatching {
        val first = YouTube.reelSequence(ReelParams.INITIAL_SEQUENCE).getOrThrow()
        val entries = first.entries.toMutableList()
        var continuation = first.continuation
        if (entries.size < limit && continuation != null) {
            YouTube.reelSequence(continuation).getOrNull()?.let { more ->
                entries += more.entries
                continuation = more.continuation ?: continuation
            }
        }
        val picked = entries.distinctBy { it.videoId }.take(limit)
        // One overlay and one watch page per reel, in flight together: the overlay names the
        // channel and the counts, the watch page is where the views and the age live.
        val shorts =
            coroutineScope {
                picked
                    .map { entry ->
                        async {
                            val overlay = YouTube.reelOverlay(entry.videoId).getOrNull()
                            val metadata = YouTube.watchMetadataLite(entry.videoId).getOrNull()
                            Short(
                                videoId = entry.videoId,
                                title = overlay?.title.orEmpty(),
                                // The watch page names the channel; the overlay's text is the
                                // handle, so it is only the fallback when the page has no owner.
                                channelName = metadata?.channelName()?.takeIf { it.isNotBlank() } ?: overlay?.channelName,
                                channelId = overlay?.channelId ?: metadata?.channelId(),
                                channelAvatarUrl =
                                    metadata?.channelAvatarUrl()?.takeIf { it.isNotBlank() }
                                        ?: overlay?.channelAvatarUrl,
                                // The sequence's own still is the reel's frame, at the reel's aspect ratio.
                                stillUrl = entry.posterUrl ?: reelPosterUrl(entry.videoId),
                                likeCountText = overlay?.likeCountText,
                                commentCountText = overlay?.commentCountText,
                                viewCountText = metadata?.viewCountText()?.takeIf { it.isNotBlank() },
                                publishedTimeText = metadata?.relativeUploadDate(),
                                soundTitle = overlay?.soundTitle?.takeIf { it.isNotBlank() },
                                params = entry.params,
                            )
                        }
                    }.awaitAll()
            }
        ShortsFeed(shorts = shorts, continuation = continuation)
    }

/**
 * The like button's label. YouTube often serves the count only inside the spoken sentence ("like
 * this video along with 2,142,866 other people"), so the number is taken out of the text and shown
 * the way the app shows a like count.
 */
private fun likeLabel(text: String?): String? {
    val raw = text?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (!raw.any(Char::isLetter)) return raw
    val count =
        Regex("\\d[\\d,]*")
            .find(raw)
            ?.value
            ?.replace(",", "")
            ?.toLongOrNull() ?: return null
    return formatViewCount(count)
}

/** YouTube's public poster for a video id, for when the list that opened the player had none. */
fun videoPosterUrl(videoId: String): String = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

/** The watch page a share or a "copy link" hands out. */
fun videoWatchUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"

/**
 * The three loads the screens perform. They are plain suspend functions rather than ViewModel
 * methods so the navigation test drives exactly the code the composables run.
 */
suspend fun loadSearchFeed(
    query: String,
    params: String? = null,
): Result<SearchFeed> = YouTube.videoSearch(query = query, params = params).map { it.toSearchFeed() }

/**
 * The page behind a filter chip. YouTube names the filter in the token rather than in `params`, and
 * the answer is a first page again, so it replaces the results the chip was shown over.
 */
suspend fun loadSearchContinuation(continuation: String): Result<SearchFeed> =
    YouTube.videoSearch(continuation = continuation).map { it.toSearchFeed() }

/** The next page of results, from the token the first page was served with. */
suspend fun loadSearchMore(continuation: String): Result<SearchFeed> =
    YouTube.videoSearch(continuation = continuation).map { it.toSearchFeed() }

/**
 * What YouTube suggests while a term is being typed: its own completion list, trimmed and without
 * repeats, so the panel is the server's ranking rather than a local guess.
 */
suspend fun loadSearchSuggestions(query: String): Result<List<String>> =
    YouTube.searchSuggestions(query).mapCatching { it.queries.toSearchQueries() }

internal fun List<String>.toSearchQueries(limit: Int = SUGGESTION_LIMIT): List<String> =
    map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
        .take(limit)

private const val SUGGESTION_LIMIT = 8

private val WatchJson =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

/**
 * The watch page: metadata and the related rail out of the first response, then the comment
 * section's own first page, because YouTube only puts its token in the watch response.
 */
suspend fun loadVideoDetail(videoId: String): Result<VideoDetail> =
    YouTube.watchNext(videoId).mapCatching { watch ->
        val detail = WatchJson.decodeFromJsonElement<WatchMetadataResponse>(watch).toVideoDetail(videoId)
        val section = YouTube.commentsSection(watch)
        // A closed section is the uploader's choice, not a failure; the page simply has no comments.
        val page =
            (section as? VideoCommentsSection.Open)
                ?.let { YouTube.comments(it.token, videoId).getOrNull() }
        detail.copy(
            comments = page?.comments.orEmpty().map { it.toFeedComment() },
            commentCountText = page?.totalText ?: YouTube.commentsPanelHeader(watch)?.totalText,
            liveChatToken = YouTube.liveChatToken(watch),
        )
    }

internal fun io.github.aedev.flow.data.model.Comment.toFeedComment(): FeedComment =
    FeedComment(
        id = id,
        author = author,
        authorThumbnailUrl = authorThumbnail.takeIf { it.isNotBlank() },
        text = text,
        likes = likeCountText.takeIf { it.isNotBlank() } ?: likeCount.takeIf { it > 0 }?.toLong()?.let(::formatViewCount),
        replyCount = replyCount,
        published = publishedTime.takeIf { it.isNotBlank() },
        isPinned = isPinned,
        pinnedBy = pinnedByText?.takeIf { it.isNotBlank() },
        isUploader = isCreator,
        replyToken = continuationToken,
    )

/**
 * The progressive stream for the player's surface. Resolution is separate from the watch page: the
 * watch page describes the video, this is what actually plays, and only Android has an engine.
 */
suspend fun loadPlayableStream(videoId: String): Result<PlayableStream?> = YouTube.playableStream(videoId)

/**
 * A live stream's chat as a cold flow: one emission per poll, in YouTube's order, mapped to the same
 * [LiveChatMessage] the reference app renders. The poll loop and its server-driven interval live in
 * [YouTube.liveChatFeed]; collecting is what keeps them running, so the chat stops polling the
 * moment the player leaves composition.
 */
fun liveChatMessages(
    token: String,
    offsetMs: Long = 0,
): Flow<List<LiveChatMessage>> = YouTube.liveChatFeed(token, offsetMs).map { batch -> batch.mapNotNull { it.toChatMessage() } }

/**
 * One chat item in the shape the UI draws. The server's ARGB super-chat colours are kept in the
 * response but not applied: the row uses theme tokens, the port's rule for every colour.
 */
private fun GetLiveChatResponse.ChatMessageRenderer.toChatMessage(): LiveChatMessage? {
    val messageId = id ?: return null
    val body = message ?: headerSubtext
    val type =
        when {
            purchaseAmountText != null -> LiveChatMessageType.SUPER_CHAT
            headerSubtext != null && message == null -> LiveChatMessageType.MEMBERSHIP
            else -> LiveChatMessageType.TEXT
        }
    var isOwner = false
    var isModerator = false
    var isVerified = false
    var isMember = false
    var memberBadgeUrl: String? = null
    authorBadges.forEach { badge ->
        val renderer = badge.liveChatAuthorBadgeRenderer ?: return@forEach
        when (renderer.icon?.iconType?.uppercase()) {
            "OWNER" -> isOwner = true
            "MODERATOR" -> isModerator = true
            "VERIFIED" -> isVerified = true
        }
        renderer.customThumbnail?.bestUrl()?.let {
            isMember = true
            memberBadgeUrl = it
        }
    }
    return LiveChatMessage(
        id = messageId,
        author = authorName?.value().orEmpty(),
        authorPhotoUrl = authorPhoto?.bestUrl(),
        message = body?.plainText().orEmpty(),
        segments = body?.toSegments().orEmpty(),
        timestamp = timestampText?.value(),
        type = type,
        isOwner = isOwner,
        isModerator = isModerator,
        isVerified = isVerified,
        isMember = isMember,
        memberBadgeUrl = memberBadgeUrl,
        superChatAmount = purchaseAmountText?.value(),
    )
}

private fun GetLiveChatResponse.Message.toSegments(): List<LiveChatSegment> =
    runs.map { run ->
        val emoji = run.emoji
        LiveChatSegment(
            text = run.text ?: emoji?.shortcuts?.firstOrNull() ?: emoji?.emojiId.orEmpty(),
            emojiImageUrl = emoji?.image?.bestUrl(),
        )
    }

/**
 * One community post's page: the post, its comment section and the token that section pages from.
 * [commentsContinuation] is null when the post's comment section served no next page.
 */
data class CommunityPostDetail(
    val post: CommunityPost,
    val comments: List<FeedComment>,
    val commentCountText: String?,
    val commentsContinuation: String?,
    /** True when the comment section's own request failed while the post itself loaded. */
    val commentsFailed: Boolean = false,
) {
    /** The next page of comments under the ones already shown. */
    fun more(next: CommunityPostComments): CommunityPostDetail =
        copy(
            comments = (comments + next.comments).distinctBy { it.id },
            commentsContinuation = next.continuation,
        )
}

/** One page of a post's comment section. */
data class CommunityPostComments(
    val comments: List<FeedComment>,
    val continuation: String?,
)

/**
 * A community post opened from its card: the post page first, then the comment section's token,
 * which is where the posts' comments actually live. A comment section that fails leaves the post
 * on screen with no comments rather than failing the whole page.
 */
suspend fun loadCommunityPost(params: String): Result<CommunityPostDetail> =
    runCatching {
        val page = YouTube.communityPost(params).getOrThrow()
        val post = page.post ?: error("The post page carried no post")
        val comments =
            page.commentsContinuation?.let { token ->
                YouTube.communityPostComments(continuation = token).getOrNull()
            }
        CommunityPostDetail(
            post = post,
            comments = comments?.comments.orEmpty().map { it.toFeedComment() },
            commentCountText = post.commentCountText.takeIf { it.isNotBlank() } ?: comments?.commentCountText,
            commentsContinuation = comments?.continuation,
            commentsFailed = page.commentsContinuation != null && comments == null,
        )
    }

/** The next page of a post's comments through the token the last page ended on. */
suspend fun loadCommunityPostComments(continuation: String): Result<CommunityPostComments> =
    YouTube.communityPostComments(continuation = continuation).map { page ->
        CommunityPostComments(
            comments = page.comments.map { it.toFeedComment() },
            continuation = page.continuation,
        )
    }

/** The replies under a post's comment, which is the same browse endpoint as its comments page. */
suspend fun loadCommunityPostReplies(continuation: String): Result<List<FeedComment>> =
    YouTube.communityPostReplies(continuation).map { page -> page.comments.map { it.toFeedComment() } }

/** The replies under a comment, in YouTube's order. A thread with none answers with an empty list. */
suspend fun loadCommentReplies(
    continuation: String,
    videoId: String?,
): Result<List<FeedComment>> = YouTube.commentReplies(continuation, videoId).map { page -> page.comments.map { it.toFeedComment() } }
