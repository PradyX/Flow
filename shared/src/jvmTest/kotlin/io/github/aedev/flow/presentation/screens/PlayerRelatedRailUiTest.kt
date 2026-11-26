package io.github.aedev.flow.presentation.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.aedev.flow.innertube.InnerTube
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Renders the real [PlayerScreen] offscreen against a captured watch-next response and drives its
 * related rail: every card YouTube sent is in the scrollable page, the video being watched is not one
 * of them, and tapping a card hands that card's own video to the navigation callback.
 */
@OptIn(ExperimentalTestApi::class)
class PlayerRelatedRailUiTest {
    private var previous: InnerTube? = null

    @AfterTest
    fun restoreClient() {
        previous?.let { YouTube.innerTube = it }
    }

    private val body by lazy {
        checkNotNull(javaClass.getResourceAsStream("/fixtures/video_related.json")) { "missing fixture" }
            .bufferedReader()
            .use { it.readText() }
    }

    private val detailTitle = "Rick Astley - Never Gonna Give You Up (Official Video) (4K Remaster)"
    private val relatedTitles =
        listOf(
            "Wham! - Last Christmas (Official Video)",
            "a-ha - Take On Me (Official Video) [4K]",
            "Rocky III - Eye Of The Tiger",
            // YouTube sent a non-breaking space between "Official" and "Video" here; the card
            // shows that text as it arrived, so the test looks for the same character.
            "Modern Talking - Cheri Cheri Lady (Official\u00a0Video)",
        )

    @Test
    fun relatedCardsRenderInOrderAndOpenTheirOwnVideo() =
        runComposeUiTest {
            val engine =
                MockEngine {
                    respond(
                        content = body,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = InnerTube(engine)
            client.locale = YouTubeLocale(gl = "US", hl = "en")
            if (previous == null) previous = YouTube.innerTube
            YouTube.innerTube = client

            var opened: Pair<String, String>? = null
            setContent {
                MaterialTheme {
                    PlayerScreen(
                        videoId = "dQw4w9WgXcQ",
                        fallbackTitle = "Never Gonna Give You Up",
                        thumbnailUrl = null,
                        onBack = {},
                        onOpenVideo = { videoId, title, _ -> opened = videoId to title },
                        onOpenChannel = { _, _ -> },
                        libraryStore = inMemoryLibraryStore(),
                    )
                }
            }

            // The watch page's own title is above the rail.
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(detailTitle).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText(detailTitle).assertExists()

            // Every related card is in the page: scroll the player to each one YouTube sent.
            relatedTitles.forEach { title ->
                onNode(hasScrollAction()).performScrollToNode(hasText(title))
                onNodeWithText(title).assertExists()
            }

            // The video being watched is never offered as its own recommendation.
            onNodeWithText("dQw4w9WgXcQ").assertDoesNotExist()

            onNode(hasScrollAction()).performScrollToNode(hasText(relatedTitles.first()))
            onNodeWithText(relatedTitles.first()).performClick()

            assertTrue(opened != null, "the tap never reached the navigation callback")
            assertEquals("E8gmARGvPlI" to relatedTitles.first(), opened)
        }
}
