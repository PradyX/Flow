package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.innertube.pages.search.SearchChip
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The search page as it stood when the app last ran: what the user typed, the narrowing they chose,
 * the page YouTube answered with and where the grid was scrolled to.
 *
 * Only restorable data lives here — no request, no loading or failed state — so a relaunch can draw
 * the page from it, and a session whose page did not survive asks YouTube again with the same
 * filter rather than showing an error over a query the user typed.
 */
@Serializable
data class SearchSession(
    val query: String,
    val filter: SearchFilter = SearchFilter(),
    val entries: List<FeedEntry> = emptyList(),
    val estimatedResults: Long? = null,
    val chips: List<SearchChip> = emptyList(),
    /** The token the page after the cached one opens from, so a restore keeps paging. */
    val continuation: String? = null,
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
) {
    /** Nothing was searched, so there is nothing to restore. */
    val isEmpty: Boolean
        get() = query.isBlank() && entries.isEmpty()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String?): SearchSession? =
            text?.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString<SearchSession>(it) }.getOrNull() }

        fun toJson(session: SearchSession): String = json.encodeToString(session)
    }
}
