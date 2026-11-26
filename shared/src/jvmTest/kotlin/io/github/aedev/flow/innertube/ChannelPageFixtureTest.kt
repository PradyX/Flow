package io.github.aedev.flow.innertube

import io.github.aedev.flow.innertube.models.response.ChannelSearchResponse
import io.github.aedev.flow.innertube.pages.channel.ChannelSearchRow
import io.github.aedev.flow.innertube.pages.channel.ChannelTabKind
import io.github.aedev.flow.innertube.pages.channel.parseChannelSearchResponse
import io.github.aedev.flow.innertube.pages.channel.toChannelTabContent
import io.github.aedev.flow.innertube.pages.renderer.PostAttachment
import io.github.aedev.flow.presentation.feed.toChannelShelves
import io.github.aedev.flow.presentation.feed.toFeedEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The channel page's two paging paths, against captured live traffic: a channel-scoped search page
 * (Kurzgesagt, term "space", 2026-11-25) and its next page, plus a Videos tab and its next page.
 * Both captures answer in the same shapes the parsers take on the wire: the search's first page is
 * an `expandableTabRenderer` with a section list, its later pages are appended `itemSectionRenderer`
 * rows; the grid's first page is a `richGridRenderer`, its later ones appended `richGridRenderer`
 * items.
 */
class ChannelPageFixtureTest {
    private val reader =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) { "missing fixture $name" }
            .bufferedReader()
            .use { it.readText() }

    private fun searchPage(name: String): ChannelSearchResponse = reader.decodeFromString(fixture(name))

    private fun tab(name: String) = reader.parseToJsonElement(fixture(name))

    @Test
    fun theFirstPageOfAChannelSearchKeepsThePlaylistRowAboveTheVideos() {
        val page = parseChannelSearchResponse(searchPage("channel_search_page.json"), "UCsXVk37bltHxD1rDPwtNM8Q", "Kurzgesagt", "")

        assertEquals(3, page.rows.size)
        val playlist = page.rows.first() as ChannelSearchRow.PlaylistRow
        assertEquals("PLFs4vir_WsTwEd-nJgVJCZPNL3HALHHpF", playlist.playlist.id)
        assertEquals("The Universe and Space stuff", playlist.playlist.name)
        assertEquals(54, playlist.playlist.videoCount)
        assertTrue(playlist.playlist.thumbnailUrl.isNotBlank())
        assertEquals(2, page.videos.size)
        assertTrue(page.continuation?.isNotBlank() == true)
        assertTrue(page.videos.all { it.id.isNotBlank() && it.title.isNotBlank() })
        assertTrue(page.videos.all { it.channelId == "UCsXVk37bltHxD1rDPwtNM8Q" })
    }

    @Test
    fun aChannelSearchContinuationAnswersWithItsOwnPageAndAnotherToken() {
        val page = parseChannelSearchResponse(searchPage("channel_search_more.json"), "UCsXVk37bltHxD1rDPwtNM8Q", "Kurzgesagt", "")

        assertEquals(4, page.videos.size)
        assertTrue(page.continuation?.isNotBlank() == true)
        // The continuation pages carry no header, so the caller's owner fills the rows in.
        assertTrue(page.videos.all { it.channelName == "Kurzgesagt" })
    }

    @Test
    fun theVideosTabPagesThroughTheSameGridReader() {
        val first = tab("channel_videos_page_one.json").toChannelTabContent(ChannelTabKind.Videos)
        val more = tab("channel_videos_page_more.json").toChannelTabContent(ChannelTabKind.Videos)

        assertEquals(4, first.items.size)
        assertEquals(4, more.items.size)
        assertTrue(first.continuation?.isNotBlank() == true)
        assertTrue(more.continuation?.isNotBlank() == true)
        assertTrue(first.items.none { it in more.items }, "the two pages hold different videos")
    }

    @Test
    fun aCapturedGridCarriesTheRowsTheCardNeeds() {
        val first = tab("channel_videos_page_one.json").toChannelTabContent(ChannelTabKind.Videos)
        val entry = first.items.mapNotNull { it.toFeedEntry() }.first()

        assertTrue(entry.videoId != null)
        assertTrue(entry.title.isNotBlank())
        assertTrue(entry.thumbnailUrl != null)
        assertTrue(entry.durationSeconds != null)
    }

    @Test
    fun theCapturedSearchSectionsAreTheOnesTheModelSees() {
        val raw = reader.parseToJsonElement(fixture("channel_search_page.json"))
        val sections =
            raw.jsonObject["contents"]!!
                .jsonObject["twoColumnBrowseResultsRenderer"]!!
                .jsonObject["tabs"]!!
                .jsonArray[0]
                .jsonObject["expandableTabRenderer"]!!
                .jsonObject["content"]!!
                .jsonObject["sectionListRenderer"]!!
                .jsonObject["contents"]!!
                .jsonArray
        assertTrue(sections.any { it.jsonObject["continuationItemRenderer"] != null })
    }

    @Test
    fun thePostsTabReadsEveryPostInTheCapturedPageAndItsNextPageToken() {
        val page = tab("channel_posts_page.json").toChannelTabContent(ChannelTabKind.Posts)

        assertEquals(3, page.posts.size)
        assertTrue(page.items.isEmpty(), "a post is not a grid card")
        assertTrue(page.continuation?.isNotBlank() == true)
        val first = page.posts.first()
        assertEquals("UgkxfIg9dbMuFZkWV_PAFNvu_9AuTiDlkZmK", first.id)
        assertEquals("Kurzgesagt – In a Nutshell", first.authorName)
        assertTrue(first.text.startsWith("The Black Hole Week Deals"))
        assertEquals("2.1K", first.likeCountText)
        assertEquals("2 days ago", first.publishedTimeText)
        assertTrue(first.attachment is PostAttachment.Images)
    }

    @Test
    fun aPostsContinuationArrivesInTheNewerEndpointEnvelopeAndStillPages() {
        val page = tab("channel_posts_more.json").toChannelTabContent(ChannelTabKind.Posts)

        assertEquals(3, page.posts.size)
        assertTrue(page.posts.all { it.id.isNotBlank() && it.text.isNotBlank() })
        assertTrue(page.continuation?.isNotBlank() == true)
    }

    @Test
    fun theHomeTabIsItsFeaturedTrailerAndTheNamedShelvesYouTubeServes() {
        val content = tab("channel_home_page.json").toChannelTabContent(ChannelTabKind.Home)

        assertTrue(content.items.isEmpty(), "a shelf tab is not a grid")
        // WEB serves Home as one section list; the only page tokens in the capture belong to the
        // header's own description panel, so the tab must not offer a footer that loads nothing.
        assertNull(content.continuation)
        val shelves = content.sections.toChannelShelves()
        assertEquals(
            listOf(
                "",
                "Videos",
                "Our Best Stuff",
                "The Existential Crisis Playlist",
                "The Universe and Space stuff",
                "Human Stuff",
                "Futurism",
                "Medicine & Biology",
                "Science and other stuff explained",
                "Shorts",
                "Good Stuff",
                "Posts",
                "Collaborations",
            ),
            shelves.map { it.title },
        )

        val featured = shelves.first()
        assertTrue(featured.isFeatured)
        assertEquals("_27eD49ePQE", featured.entries.single().videoId)
        assertEquals("Kurzgesagt Channel Trailer", featured.entries.single().title)
        assertEquals(2, shelves[1].entries.size)
        assertNull(shelves[1].playlistId, "a video shelf's endpoint is the channel's own tab")
        assertEquals(
            "PLFs4vir_WsTxontcYm5ctqp89cNBJKNrs",
            shelves.first { it.title == "The Existential Crisis Playlist" }.playlistId,
        )

        val shorts = shelves.first { it.title == "Shorts" }
        assertEquals(3, shorts.entries.size)
        assertTrue(shorts.entries.all { it.videoId != null && it.title.isNotBlank() })

        val channels = shelves.first { it.title == "Good Stuff" }
        assertTrue(channels.entries.size == 2)
        assertTrue(channels.entries.all { it.channelId != null && it.videoId == null })
        assertTrue(
            channels.entries
                .first()
                .title
                .isNotBlank(),
        )

        val posts = shelves.first { it.title == "Posts" }
        assertTrue(posts.entries.isEmpty())
        assertEquals(2, posts.posts.size)
        assertEquals("Kurzgesagt – In a Nutshell", posts.posts.first().authorName)
    }

    @Test
    fun theReleasesTabIsOneShelfPerReleaseWithItsSongs() {
        val page = tab("channel_releases_page.json").toChannelTabContent(ChannelTabKind.Releases)

        assertEquals(2, page.sections.size)
        assertEquals("HIT ME HARD AND SOFT", page.sections.first().title)
        assertEquals("PLsCPTY_MPoPYKdrO_v930g19ElQI1XCgY", page.sections.first().morePlaylistId)
        assertEquals(
            4,
            page.sections
                .first()
                .items.size,
        )
        assertEquals(
            4,
            page.sections
                .last()
                .items.size,
        )
        // A release page is one response: its per-shelf tokens expand a carousel, not the page.
        assertNull(page.continuation)
    }
}
