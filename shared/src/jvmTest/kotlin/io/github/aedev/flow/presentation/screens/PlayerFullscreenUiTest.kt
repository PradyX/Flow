package io.github.aedev.flow.presentation.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

/**
 * Renders the real [PlayerScreen] and drives its fullscreen control: the page collapses to the
 * surface, the control turns into its exit form, and leaving fullscreen brings the page back. The
 * window effects themselves are Android-only and have no counterpart offscreen.
 */
@OptIn(ExperimentalTestApi::class)
class PlayerFullscreenUiTest {
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

    @Test
    fun fullscreenCollapsesThePageAndTogglesBack() =
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
                onAllNodesWithText(detailTitle).fetchSemanticsNodes().isNotEmpty()
            }

            onNodeWithContentDescription("Enter fullscreen").performClick()

            onNodeWithContentDescription("Exit fullscreen").assertExists()
            // Fullscreen is the video and nothing else: the page under it, and its back control,
            // are out of the list.
            onNodeWithText(detailTitle).assertDoesNotExist()
            onNodeWithContentDescription("Back").assertDoesNotExist()

            onNodeWithContentDescription("Exit fullscreen").performClick()

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(detailTitle).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithContentDescription("Enter fullscreen").assertExists()
        }
}
