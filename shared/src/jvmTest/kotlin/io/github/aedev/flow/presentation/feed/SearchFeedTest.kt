package io.github.aedev.flow.presentation.feed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchFeedTest {
    private fun entry(id: String) =
        FeedEntry(
            id = id,
            title = "Result $id",
            subtitle = null,
            videoId = id,
        )

    @Test
    fun `the next page lands after the results already shown`() {
        val first = SearchFeed(entries = listOf(entry("a"), entry("b")), estimatedResults = 12, continuation = "t1")
        val next = SearchFeed(entries = listOf(entry("c")), estimatedResults = null, continuation = "t2")

        val merged = first.more(next)

        assertEquals(listOf("a", "b", "c"), merged.entries.map { it.id })
        assertEquals(12, merged.estimatedResults)
        assertEquals("t2", merged.continuation)
    }

    @Test
    fun `a result the next page repeats is drawn once`() {
        val first = SearchFeed(entries = listOf(entry("a"), entry("b")), estimatedResults = null, continuation = "t1")
        val next = SearchFeed(entries = listOf(entry("b"), entry("c")), estimatedResults = null, continuation = null)

        val merged = first.more(next)

        assertEquals(listOf("a", "b", "c"), merged.entries.map { it.id })
        assertNull(merged.continuation)
    }

    @Test
    fun `a last page keeps what is shown and ends the paging`() {
        val first = SearchFeed(entries = listOf(entry("a")), estimatedResults = 1, continuation = "t1")

        val merged = first.more(SearchFeed(entries = emptyList(), estimatedResults = null))

        assertEquals(listOf("a"), merged.entries.map { it.id })
        assertNull(merged.continuation)
    }

    @Test
    fun `suggestions are trimmed, deduped by their casing and capped`() {
        val queries =
            listOf("  cats ", "cats", "", "Cats", "dogs", "birds", "fish", "cars", "keys", "mice", "owls")
                .toSearchQueries()

        assertEquals(8, queries.size)
        assertEquals(listOf("cats", "dogs", "birds", "fish", "cars", "keys", "mice", "owls"), queries)
    }

    @Test
    fun `no suggestion is an empty list, not a blank row`() {
        assertTrue(listOf("", "  ").toSearchQueries().isEmpty())
    }

    @Test
    fun `a channel result is a channel row, not a video card`() {
        val channel =
            io.github.aedev.flow.data.model.Channel(
                id = "UC123",
                name = "Kurzgesagt",
                thumbnailUrl = "https://yt3/avatar.jpg",
                subscriberCount = 24_000_000,
                handle = "@kurzgesagt",
            )
        val page =
            io.github.aedev.flow.innertube.pages.search.SearchResultsPage(
                sections =
                    listOf(
                        io.github.aedev.flow.innertube.pages.search.SearchSection.Result(
                            io.github.aedev.flow.innertube.pages.renderer.FeedItem
                                .RelatedChannelItem(channel),
                        ),
                    ),
            )

        val result = page.toSearchFeed().entries.single()

        assertEquals("UC123", result.channelId)
        assertEquals("Kurzgesagt", result.title)
        assertNull(result.videoId)
        assertEquals(24_000_000, result.channelSubscriberCount)
        assertEquals("@kurzgesagt", result.subtitle)
    }
}
