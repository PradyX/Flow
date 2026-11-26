package io.github.aedev.flow.presentation.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.aedev.flow.data.subscriptions.Subscription
import io.github.aedev.flow.innertube.InnerTube
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.github.aedev.flow.presentation.feed.ChannelBook
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Renders the real [ChannelScreen] offscreen against the captured channel page: the header YouTube
 * sent, its captured subscriber line, the first page of its Videos tab, a tap on a card, and the
 * subscribe control's two states.
 */
@OptIn(ExperimentalTestApi::class)
class ChannelScreenUiTest {
    private var previous: InnerTube? = null

    @AfterTest
    fun restoreClient() {
        previous?.let { YouTube.innerTube = it }
    }

    private val page by lazy { fixture("channel_page.json") }
    private val videos by lazy { fixture("channel_videos_page.json") }
    private val emptyTab by lazy { fixture("channel_empty_tab.json") }

    private val channelId = "UCiMhD4jzUqG-IgPzUmmytRQ"
    private val channelName = "Queen Official"
    private val firstVideo = "Queen - Sheer Heart Attack (Musikbox, February 11, 1979)"
    private val firstVideoId = "QIbT7qqnEDo"

    /** How many tab requests the mocked client has answered; the landing request carries no params. */
    private var tabRequests = 0

    /** How many sort-chip requests it has answered; those carry the chip's continuation token. */
    private var sortRequests = 0

    /** The channel landing and its tabs: the landing names them, a tab asks by params, a sort by token. */
    private fun installChannel(
        emptyTabs: Boolean = false,
        failFirstTab: Boolean = false,
    ) {
        tabRequests = 0
        sortRequests = 0
        val engine =
            MockEngine { request ->
                val body = request.body.toByteArray().decodeToString()
                val isTab = body.contains("\"params\"")
                val isSort = body.contains("\"continuation\"")
                if (isTab) tabRequests++
                if (isSort) sortRequests++
                respond(
                    content =
                        when {
                            // The landing opens Videos itself, so the empty page belongs to the tab
                            // the test taps afterwards.
                            isTab && failFirstTab && tabRequests == 2 -> ""

                            isTab && emptyTabs && tabRequests > 1 -> emptyTab

                            isTab || isSort -> videos

                            else -> page
                        },
                    status = if (isTab && failFirstTab && tabRequests == 2) HttpStatusCode.InternalServerError else HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        val client = InnerTube(engine)
        client.locale = YouTubeLocale(gl = "US", hl = "en")
        if (previous == null) previous = YouTube.innerTube
        YouTube.innerTube = client
    }

    @Test
    fun theCapturedChannelRendersItsHeaderAndVideos() =
        runComposeUiTest {
            installChannel()

            var opened: Pair<String, String>? = null
            setContent {
                MaterialTheme {
                    ChannelScreen(
                        channelId = channelId,
                        fallbackName = "Channel",
                        subscribed = false,
                        onSubscriptionChange = {},
                        onOpenVideo = { videoId, title, _ -> opened = videoId to title },
                        onOpenPlaylist = { _, _, _ -> },
                        onOpenChannel = { _, _ -> },
                        onOpenPost = {},
                        onBack = {},
                        book = ChannelBook(),
                    )
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(firstVideo).fetchSemanticsNodes().isNotEmpty()
            }
            // The name is in the top bar and again in the header block, as the app draws it.
            assertTrue(onAllNodesWithText(channelName).fetchSemanticsNodes().size >= 2)
            onNodeWithText(firstVideo).assertExists()

            onNodeWithText(firstVideo).performClick()
            assertTrue(opened != null, "the tap never reached the navigation callback")
            assertEquals(firstVideoId to firstVideo, opened)
        }

    /** The header offers Subscribe, then Subscribed with Unsubscribe in its menu, as the app does. */
    @Test
    fun theHeaderSubscribesAndUnsubscribes() =
        runComposeUiTest {
            installChannel()

            var subscribed by mutableStateOf(false)
            var changed: Subscription? = null
            setContent {
                MaterialTheme {
                    ChannelScreen(
                        channelId = channelId,
                        fallbackName = "Channel",
                        subscribed = subscribed,
                        onSubscriptionChange = { channel ->
                            changed = channel
                            subscribed = !subscribed
                        },
                        onOpenVideo = { _, _, _ -> },
                        onOpenPlaylist = { _, _, _ -> },
                        onOpenChannel = { _, _ -> },
                        onOpenPost = {},
                        onBack = {},
                        book = ChannelBook(),
                    )
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(firstVideo).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Subscribe").assertExists().performClick()
            assertTrue(subscribed)
            assertEquals(channelId, changed?.id)
            assertEquals(channelName, changed?.name)

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Subscribed").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Subscribed").performClick()
            onNodeWithText("Unsubscribe").assertExists().performClick()
            assertFalse(subscribed)
        }

    /** The sort chips above the Videos grid ask YouTube for the order they name. */
    @Test
    fun theSortChipsRequestTheChosenOrder() =
        runComposeUiTest {
            installChannel()

            setContent {
                MaterialTheme {
                    ChannelScreen(
                        channelId = channelId,
                        fallbackName = "Channel",
                        subscribed = false,
                        onSubscriptionChange = {},
                        onOpenVideo = { _, _, _ -> },
                        onOpenPlaylist = { _, _, _ -> },
                        onOpenChannel = { _, _ -> },
                        onOpenPost = {},
                        onBack = {},
                        book = ChannelBook(),
                    )
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(firstVideo).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Latest").assertExists()

            // The landing's own About request is not a sort; only the chip after it counts.
            sortRequests = 0
            onNodeWithText("Popular").performClick()
            waitUntil(timeoutMillis = 5_000) { sortRequests == 1 }
            onNodeWithText(firstVideo).assertExists()
        }

    /** Every tab asks for its own page, and About draws the facts the landing header carried. */
    @Test
    fun eachTabLoadsItsOwnPageAndAboutShowsTheHeaderFacts() =
        runComposeUiTest {
            installChannel()

            setContent {
                MaterialTheme {
                    ChannelScreen(
                        channelId = channelId,
                        fallbackName = "Channel",
                        subscribed = false,
                        onSubscriptionChange = {},
                        onOpenVideo = { _, _, _ -> },
                        onOpenPlaylist = { _, _, _ -> },
                        onOpenChannel = { _, _ -> },
                        onOpenPost = {},
                        onBack = {},
                        book = ChannelBook(),
                    )
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(firstVideo).fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(1, tabRequests, "the landing fetch already opened the Videos tab")

            onNodeWithText("Shorts").performClick()
            waitUntil(timeoutMillis = 5_000) { tabRequests == 2 }

            onNodeWithText("About").performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("More info").fetchSemanticsNodes().isNotEmpty()
            }
            // Opening About asks YouTube for nothing: its facts came with the landing fetch.
            assertEquals(2, tabRequests)
        }

    /** A tab the channel answers with no uploads draws its empty state instead of an empty grid. */
    @Test
    fun aTabWithNoUploadsShowsTheEmptyState() =
        runComposeUiTest {
            installChannel(emptyTabs = true)

            setContent {
                MaterialTheme {
                    ChannelScreen(
                        channelId = channelId,
                        fallbackName = "Channel",
                        subscribed = false,
                        onSubscriptionChange = {},
                        onOpenVideo = { _, _, _ -> },
                        onOpenPlaylist = { _, _, _ -> },
                        onOpenChannel = { _, _ -> },
                        onOpenPost = {},
                        onBack = {},
                        book = ChannelBook(),
                    )
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(firstVideo).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Shorts").performClick()
            waitUntil(timeoutMillis = 5_000) { tabRequests == 2 }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Nothing to show here yet").fetchSemanticsNodes().isNotEmpty()
            }
            assertTrue(onAllNodesWithText(firstVideo).fetchSemanticsNodes().isEmpty())
        }

    /** Home is a shelf tab: the same empty state covers it rather than an empty grid. */
    @Test
    fun theHomeTabWithNoShelvesShowsTheEmptyState() =
        runComposeUiTest {
            installChannel(emptyTabs = true)

            setContent {
                MaterialTheme {
                    ChannelScreen(
                        channelId = channelId,
                        fallbackName = "Channel",
                        subscribed = false,
                        onSubscriptionChange = {},
                        onOpenVideo = { _, _, _ -> },
                        onOpenPlaylist = { _, _, _ -> },
                        onOpenChannel = { _, _ -> },
                        onOpenPost = {},
                        onBack = {},
                        book = ChannelBook(),
                    )
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(firstVideo).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Home").performClick()
            waitUntil(timeoutMillis = 5_000) { tabRequests == 2 }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Nothing to show here yet").fetchSemanticsNodes().isNotEmpty()
            }
            assertTrue(onAllNodesWithText(firstVideo).fetchSemanticsNodes().isEmpty())
        }

    /** A failed Home request draws the retry line, and the retry asks again. */
    @Test
    fun aFailedHomeTabOffersARetryThatAsksAgain() =
        runComposeUiTest {
            installChannel(failFirstTab = true)

            setContent {
                MaterialTheme {
                    ChannelScreen(
                        channelId = channelId,
                        fallbackName = "Channel",
                        subscribed = false,
                        onSubscriptionChange = {},
                        onOpenVideo = { _, _, _ -> },
                        onOpenPlaylist = { _, _, _ -> },
                        onOpenChannel = { _, _ -> },
                        onOpenPost = {},
                        onBack = {},
                        book = ChannelBook(),
                    )
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(firstVideo).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Home").performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Could not load.").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Retry").performClick()
            waitUntil(timeoutMillis = 5_000) { tabRequests == 3 }
            assertTrue(onAllNodesWithText("Could not load.").fetchSemanticsNodes().isEmpty())
        }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) { "missing fixture $name" }
            .bufferedReader()
            .use { it.readText() }
}
