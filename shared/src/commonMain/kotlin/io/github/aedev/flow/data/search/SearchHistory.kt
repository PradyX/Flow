package io.github.aedev.flow.data.search

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The searches this device ran, newest first, one entry per term regardless of its casing. The same
 * term searched twice moves back to the front instead of appearing again, and the list is capped so
 * the panel under the field never grows without bound.
 */
@Serializable
data class SearchHistory(
    val queries: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = queries.isEmpty()

    /** [query] as the newest term; a blank term is not a search and is not recorded. */
    fun with(
        query: String,
        limit: Int = MAX_QUERIES,
    ): SearchHistory {
        val term = query.trim()
        if (term.isEmpty()) return this
        val kept = queries.filterNot { it.equals(term, ignoreCase = true) }
        return SearchHistory((listOf(term) + kept).take(limit.coerceAtLeast(1)))
    }

    /** One term taken out, by the row that draws it. */
    fun without(query: String): SearchHistory = SearchHistory(queries.filterNot { it.equals(query, ignoreCase = true) })

    fun cleared(): SearchHistory = SearchHistory()

    fun encode(): String = HistoryJson.encodeToString(serializer(), this)

    companion object {
        const val MAX_QUERIES = 20

        private val HistoryJson =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        fun decode(json: String?): SearchHistory {
            val text = json?.takeIf { it.isNotBlank() } ?: return SearchHistory()
            val parsed = runCatching { HistoryJson.decodeFromString(serializer(), text) }.getOrNull()
            return SearchHistory(
                parsed
                    ?.queries
                    .orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinctBy { it.lowercase() }
                    .take(MAX_QUERIES),
            )
        }
    }
}

/** Where the recent searches are kept between runs, as text any host can write. */
expect fun loadSearchHistoryJson(): String?

expect fun saveSearchHistoryJson(json: String)
