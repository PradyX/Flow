package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.data.model.Channel
import io.github.aedev.flow.data.model.Playlist
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.innertube.pages.renderer.CommunityPost
import io.github.aedev.flow.innertube.pages.renderer.FeedItem
import io.github.aedev.flow.innertube.pages.renderer.FeedShelf
import io.github.aedev.flow.innertube.pages.renderer.FeedShelfStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The channel page's second page: one appended grid, one appended search, one paging guard. */
class ChannelPagingTest {
    private fun entry(id: String) = FeedEntry(id = id, title = "Row $id", subtitle = null, videoId = id)

    private val video =
        Video(
            id = "v1",
            title = "A video",
            channelName = "Kurzgesagt",
            channelId = "UCsXVk37bltHxD1rDPwtNM8Q",
            thumbnailUrl = "https://example.com/v.jpg",
            duration = 300,
            viewCount = 1_000,
            uploadDate = "2 days ago",
        )

    private val channel =
        Channel(
            id = "UCother",
            name = "Another channel",
            thumbnailUrl = "https://example.com/a.jpg",
            subscriberCount = 1_000,
            description = "",
            url = "https://www.youtube.com/channel/UCother",
        )

    @Test
    fun aTabPageAppendsTheNextOneUnderTheRowsAlreadyShown() {
        val first = ChannelTabPage(entries = listOf(entry("a"), entry("b")), continuation = "t1")
        val next = ChannelTabPage(entries = listOf(entry("c")), continuation = "t2")

        val merged = first.more(next)

        assertEquals(listOf("a", "b", "c"), merged.entries.map { it.id })
        assertEquals("t2", merged.continuation)
    }

    @Test
    fun aRowTheNextPageRepeatsIsDrawnOnceAndTheLastPageEndsPaging() {
        val first = ChannelTabPage(entries = listOf(entry("a"), entry("b")), continuation = "t1")

        val merged = first.more(ChannelTabPage(entries = listOf(entry("b"), entry("c")), continuation = null))

        assertEquals(listOf("a", "b", "c"), merged.entries.map { it.id })
        assertNull(merged.continuation)
    }

    @Test
    fun theSearchPageAppendsTheSameWay() {
        val first = ChannelSearchResults(entries = listOf(entry("a")), continuation = "t1")

        val merged = first.more(ChannelSearchResults(entries = listOf(entry("a"), entry("b")), continuation = "t2"))

        assertEquals(listOf("a", "b"), merged.entries.map { it.id })
        assertEquals("t2", merged.continuation)
        assertTrue(!merged.isEmpty)
    }

    @Test
    fun aPostsPageAndTheNextOneAreAppendedAsPostsNotAsCards() {
        fun post(id: String) =
            CommunityPost(
                id = id,
                authorName = "Kurzgesagt",
                authorAvatarUrl = "",
                text = "Post $id",
                attachment = null,
                likeCountText = "1K",
                commentCountText = "20",
                commentEndpointParams = null,
                publishedTimeText = "1 day ago",
            )
        val first = ChannelTabPage(posts = listOf(post("a")), continuation = "t1")
        val next = ChannelTabPage(posts = listOf(post("a"), post("b")), continuation = null)

        val merged = first.more(next)

        assertEquals(listOf("a", "b"), merged.posts.map { it.id })
        assertTrue(merged.entries.isEmpty())
        assertNull(merged.continuation)
    }

    @Test
    fun aShelfPageAppendsItsNextShelvesUnderTheOnesAlreadyShown() {
        val first = ChannelTabPage(sections = listOf(ChannelShelf("Album one", listOf(entry("a")))), continuation = null)
        val next = ChannelTabPage(sections = listOf(ChannelShelf("Album two", listOf(entry("b")))))

        val merged = first.more(next)

        assertEquals(listOf("Album one", "Album two"), merged.sections.map { it.title })
    }

    @Test
    fun aShelfOfPostsSurvivesAsPostsInsteadOfBeingDroppedAsAnEmptyGrid() {
        val post =
            CommunityPost(
                id = "p1",
                authorName = "Kurzgesagt",
                authorAvatarUrl = "",
                text = "A post",
                attachment = null,
                likeCountText = "1K",
                commentCountText = "12",
                commentEndpointParams = null,
                publishedTimeText = "2 days ago",
            )
        val shelf =
            FeedShelf(
                id = "shelf:posts",
                title = "Posts",
                items = listOf(FeedItem.PostItem(post)),
            )

        val mapped = listOf(shelf).toChannelShelves().single()

        assertEquals("Posts", mapped.title)
        assertTrue(mapped.entries.isEmpty())
        assertEquals(listOf("p1"), mapped.posts.map { it.id })
    }

    @Test
    fun aRelatedChannelShelfMapsToChannelRowsThatKeepTheirId() {
        val channels =
            listOf(
                FeedShelf(
                    id = "shelf:channels",
                    title = "Good Stuff",
                    items = listOf(FeedItem.RelatedChannelItem(channel)),
                ),
            ).toChannelShelves()
                .single()

        assertEquals("Good Stuff", channels.title)
        assertEquals(listOf("UCother"), channels.entries.map { it.channelId })
        assertNull(channels.entries.single().videoId)
    }

    @Test
    fun aPlaylistIdLosesItsBrowsePrefixAndOnlyAPlaylistIsLinked() {
        fun shelf(
            id: String,
            browseId: String,
        ) = FeedShelf(id = id, title = "A shelf", items = listOf(FeedItem.VideoItem(video)), morePlaylistId = browseId)

        val mapped =
            listOf(
                shelf("playlist", "VLPLabc"),
                shelf("channel", "UCchannel"),
                FeedShelf(id = "featured", style = FeedShelfStyle.Trailer, items = listOf(FeedItem.VideoItem(video))),
            ).toChannelShelves()

        assertEquals("PLabc", mapped[0].playlistId)
        assertNull(mapped[1].playlistId, "a video shelf's endpoint is the channel's own tab")
        assertTrue(mapped[2].isFeatured)
    }

    @Test
    fun aReleaseShelfKeepsItsPlaylistUnlessItIsAnAlbumAudioPlaylist() {
        fun shelf(playlistId: String?) =
            FeedShelf(
                id = "shelf:$playlistId",
                title = "A release",
                items =
                    listOf(
                        FeedItem.PlaylistItem(Playlist(id = "p", name = "Song", thumbnailUrl = "", videoCount = 0)),
                    ),
                morePlaylistId = playlistId,
            )

        val mapped = listOf(shelf("PLrelease"), shelf("OLAK5uy_audio"), shelf(null)).toChannelShelves()

        assertEquals("PLrelease", mapped[0].playlistId)
        assertNull(mapped[1].playlistId, "an album's audio playlist has no WEB_REMIX page")
        assertNull(mapped[2].playlistId)
    }

    @Test
    fun theSearchStateAsksForOnePageAtATime() {
        val state = ChannelSearchState()
        state.start("space")
        assertEquals("space", state.query)
        assertTrue(state.results is LoadState.Loading)
        assertNull(state.beginMore(), "nothing to page yet")

        state.put(LoadState.Ready(ChannelSearchResults(entries = listOf(entry("a")), continuation = "t1")))

        assertEquals("t1", state.beginMore())
        assertTrue(state.loadingMore)
        assertNull(state.beginMore(), "one page in flight at a time")

        state.append(ChannelSearchResults(entries = listOf(entry("b"))))
        assertEquals(listOf("a", "b"), (state.results as LoadState.Ready).value.entries.map { it.id })
        assertTrue(!state.loadingMore)
        assertNull(state.beginMore())
    }

    @Test
    fun closingOrClearingSearchDropsTheTermAndItsPage() {
        val state = ChannelSearchState()
        state.open()
        state.start("space")
        state.put(LoadState.Ready(ChannelSearchResults(entries = listOf(entry("a")), continuation = "t1")))

        state.type("")
        assertNull(state.results)
        assertTrue(!state.loadingMore)

        state.start("mars")
        state.close()
        assertTrue(!state.active)
        assertEquals("", state.query)
        assertNull(state.results)
    }

    @Test
    fun aPlaylistRowIsACardThatOpensThePlaylistPage() {
        val card =
            playlistCardEntry(
                Playlist(
                    id = "PL123",
                    name = "Deep space",
                    thumbnailUrl = "https://i.ytimg.com/vi/x/hq720.jpg",
                    videoCount = 2400,
                    isLocal = false,
                ),
            )

        assertEquals("PL123", card.playlistId)
        assertNull(card.videoId)
        assertEquals("Deep space", card.title)
        assertEquals("2K", card.overlayText)
    }

    @Test
    fun aTabOnlyPagesWhileTheServerStillNamesOne() {
        assertNull(ChannelTabPage(entries = listOf(entry("a"))).continuation)
    }
}
