package io.github.aedev.flow.presentation.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.aedev.flow.data.library.LibraryStore
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Renders the real [SavedShortsScreen] offscreen over the Room store: the empty collection the
 * library starts with, the shorts the reel saved, and the tap that opens one in the player.
 */
@OptIn(ExperimentalTestApi::class)
class SavedShortsScreenUiTest {
    private val shortId = "QUIJ8UtYBzA"
    private val shortTitle = "Uncle's kindness towards his friend"
    private val shortChannel = "@shorts_kingdom01"
    private val videoId = "QIbT7qqnEDo"
    private val videoTitle = "Queen - Sheer Heart Attack (Musikbox, February 11, 1979)"

    private suspend fun LibraryStore.seedSavedShorts() {
        toggleSaved(
            videoId = shortId,
            title = shortTitle,
            channelName = shortChannel,
            thumbnailUrl = null,
            nowMillis = 1_700_000_000_000,
            isShort = true,
        )
        toggleSaved(
            videoId = "xMHuKwf18vM",
            title = "Virat Kohli's cover drive",
            channelName = "@Virat_zxv",
            thumbnailUrl = null,
            nowMillis = 1_700_000_000_001,
            isShort = true,
        )
        // A saved video: the collection must not draw it.
        toggleSaved(videoId, videoTitle, "Queen Official", null, nowMillis = 1_700_000_000_002)
    }

    @Test
    fun anEmptyCollectionDrawsItsOwnLineAndNoGrid() =
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    SavedShortsScreen(
                        store = inMemoryLibraryStore(),
                        onBack = {},
                        onOpenVideo = { _, _, _ -> },
                    )
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Shorts you save from the reel show up here.").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag(SAVED_SHORTS_GRID_TEST_TAG).assertDoesNotExist()
        }

    @Test
    fun onlyTheSavedShortsAreDrawnAndOneOpensInThePlayer() =
        runComposeUiTest {
            val store = inMemoryLibraryStore()
            runBlocking { store.seedSavedShorts() }

            var opened: Triple<String, String, String?>? = null
            setContent {
                MaterialTheme {
                    SavedShortsScreen(
                        store = store,
                        onBack = {},
                        onOpenVideo = { videoId, title, thumbnailUrl -> opened = Triple(videoId, title, thumbnailUrl) },
                    )
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(shortTitle).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText(shortTitle).assertExists()
            onNodeWithText(shortChannel).assertExists()
            onNodeWithTag(SAVED_SHORTS_GRID_TEST_TAG).assertExists()
            assertTrue(
                onAllNodesWithText(videoTitle).fetchSemanticsNodes().isEmpty(),
                "a saved video that is not a short was drawn in the saved-shorts grid",
            )

            onNodeWithTag(savedShortTag(shortId)).performClick()
            waitUntil(timeoutMillis = 5_000) { opened != null }
            assertEquals(Triple(shortId, shortTitle, null as String?), opened)
        }

    @Test
    fun backLeavesTheCollection() =
        runComposeUiTest {
            var backed = false
            setContent {
                MaterialTheme {
                    SavedShortsScreen(
                        store = inMemoryLibraryStore(),
                        onBack = { backed = true },
                        onOpenVideo = { _, _, _ -> },
                    )
                }
            }

            onNodeWithContentDescription("Back").performClick()
            assertTrue(backed, "the back control did not leave the saved shorts")
        }
}
