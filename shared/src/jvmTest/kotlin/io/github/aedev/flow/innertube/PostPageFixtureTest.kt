package io.github.aedev.flow.innertube

import io.github.aedev.flow.innertube.pages.renderer.FeedItemOwner
import io.github.aedev.flow.innertube.pages.renderer.PostAttachment
import io.github.aedev.flow.innertube.pages.renderer.toCommunityCommentsPage
import io.github.aedev.flow.innertube.pages.renderer.toCommunityPostPage
import io.github.aedev.flow.innertube.pages.renderer.toCommunityPostsPage
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The community post page against captured live traffic (Kurzgesagt's Black Hole Week post,
 * 2026-11-26): `browse` with `browseId: "FEpost_detail"` and the post's own token, then the
 * comment-section token that page carries. The two responses are the two halves of one screen.
 */
class PostPageFixtureTest {
    private val reader =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) { "missing fixture $name" }
            .bufferedReader()
            .use { it.readText() }

    private fun tab(name: String) = reader.parseToJsonElement(fixture(name))

    @Test
    fun thePostPageCarriesThePostAndTheCommentsTokenInItsOwnSection() {
        val page = tab("post_page.json").toCommunityPostPage(FeedItemOwner())
        val post = checkNotNull(page.post)

        assertEquals("UgkxfIg9dbMuFZkWV_PAFNvu_9AuTiDlkZmK", post.id)
        assertEquals("Kurzgesagt – In a Nutshell", post.authorName)
        assertEquals("2.1K", post.likeCountText)
        assertEquals("2 days ago", post.publishedTimeText)
        assertTrue(post.text.startsWith("The Black Hole Week Deals"))
        assertEquals("UCsXVk37bltHxD1rDPwtNM8Q", post.authorChannelId)
        // The post's gallery is a pager over every picture the renderer served, not just its first:
        // both of the capture's `postMultiImageRenderer` pictures survive parsing.
        val gallery = post.attachment as PostAttachment.Images
        assertEquals(2, gallery.urls.size)
        assertEquals(2, gallery.urls.distinct().size)
        assertTrue(gallery.urls.all { it.isNotBlank() })
        // The comments themselves are not on this page: the comment section is a token.
        assertTrue(page.commentsContinuation?.isNotBlank() == true)
    }

    @Test
    fun everyPostShapeKeepsItsGalleryAndItsAuthorsChannel() {
        val posts = tab("channel_posts_page.json").toCommunityPostsPage("", "").posts

        assertEquals(3, posts.size)
        // The tab mixes a multi-image post, a single-image post and another gallery; the single
        // image arrives as `backstageImageRenderer`, the galleries as nested renderers.
        assertEquals(listOf(2, 1, 8), posts.map { (it.attachment as PostAttachment.Images).urls.size })
        assertTrue(posts.all { it.authorChannelId == "UCsXVk37bltHxD1rDPwtNM8Q" })
    }

    @Test
    fun theCommentsArriveInAReloadCommandWithTheirCountInAHeader() {
        val page = tab("post_comments.json").toCommunityCommentsPage()

        assertEquals(3, page.comments.size)
        assertTrue(page.commentCountText.orEmpty().contains("22"))
        assertEquals("Alas an event horizon has formed around my finances", page.comments.first().text)
        assertEquals(1, page.comments.first().replyCount)
        assertTrue(
            page.comments
                .first()
                .continuationToken
                ?.isNotBlank() == true,
        )
        // The capture ends the thread list: the last answer carries no next-page token.
        assertNull(page.continuation)
    }
}
