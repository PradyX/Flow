package io.github.aedev.flow.presentation.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
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
import java.util.Collections
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The channel page's two ways of reading more of YouTube: its own search (the magnifier on the
 * channel bar) and the next page of a tab. Both run against captured pages through the real screen,
 * so the request shapes, the field, the results and the footer are exercised rather than the
 * loaders alone.
 */
@OptIn(ExperimentalTestApi::class)
class ChannelSearchUiTest {
    private var previous: InnerTube? = null

    @AfterTest
    fun restoreClient() {
        previous?.let { YouTube.innerTube = it }
    }

    private val landing by lazy { fixture("channel_page.json") }
    private val tabOne by lazy { fixture("channel_videos_page_one.json") }
    private val tabMore by lazy { fixture("channel_videos_page_more.json") }
    private val searchPage by lazy { fixture("channel_search_page.json") }
    private val searchMore by lazy { fixture("channel_search_more.json") }

    // The landing page is the captured Queen channel; the tab and search captures are other
    // channels' real pages, served in its place, which is what makes this a mock harness.
    private val channelId = "UCiMhD4jzUqG-IgPzUmmytRQ"
    private val channelName = "Queen Official"
    private val term = "space"

    private fun install(bodies: MutableList<String> = Collections.synchronizedList(mutableListOf())) {
        val searchToken = tokenOf(searchPage)
        val tabToken = tokenOf(tabOne)
        val engine =
            MockEngine { request ->
                val body = request.body.toByteArray().decodeToString()
                bodies += body
                respond(
                    content =
                        when {
                            body.contains("\"query\"") -> searchPage
                            body.contains(searchToken) -> searchMore
                            body.contains(tabToken) -> tabMore
                            body.contains("\"params\"") -> tabOne
                            else -> landing
                        },
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        val client = InnerTube(engine)
        client.locale = YouTubeLocale(gl = "US", hl = "en")
        if (previous == null) previous = YouTube.innerTube
        YouTube.innerTube = client
    }

    @Test
    fun theMagnifierSearchesThisChannelAndTypingShowsItsRows() =
        runComposeUiTest {
            val bodies = Collections.synchronizedList(mutableListOf<String>())
            install(bodies)
            val searchTitle = firstVideoTitle(searchPage)

            setContent { MaterialTheme { Screen() } }
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(channelName).fetchSemanticsNodes().isNotEmpty() }

            onNodeWithContentDescription("Search this channel").performClick()
            onNode(hasSetTextAction()).performTextInput(term)

            waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(searchTitle).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText(searchTitle).assertExists()

            // The term went out against this channel, through the search tab's own token.
            val searchBodies = bodies.filter { it.contains("\"query\"") }
            assertEquals(1, searchBodies.size)
            assertTrue(searchBodies.single().contains(channelId))
            assertTrue(searchBodies.single().contains(term))

            onNodeWithContentDescription("Cancel").performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(searchTitle).fetchSemanticsNodes().isEmpty() }
        }

    @Test
    fun theGridFooterReadsTheNextPageOfTheSameTab() =
        runComposeUiTest {
            val bodies = Collections.synchronizedList(mutableListOf<String>())
            install(bodies)
            val firstTitle = firstVideoTitle(tabOne)
            val nextTitle = firstVideoTitle(tabMore)

            setContent { MaterialTheme { Screen() } }
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(firstTitle).fetchSemanticsNodes().isNotEmpty() }

            onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("Load more"))
            onNodeWithText("Load more").performClick()

            waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(nextTitle).fetchSemanticsNodes().isNotEmpty() }

            assertEquals(1, bodies.count { it.contains(tokenOf(tabOne)) })
            assertTrue(bodies.none { it.contains("\"query\"") }, "paging a tab is not a search")
        }

    @androidx.compose.runtime.Composable
    private fun Screen() {
        ChannelScreen(
            channelId = channelId,
            fallbackName = channelName,
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

    /** The first token in the page: both captures hold exactly one continuation. */
    private fun tokenOf(json: String): String = Regex("\"token\":\"([^\"]+)\"").find(json)!!.groupValues[1]

    /**
     * The first video title in a captured page, in either shape YouTube sends it: a
     * `videoRenderer` whose title is a run list, or a `lockupViewModel` whose title is one string.
     */
    private fun firstVideoTitle(json: String): String {
        Regex("\"videoRenderer\":\\{\"videoId\":\".*?\"title\":\\{\"runs\":\\[(.*?)\\]")
            .find(json)
            ?.let { match ->
                return Regex("\"text\":\"(.*?)\"").findAll(match.groupValues[1]).joinToString("") { it.groupValues[1] }
            }
        val lockup =
            Regex("\"lockupViewModel\":.*?\"lockupMetadataViewModel\":\\{\"title\":\\{\"content\":\"([^\"]+)\"")
                .find(json) ?: error("no video title in the captured page")
        return lockup.groupValues[1]
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) { "missing fixture $name" }
            .bufferedReader()
            .use { it.readText() }
}
