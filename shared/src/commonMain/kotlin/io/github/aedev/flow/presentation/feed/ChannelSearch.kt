package io.github.aedev.flow.presentation.feed

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.pages.channel.ChannelSearchRow

/**
 * What a channel-scoped search found: the rows in the order YouTube ranked them, and the token the
 * next page opens from. [more] appends, because the server's ranking is the order and a later page
 * can repeat a row the first page already showed.
 */
data class ChannelSearchResults(
    val entries: List<FeedEntry> = emptyList(),
    val continuation: String? = null,
) {
    fun more(next: ChannelSearchResults): ChannelSearchResults =
        ChannelSearchResults(
            entries = (entries + next.entries).distinctBy { it.id },
            continuation = next.continuation,
        )

    val isEmpty: Boolean get() = entries.isEmpty()
}

/**
 * The channel page's own search: YouTube's `browse` on that channel with the search tab's token, so
 * the answer is restricted to the channel the viewer is on. A continuation reads the next page of
 * the same term.
 */
suspend fun loadChannelSearch(
    channelId: String,
    channelName: String,
    channelThumbnailUrl: String,
    query: String,
    continuation: String? = null,
): Result<ChannelSearchResults> =
    YouTube
        .channelSearch(
            channelId = channelId,
            channelName = channelName,
            channelThumbnailUrl = channelThumbnailUrl,
            query = query.trim(),
            continuation = continuation,
        ).map { page ->
            ChannelSearchResults(
                entries =
                    page.rows
                        .map { row ->
                            when (row) {
                                is ChannelSearchRow.VideoRow -> videoCardEntry(row.video)
                                is ChannelSearchRow.PlaylistRow -> playlistCardEntry(row.playlist)
                            }
                        }.distinctBy { it.id },
                continuation = page.continuation,
            )
        }

/**
 * The in-channel search as the screen keeps it: the term, the page it found, and whether a page is
 * on its way. A blank term is not a search, so clearing the field empties the page rather than
 * leaving the last term's results under an empty box.
 */
@Stable
class ChannelSearchState {
    var active: Boolean by mutableStateOf(false)
        private set

    var query: String by mutableStateOf("")
        private set

    var results: LoadState<ChannelSearchResults>? by mutableStateOf(null)
        private set

    var loadingMore: Boolean by mutableStateOf(false)
        private set

    fun open() {
        active = true
    }

    /** Leaving search drops the term and its page, as the reference does when the field closes. */
    fun close() {
        active = false
        query = ""
        results = null
        loadingMore = false
    }

    fun type(term: String) {
        query = term
        if (term.isBlank()) {
            results = null
            loadingMore = false
        }
    }

    fun start(term: String = query) {
        query = term
        loadingMore = false
        results =
            if (term.isBlank()) {
                null
            } else {
                LoadState.Loading
            }
    }

    fun put(page: LoadState<ChannelSearchResults>) {
        results = page
        loadingMore = false
    }

    fun append(next: ChannelSearchResults) {
        val kept = (results as? LoadState.Ready)?.value ?: return
        results = LoadState.Ready(kept.more(next))
        loadingMore = false
    }

    /** True only for the first caller, so the footer cannot ask for one page twice. */
    fun beginMore(): String? {
        if (loadingMore) return null
        val token = (results as? LoadState.Ready)?.value?.continuation ?: return null
        loadingMore = true
        return token
    }

    fun failMore() {
        loadingMore = false
    }
}
