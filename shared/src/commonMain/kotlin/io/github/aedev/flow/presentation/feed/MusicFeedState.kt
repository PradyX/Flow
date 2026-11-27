package io.github.aedev.flow.presentation.feed

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The music tab's page, owned by the shell the way Home's is: leaving the tab and coming back shows
 * the shelves that were already fetched instead of asking YouTube again.
 */
@Stable
internal class MusicFeedState {
    var feed: LoadState<MusicHomeFeed> by mutableStateOf(LoadState.Loading)

    /** True while the next page of shelves is on its way, so the footer is asked for only once. */
    var loadingMore: Boolean by mutableStateOf(false)

    /** Where the page is scrolled, kept so a page opened from a shelf returns to its place. */
    val listState: LazyListState = LazyListState()
}
