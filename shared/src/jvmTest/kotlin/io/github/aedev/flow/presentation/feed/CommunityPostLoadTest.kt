package io.github.aedev.flow.presentation.feed

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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The post page's two requests against the captured payloads, without the screen in the way. */
class CommunityPostLoadTest {
    private var previous: InnerTube? = null
    private var pageRequests = 0
    private var commentRequests = 0

    @AfterTest
    fun restoreClient() {
        previous?.let { YouTube.innerTube = it }
    }

    private val postPage by lazy { fixture("post_page.json") }
    private val comments by lazy { fixture("post_comments.json") }
    private val params =
        "wgNaEhhVQ3NYVmszN2JsdEh4RDFyRFB3dE5NOFEaJFVna3hmSWc5ZGJNdUZaa1dWX1BBRk52dV85QXVUaURsa1ptS1oYVUNzWFZrMzdibHRIeEQxckRQd3ROTThR"

    private fun install() {
        pageRequests = 0
        commentRequests = 0
        val engine =
            MockEngine { request ->
                val body = request.body.toByteArray().decodeToString()
                val isPage = body.contains("\"params\"")
                if (isPage) pageRequests++ else commentRequests++
                respond(
                    content = if (isPage) postPage else comments,
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
    fun thePostAndItsCommentsComeFromTheTwoCapturedResponses() =
        runBlocking {
            install()

            val detail = loadCommunityPost(params).getOrThrow()

            assertEquals("UgkxfIg9dbMuFZkWV_PAFNvu_9AuTiDlkZmK", detail.post.id)
            assertEquals("2.1K", detail.post.likeCountText)
            assertEquals(3, detail.comments.size)
            assertTrue(
                detail.comments
                    .first()
                    .text
                    .startsWith("Alas an event horizon"),
            )
            assertEquals(null, detail.commentsContinuation)
            assertEquals(1, pageRequests)
            assertEquals(1, commentRequests)
        }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) { "missing fixture $name" }
            .bufferedReader()
            .use { it.readText() }
}
