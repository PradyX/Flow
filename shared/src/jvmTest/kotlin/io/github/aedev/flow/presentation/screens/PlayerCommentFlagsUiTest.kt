package io.github.aedev.flow.presentation.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.aedev.flow.innertube.InnerTube
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Renders the real [PlayerScreen] offscreen against a captured watch page whose comment section
 * really does hold a pinned uploader comment. Both flags come from YouTube's own fields in that
 * payload — the `RENDERING_PRIORITY_PINNED_COMMENT` priority with its `Pinned by @Queen` line, and
 * the author's `isCreator` mark — so this proves the row treatment on real data.
 */
@OptIn(ExperimentalTestApi::class)
class PlayerCommentFlagsUiTest {
    private var previous: InnerTube? = null

    @AfterTest
    fun restoreClient() {
        previous?.let { YouTube.innerTube = it }
    }

    private val entry by lazy { fixture("video_comments_pinned_entry.json") }
    private val page by lazy { fixture("video_comments_pinned_page.json") }

    private val videoTitle = "Queen – Bohemian Rhapsody (Official Video Remastered)"
    private val pinnedBadge = "Pinned by @Queen"
    private val pinnedText =
        "Thank you to all our fans across the World for helping us hit ONE BILLION VIEWS. " +
            "Comment down below your favorite memory of Queen"
    private val ordinaryAuthor = "@bigc20"

    @Test
    fun theCapturedPinnedUploaderCommentWearsBothMarkers() =
        runComposeUiTest {
            val engine =
                MockEngine { request ->
                    val body = request.body.toByteArray().decodeToString()
                    respond(
                        content = if (body.contains("\"continuation\"")) page else entry,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = InnerTube(engine)
            client.locale = YouTubeLocale(gl = "US", hl = "en")
            if (previous == null) previous = YouTube.innerTube
            YouTube.innerTube = client

            setContent {
                MaterialTheme {
                    PlayerScreen(
                        videoId = "fJ9rUzIMcZQ",
                        fallbackTitle = "Bohemian Rhapsody",
                        thumbnailUrl = null,
                        onBack = {},
                        onOpenVideo = { _, _, _ -> },
                        onOpenChannel = { _, _ -> },
                        libraryStore = inMemoryLibraryStore(),
                    )
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(videoTitle).fetchSemanticsNodes().isNotEmpty()
            }

            onNode(hasScrollAction()).performScrollToNode(hasText(pinnedBadge))
            onNodeWithText(pinnedBadge).assertExists()
            onNodeWithText(pinnedText, substring = true).assertExists()
            onNodeWithText("@Queen").assertExists()
            onNodeWithText("533K").assertExists()
            onNodeWithText("934 replies").assertExists()

            // The badge is YouTube's own line on the pinned thread, so it appears exactly once.
            assertEquals(1, onAllNodesWithText(pinnedBadge).fetchSemanticsNodes().size)

            // The uploader marker is on the channel owner's row only, not on the fan comments.
            onNodeWithContentDescription("Creator").assertExists()
            onNode(hasScrollAction()).performScrollToNode(hasText(ordinaryAuthor))
            onNodeWithText(ordinaryAuthor).assertExists()
            assertEquals(1, onAllNodesWithContentDescription("Creator").fetchSemanticsNodes().size)
        }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) { "missing fixture $name" }
            .bufferedReader()
            .use { it.readText() }
}
