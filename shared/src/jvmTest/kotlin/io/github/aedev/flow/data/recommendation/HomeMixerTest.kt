package io.github.aedev.flow.data.recommendation

import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.presentation.feed.freshnessOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The ported Home rank and diversity pass: exact orders, tie-breaks and exclusions. */
class HomeMixerTest {
    private fun video(
        id: String,
        channelId: String = "UC$id",
        title: String = id,
        views: Long = 0,
        date: String = "3 weeks ago",
        isLive: Boolean = false,
        tags: List<String> = emptyList(),
    ): Video =
        Video(
            id = id,
            title = title,
            channelName = "Channel $channelId",
            channelId = channelId,
            thumbnailUrl = "",
            duration = 300,
            viewCount = views,
            uploadDate = date,
            isLive = isLive,
            tags = tags,
        )

    @Test
    fun `a subscribed channel outranks an identical stranger`() {
        val ranked =
            rankHomeVideos(
                listOf(video("stranger"), video("followed")),
                HomeMixState(userSubs = setOf("UCfollowed")),
            )

        assertEquals(listOf("followed", "stranger"), ranked.map { it.id })
    }

    @Test
    fun `a fresh upload outranks a month old one`() {
        val ranked =
            rankHomeVideos(
                listOf(video("month-old", date = "1 month ago"), video("hours-old", date = "5 hours ago")),
                HomeMixState(),
            )

        assertEquals(listOf("hours-old", "month-old"), ranked.map { it.id })
    }

    @Test
    fun `equal scores keep the source order`() {
        val ranked =
            rankHomeVideos(
                listOf(video("first"), video("second"), video("third")),
                HomeMixState(),
            )

        assertEquals(listOf("first", "second", "third"), ranked.map { it.id })
    }

    @Test
    fun `suppressed videos, suppressed channels and already shown ids are removed`() {
        val ranked =
            rankHomeVideos(
                listOf(
                    video("kept"),
                    video("rejected"),
                    video("from-blocked-channel", channelId = "UCblocked"),
                    video("already-shown"),
                ),
                HomeMixState(
                    suppressedVideoIds = setOf("rejected"),
                    suppressedChannels = setOf("UCblocked"),
                    feedHistory = setOf("already-shown"),
                ),
            )

        assertEquals(listOf("kept"), ranked.map { it.id })
    }

    @Test
    fun `a blocked topic removes the video that carries it`() {
        val ranked =
            rankHomeVideos(
                listOf(video("news-video", title = "Breaking politics today"), video("cooking", title = "Cooking pasta")),
                HomeMixState(blockedTopics = setOf("politics")),
            )

        assertEquals(listOf("cooking"), ranked.map { it.id })
    }

    @Test
    fun `taste lifts the topic the viewer leans towards`() {
        val ranked =
            rankHomeVideos(
                listOf(video("off-topic", title = "Gardening tips"), video("on-topic", title = "Minecraft build guide")),
                HomeMixState(interests = mapOf("minecraft" to 1.0), totalInteractions = 1_000),
            )

        assertEquals(listOf("on-topic", "off-topic"), ranked.map { it.id })
        assertTrue(scoreFor("on-topic") > scoreFor("off-topic"))
    }

    @Test
    fun `diversity places a second video from the same channel after another channel`() {
        val ranked =
            rankHomeVideos(
                listOf(
                    video("a1", channelId = "UCa", views = 100_000, date = "1 day ago"),
                    video("a2", channelId = "UCa", views = 50_000, date = "1 day ago"),
                    video("b1", channelId = "UCb", views = 30_000, date = "1 day ago"),
                ),
                HomeMixState(),
            )

        assertEquals(listOf("a1", "b1", "a2"), ranked.map { it.id })
    }

    @Test
    fun `nearly identical titles are dropped after the title gates`() {
        val ranked =
            rankHomeVideos(
                listOf(
                    video("build-1", title = "Minecraft survival episode build house part one", views = 100_000),
                    video("build-2", title = "Minecraft survival episode build house part two", views = 90_000),
                    video("other", title = "Completely unrelated cooking video", views = 20_000),
                ),
                HomeMixState(),
            )

        assertEquals("build-1", ranked.first().id)
        // A near-duplicate title fails both title gates, so it is dropped, as the app drops it.
        assertEquals(listOf("build-1", "other"), ranked.map { it.id })
    }

    @Test
    fun `the recently uploaded chip ranks first, then applies the freshness lean`() {
        val now = 10_000_000_000_000L
        val day = 24L * 60L * 60L * 1000L
        val pool =
            listOf(
                video("top-today").copy(timestamp = now - day, viewCount = 500_000, uploadDate = "1 day ago", channelId = "UCone"),
                video("mid-week").copy(timestamp = now - 5 * day, viewCount = 50_000, uploadDate = "5 days ago", channelId = "UCtwo"),
                video("top-week").copy(timestamp = now - 6 * day, viewCount = 900_000, uploadDate = "6 days ago", channelId = "UCthree"),
            )

        val ranked = freshnessOrder(rankHomeVideos(pool, HomeMixState()), now)

        assertEquals(listOf("top-week", "top-today", "mid-week"), ranked.map { it.id })
    }

    @Test
    fun `the cold start path is the app's, not a skipped mixer`() {
        // No interests and no interactions: personality is 0, novelty is 1, popularity and age decide.
        val ranked =
            rankHomeVideos(
                listOf(
                    video("quiet", views = 0, date = "1 year ago"),
                    video("popular", views = 10_000_000, date = "1 day ago"),
                ),
                HomeMixState(),
            )

        assertEquals(listOf("popular", "quiet"), ranked.map { it.id })
        assertTrue(cosine(HomeMixState().interests, homeTopicVector(video("quiet"))) == 0.0)
    }

    private fun scoreFor(id: String): Double {
        val video = video(id, title = if (id == "on-topic") "Minecraft build guide" else "Gardening tips")
        return scoreHomeVideo(video, homeTopicVector(video), HomeMixState(interests = mapOf("minecraft" to 1.0), totalInteractions = 1_000))
    }
}
