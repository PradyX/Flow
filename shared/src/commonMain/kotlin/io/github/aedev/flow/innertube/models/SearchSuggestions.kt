package io.github.aedev.flow.innertube.models

import io.github.aedev.flow.innertube.models.response.GetSearchSuggestionsResponse
import io.github.aedev.flow.innertube.pages.SearchSuggestionPage

data class SearchSuggestions(
    val queries: List<String>,
    val recommendedItems: List<YTItem>,
)

/**
 * The completions and the recommendations out of one `music/get_search_suggestions` response. A
 * suggestion's `runs` are the typed part followed by what it adds (`["arijit", " singh"]`), so they
 * are joined into the term YouTube means; the recommendation section is read with the same music
 * row reader the search page uses.
 */
internal fun GetSearchSuggestionsResponse.toSearchSuggestions(): SearchSuggestions =
    SearchSuggestions(
        queries =
            contents
                ?.getOrNull(0)
                ?.searchSuggestionsSectionRenderer
                ?.contents
                ?.mapNotNull { content ->
                    content.searchSuggestionRenderer
                        ?.suggestion
                        ?.runs
                        ?.joinToString(separator = "") { it.text }
                }.orEmpty(),
        recommendedItems =
            contents
                ?.getOrNull(1)
                ?.searchSuggestionsSectionRenderer
                ?.contents
                ?.mapNotNull { content ->
                    content.musicResponsiveListItemRenderer?.let { renderer ->
                        SearchSuggestionPage.fromMusicResponsiveListItemRenderer(renderer)
                    }
                }.orEmpty(),
    )
