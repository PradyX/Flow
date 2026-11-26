package io.github.aedev.flow.presentation.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.aedev.flow.innertube.InnerTube
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.SearchFeedState
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import java.util.Collections
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Renders the real [SearchScreen] offscreen and drives its chip bar the way a person does: submit a
 * query, read the chips YouTube sent, tap one, and let its continuation answer arrive.
 *
 * The fake client stands in for YouTube and holds the continuation open until the test releases it,
 * so the in-flight frame can be inspected. Every request body is recorded, which is what proves a
 * tap goes out as a continuation rather than as a second query.
 */
@OptIn(ExperimentalTestApi::class)
class SearchFilterChipUiTest {
    private var previous: InnerTube? = null

    @AfterTest
    fun restoreClient() {
        previous?.let { YouTube.innerTube = it }
    }

    private val firstPage by lazy { fixture("search_chips.json") }
    private val shortsPage by lazy { fixture("search_chips_shorts.json") }

    private val chipLabels = listOf("All", "Shorts", "Videos")
    private val firstTitle = "Rick Astley - Never Gonna Give You Up (Official Music Video) (1987)"
    private val shortsTitle = "Never Gonna Give You Up | Rick Astley Rocks New Year's Eve - BBC"

    @Test
    fun chipTapLoadsItsContinuationAndKeepsTheBar() =
        runComposeUiTest {
            val continuationRequested = CompletableDeferred<String>()
            val releaseContinuation = CompletableDeferred<Unit>()
            val bodies = Collections.synchronizedList(mutableListOf<String>())

            val engine =
                MockEngine { request ->
                    val body = request.body.toByteArray().decodeToString()
                    bodies += body
                    val token = CHIP_TOKENS.firstOrNull { body.contains(it) }
                    val page =
                        if (token == null) {
                            firstPage
                        } else {
                            continuationRequested.complete(token)
                            releaseContinuation.await()
                            shortsPage
                        }
                    respond(
                        content = page,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = InnerTube(engine)
            client.locale = YouTubeLocale(gl = "US", hl = "en")
            if (previous == null) previous = YouTube.innerTube
            YouTube.innerTube = client

            val state = SearchFeedState()
            setContent {
                MaterialTheme {
                    SearchScreen(
                        state = state,
                        onOpenVideo = { _, _, _ -> },
                        onOpenPlaylist = { _, _, _ -> },
                        onOpenChannel = { _, _ -> },
                    )
                }
            }

            onNode(hasSetTextAction()).performTextInput("rick astley")
            onNode(hasSetTextAction()).performImeAction()

            waitUntil(timeoutMillis = 5_000) { onAllNodes(isSelectable()).fetchSemanticsNodes().size == chipLabels.size }

            // The bar is YouTube's own set, in YouTube's own order, with YouTube's own selection.
            val lefts = chipLabels.map { label -> onNodeWithText(label).fetchSemanticsNode().boundsInRoot.left }
            assertEquals(lefts.sorted(), lefts)
            chipLabels.forEach { label -> onNodeWithText(label).assertExists() }
            onNodeWithText("All").assertIsSelected()
            onNodeWithText("Shorts").assertIsNotSelected()
            onNodeWithText("Videos").assertIsNotSelected()
            onNodeWithText(firstTitle).assertExists()

            onNodeWithText("Shorts").performClick()

            // The tap went out as that chip's continuation, and the bar is still up while it is in flight.
            waitUntil(timeoutMillis = 5_000) { continuationRequested.isCompleted }
            assertEquals("TOKEN_SHORTS", continuationRequested.await())
            assertTrue(state.results is LoadState.Loading)
            chipLabels.forEach { label -> onNodeWithText(label).assertExists() }

            releaseContinuation.complete(Unit)

            waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(shortsTitle).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText(shortsTitle).assertExists()
            onNodeWithText(firstTitle).assertDoesNotExist()
            onNodeWithText("Shorts").assertIsSelected()
            onNodeWithText("All").assertIsNotSelected()
            chipLabels.forEach { label -> onNodeWithText(label).assertExists() }

            // Exactly one first-page query and one continuation: the filter re-sent no query.
            assertEquals(2, bodies.size)
            assertTrue(bodies[1].contains("TOKEN_SHORTS"))
            assertTrue(!bodies[1].contains("rick astley"))
        }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) { "missing fixture $name" }
            .bufferedReader()
            .use { it.readText() }

    private companion object {
        val CHIP_TOKENS = listOf("TOKEN_SHORTS", "TOKEN_VIDEOS", "TOKEN_ALL")
    }
}
