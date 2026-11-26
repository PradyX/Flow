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
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Renders the real [PlayerScreen] offscreen against the captured watch page and expands the pinned
 * comment's real reply thread: the toggle carries YouTube's own reply count, the expansion shows the
 * reply authors and texts the replies continuation actually returned, and collapsing hides them
 * again without disturbing the top-level rows.
 */
@OptIn(ExperimentalTestApi::class)
class PlayerReplyThreadUiTest {
    private var previous: InnerTube? = null

    @AfterTest
    fun restoreClient() {
        previous?.let { YouTube.innerTube = it }
    }

    private val entry by lazy { fixture("video_comments_pinned_entry.json") }
    private val page by lazy { fixture("video_comments_pinned_page.json") }
    private val replies by lazy { fixture("video_comment_replies.json") }

    private val videoTitle = "Queen – Bohemian Rhapsody (Official Video Remastered)"
    private val pinnedBadge = "Pinned by @Queen"
    private val nextTopLevel = "@bigc20"
    private val firstReplyAuthor = "@williamdebarros196"
    private val secondReplyAuthor = "@benjadominguez9600"
    private val firstReplyText = "Seek God. It's not about religion, it's not about rituals"

    @Test
    fun thePinnedCommentExpandsItsCapturedReplies() =
        runComposeUiTest {
            val replyToken =
                Regex("\"commentRepliesRenderer\".*?\"token\":\"([^\"]+)\"", RegexOption.DOT_MATCHES_ALL)
                    .find(page)
                    ?.groupValues
                    ?.get(1)
                    ?: error("the pinned page carries no replies token")
            val engine =
                MockEngine { request ->
                    val body = request.body.toByteArray().decodeToString()
                    respond(
                        content =
                            when {
                                body.contains(replyToken) -> replies
                                body.contains("\"continuation\"") -> page
                                else -> entry
                            },
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

            // YouTube's own reply count is the affordance, and the thread is closed until it is tapped.
            onNodeWithText("934 replies").performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(firstReplyAuthor).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText(firstReplyAuthor).assertExists()
            onNodeWithText(secondReplyAuthor).assertExists()
            onNodeWithText(firstReplyText, substring = true).assertExists()
            onNodeWithText("16K").assertExists()
            onNodeWithText("17K").assertExists()
            onNode(hasScrollAction()).performScrollToNode(hasText("Hide replies"))

            // Collapsing removes the thread and leaves the top-level rows in place.
            onNodeWithText("Hide replies").performClick()
            assertTrue(onAllNodesWithText(firstReplyAuthor).fetchSemanticsNodes().isEmpty())
            onNode(hasScrollAction()).performScrollToNode(hasText("934 replies"))
            onNodeWithText("934 replies").assertExists()
            onNodeWithText(nextTopLevel).assertExists()
        }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) { "missing fixture $name" }
            .bufferedReader()
            .use { it.readText() }
}
