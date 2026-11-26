package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.data.subscriptions.Subscription
import io.github.aedev.flow.innertube.InnerTube
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.github.aedev.flow.presentation.nav.AppDestination
import io.github.aedev.flow.presentation.nav.AppRouter
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Subscriptions, against the captured channel responses: each subscribed channel's uploads merge
 * into one grid, newest first, and the tab navigates as a tab rather than a pushed page.
 */
class SubscriptionsFeedTest {
    private var previous: InnerTube? = null

    @AfterTest
    fun restoreClient() {
        previous?.let { YouTube.innerTube = it }
    }

    private fun fixture(name: String): String =
        assertNotNull(javaClass.getResourceAsStream("/fixtures/$name"), "missing fixture $name")
            .bufferedReader()
            .use { it.readText() }

    /** The channel landing and its Videos tab, the two requests [loadChannel] makes. */
    private fun installChannel() {
        val landing = fixture("channel_page.json")
        val videos = fixture("channel_videos_page.json")
        val engine =
            MockEngine { request ->
                val body = request.body.toByteArray().decodeToString()
                respond(
                    content = if (body.contains("\"params\"")) videos else landing,
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
    fun aSubscribedChannelsUploadsFillTheGrid() =
        runTest {
            installChannel()

            val feed =
                loadSubscriptions(listOf(Subscription("UCiMhD4jzUqG-IgPzUmmytRQ", "Queen Official")))
                    .getOrThrow()

            assertEquals(listOf("Queen Official"), feed.channels.map { it.name })
            assertEquals(6, feed.videos.size)
            assertTrue(feed.videos.all { it.channelId == "UCiMhD4jzUqG-IgPzUmmytRQ" })
            assertTrue(feed.videos.all { it.channelName == "Queen Official" })
            assertTrue(feed.videos.all { it.entry.videoId == it.entry.id })
            assertTrue(feed.videos.all { it.entry.thumbnailUrl?.startsWith("https://") == true })
            assertTrue(
                feed.channels
                    .single()
                    .avatarUrl
                    ?.startsWith("https://") == true,
            )

            // The card draws the channel itself, so the meta line must not repeat its name.
            assertTrue(
                feed.videos.all { it.entry.subtitle?.contains("Queen Official") != true },
                feed.videos
                    .first()
                    .entry.subtitle
                    .orEmpty(),
            )
            assertEquals(0, feed.failedChannelCount)
        }

    /** The same channel listed twice is one subscription, not two copies of every upload. */
    @Test
    fun duplicateSubscriptionsCollapse() =
        runTest {
            installChannel()

            val feed =
                loadSubscriptions(
                    listOf(
                        Subscription("UCiMhD4jzUqG-IgPzUmmytRQ", "Queen Official"),
                        Subscription("UCiMhD4jzUqG-IgPzUmmytRQ", "Queen Official"),
                    ),
                ).getOrThrow()

            assertEquals(1, feed.channels.size)
            assertEquals(6, feed.videos.size)
            assertEquals(feed.videos.size, feed.videos.distinctBy { it.entry.id }.size)
        }

    /** Several subscriptions each contribute their uploads, and one card per video survives. */
    @Test
    fun severalSubscriptionsMergeIntoOneGrid() =
        runTest {
            installChannel()

            val feed =
                loadSubscriptions(
                    listOf(
                        Subscription("UCQueen", "Queen Official"),
                        Subscription("UCBowie", "David Bowie"),
                    ),
                ).getOrThrow()

            // The captured fixture answers both ids, so both channels are listed and their
            // identical uploads collapse rather than appearing twice.
            assertEquals(2, feed.channels.size)
            assertEquals(6, feed.videos.size)
            assertEquals(feed.videos.size, feed.videos.distinctBy { it.entry.id }.size)
        }

    /** Setup made no channel picks: the page is empty and no request is attempted. */
    @Test
    fun noSubscriptionsIsAnEmptyFeed() =
        runTest {
            val feed = loadSubscriptions(emptyList()).getOrThrow()

            assertTrue(feed.channels.isEmpty())
            assertTrue(feed.videos.isEmpty())
            assertEquals(0, feed.failedChannelCount)
        }

    /** The merged grid is newest first, read from YouTube's own relative line. */
    @Test
    fun uploadsAreOrderedNewestFirst() =
        runTest {
            assertEquals(2 * 60 * 60L, publishedAgeSeconds("2 hours ago"))
            assertEquals(3 * 24 * 60 * 60L, publishedAgeSeconds("3 days ago"))
            assertEquals(365 * 24 * 60 * 60L, publishedAgeSeconds("1 year ago"))
            // A live archive line still carries the age; a line with no age reads as unknown.
            assertEquals(2 * 365 * 24 * 60 * 60L, publishedAgeSeconds("Streamed 2 years ago"))
            assertEquals(null, publishedAgeSeconds("just now"))
            assertEquals(null, publishedAgeSeconds(null))
        }

    /** The tab replaces the stack, as every tab does in the app's bar. */
    @Test
    fun subscriptionsIsATabNotAPushedPage() {
        val router = AppRouter()
        router.push(AppDestination.Channel("UCiMhD4jzUqG-IgPzUmmytRQ", "Queen Official"))
        router.openTab(AppDestination.Subscriptions)

        assertEquals(AppDestination.Subscriptions, router.current)
        assertFalse(router.canGoBack)
    }
}
