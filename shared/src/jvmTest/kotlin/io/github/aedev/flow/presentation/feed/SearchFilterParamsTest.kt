package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.data.model.Playlist
import io.github.aedev.flow.innertube.pages.renderer.FeedItem
import io.github.aedev.flow.innertube.pages.search.SearchResultsPage
import io.github.aedev.flow.innertube.pages.search.SearchSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * The narrowing a search carries: a type that cannot hold a video filter drops the filter instead of
 * sending a token YouTube answers with nothing, and a playlist result reaches the grid as a card.
 */
class SearchFilterParamsTest {
    @Test
    fun aPlaylistSearchDropsTheVideoOnlyNarrowings() {
        val withLeftovers =
            SearchFilter(
                type = SearchType.PLAYLIST,
                duration = SearchDuration.OVER_20_MINUTES,
                uploadDate = SearchUploadDate.THIS_MONTH,
                features = setOf(SearchFeature.HD),
            )

        assertEquals(SearchFilter(type = SearchType.PLAYLIST).params(), withLeftovers.params())
    }

    @Test
    fun aVideoSearchKeepsItsDuration() {
        val plain = SearchFilter(type = SearchType.VIDEO)
        val narrowed = plain.copy(duration = SearchDuration.OVER_20_MINUTES)

        assertNotEquals(plain.params(), narrowed.params())
    }

    @Test
    fun nothingNarrowedIsNoTokenAtAll() {
        assertNull(SearchFilter().params())
    }

    @Test
    fun aPlaylistResultReachesTheGridAsItsOwnCard() {
        val page =
            SearchResultsPage(
                sections =
                    listOf(
                        SearchSection.Result(
                            FeedItem.PlaylistItem(
                                Playlist(
                                    id = "PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI",
                                    name = "Top Tracks",
                                    thumbnailUrl = "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg",
                                    videoCount = 12,
                                    isLocal = false,
                                ),
                            ),
                        ),
                    ),
            )

        val entry = page.toSearchEntries().single()

        assertEquals("PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI", entry.playlistId)
        assertEquals("Top Tracks", entry.title)
        assertEquals("12 videos", entry.overlayText)
        assertNull(entry.videoId)
    }
}
