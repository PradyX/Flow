package io.github.aedev.flow.presentation.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.aedev.flow.data.search.SearchHistory
import io.github.aedev.flow.innertube.InnerTube
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.github.aedev.flow.presentation.feed.FeedEntry
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.SearchFeed
import io.github.aedev.flow.presentation.feed.SearchFeedState
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.util.Collections
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Drives the real [SearchScreen] the way a person does — type into the field, pick a completion,
 * come back to an empty field, scroll to the end of the results — against a fake client that serves
 * the captured YouTube pages. Each test asserts both what is drawn and the state the screen reports,
 * since the panel, the recents and the footer are the screen's own behaviour rather than the
 * loader's.
 */
@OptIn(ExperimentalTestApi::class)
class SearchFlowUiTest {
    private var previous: InnerTube? = null

    @AfterTest
    fun restoreClient() {
        previous?.let { YouTube.innerTube = it }
    }

    private val searchPage by lazy { fixture("search_chips.json") }
    private val nextPage by lazy { fixture("search_chips_shorts.json") }
    private val suggestionsPage by lazy { fixture("search_suggestions.json") }

    private val firstTitle = "Rick Astley - Never Gonna Give You Up (Official Music Video) (1987)"
    private val shortsTitle = "Never Gonna Give You Up | Rick Astley Rocks New Year's Eve - BBC"
    private val completion = "arijit singh songs"

    @Test
    fun typedTermShowsYouTubeCompletionsAndPickingOneRunsIt() =
        runComposeUiTest {
            installClient { _, url -> if (url.contains("get_search_suggestions")) suggestionsPage else searchPage }
            val state = SearchFeedState()
            setContent { MaterialTheme { Screen(state) } }

            onNode(hasSetTextAction()).performTextInput("arijit")
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(completion).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText(completion).assertExists()

            onNodeWithText(completion).performClick()

            waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(firstTitle).fetchSemanticsNodes().isNotEmpty() }
            assertEquals(completion, state.query)
            assertEquals(completion, state.history.queries.first())
            // The panel is gone: the field keeps the term, the results are under it.
            onAllNodesWithText("arijit singh mashup").assertCountEquals(0)
        }

    @Test
    fun anEmptyFieldShowsTheSearchesThisDeviceRan() =
        runComposeUiTest {
            installClient { _, _ -> searchPage }
            val state = SearchFeedState(history = SearchHistory(listOf("cats", "dogs")))
            setContent { MaterialTheme { Screen(state) } }

            onNodeWithText("Recent searches").assertExists()
            onNodeWithText("cats").assertExists()
            onNodeWithText("dogs").assertExists()

            onAllNodesWithContentDescription("Remove this search")[0].performClick()
            waitUntil(timeoutMillis = 5_000) { state.history.queries == listOf("dogs") }

            onNodeWithText("Clear").performClick()
            waitUntil(timeoutMillis = 5_000) { state.history.isEmpty }
            onNodeWithText("Recent searches").assertDoesNotExist()
        }

    @Test
    fun theEndOfTheResultsAsksForTheNextPageAndAppendsIt() =
        runComposeUiTest {
            val continuedBodies = Collections.synchronizedList(mutableListOf<String>())
            installClient { body, _ ->
                if (body.contains("TOKEN_MORE")) {
                    continuedBodies += body
                    nextPage
                } else {
                    searchPage
                }
            }
            val seeded = FeedEntry(id = "seed", title = "Seeded card", subtitle = null, videoId = "seed")
            val state =
                SearchFeedState(
                    query = "rick astley",
                    results =
                        LoadState.Ready(
                            SearchFeed(entries = listOf(seeded), estimatedResults = null, continuation = "TOKEN_MORE"),
                        ),
                )
            setContent { MaterialTheme { Screen(state) } }

            waitUntil(timeoutMillis = 5_000) {
                val ready = state.results as? LoadState.Ready
                ready != null && ready.value.entries.size > 1
            }

            val ready = state.results as LoadState.Ready
            assertTrue(ready.value.entries.any { it.id == "seed" })
            assertTrue(ready.value.entries.any { it.title == shortsTitle })
            assertEquals(1, continuedBodies.size)
            assertTrue(continuedBodies.single().contains("TOKEN_MORE"))
        }

    @androidx.compose.runtime.Composable
    private fun Screen(state: SearchFeedState) {
        SearchScreen(
            state = state,
            onOpenVideo = { _, _, _ -> },
            onOpenPlaylist = { _, _, _ -> },
            onOpenChannel = { _, _ -> },
        )
    }

    private fun installClient(response: (body: String, url: String) -> String) {
        val engine =
            MockEngine { request ->
                val body = request.body.toByteArray().decodeToString()
                respond(
                    content = response(body, request.url.toString()),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        val client = InnerTube(engine)
        client.locale = YouTubeLocale(gl = "US", hl = "en")
        if (previous == null) previous = YouTube.innerTube
        YouTube.innerTube = client
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) { "missing fixture $name" }
            .bufferedReader()
            .use { it.readText() }
}
