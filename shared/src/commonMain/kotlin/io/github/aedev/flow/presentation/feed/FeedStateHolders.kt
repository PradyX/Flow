package io.github.aedev.flow.presentation.feed

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.data.search.SearchHistory
import io.github.aedev.flow.innertube.pages.search.SearchChip

/**
 * Home's selected chip, its feed and scroll position, held by the shell rather than the screen.
 * Pushing the player disposes the table, so owning this here is what keeps a back press from
 * refetching the grid and jumping it to the top. Each chip's last result is kept beside the feed so
 * switching back to it does not ask again.
 */
@Stable
class HomeFeedState {
    var selectedChip: HomeChip by mutableStateOf(HomeChip.All)
    var feed: LoadState<List<Video>> by mutableStateOf(LoadState.Loading)
    val chipFeeds: MutableMap<HomeChip, List<Video>> = mutableMapOf()
    val gridState: LazyGridState = LazyGridState()
}

/**
 * The search page's query, results and scroll position, with the same lifetime as [HomeFeedState]:
 * a video opened from the results returns to the results that opened it.
 */
@Stable
class SearchFeedState(
    query: String = "",
    results: LoadState<SearchFeed>? = null,
    filters: SearchFilter = SearchFilter(),
    chips: List<SearchChip> = emptyList(),
    history: SearchHistory = SearchHistory(),
    scrollIndex: Int = 0,
    scrollOffset: Int = 0,
) {
    var query: String by mutableStateOf(query)
    var results: LoadState<SearchFeed>? by mutableStateOf(results)

    /** The searches this device ran, newest first, shown under the field before anything is typed. */
    var history: SearchHistory by mutableStateOf(history)

    /** YouTube's completions for the term being typed; empty when the field is not being edited. */
    var suggestions: List<String> by mutableStateOf(emptyList())

    /** True while the field is being edited, which is when the suggestion panel replaces results. */
    var typing: Boolean by mutableStateOf(false)

    /** True while the next page of results is on its way, so the footer is asked for only once. */
    var loadingMore: Boolean by mutableStateOf(false)

    /** The narrowing the page was last asked with; a new query keeps it, as the app's page does. */
    var filters: SearchFilter by mutableStateOf(filters)

    /**
     * The chips of the last page, kept beside [results] so the bar stays put while a chip's own
     * request is in flight.
     */
    var chips: List<SearchChip> by mutableStateOf(chips)
    val gridState: LazyGridState = LazyGridState(scrollIndex, scrollOffset)

    /** The page as restorable data, for the store that outlives the process. */
    fun session(): SearchSession {
        val page = (results as? LoadState.Ready)?.value
        return SearchSession(
            query = query,
            filter = filters,
            entries = page?.entries.orEmpty(),
            estimatedResults = page?.estimatedResults,
            chips = chips,
            continuation = page?.continuation,
            scrollIndex = gridState.firstVisibleItemIndex,
            scrollOffset = gridState.firstVisibleItemScrollOffset,
        )
    }

    companion object {
        /**
         * The page a relaunch opens on. A session without its cached page keeps its query and its
         * filter but no results, which is the signal to ask YouTube again rather than show an
         * empty page over the term the user typed.
         */
        fun fromSession(
            session: SearchSession?,
            history: SearchHistory = SearchHistory(),
        ): SearchFeedState {
            if (session == null || session.isEmpty) return SearchFeedState(history = history)
            val page =
                if (session.entries.isEmpty()) {
                    null
                } else {
                    LoadState.Ready(
                        SearchFeed(
                            entries = session.entries,
                            estimatedResults = session.estimatedResults,
                            chips = session.chips,
                            continuation = session.continuation,
                        ),
                    )
                }
            return SearchFeedState(
                query = session.query,
                results = page,
                filters = session.filter,
                chips = session.chips,
                history = history,
                scrollIndex = session.scrollIndex,
                scrollOffset = session.scrollOffset,
            )
        }
    }
}
