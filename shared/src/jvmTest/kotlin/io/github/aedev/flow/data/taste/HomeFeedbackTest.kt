package io.github.aedev.flow.data.taste

import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.data.recommendation.HomeMixState
import io.github.aedev.flow.data.recommendation.rankHomeVideos
import io.github.aedev.flow.presentation.feed.rankForHome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The card menu's negative feedback: what it stores, and how it reaches the mixer. */
class HomeFeedbackTest {
    private fun video(
        id: String,
        channelId: String = "UC$id",
        views: Long = 0,
    ): Video =
        Video(
            id = id,
            title = id,
            channelName = "Channel $channelId",
            channelId = channelId,
            thumbnailUrl = "",
            duration = 300,
            viewCount = views,
            uploadDate = "3 weeks ago",
        )

    @Test
    fun `marking a video as not interested keeps one entry`() {
        val once = HomeFeedback().withoutVideo("v1")
        assertEquals(listOf("v1"), once.videoIds)
        assertEquals(once, once.withoutVideo("v1"))
        assertEquals(listOf("v1"), HomeFeedback().withoutVideo("v1").withoutVideo("v1").videoIds)
    }

    @Test
    fun `a blank video id is not a mark`() {
        assertTrue(HomeFeedback().withoutVideo("").isEmpty)
    }

    @Test
    fun `a channel mark keeps one entry per channel`() {
        val channel = TasteChannel("UC1", "Creator", null)
        val once = HomeFeedback().withoutChannel(channel)
        assertEquals(listOf(channel), once.channels)
        assertEquals(once, once.withoutChannel(channel))
        assertTrue(HomeFeedback().withoutChannel(TasteChannel("", "Nobody", null)).isEmpty)
    }

    @Test
    fun `feedback merges into the mixer state without losing the taste`() {
        val state =
            HomeFeedback(
                videoIds = listOf("v1"),
                channels = listOf(TasteChannel("UC2", "Two", null)),
            ).toMixState(
                HomeMixState(userSubs = setOf("UCfollowed"), interests = mapOf("Music" to 1.0)),
            )

        assertEquals(setOf("v1"), state.suppressedVideoIds)
        assertEquals(setOf("UC2"), state.suppressedChannels)
        assertEquals(setOf("UCfollowed"), state.userSubs)
        assertEquals(mapOf("Music" to 1.0), state.interests)
    }

    @Test
    fun `the suppressed video is dropped before ranking`() {
        val ranked =
            rankHomeVideos(
                listOf(video("keep", views = 100), video("hide", views = 900)),
                HomeFeedback(videoIds = listOf("hide")).toMixState(),
            )

        assertEquals(listOf("keep"), ranked.map { it.id })
    }

    @Test
    fun `the suppressed channel's videos are all dropped`() {
        val ranked =
            rankHomeVideos(
                listOf(video("a", channelId = "UCone"), video("b", channelId = "UCtwo"), video("c", channelId = "UCone")),
                HomeFeedback(channels = listOf(TasteChannel("UCone", "One", null))).toMixState(),
            )

        assertEquals(listOf("b"), ranked.map { it.id })
    }

    @Test
    fun `a fully suppressed chip falls back to the unsuppressed order, never a blank feed`() {
        val videos = listOf(video("a", views = 10), video("b", views = 20))
        val state = HomeFeedback(videoIds = listOf("a", "b")).toMixState()

        assertTrue(rankHomeVideos(videos, state).isEmpty())
        // The fallback reruns the same ranking without the marks, so it is scored, not source order.
        assertEquals(listOf("b", "a"), videos.rankForHome(state).map { it.id })
    }

    @Test
    fun `partial suppression still ranks only the rows that survived`() {
        val videos = listOf(video("a", views = 10), video("b", views = 20), video("c", views = 30))
        val ranked = videos.rankForHome(HomeFeedback(videoIds = listOf("b")).toMixState())

        assertEquals(listOf("c", "a"), ranked.map { it.id })
        assertFalse(ranked.any { it.id == "b" })
    }

    @Test
    fun `feedback survives the round trip through storage`() {
        val feedback =
            HomeFeedback(
                videoIds = listOf("v1", "v2"),
                channels = listOf(TasteChannel("UC1", "One", "https://example.test/one.jpg")),
            )

        assertEquals(feedback, HomeFeedback.fromJson(HomeFeedback.toJson(feedback)))
    }

    @Test
    fun `missing or unreadable storage answers nothing, not a crash`() {
        assertNull(HomeFeedback.fromJson(null))
        assertNull(HomeFeedback.fromJson(""))
        assertNull(HomeFeedback.fromJson("{not json"))
    }

    @Test
    fun `a like marks the video and its channel once, ignoring blank ids`() {
        val channel = TasteChannel("UC1", "Creator", null)
        val once = HomeFeedback().withVideoLike("v1", channel)

        assertEquals(listOf("v1"), once.likedVideoIds)
        assertEquals(listOf(channel), once.likedChannels)
        assertEquals(once, once.withVideoLike("v1", channel))
        assertTrue(HomeFeedback().withVideoLike("", null).isEmpty)
        assertTrue(HomeFeedback().withVideoLike("", TasteChannel("", "Nobody", null)).isEmpty)
    }

    @Test
    fun `likes merge into the mixer state beside the taste`() {
        val state =
            HomeFeedback(
                likedVideoIds = listOf("v1"),
                likedChannels = listOf(TasteChannel("UC2", "Two", null)),
            ).toMixState(HomeMixState(userSubs = setOf("UCfollowed"), interests = mapOf("Music" to 1.0)))

        assertEquals(setOf("v1"), state.boostedVideoIds)
        assertEquals(setOf("UC2"), state.boostedChannels)
        assertEquals(setOf("UCfollowed"), state.userSubs)
        assertEquals(mapOf("Music" to 1.0), state.interests)
        assertTrue(state.suppressedVideoIds.isEmpty())
    }

    @Test
    fun `a like raises a low-view row above a popular one`() {
        val videos = listOf(video("popular", views = 5_000_000), video("liked", views = 1_000))
        val unmarked = rankHomeVideos(videos, HomeMixState()).map { it.id }
        val marked = rankHomeVideos(videos, HomeFeedback(likedVideoIds = listOf("liked")).toMixState()).map { it.id }

        assertEquals(listOf("popular", "liked"), unmarked)
        assertEquals(listOf("liked", "popular"), marked)
    }

    @Test
    fun `a liked channel lifts all of its videos`() {
        val videos =
            listOf(
                video("a", channelId = "UCpopular", views = 4_000_000),
                video("b", channelId = "UCquiet"),
                video("c", channelId = "UCquiet"),
            )
        val ranked =
            rankHomeVideos(
                videos,
                HomeFeedback(likedChannels = listOf(TasteChannel("UCquiet", "Quiet", null))).toMixState(),
            ).map { it.id }

        assertTrue(ranked.first() in setOf("b", "c"))
        assertTrue(ranked.containsAll(listOf("b", "c")))
    }

    @Test
    fun `suppression wins when the same video is liked and not interested`() {
        val feedback =
            HomeFeedback(
                likedVideoIds = listOf("both"),
                videoIds = listOf("both"),
                likedChannels = listOf(TasteChannel("UCblocked", "Blocked", null)),
                channels = listOf(TasteChannel("UCblocked", "Blocked", null)),
            )
        val ranked =
            rankHomeVideos(
                listOf(video("both"), video("other"), video("x", channelId = "UCblocked")),
                feedback.toMixState(),
            ).map { it.id }

        assertEquals(listOf("other"), ranked)
    }

    @Test
    fun `undoing a like removes both marks and the video keeps its place`() {
        val channel = TasteChannel("UC1", "Creator", null)
        val liked = HomeFeedback().withVideoLike("v1", channel)
        val undone = liked.withoutVideoLike("v1", "UC1")

        assertTrue(undone.isEmpty)
        assertEquals(listOf("v1"), rankHomeVideos(listOf(video("v1")), undone.toMixState()).map { it.id })
    }

    @Test
    fun `undoing a negative mark brings the video and channel back`() {
        val channel = TasteChannel("UCv1", "Channel UCv1", null)
        val hidden = HomeFeedback().withoutVideo("v1").withoutChannel(channel)

        assertEquals(listOf("v1"), hidden.videoIds)
        assertTrue(hidden.restoreVideo("v1").videoIds.isEmpty())
        assertTrue(hidden.restoreChannel("UCv1").channels.isEmpty())

        val restored = hidden.restoreVideo("v1").restoreChannel("UCv1")
        assertEquals(
            listOf("v1"),
            rankHomeVideos(listOf(video("v1")), restored.toMixState()).map { it.id },
        )
    }

    @Test
    fun `a negative mark clears a positive one on the same target`() {
        val channel = TasteChannel("UC1", "Creator", null)
        val liked = HomeFeedback().withVideoLike("v1", channel)

        assertTrue(liked.withoutVideo("v1").likedVideoIds.isEmpty())
        assertTrue(liked.withoutChannel(channel).likedChannels.isEmpty())
    }

    @Test
    fun `the enlarged feedback survives the round trip through storage`() {
        val feedback =
            HomeFeedback(
                likedVideoIds = listOf("v1"),
                likedChannels = listOf(TasteChannel("UC1", "One", "https://example.test/one.jpg")),
                videoIds = listOf("v2"),
                channels = listOf(TasteChannel("UC2", "Two", null)),
            )

        assertEquals(feedback, HomeFeedback.fromJson(HomeFeedback.toJson(feedback)))
    }
}
