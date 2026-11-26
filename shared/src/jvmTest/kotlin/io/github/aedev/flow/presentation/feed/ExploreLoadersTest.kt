package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.data.model.Playlist
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.innertube.pages.explore.ExploreDestination
import io.github.aedev.flow.innertube.pages.explore.ExploreSectionKind
import io.github.aedev.flow.innertube.pages.renderer.FeedItem
import io.github.aedev.flow.innertube.pages.renderer.FeedShelf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExploreLoadersTest {
    private fun video(id: String) =
        Video(
            id = id,
            title = "Video $id",
            channelName = "Channel",
            channelId = "channel",
            thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
            duration = 120,
            viewCount = 1_000,
            uploadDate = "1 day ago",
        )

    private fun playlist(
        id: String,
        count: Int = 12,
    ) = Playlist(id = id, name = "Playlist $id", thumbnailUrl = "https://i/$id.jpg", videoCount = count)

    private fun shelf(
        id: String,
        title: String? = "Shelf $id",
        items: List<FeedItem> = listOf(FeedItem.VideoItem(video("$id-1"))),
        moreParams: String? = null,
    ) = FeedShelf(id = id, title = title, items = items, moreParams = moreParams)

    @Test
    fun `shelves become sections in the order they arrived`() {
        val page = listOf(shelf("a"), shelf("b"), shelf("c")).toExplorePage(title = "Live")
        assertEquals("Live", page.title)
        assertEquals(listOf("a", "b", "c"), page.sections.map { it.id })
    }

    @Test
    fun `a shelf with nothing playable is not a section`() {
        val empty = shelf("empty", items = emptyList())
        val page = listOf(empty, shelf("ok")).toExplorePage()
        assertEquals(listOf("ok"), page.sections.map { it.id })
    }

    @Test
    fun `a video item is a card that plays its own video`() {
        val section = shelf("v", items = listOf(FeedItem.VideoItem(video("abc")))).toExploreSection()
        val entry = section.entries.single()
        assertEquals("abc", entry.videoId)
        assertEquals("Video abc", entry.title)
        assertEquals(120, entry.durationSeconds)
        assertTrue(entry.subtitle.orEmpty().contains("Channel"))
    }

    @Test
    fun `a reel card plays as well`() {
        val section =
            shelf("s", items = listOf(FeedItem.ShortItem(video("reel")))).toExploreSection()
        assertEquals("reel", section.entries.single().videoId)
    }

    @Test
    fun `a playlist card opens the playlist rather than a video`() {
        val section = shelf("p", items = listOf(FeedItem.PlaylistItem(playlist("PL1", count = 30)))).toExploreSection()
        val entry = section.entries.single()
        assertNull(entry.videoId)
        assertEquals("PL1", entry.playlistId)
        assertEquals("30", entry.overlayText)
    }

    @Test
    fun `the same video twice in one shelf is one card`() {
        val section =
            shelf(
                "dupe",
                items = listOf(FeedItem.VideoItem(video("x")), FeedItem.VideoItem(video("x"))),
            ).toExploreSection()
        assertEquals(1, section.entries.size)
    }

    @Test
    fun `a shelf keeps the token its see all opens`() {
        val section = shelf("a", moreParams = "EgZ0b2tlbg%3D%3D").toExploreSection()
        assertEquals("EgZ0b2tlbg%3D%3D", section.moreParams)
    }

    @Test
    fun `the tabs are the app's seven, in the app's order`() {
        assertEquals(
            listOf(
                ExploreDestination.LIVE,
                ExploreDestination.GAMING,
                ExploreDestination.MUSIC,
                ExploreDestination.NEWS,
                ExploreDestination.SPORTS,
                ExploreDestination.LEARNING,
                ExploreDestination.FASHION,
            ),
            EXPLORE_CATEGORIES.map { it.destination },
        )
    }

    @Test
    fun `gaming pages through its own token and music reads the chart`() {
        val gaming = EXPLORE_CATEGORIES.first { it.destination == ExploreDestination.GAMING }.destination
        assertEquals(ExploreSectionKind.GRID, gaming.kind)
        assertTrue(!gaming.params.isNullOrBlank())
        val music = EXPLORE_CATEGORIES.first { it.destination == ExploreDestination.MUSIC }.destination
        assertEquals(ExploreSectionKind.CHART, music.kind)
    }
}
