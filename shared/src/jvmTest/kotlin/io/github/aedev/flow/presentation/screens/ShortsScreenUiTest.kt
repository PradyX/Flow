package io.github.aedev.flow.presentation.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
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
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Renders the real [ShortsScreen] offscreen against the captured reel sequence and reel overlays:
 * the title and channel YouTube sent for the first reel, its still, and a vertical swipe that moves
 * the pager onto the second captured reel.
 */
@OptIn(ExperimentalTestApi::class)
class ShortsScreenUiTest {
    private var previous: InnerTube? = null

    @AfterTest
    fun restoreClient() {
        previous?.let { YouTube.innerTube = it }
    }

    private val firstSequence by lazy { fixture("shorts_sequence.json") }
    private val moreSequence by lazy { fixture("shorts_sequence_more.json") }

    private val firstId = "QUIJ8UtYBzA"
    private val secondId = "xMHuKwf18vM"
    private val firstChannel = "@shorts_kingdom01"
    private val secondChannel = "@Virat_zxv"
    private val firstTitlePrefix = "Uncle's kindness towards his friend"

    @Test
    fun theCapturedReelRendersItsStillTitleAndChannelAndSwipesOn() =
        runComposeUiTest {
            val engine =
                MockEngine { request ->
                    val body = request.body.toByteArray().decodeToString()
                    val path = request.url.encodedPath
                    val content =
                        when {
                            // The next page of the sequence goes back in the same field, as a token.
                            path.endsWith("reel/reel_watch_sequence") && !body.contains("\"sequenceParams\":\"CA8%3D\"") -> {
                                moreSequence
                            }

                            path.endsWith("reel/reel_watch_sequence") -> {
                                firstSequence
                            }

                            else -> {
                                val id =
                                    Regex("\"videoId\":\"([^\"]+)\"")
                                        .find(body)
                                        ?.groupValues
                                        ?.get(1)
                                        ?: error("no videoId in ${request.url}")
                                fixture("shorts_overlay_$id.json")
                            }
                        }
                    respond(
                        content = content,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = InnerTube(engine)
            client.locale = YouTubeLocale(gl = "US", hl = "en")
            if (previous == null) previous = YouTube.innerTube
            YouTube.innerTube = client

            val store = inMemoryLibraryStore()
            setContent {
                MaterialTheme {
                    ShortsScreen(
                        libraryStore = store,
                        onOpenVideo = { _, _, _ -> },
                        onOpenChannel = { _, _ -> },
                    )
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(firstChannel).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText(firstChannel).assertExists()
            assertTrue(
                onAllNodesWithText(firstTitlePrefix, substring = true).fetchSemanticsNodes().isNotEmpty(),
                "the captured reel title is not on screen",
            )
            onNodeWithTag(shortStillTag(firstId)).assertExists()

            // The reel's save control writes the short into the library's own collection.
            onNodeWithContentDescription("Save").performClick()
            waitUntil(timeoutMillis = 5_000) { runBlocking { store.savedShorts().isNotEmpty() } }
            assertTrue(
                runBlocking { store.savedShorts() }.any { it.videoId == firstId },
                "the saved reel is not in the saved-shorts collection",
            )

            onNodeWithTag(SHORTS_PAGER_TEST_TAG).performTouchInput { swipeUp(durationMillis = 600) }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(secondChannel).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText(secondChannel).assertExists()
            onNodeWithTag(shortStillTag(secondId)).assertExists()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(shortStillTag(secondId), useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .any { it.boundsInRoot.width > 0f }
            }
            // The reel that swiped away is no longer the frame on screen; the pager may hold its
            // node for a frame after the settle, so the check is on what is actually laid out.
            assertTrue(
                onAllNodesWithTag(shortStillTag(firstId), useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .none { it.boundsInRoot.width > 0f },
                "the first reel is still on screen after paging",
            )
        }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) { "missing fixture $name" }
            .bufferedReader()
            .use { it.readText() }
}
