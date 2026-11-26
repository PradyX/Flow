package io.github.aedev.flow.innertube

import io.github.aedev.flow.innertube.models.ArtistItem
import io.github.aedev.flow.innertube.models.SongItem
import io.github.aedev.flow.innertube.models.response.GetSearchSuggestionsResponse
import io.github.aedev.flow.innertube.models.toSearchSuggestions
import io.github.aedev.flow.presentation.feed.toSearchQueries
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * One real `music/get_search_suggestions` response for the input `arijit`: each suggestion's runs
 * are the typed part plus its continuation, so joining them is what yields six distinct terms.
 */
class SearchSuggestionsFixtureTest {
    private fun fixture(): String =
        javaClass.classLoader!!
            .getResourceAsStream("fixtures/search_suggestions.json")!!
            .bufferedReader()
            .use { it.readText() }

    private fun response(): GetSearchSuggestionsResponse =
        // The client's own reader: unknown keys are ignored and absent nullable fields are null.
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }.decodeFromString<GetSearchSuggestionsResponse>(fixture())

    @Test
    fun `the runs of a suggestion are joined into the term it stands for`() {
        val suggestions = response().toSearchSuggestions()

        assertEquals(
            listOf(
                "arijit singh",
                "arijit singh songs",
                "arijit singh song",
                "arijit singh mashup",
                "arijit singh sad song",
                "arijit",
            ),
            suggestions.queries,
        )
    }

    @Test
    fun `the recommendations are read as the mix the server sent`() {
        val items = response().toSearchSuggestions().recommendedItems

        // The captured section holds an artist, a playlist and three songs; the playlist row is not
        // one of the music row shapes the reader takes, so the artist and her three songs come
        // through, in the order the server sent them.
        assertEquals(4, items.size)
        assertEquals("Arijit Singh", items.first().title)
        assertTrue(items.first() is ArtistItem)
        assertTrue(items.drop(1).all { it is SongItem })
    }

    @Test
    fun `the panel shows no repeated term`() {
        val queries = response().toSearchSuggestions().queries.toSearchQueries()

        assertEquals(queries.distinct(), queries)
        assertTrue(queries.all { it.isNotBlank() && it == it.trim() })
    }
}
