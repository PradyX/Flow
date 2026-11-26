package io.github.aedev.flow.presentation.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.aedev.flow.data.downloads.DownloadEntry
import io.github.aedev.flow.data.downloads.Downloads
import io.github.aedev.flow.data.library.LibraryStore
import io.github.aedev.flow.innertube.InnerTube
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Renders the real [LibraryScreen] offscreen. The library request is the captured signed-out
 * landing response (YouTube's own sign-in message), and the saved rows come from the Room store,
 * seeded with the video the channel capture carried.
 */
@OptIn(ExperimentalTestApi::class)
class LibraryScreenUiTest {
    private var previous: InnerTube? = null

    @AfterTest
    fun restoreClient() {
        previous?.let { YouTube.innerTube = it }
    }

    private val landing by lazy { fixture("library_landing_signed_out.json") }

    private val capturedId = "QIbT7qqnEDo"
    private val capturedTitle = "Queen - Sheer Heart Attack (Musikbox, February 11, 1979)"
    private val capturedChannel = "Queen Official"
    private val capturedThumbnail = "https://i.ytimg.com/vi/QIbT7qqnEDo/hq720.jpg"

    private val notice = "Explore your favorites"
    private val noticeSubtext = "Sign in to access tracks that you liked or saved"

    private fun installLibrary() {
        val engine =
            MockEngine {
                respond(
                    content = landing,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        val client = InnerTube(engine)
        client.locale = YouTubeLocale(gl = "US", hl = "en")
        if (previous == null) previous = YouTube.innerTube
        YouTube.innerTube = client
    }

    private suspend fun LibraryStore.seedCapturedVideo() {
        toggleSaved(capturedId, capturedTitle, capturedChannel, capturedThumbnail, nowMillis = 1_700_000_000_000)
    }

    @Test
    fun theSavedRowRendersAndItsDownloadToggleAsksTheHostForADownload() =
        runComposeUiTest {
            installLibrary()
            val store = inMemoryLibraryStore()
            kotlinx.coroutines.runBlocking { store.seedCapturedVideo() }

            var opened: Pair<String, String>? = null
            var requested: String? = null
            // The host owns the bytes: the row reports the tap, and the state it draws comes back
            // in as the download record once the fetch lands.
            var downloads by mutableStateOf(Downloads())
            setContent {
                MaterialTheme {
                    LibraryScreen(
                        store = store,
                        downloads = downloads,
                        onToggleDownload = { item -> requested = item.videoId },
                        onOpenVideo = { videoId, title, _ -> opened = videoId to title },
                    )
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(capturedTitle).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText(capturedTitle).assertExists()
            onNodeWithText(capturedChannel).assertExists()
            // YouTube's own sign-in message from the captured response, not a hardcoded string.
            onNodeWithText(notice).assertExists()
            onNodeWithText(noticeSubtext).assertExists()

            onNodeWithContentDescription("Download").performClick()
            waitUntil(timeoutMillis = 5_000) { requested == capturedId }
            assertEquals(capturedId, requested, "the row never asked the host to download it")

            // The download landing is what turns the row into its downloaded state and puts it
            // under the downloads filter.
            downloads =
                Downloads().with(
                    DownloadEntry(videoId = capturedId, title = capturedTitle, videoUrl = "https://example.test/v"),
                )
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithContentDescription("Downloaded").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithContentDescription("Downloaded").assertExists()
            onNodeWithText("Downloads").performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(capturedTitle).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText(capturedTitle).assertExists()

            onNodeWithText(capturedTitle).performClick()
            assertEquals(capturedId to capturedTitle, opened)
        }

    @Test
    fun aFailedDownloadIsReportedAboveTheRows() =
        runComposeUiTest {
            installLibrary()
            val store = inMemoryLibraryStore()
            kotlinx.coroutines.runBlocking { store.seedCapturedVideo() }
            val failure = "Could not reach this video's streams"

            setContent {
                MaterialTheme {
                    LibraryScreen(
                        store = store,
                        downloadError = failure,
                        onOpenVideo = { _, _, _ -> },
                    )
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(capturedTitle).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText(failure).assertExists()
        }

    @Test
    fun anEmptyStoreShowsTheEmptyState() =
        runComposeUiTest {
            installLibrary()
            val store = inMemoryLibraryStore()

            setContent {
                MaterialTheme {
                    LibraryScreen(store = store, onOpenVideo = { _, _, _ -> })
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Nothing saved yet. Save a video from the player to see it here.")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            onNodeWithText(notice).assertExists()
        }

    @Test
    fun aFailedLibraryRequestShowsTheRetryState() =
        runComposeUiTest {
            val engine =
                MockEngine {
                    respond(
                        content = "{}",
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = InnerTube(engine)
            client.locale = YouTubeLocale(gl = "US", hl = "en")
            if (previous == null) previous = YouTube.innerTube
            YouTube.innerTube = client

            setContent {
                MaterialTheme {
                    LibraryScreen(store = inMemoryLibraryStore(), onOpenVideo = { _, _, _ -> })
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Could not load.").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Retry").assertExists()
        }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) { "missing fixture $name" }
            .bufferedReader()
            .use { it.readText() }
}
