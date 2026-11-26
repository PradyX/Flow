package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.innertube.pages.search.SearchChip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The search page's restorable session: after a relaunch the query, the narrowing and the page come
 * back from stored data alone, and a session whose page did not survive asks again rather than
 * pretending to have no results.
 */
class SearchSessionTest {
    private val entry =
        FeedEntry(
            id = "dPFlTv9xY9Q",
            title = "I Found A Buried Giant Scary Herobrine Robot In Minecraft",
            subtitle = "MrAlpha · 495K views · 9 hours ago",
            videoId = "dPFlTv9xY9Q",
            durationSeconds = 1886,
            thumbnailUrl = "https://i.ytimg.com/vi/dPFlTv9xY9Q/hqdefault.jpg",
        )

    private val filter =
        SearchFilter(
            duration = SearchDuration.OVER_20_MINUTES,
            sort = SearchSort.VIEW_COUNT,
        )

    @Test
    fun aSearchedPageComesBackFromItsJson() {
        val state =
            SearchFeedState(
                query = "minecraft",
                results = LoadState.Ready(SearchFeed(listOf(entry), estimatedResults = 83_000_000L, chips = chips())),
                filters = filter,
                chips = chips(),
            )

        val restored = SearchFeedState.fromSession(SearchSession.fromJson(SearchSession.toJson(state.session())))

        assertEquals("minecraft", restored.query)
        assertEquals(filter, restored.filters)
        assertEquals(2, restored.filters.activeCount)
        assertEquals(chips(), restored.chips)
        val page = (restored.results as? LoadState.Ready)?.value
        assertEquals(listOf(entry), page?.entries)
        assertEquals(83_000_000L, page?.estimatedResults)
    }

    @Test
    fun aSessionWithoutItsPageKeepsTheQueryAndAsksAgain() {
        val session = SearchSession(query = "minecraft", filter = filter)

        val restored = SearchFeedState.fromSession(session)

        assertEquals("minecraft", restored.query)
        assertEquals(filter, restored.filters)
        assertNull(restored.results)
    }

    @Test
    fun anEmptySessionLeavesThePlaceholder() {
        val restored = SearchFeedState.fromSession(SearchSession(query = ""))

        assertEquals("", restored.query)
        assertNull(restored.results)
        assertEquals(SearchFilter(), restored.filters)
        assertTrue(SearchSession.fromJson("not json") == null)
    }

    private fun chips(): List<SearchChip> =
        listOf(
            SearchChip(label = "All", selected = true, continuation = "ALL_TOKEN"),
            SearchChip(label = "Over 20 minutes", continuation = "LONG_TOKEN"),
        )
}
