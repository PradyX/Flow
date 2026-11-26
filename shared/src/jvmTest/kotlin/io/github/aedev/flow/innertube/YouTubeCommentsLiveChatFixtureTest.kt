package io.github.aedev.flow.innertube

import io.github.aedev.flow.innertube.models.YouTubeClient.Companion.WEB
import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Community-post comments, the Timed comment order, and the live-chat seed/poll pair.
 *
 * The answer bodies under `src/jvmTest/resources/fixtures/` are **captured from live InnerTube
 * traffic**, trimmed to the entities the parser reads but otherwise unedited. Each file names the
 * endpoint and the request arguments that produced it in `fixtures/PROVENANCE.md`. Tokens asserted
 * on are read back out of those files, so the test cannot drift from the capture.
 */
class YouTubeCommentsLiveChatFixtureTest {
    private lateinit var previous: InnerTube
    private val requests = mutableListOf<HttpRequestData>()

    @AfterTest
    fun restoreClient() {
        if (::previous.isInitialized) YouTube.innerTube = previous
    }

    /** Reads a checked-in answer body. A missing file fails loudly rather than skipping the parse. */
    private fun fixture(name: String): String =
        assertNotNull(javaClass.getResourceAsStream("/fixtures/$name"), "missing fixture $name")
            .bufferedReader()
            .use { it.readText() }

    private fun jsonOf(name: String) = Json.parseToJsonElement(fixture(name)).jsonObject

    private fun JsonElement?.step(key: String): JsonElement? = this?.jsonObject?.get(key)

    /** The post-comments continuation the captured body actually carries. */
    private fun postContinuation(): String =
        jsonOf("community_post_comments.json")
            .step("onResponseReceivedEndpoints")
            ?.jsonArray
            ?.get(0)
            ?.step("reloadContinuationItemsCommand")
            ?.step("continuationItems")
            ?.jsonArray
            ?.map { it.jsonObject }
            ?.first { it.containsKey("continuationItemRenderer") }
            ?.step("continuationItemRenderer")
            ?.step("continuationEndpoint")
            ?.step("continuationCommand")
            ?.step("token")
            ?.jsonPrimitive
            ?.content
            ?: error("no continuation in the post fixture")

    /** The live-chat seed token the captured seed body actually carries. */
    private fun seedToken(): String =
        jsonOf("live_chat_seed.json")
            .step("contents")
            ?.step("twoColumnWatchNextResults")
            ?.step("conversationBar")
            ?.step("liveChatRenderer")
            ?.step("continuations")
            ?.jsonArray
            ?.get(0)
            ?.step("reloadContinuationData")
            ?.step("continuation")
            ?.jsonPrimitive
            ?.content
            ?: error("no seed token in the seed fixture")

    /** The next token and interval the captured poll body actually carries. */
    private fun pollContinuation(): JsonObject =
        jsonOf("live_chat_poll.json")
            .step("continuationContents")
            ?.step("liveChatContinuation")
            ?.step("continuations")
            ?.jsonArray
            ?.get(0)
            ?.step("invalidationContinuationData")
            ?.jsonObject
            ?: error("no continuation in the poll fixture")

    private fun lastRequest(): HttpRequestData = requests.lastOrNull() ?: error("no request was sent")

    private suspend fun lastBody(): String = lastRequest().body.toByteArray().decodeToString()

    private fun install(bodyName: String): InnerTube {
        val responseJson = fixture(bodyName)
        requests.clear()
        val engine =
            MockEngine { request ->
                requests += request
                respond(
                    content = responseJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        val client = InnerTube(engine)
        client.locale = YouTubeLocale(gl = "US", hl = "en")
        previous = YouTube.innerTube
        YouTube.innerTube = client
        return client
    }

    @Test
    fun communityPostCommentsParsesThreads() =
        runTest {
            install("community_post_comments.json")

            val page = YouTube.communityPostComments(continuation = postContinuation()).getOrThrow()

            val request = lastRequest()
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("www.youtube.com", request.url.host)
            assertEquals("/youtubei/v1/browse", request.url.encodedPath)
            assertEquals(WEB.clientId, request.headers["X-YouTube-Client-Name"])
            assertEquals("https://www.youtube.com", request.headers["X-Origin"])

            assertEquals(2, page.comments.size)
            val first = page.comments[0]
            assertEquals("UgzoA-VyRcGStiLcj8B4AaABAg", first.id)
            assertEquals("It Would Take a Strong Strong Man ❤❤❤❤❤", first.text)
            assertEquals("@준하-t8r", first.author)
            assertTrue(first.authorThumbnail.contains("yt3"), first.authorThumbnail)
            assertEquals(0, first.likeCount)
            assertEquals(0, first.replyCount)
            assertEquals("11 hours ago", first.publishedTime)
            assertEquals(postContinuation(), page.continuation)

            assertEquals(WEB_CONTEXT + ",\"continuation\":\"" + postContinuation() + "\"}", lastBody())
            assertFalse(lastBody().contains(":null"), lastBody())
        }

    @Test
    fun timedCommentsParseInPlaybackOrder() =
        runTest {
            install("timed_comments.json")

            val comments = YouTube.timedComments("TIMED_CONT").getOrThrow()

            val request = lastRequest()
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("www.youtube.com", request.url.host)
            assertEquals("/youtubei/v1/next", request.url.encodedPath)
            assertEquals("frameworkUpdates.entityBatchUpdate.mutations", request.headers["X-Goog-FieldMask"])

            assertEquals(3, comments.size)
            assertEquals(listOf(1000L, 9000L, 18000L), comments.map { it.timedAtMs })
            assertEquals(
                listOf("UgyV0ziROiET_c5PQ514AaABAg", "UgyhMRIdnQRMSTHHmEF4AaABAg", "UgzbcwAI9vPj0cGPFfl4AaABAg"),
                comments.map { it.id },
            )
            assertEquals(listOf("@diegoquintero3458", "@ricardosantos2207", "@phillycheesesteaks4459"), comments.map { it.author })
            assertEquals(listOf(21, 3, 3900), comments.map { it.likeCount })
            assertEquals(listOf("6mo ago (edited)", "5mo ago", "1y ago (edited)"), comments.map { it.publishedTime })
            assertTrue(comments[0].text.contains("German me trolleo"), comments[0].text)
            assertTrue(comments[2].text.contains("WE’RE NO STRANGERS TO LOVE"), comments[2].text)

            assertEquals(
                "{\"context\":{\"client\":{\"clientName\":\"IOS\",\"clientVersion\":\"21.03.1\"," +
                    "\"osVersion\":\"18.2.22C152\",\"gl\":\"US\",\"hl\":\"en\"}," +
                    "\"request\":{\"internalExperimentFlags\":[],\"useSsl\":true}," +
                    "\"user\":{\"lockedSafetyMode\":false}}," +
                    "\"continuation\":\"TIMED_CONT\",\"contentCheckOk\":true,\"racyCheckOk\":true," +
                    "\"formData\":{\"commentsFilterFormData\":{\"commentsFilter\":{\"sortBy\":\"SORT_BY_TIMED\"}}}}",
                lastBody(),
            )
            assertFalse(lastBody().contains(":null"), lastBody())
        }

    @Test
    fun liveChatContinuationComesFromTheConversationBar() =
        runTest {
            install("live_chat_seed.json")

            val continuation = YouTube.liveChatContinuation("rFZHOHl-L8A").getOrThrow()

            val request = lastRequest()
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("www.youtube.com", request.url.host)
            assertEquals("/youtubei/v1/next", request.url.encodedPath)
            assertEquals(WEB.clientId, request.headers["X-YouTube-Client-Name"])
            assertEquals(192, continuation?.length)
            assertEquals(seedToken(), continuation)

            assertEquals(
                WEB_CONTEXT + ",\"videoId\":\"rFZHOHl-L8A\",\"contentCheckOk\":true,\"racyCheckOk\":true}",
                lastBody(),
            )
            assertFalse(lastBody().contains(":null"), lastBody())
        }

    @Test
    fun liveChatParsesOnePoll() =
        runTest {
            install("live_chat_poll.json")

            val response = YouTube.liveChat(seedToken(), offsetMs = 1000).getOrThrow()

            val request = lastRequest()
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/youtubei/v1/live_chat/get_live_chat", request.url.encodedPath)
            assertEquals(WEB.clientId, request.headers["X-YouTube-Client-Name"])

            val actions =
                response.continuationContents
                    ?.liveChatContinuation
                    ?.actions
                    .orEmpty()
            assertEquals(2, actions.size)
            val renderer = assertNotNull(actions[0].addChatItemAction?.item?.liveChatTextMessageRenderer)
            assertEquals("ChwKGkNPNm0zNW1jbkpnREZjTXl6QVFkellNQUxn", renderer.id)
            assertEquals("u", renderer.message?.plainText())
            assertEquals("@aarvi-b7r", renderer.authorName?.value())
            // The captured message carries no timestamp text, and the model must not invent one.
            assertNull(renderer.timestampText?.value())
            assertEquals(
                "@MYOWNSPACE_02",
                actions[1]
                    .addChatItemAction
                    ?.item
                    ?.liveChatTextMessageRenderer
                    ?.authorName
                    ?.value(),
            )

            assertEquals(
                WEB_CONTEXT +
                    ",\"continuation\":\"" + seedToken() + "\",\"currentPlayerState\":{\"playerOffsetMs\":\"1000\"}}",
                lastBody(),
            )
            assertFalse(lastBody().contains(":null"), lastBody())
        }

    /**
     * The second poll is driven entirely by the captured first answer: its token is that body's
     * `invalidationContinuationData.continuation` and its `playerOffsetMs` is that body's
     * `timeoutMs`, so both are proven by body content rather than by a mock's own values.
     */
    @Test
    fun liveChatFeedPollsTwiceWithTheServersInterval() =
        runTest {
            install("live_chat_poll.json")

            val nextToken = pollContinuation()["continuation"]?.jsonPrimitive?.content ?: error("no token")
            val interval = pollContinuation()["timeoutMs"]?.jsonPrimitive?.content ?: error("no interval")
            assertEquals("10000", interval)

            val batches = YouTube.liveChatFeed(seedToken(), offsetMs = 0).take(2).toList()

            assertEquals(2, batches.size)
            assertEquals(2, batches[0].size)
            assertEquals(2, batches[1].size)
            assertEquals("u", batches[0][0].message?.plainText())
            assertEquals("ndhuku nak e confusion 🤕", batches[1][1].message?.plainText())
            assertEquals(2, requests.size)

            val first = requests[0].body.toByteArray().decodeToString()
            assertEquals(
                WEB_CONTEXT + ",\"continuation\":\"" + seedToken() + "\",\"currentPlayerState\":{\"playerOffsetMs\":\"0\"}}",
                first,
            )

            val second = requests[1].body.toByteArray().decodeToString()
            assertEquals(
                WEB_CONTEXT +
                    ",\"continuation\":\"" + nextToken + "\",\"currentPlayerState\":{\"playerOffsetMs\":\"" +
                    interval + "\"}}",
                second,
            )
            assertFalse(second.contains(":null"), second)
        }

    private companion object {
        const val WEB_CONTEXT =
            "{\"context\":{\"client\":{\"clientName\":\"WEB\",\"clientVersion\":\"2.20260710.06.00\"," +
                "\"originalUrl\":\"https://www.youtube.com\",\"platform\":\"DESKTOP\",\"utcOffsetMinutes\":0," +
                "\"gl\":\"US\",\"hl\":\"en\"}," +
                "\"request\":{\"internalExperimentFlags\":[],\"useSsl\":true}," +
                "\"user\":{\"lockedSafetyMode\":false}}"
    }
}
