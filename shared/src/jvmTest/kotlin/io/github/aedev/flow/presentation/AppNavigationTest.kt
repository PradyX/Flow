package io.github.aedev.flow.presentation

import io.github.aedev.flow.data.model.LiveChatMessageType
import io.github.aedev.flow.innertube.InnerTube
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.liveChatMessages
import io.github.aedev.flow.presentation.feed.loadChannel
import io.github.aedev.flow.presentation.feed.loadCommentReplies
import io.github.aedev.flow.presentation.feed.loadLibrary
import io.github.aedev.flow.presentation.feed.loadSearchFeed
import io.github.aedev.flow.presentation.feed.loadShorts
import io.github.aedev.flow.presentation.feed.loadVideoDetail
import io.github.aedev.flow.presentation.feed.loadVideoHome
import io.github.aedev.flow.presentation.nav.AppDestination
import io.github.aedev.flow.presentation.nav.AppRouter
import io.github.aedev.flow.presentation.nav.restoredTab
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Drives the code the composables run, without a window: [AppRouter] is plain Compose state and the
 * three loads are plain suspend functions, so this exercises the same navigation transitions and the
 * same `/browse`, `/search` and `/next` parsing the screens call — against captured responses.
 *
 * No GUI is launched here: this environment has no display for a Compose window, and the JVM target
 * ships no Compose UI test harness.
 */
class AppNavigationTest {
    private var previous: InnerTube? = null

    @AfterTest
    fun restoreClient() {
        previous?.let { YouTube.innerTube = it }
    }

    private fun fixture(name: String): String =
        assertNotNull(javaClass.getResourceAsStream("/fixtures/$name"), "missing fixture $name")
            .bufferedReader()
            .use { it.readText() }

    private fun install(bodyName: String) {
        val body = fixture(bodyName)
        val engine =
            MockEngine {
                respond(
                    content = body,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        val client = InnerTube(engine)
        client.locale = YouTubeLocale(gl = "US", hl = "en")
        if (previous == null) previous = YouTube.innerTube
        YouTube.innerTube = client
    }

    /** A watch page plus the comment page its section token opens. */
    private fun installComments() {
        installComments("video_comments_entry.json", "video_comments_page.json")
    }

    private fun installComments(
        entryName: String,
        pageName: String,
    ) {
        val entry = fixture(entryName)
        val page = fixture(pageName)
        val engine =
            MockEngine { request ->
                val body = request.body.toByteArray().decodeToString()
                respond(
                    content = if (body.contains("\"continuation\"")) page else entry,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        val client = InnerTube(engine)
        client.locale = YouTubeLocale(gl = "US", hl = "en")
        if (previous == null) previous = YouTube.innerTube
        YouTube.innerTube = client
    }

    /** The watch page, its comment page, and the replies under the pinned comment. */
    private fun installCommentsWithReplies() {
        val entry = fixture("video_comments_pinned_entry.json")
        val page = fixture("video_comments_pinned_page.json")
        val replies = fixture("video_comment_replies.json")
        val replyToken =
            Regex("\"commentRepliesRenderer\".*?\"token\":\"([^\"]+)\"", RegexOption.DOT_MATCHES_ALL)
                .find(page)
                ?.groupValues
                ?.get(1)
                ?: error("the pinned page carries no replies token")
        val engine =
            MockEngine { request ->
                val body = request.body.toByteArray().decodeToString()
                respond(
                    content =
                        when {
                            body.contains(replyToken) -> replies
                            body.contains("\"continuation\"") -> page
                            else -> entry
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

    private fun installRaw(body: String) {
        val engine =
            MockEngine {
                respond(
                    content = body,
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
    fun routerWalksHomeSearchAndPlayer() {
        val router = AppRouter()
        assertEquals(AppDestination.Home, router.current)
        assertFalse(router.canGoBack)

        router.push(AppDestination.Player("i1o1p_DD6TU", "Gehra Hua"))
        assertEquals(2, router.depth)
        assertTrue(router.canGoBack)

        router.openTab(AppDestination.Search)
        assertEquals(AppDestination.Search, router.current)
        assertFalse(router.canGoBack, "a tab reset must drop the detail stack")

        router.push(AppDestination.Player("abc", "Second"))
        assertTrue(router.pop())
        assertEquals(AppDestination.Search, router.current)
        assertFalse(router.pop(), "nothing left beneath the tab")
        assertEquals(
            AppDestination.Home,
            AppRouter(AppDestination.Search).let {
                it.openTab(AppDestination.Home)
                it.current
            },
        )
    }

    /**
     * A relaunch reopens the tab the last run left: the stored name turns back into that tab, and a
     * missing or unknown name leaves Home. Detail pages have no name, so they never reopen.
     */
    @Test
    fun aRelaunchReopensTheStoredTab() {
        assertEquals(AppDestination.Search, restoredTab("search"))
        assertEquals(AppDestination.Library, restoredTab("library"))
        assertEquals(AppDestination.Subscriptions, AppRouter(requireNotNull(restoredTab("subscriptions"))).current)
        assertNull(restoredTab(null), "a first launch has no tab to reopen")
        assertNull(restoredTab("player"), "a page that is not a tab is not restored")
        assertEquals(AppDestination.Home, AppRouter(restoredTab(null) ?: AppDestination.Home).current)
        assertNull(AppDestination.Player("i1o1p_DD6TU", "Gehra Hua").tabName)
        assertNull(AppDestination.Channel("UCiMhD4jzUqG-IgPzUmmytRQ", "Queen Official").tabName)
    }

    /** Home is the video grid now: the captured browse answers with the cards it lists. */
    @Test
    fun theHomeFeedParsesTheCapturedVideoGrid() =
        runTest {
            install("video_home.json")

            val videos = loadVideoHome().getOrThrow()

            assertEquals(listOf("aaaaaaaaaaa", "ccccccccccc"), videos.map { it.id })
            assertEquals("First long video", videos.first().title)
            assertEquals(12 * 60 + 34, videos.first().duration)
            assertTrue(videos.all { it.channelName.isNotBlank() })
        }

    @Test
    fun searchReturnsRankedVideos() =
        runTest {
            install("search_results.json")

            val feed = loadSearchFeed("rick astley").getOrThrow()
            val entries = feed.entries

            assertEquals(3, entries.size)
            assertEquals(
                listOf("5SZYz7lZRRI", "XGxIE1hr0w4", "eYuUAGXN0KM"),
                entries.map { it.id },
            )
            assertEquals("Rick Astley - Never Gonna Give You Up (Official Music Video) (1987)", entries[0].title)
            assertTrue(entries.all { it.videoId == it.id })
            assertTrue(entries[0].subtitle?.isNotBlank() == true, "channel/date subtitle missing")
        }

    /** The chips above the results are YouTube's, each carrying the token that re-runs the search. */
    @Test
    fun searchCarriesYouTubeFilterChips() =
        runTest {
            install("search_chips.json")

            val feed = loadSearchFeed("rick astley").getOrThrow()

            assertEquals(listOf("All", "Shorts", "Videos"), feed.chips.map { it.label })
            assertEquals("All", feed.chips.single { it.selected }.label)
            assertEquals(listOf("TOKEN_ALL", "TOKEN_SHORTS", "TOKEN_VIDEOS"), feed.chips.map { it.continuation })
        }

    @Test
    fun playerDetailLoadsMetadata() =
        runTest {
            install("video_detail.json")

            val detail = loadVideoDetail("i1o1p_DD6TU").getOrThrow()

            assertEquals("i1o1p_DD6TU", detail.videoId)
            assertTrue(detail.title.contains("Gehra Hua"), detail.title)
            assertEquals("Shashwat Sachdev", detail.channel)
            assertTrue(detail.description?.isNotBlank() == true, "description missing")
        }

    /** Search → tap a video → Player, through the same calls the two screens make. */
    @Test
    fun navigationReachesThePlayerWithAVideoFromTheResults() =
        runTest {
            install("search_results.json")
            val entries = loadSearchFeed("rick astley").getOrThrow().entries
            val entry = entries.first { it.videoId != null }
            assertEquals("5SZYz7lZRRI", entry.videoId)

            val router = AppRouter()
            val videoId = requireNotNull(entry.videoId)
            router.push(AppDestination.Player(videoId, entry.title))

            val destination = router.current as AppDestination.Player
            assertEquals(videoId, destination.videoId)
            assertEquals(entry.title, destination.title)

            install("video_detail.json")
            val detail = loadVideoDetail(destination.videoId).getOrThrow()
            assertTrue(detail.title.isNotBlank())
            assertEquals("Shashwat Sachdev", detail.channel)
        }

    /** The player's rail is the watch response's own recommendations, never the current video. */
    @Test
    fun playerCarriesItsRelatedRail() =
        runTest {
            install("video_related.json")

            val detail = loadVideoDetail("dQw4w9WgXcQ").getOrThrow()

            assertEquals("Rick Astley - Never Gonna Give You Up (Official Video) (4K Remaster)", detail.title)
            assertEquals("Rick Astley", detail.channel)
            assertEquals(
                listOf("E8gmARGvPlI", "djV11Xbc914", "TicIH5VBSqM", "eNvUS-6PTbs"),
                detail.related.map { it.id },
            )
            assertTrue(detail.related.none { it.videoId == "dQw4w9WgXcQ" })
            val first = detail.related.first()
            assertEquals("Wham! - Last Christmas (Official Video)", first.title)
            assertEquals("WHAM! · 1.1B · 17y ago", first.subtitle)
            assertEquals(279, first.durationSeconds)
            assertTrue(first.thumbnailUrl?.startsWith("https://") == true)
        }

    /** The player's comments are the section's first page in YouTube's order, with its own total. */
    @Test
    fun playerCarriesItsCommentSection() =
        runTest {
            installComments()

            val detail = loadVideoDetail("dQw4w9WgXcQ").getOrThrow()

            assertEquals("2.4M", detail.commentCountText)
            assertEquals(
                listOf(
                    "@ELIBRON-i2g",
                    "@GabrielsEpicLifeofGoals",
                    "@grustache4569",
                    "@candycake9531",
                    "@DarkstalkerThe_IcyNightwing",
                    "@JetpackGamer456",
                ),
                detail.comments.map { it.author },
            )

            val top = detail.comments.first()
            assertEquals("Yall still getting rick rolled after gta6", top.text)
            assertEquals("14", top.likes)
            assertEquals(1, top.replyCount)
            assertEquals("1 hour ago", top.published)

            val popular = detail.comments.single { it.author == "@candycake9531" }
            assertEquals("294K", popular.likes)
            assertEquals(736, popular.replyCount)
            assertTrue(popular.text.startsWith("Is everyone going to ignore the fact that Rick Astley"))

            assertTrue(detail.comments.all { !it.isPinned && !it.isUploader })
            assertTrue(detail.comments.all { it.authorThumbnailUrl?.startsWith("https://") == true })
        }

    /** The pinned and uploader flags are YouTube's own, taken from the captured pinned thread. */
    @Test
    fun playerMarksTheCapturedPinnedUploaderComment() =
        runTest {
            installComments("video_comments_pinned_entry.json", "video_comments_pinned_page.json")

            val detail = loadVideoDetail("fJ9rUzIMcZQ").getOrThrow()

            assertEquals("701K", detail.commentCountText)
            assertEquals("@Queen", detail.comments.first().author)
            val pinned = detail.comments.first()
            assertTrue(pinned.isPinned)
            assertTrue(pinned.isUploader)
            assertEquals("Pinned by @Queen", pinned.pinnedBy)
            assertEquals("533K", pinned.likes)
            assertEquals(934, pinned.replyCount)
            assertEquals("7 years ago", pinned.published)
            assertTrue(pinned.text.startsWith("Thank you to all our fans across the World"))

            // Every other captured thread is an ordinary comment: no badge, no creator mark.
            assertTrue(detail.comments.drop(1).none { it.isPinned || it.isUploader })
            assertEquals("@bigc20", detail.comments[1].author)
        }

    /** The pinned comment's replies are the real page YouTube answered for its replies token. */
    @Test
    fun playerCarriesTheCapturedReplyThreads() =
        runTest {
            installCommentsWithReplies()

            val detail = loadVideoDetail("fJ9rUzIMcZQ").getOrThrow()
            val pinned = detail.comments.first()
            val token = assertNotNull(pinned.replyToken, "the pinned comment offered no replies token")
            assertEquals(934, pinned.replyCount)

            val replies = loadCommentReplies(token, detail.videoId).getOrThrow()

            assertEquals(
                listOf("@williamdebarros196", "@benjadominguez9600", "@the_jay_21.87", "@sharlott9993"),
                replies.map { it.author },
            )
            assertTrue(replies[0].text.startsWith("Seek God. It's not about religion, it's not about rituals"))
            assertEquals(listOf("16K", "17K", "8.1K", "4.7K"), replies.map { it.likes })
            assertEquals("7 years ago (edited)", replies[0].published)
            assertEquals("All of them", replies[2].text)

            // Replies carry no replies of their own, so nothing below them can expand again.
            assertTrue(replies.all { it.replyCount == 0 && it.replyToken == null })
            assertTrue(replies.none { it.isPinned || it.isUploader })
            assertTrue(replies.all { it.authorThumbnailUrl?.startsWith("https://") == true })
        }

    /** A replies answer with no items is an empty list, not a failure. */
    @Test
    fun anEmptyRepliesAnswerParsesAsNoReplies() =
        runTest {
            installRaw("""{"onResponseReceivedEndpoints":[{"reloadContinuationItemsCommand":{"continuationItems":[]}}]}""")

            assertEquals(emptyList(), loadCommentReplies("no-replies-token", "fJ9rUzIMcZQ").getOrThrow())
        }

    /** A channel's header and Videos tab come from the captured browse responses. */
    @Test
    fun channelLoadsItsHeaderAndFirstVideos() =
        runTest {
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

            val channel = loadChannel("UCiMhD4jzUqG-IgPzUmmytRQ").getOrThrow()

            assertEquals("Queen Official", channel.name)
            assertTrue(channel.avatarUrl?.startsWith("https://") == true)
            assertTrue(channel.subscribers?.contains("subscriber") == true, channel.subscribers.orEmpty())
            assertEquals(6, channel.videos.size)
            assertEquals("QIbT7qqnEDo", channel.videos.first().id)
            assertEquals("Queen - Sheer Heart Attack (Musikbox, February 11, 1979)", channel.videos.first().title)
            assertTrue(channel.videos.all { it.videoId == it.id && it.thumbnailUrl?.startsWith("https://") == true })

            // The header row is what the player's channel tap opens, and back returns to the player.
            val router = AppRouter()
            router.push(AppDestination.Player("fJ9rUzIMcZQ", "Bohemian Rhapsody"))
            router.push(AppDestination.Channel(channel.channelId, channel.name))
            assertEquals("UCiMhD4jzUqG-IgPzUmmytRQ", (router.current as AppDestination.Channel).channelId)
            assertTrue(router.pop())
            assertTrue(router.current is AppDestination.Player)
        }

    /** The reel feed: the seedless sequence, its continuation, then one overlay per reel. */
    @Test
    fun shortsLoadFromTheCapturedReelSequenceAndBackReturnsHome() =
        runTest {
            val first = fixture("shorts_sequence.json")
            val more = fixture("shorts_sequence_more.json")
            val engine =
                MockEngine { request ->
                    val body = request.body.toByteArray().decodeToString()
                    val content =
                        when {
                            !request.url.encodedPath.endsWith("reel/reel_watch_sequence") -> {
                                val id =
                                    Regex("\"videoId\":\"([^\"]+)\"")
                                        .find(body)
                                        ?.groupValues
                                        ?.get(1)
                                        ?: error("no videoId in ${request.url}")
                                fixture("shorts_overlay_$id.json")
                            }

                            // The next page of the sequence goes back in the same field, as a token.
                            body.contains("\"sequenceParams\":\"CA8%3D\"") -> {
                                first
                            }

                            else -> {
                                more
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

            val feed = loadShorts().getOrThrow()

            // The seedless page carries one reel; its continuation is what fills the reel.
            assertEquals(listOf("QUIJ8UtYBzA", "xMHuKwf18vM", "U9KjvoAoQZA"), feed.shorts.map { it.videoId })
            val firstShort = feed.shorts.first()
            assertEquals("@shorts_kingdom01", firstShort.channelName)
            assertTrue(
                firstShort.title.startsWith("Uncle's kindness towards his friend"),
                firstShort.title,
            )
            // The still is the sequence's own frame, not the poster this UI would otherwise build.
            assertEquals("https://i.ytimg.com/vi/QUIJ8UtYBzA/frame0.jpg", firstShort.stillUrl)
            assertEquals(listOf("@shorts_kingdom01", "@Virat_zxv", "@PetsSwag"), feed.shorts.map { it.channelName })

            // Home opens the reel; back returns to Home, not to a tab switch.
            val router = AppRouter()
            router.push(AppDestination.Shorts)
            assertEquals(AppDestination.Shorts, router.current)
            assertTrue(router.pop())
            assertEquals(AppDestination.Home, router.current)
        }

    /**
     * The library landing request, captured signed out: YouTube sends a sign-in message instead of
     * a grid, and the parser turns it into an empty feed with that message rather than throwing.
     */
    @Test
    fun libraryLandingParsesTheCapturedSignInNoticeAsAnEmptyFeed() =
        runTest {
            install("library_landing_signed_out.json")

            val feed = loadLibrary().getOrThrow()

            assertTrue(feed.entries.isEmpty(), "a signed-out library must parse as empty")
            assertEquals("Explore your favorites", feed.notice)
            assertEquals("Sign in to access tracks that you liked or saved", feed.noticeSubtext)
            assertEquals(null, feed.continuation)

            // The library is a tab, so opening it replaces the stack rather than pushing on it.
            val router = AppRouter()
            router.openTab(AppDestination.Library)
            assertEquals(AppDestination.Library, router.current)
            assertFalse(router.canGoBack)
            router.openTab(AppDestination.Home)
            assertEquals(AppDestination.Home, router.current)
        }

    /**
     * Live chat: the seed token read out of the captured watch page, then one captured poll mapped
     * into the model the player draws. The poll token and its interval are the response's own.
     */
    @Test
    fun liveChatReadsTheCapturedSeedAndMapsTheCapturedPoll() =
        runTest {
            val seed = fixture("live_chat_seed.json")
            val poll = fixture("live_chat_poll.json")
            val engine =
                MockEngine { request ->
                    val content =
                        if (request.url.encodedPath.endsWith("live_chat/get_live_chat")) poll else seed
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

            val watch = YouTube.watchNext("rFZHOHl-L8A").getOrThrow()
            val token = assertNotNull(YouTube.liveChatToken(watch), "the captured watch page has no chat")
            assertEquals(192, token.length)

            val batch = liveChatMessages(token).first()

            assertEquals(2, batch.size)
            assertEquals("@aarvi-b7r", batch[0].author)
            assertEquals("u", batch[0].message)
            assertEquals(LiveChatMessageType.TEXT, batch[0].type)
            // The capture carries no timestamp for this message, and the model must not invent one.
            assertEquals(null, batch[0].timestamp)
            assertEquals("@MYOWNSPACE_02", batch[1].author)
        }

    /** A normal video's watch page carries no conversation bar, so the player never polls chat. */
    @Test
    fun aNormalWatchPageHasNoLiveChatToken() =
        runTest {
            install("video_comments_entry.json")

            val watch = YouTube.watchNext("dQw4w9WgXcQ").getOrThrow()

            assertEquals(null, YouTube.liveChatToken(watch))
        }
}
