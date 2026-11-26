package io.github.aedev.flow.presentation.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
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
import kotlin.test.assertTrue

/**
 * Renders the real [PlayerScreen] offscreen against the captured watch page and drives its comment
 * section: the heading carries the total the watch response sent, every comment in the captured
 * page is in the scrollable body, and a specific comment's author, text and like count are the ones
 * YouTube served.
 */
@OptIn(ExperimentalTestApi::class)
class PlayerCommentsUiTest {
    private var previous: InnerTube? = null

    @AfterTest
    fun restoreClient() {
        previous?.let { YouTube.innerTube = it }
    }

    private val entry by lazy { fixture("video_comments_entry.json") }
    private val page by lazy { fixture("video_comments_page.json") }

    private val videoTitle = "Rick Astley - Never Gonna Give You Up (Official Video) (4K Remaster)"
    private val total = "2.4M"
    private val authors =
        listOf(
            "@ELIBRON-i2g",
            "@GabrielsEpicLifeofGoals",
            "@grustache4569",
            "@candycake9531",
            "@DarkstalkerThe_IcyNightwing",
            "@JetpackGamer456",
        )
    private val topComment = "Yall still getting rick rolled after gta6"
    private val popularComment = "Is everyone going to ignore the fact that Rick Astley looks like a twelve year old boy"

    @Test
    fun commentsShowTheCapturedAuthorsTextAndCounts() =
        runComposeUiTest {
            val engine =
                MockEngine { request ->
                    // The watch request names the video; the comment page is the continuation that
                    // follows it, exactly as the loader asks for it.
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
                        videoId = "dQw4w9WgXcQ",
                        fallbackTitle = "Never Gonna Give You Up",
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

            // The heading is the watch page's own total, and every comment the page carried is below it.
            onNode(hasScrollAction()).performScrollToNode(hasText("Comments"))
            onNodeWithText("Comments").assertExists()
            onNodeWithText(total).assertExists()
            authors.forEach { author ->
                onNode(hasScrollAction()).performScrollToNode(hasText(author))
                onNodeWithText(author).assertExists()
            }

            // The top comment's text and like/reply counts are the ones YouTube sent.
            onNode(hasScrollAction()).performScrollToNode(hasText(topComment))
            onNodeWithText(topComment).assertExists()
            onNodeWithText("@ELIBRON-i2g").assertExists()
            onNodeWithText("14").assertExists()
            onNodeWithText("1 reply").assertExists()

            // A comment further down keeps YouTube's own abbreviated like count and reply total.
            onNode(hasScrollAction()).performScrollToNode(hasText(popularComment, substring = true))
            onNodeWithText(popularComment, substring = true).assertExists()
            onNodeWithText("294K").assertExists()
            onNodeWithText("736 replies").assertExists()
            assertTrue(authors.size == 6)
        }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) { "missing fixture $name" }
            .bufferedReader()
            .use { it.readText() }
}
