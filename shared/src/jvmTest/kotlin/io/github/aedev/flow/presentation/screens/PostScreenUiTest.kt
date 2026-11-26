package io.github.aedev.flow.presentation.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import io.github.aedev.flow.innertube.InnerTube
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The community post page against the captured post, its comment section and one comment's replies:
 * the post's own text, the 22-comment thread's first three comments, the reply toggle, the empty
 * comment section, and the retry state.
 */
@OptIn(ExperimentalTestApi::class)
class PostScreenUiTest {
    private var previous: InnerTube? = null

    @AfterTest
    fun restoreClient() {
        previous?.let { YouTube.innerTube = it }
    }

    private val postPage by lazy { fixture("post_page.json") }
    private val comments by lazy { fixture("post_comments.json") }
    private val replies by lazy { fixture("post_replies.json") }

    private val postId = "UgkxfIg9dbMuFZkWV_PAFNvu_9AuTiDlkZmK"
    private val params =
        "wgNaEhhVQ3NYVmszN2JsdEh4RDFyRFB3dE5NOFEaJFVna3hmSWc5ZGJNdUZaa1dWX1BBRk52dV85QXVUaURsa1ptS1oYVUNzWFZrMzdibHRIeEQxckRQd3ROTThR"
    private val author = "Kurzgesagt – In a Nutshell"
    private val postText = "The Black Hole Week Deals"
    private val firstComment = "Alas an event horizon has formed around my finances"
    private val firstReply = "You can wait some googol years"

    private var pageRequests = 0
    private var commentsRequests = 0

    /** The reply token the captured comment section carries, which its reply answer is asked for. */
    private val replyToken: String by lazy {
        val tokens = mutableListOf<String>()

        fun collect(element: JsonElement) {
            when (element) {
                is JsonObject -> {
                    element["continuationCommand"]
                        ?.jsonObject
                        ?.get("token")
                        ?.jsonPrimitive
                        ?.content
                        ?.let(tokens::add)
                    element.values.forEach(::collect)
                }

                is JsonArray -> {
                    element.forEach(::collect)
                }

                else -> {
                    Unit
                }
            }
        }
        collect(Json.parseToJsonElement(comments))
        tokens.last()
    }

    /**
     * Serves the captured responses in the order the screen asks for them: the page, the comment
     * section, then a comment's replies.
     */
    private fun install(
        failFirstPage: Boolean = false,
        failFirstComments: Boolean = false,
        commentsBody: String = comments,
        pageBody: String = postPage,
    ) {
        pageRequests = 0
        commentsRequests = 0
        val engine =
            MockEngine { request ->
                val body = request.body.toByteArray().decodeToString()
                val isPage = body.contains("\"params\"")
                val isReply = !isPage && body.contains(replyToken)
                val firstCommentRequest = !isPage && !isReply && commentsRequests++ == 0
                val failPage = isPage && failFirstPage && pageRequests++ == 0
                val failComments = firstCommentRequest && failFirstComments
                val response =
                    when {
                        isPage -> if (failPage) "" else pageBody
                        isReply -> replies
                        failComments -> ""
                        else -> commentsBody
                    }
                respond(
                    content = response,
                    status =
                        if (failPage || failComments) {
                            HttpStatusCode.InternalServerError
                        } else {
                            HttpStatusCode.OK
                        },
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        val client = InnerTube(engine)
        client.locale = YouTubeLocale(gl = "US", hl = "en")
        if (previous == null) previous = YouTube.innerTube
        YouTube.innerTube = client
    }

    @Composable
    private fun Screen(onOpenChannel: (String, String) -> Unit = { _, _ -> }) {
        PostScreen(
            postId = postId,
            params = params,
            fallbackAuthor = author,
            onOpenChannel = onOpenChannel,
            onBack = {},
        )
    }

    @Test
    fun theCapturedPostAndItsCommentsRender() =
        runComposeUiTest {
            install()

            setContent { MaterialTheme { Screen() } }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(postText, substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            scrollTo(firstComment)
            onNodeWithText(firstComment, substring = true).assertExists()
            assertTrue(onAllNodesWithText(author, substring = true).fetchSemanticsNodes().isNotEmpty())
        }

    @Test
    fun aCommentRepliesToggleFetchesTheCapturedReply() =
        runComposeUiTest {
            install()

            setContent { MaterialTheme { Screen() } }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(postText, substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            scrollTo(firstComment)
            onNodeWithText("1 reply").performClick()
            waitForText(firstReply)
            onNodeWithText("Hide replies").assertExists()
        }

    @Test
    fun anEmptyCommentSectionSaysSo() =
        runComposeUiTest {
            install(commentsBody = commentsWithoutThreads())

            setContent { MaterialTheme { Screen() } }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(postText, substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            scrollTo("No comments yet")
            onNodeWithText("No comments yet").assertExists()
        }

    @Test
    fun aFailedPostPageOffersARetryThatAsksAgain() =
        runComposeUiTest {
            install(failFirstPage = true)

            setContent { MaterialTheme { Screen() } }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Could not load.").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Retry").performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(postText, substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            assertTrue(onAllNodesWithText("Could not load.").fetchSemanticsNodes().isEmpty())
        }

    @Test
    fun aFailedCommentSectionKeepsThePostAndOffersARetry() =
        runComposeUiTest {
            install(failFirstComments = true)

            setContent { MaterialTheme { Screen() } }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(postText, substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            scrollTo("Could not load.")
            onNodeWithText("Could not load.").assertExists()
            onNodeWithText("Retry").performClick()
            waitForText(firstComment)
        }

    @Test
    fun theGalleryPagerSwipesThroughEveryCapturedImage() =
        runComposeUiTest {
            install()

            setContent { MaterialTheme { Screen() } }

            waitForText(postText)
            onNodeWithText("1 / 2").assertExists()
            onNodeWithTag(POST_GALLERY_TEST_TAG).performTouchInput { swipeLeft() }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("2 / 2").fetchSemanticsNodes().isNotEmpty()
            }
        }

    @Test
    fun aSingleImagePostDrawsOnePageWithoutAPositionBadge() =
        runComposeUiTest {
            install(pageBody = pageWithPost(singleImagePost()))

            setContent { MaterialTheme { Screen() } }

            waitForText(postText)
            onNodeWithTag(POST_GALLERY_TEST_TAG).assertExists()
            assertTrue(onAllNodesWithText("1 / 1").fetchSemanticsNodes().isEmpty())
        }

    @Test
    fun aPostWhoseGalleryServesNoImagesDrawsNoPager() =
        runComposeUiTest {
            install(pageBody = pageWithPost(postWithoutGallery()))

            setContent { MaterialTheme { Screen() } }

            waitForText(postText)
            assertTrue(onAllNodesWithTag(POST_GALLERY_TEST_TAG).fetchSemanticsNodes().isEmpty())
        }

    @Test
    fun anImageThatCannotLoadLeavesThePagerUsable() =
        runComposeUiTest {
            install(pageBody = pageWithPost(postWithBrokenImages()))

            setContent { MaterialTheme { Screen() } }

            waitForText(postText)
            onNodeWithText("1 / 2").assertExists()
            onNodeWithTag(POST_GALLERY_TEST_TAG).performTouchInput { swipeLeft() }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("2 / 2").fetchSemanticsNodes().isNotEmpty()
            }
        }

    @Test
    fun theAuthorRowOpensTheCapturedChannel() =
        runComposeUiTest {
            install()
            var opened: Pair<String, String>? = null

            setContent { MaterialTheme { Screen(onOpenChannel = { id, name -> opened = id to name }) } }

            waitForText(postText)
            onNode(hasText(author) and hasClickAction()).performClick()
            assertEquals("UCsXVk37bltHxD1rDPwtNM8Q", opened?.first)
            assertEquals(author, opened?.second)
        }

    @Test
    fun aPostWithoutAnAuthorChannelLeavesTheRowUnlinked() =
        runComposeUiTest {
            install(pageBody = pageWithPost(postWithoutAuthorChannel()))

            setContent { MaterialTheme { Screen() } }

            waitForText(postText)
            assertTrue(onAllNodes(hasText(author) and hasClickAction()).fetchSemanticsNodes().isEmpty())
        }

    /** The page's post renderer, the one the capture serves inside its backstage thread. */
    private fun postRenderer(): JsonObject =
        Json
            .parseToJsonElement(postPage)
            .jsonObject["contents"]!!
            .jsonObject["twoColumnBrowseResultsRenderer"]!!
            .jsonObject["tabs"]!!
            .jsonArray[0]
            .jsonObject["tabRenderer"]!!
            .jsonObject["content"]!!
            .jsonObject["sectionListRenderer"]!!
            .jsonObject["contents"]!!
            .jsonArray[0]
            .jsonObject["itemSectionRenderer"]!!
            .jsonObject["contents"]!!
            .jsonArray[0]
            .jsonObject["backstagePostThreadRenderer"]!!
            .jsonObject["post"]!!
            .jsonObject["backstagePostRenderer"]!!
            .jsonObject

    private fun singleImagePost(): JsonObject {
        val post = postRenderer()
        val image =
            post["backstageAttachment"]!!
                .jsonObject["postMultiImageRenderer"]!!
                .jsonObject["images"]!!
                .jsonArray[0]
                .jsonObject["backstageImageRenderer"]!!
                .jsonObject
        return JsonObject(
            post +
                (
                    "backstageAttachment" to
                        JsonObject(mapOf("backstageImageRenderer" to image))
                ),
        )
    }

    private fun postWithoutGallery(): JsonObject {
        val post = postRenderer()
        return JsonObject(
            post +
                (
                    "backstageAttachment" to
                        JsonObject(
                            mapOf(
                                "postMultiImageRenderer" to
                                    JsonObject(mapOf("images" to JsonArray(emptyList()))),
                            ),
                        )
                ),
        )
    }

    private fun postWithBrokenImages(): JsonObject {
        val post = postRenderer()
        val images =
            post["backstageAttachment"]!!
                .jsonObject["postMultiImageRenderer"]!!
                .jsonObject["images"]!!
                .jsonArray
        val broken =
            JsonArray(
                images.map { entry ->
                    val renderer = entry.jsonObject["backstageImageRenderer"]!!.jsonObject
                    JsonObject(
                        entry.jsonObject +
                            (
                                "backstageImageRenderer" to
                                    JsonObject(
                                        renderer +
                                            (
                                                "image" to
                                                    JsonObject(
                                                        mapOf(
                                                            "thumbnails" to
                                                                JsonArray(
                                                                    listOf(
                                                                        JsonObject(
                                                                            mapOf(
                                                                                "url" to JsonPrimitive("https://invalid.invalid/p.jpg"),
                                                                                "width" to JsonPrimitive(120),
                                                                                "height" to JsonPrimitive(120),
                                                                            ),
                                                                        ),
                                                                    ),
                                                                ),
                                                        ),
                                                    )
                                            ),
                                    )
                            ),
                    )
                },
            )
        return JsonObject(
            post +
                (
                    "backstageAttachment" to
                        JsonObject(
                            mapOf(
                                "postMultiImageRenderer" to
                                    JsonObject(mapOf("images" to broken)),
                            ),
                        )
                ),
        )
    }

    private fun postWithoutAuthorChannel(): JsonObject {
        val post = postRenderer()
        val authorText = post["authorText"]!!.jsonObject
        val runs =
            JsonArray(
                authorText["runs"]!!.jsonArray.map { run ->
                    JsonObject(run.jsonObject - "navigationEndpoint")
                },
            )
        return JsonObject(
            (post - "authorEndpoint") +
                ("authorText" to JsonObject(authorText + ("runs" to runs))),
        )
    }

    private fun pageWithPost(post: JsonObject): String {
        var replaced = false

        fun walk(element: JsonElement): JsonElement =
            when (element) {
                is JsonObject -> {
                    JsonObject(
                        element
                            .map { (key, value) ->
                                if (key == "backstagePostRenderer" && value is JsonObject && !replaced) {
                                    replaced = true
                                    key to post
                                } else {
                                    key to walk(value)
                                }
                            }.toMap(),
                    )
                }

                is JsonArray -> {
                    JsonArray(element.map(::walk))
                }

                else -> {
                    element
                }
            }
        return walk(Json.parseToJsonElement(postPage)).toString()
    }

    /** Scrolls the page until [text] is composed, retrying while the reload it waits for lands. */
    private fun ComposeUiTest.waitForText(text: String) {
        waitUntil(timeoutMillis = 5_000) {
            runCatching { scrollTo(text) }.isSuccess
        }
    }

    /** Scrolls the page's list until [text] is composed, since a post's comments sit below it. */
    private fun ComposeUiTest.scrollTo(text: String) {
        // The gallery is a horizontal scroller inside the page's list, so only the first scroller is
        // the LazyColumn the comments live in.
        onAllNodes(hasScrollAction())[0].performScrollToNode(hasText(text, substring = true))
    }

    /** The captured comment answer with its thread list removed, header and all. */
    private fun commentsWithoutThreads(): String {
        val root = Json.parseToJsonElement(comments).jsonObject
        val endpoints = root["onResponseReceivedEndpoints"]!!.jsonArray
        return JsonObject(
            mapOf(
                "onResponseReceivedEndpoints" to JsonArray(listOf(endpoints.first())),
            ),
        ).toString()
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) { "missing fixture $name" }
            .bufferedReader()
            .use { it.readText() }
}
