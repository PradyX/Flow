package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.aedev.flow.data.model.LiveChatMessage
import io.github.aedev.flow.innertube.InnerTube
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.github.aedev.flow.presentation.feed.liveChatMessages
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Renders the player's live-chat rows over the captured poll: the authors and messages YouTube
 * sent, which are what the section must draw.
 */
@OptIn(ExperimentalTestApi::class)
class LiveChatSectionUiTest {
    private var previous: InnerTube? = null

    @AfterTest
    fun restoreClient() {
        previous?.let { YouTube.innerTube = it }
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) { "missing fixture $name" }
            .bufferedReader()
            .use { it.readText() }

    private fun install() {
        val seed = fixture("live_chat_seed.json")
        val poll = fixture("live_chat_poll.json")
        val engine =
            MockEngine { request ->
                respond(
                    content = if (request.url.encodedPath.endsWith("live_chat/get_live_chat")) poll else seed,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        val client = InnerTube(engine)
        client.locale = YouTubeLocale(gl = "US", hl = "en")
        if (previous == null) previous = YouTube.innerTube
        YouTube.innerTube = client
    }

    private fun capturedMessages(): List<LiveChatMessage> {
        install()
        return runBlocking {
            val watch = YouTube.watchNext("rFZHOHl-L8A").getOrThrow()
            val token = checkNotNull(YouTube.liveChatToken(watch)) { "no chat in the captured watch page" }
            liveChatMessages(token).first()
        }
    }

    @Test
    fun theCapturedChatMessagesRenderTheirAuthorsAndText() =
        runComposeUiTest {
            val messages = capturedMessages()
            assertEquals(2, messages.size)

            setContent {
                MaterialTheme {
                    LazyColumn {
                        liveChatSection(messages)
                    }
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("@aarvi-b7r").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Live chat").assertExists()
            onNodeWithText("@aarvi-b7r").assertExists()
            onNodeWithText("u").assertExists()
            onNodeWithText("@MYOWNSPACE_02").assertExists()
        }
}
