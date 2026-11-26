package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.data.taste.UserTaste
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The video Home's parsing and its chips' orderings, against the captured browse shape. */
class VideoHomeFeedTest {
    private fun home() =
        Json.parseToJsonElement(
            assertNotNull(javaClass.getResourceAsStream("/fixtures/video_home.json"), "missing fixture video_home.json")
                .bufferedReader()
                .use { it.readText() },
        )

    @Test
    fun `reads the grid in the response's order, without shorts or premieres`() {
        val videos = home().toVideoHomeItems()

        assertEquals(listOf("aaaaaaaaaaa", "ccccccccccc"), videos.map { it.id })
        assertEquals("First long video", videos.first().title)
        assertEquals("Channel One", videos.first().channelName)
        assertEquals(12 * 60 + 34, videos.first().duration)
        assertEquals("2 days ago", videos.first().uploadDate)
        assertTrue(videos.none { it.isShort })
        assertTrue(videos.none { it.isUpcoming })
    }

    @Test
    fun `reads the browse document's own chip labels`() {
        assertEquals(listOf("All", "Music", "Gaming"), home().toVideoHomeChipLabels())
    }

    @Test
    fun `keeps only long-form uploads from the last week`() {
        val now = 10_000_000_000_000L
        val day = 24L * 60L * 60L * 1000L
        val videos =
            listOf(
                video(id = "recent", timestamp = now - day),
                video(id = "old", timestamp = now - 10 * day),
                video(id = "unknown-date", timestamp = 0L),
                video(id = "live", timestamp = now - day, isLive = true),
                video(id = "short", timestamp = now - day, isShort = true),
            )

        assertEquals(listOf("recent"), uploadedThisWeek(videos, now).map { it.id })
    }

    @Test
    fun `freshness order leans towards the newest without discarding the rank`() {
        val now = 10_000_000_000_000L
        val day = 24L * 60L * 60L * 1000L
        val ranked =
            listOf(
                video(id = "rank0-old", timestamp = now - 7 * day),
                video(id = "rank1-new", timestamp = now),
                video(id = "rank2-old", timestamp = now - 7 * day),
            )

        // A week-old video keeps its rank; today's climbs a fifth of the way up.
        assertEquals(
            listOf("rank1-new", "rank0-old", "rank2-old"),
            freshnessOrder(listOf(ranked[1], ranked[0], ranked[2]), now).map { it.id },
        )
    }

    @Test
    fun `new to you never repeats a channel`() {
        val videos =
            listOf(
                video(id = "a1", channelId = "UCa"),
                video(id = "a2", channelId = "UCa"),
                video(id = "b1", channelId = "UCb"),
                video(id = "unknown", channelId = ""),
            )

        val unknown = fromUnknownChannels(videos, knownChannels = setOf("UCb"))

        assertEquals(listOf("a1"), unknown.map { it.id })
        assertFalse(unknown.any { it.channelId == "UCb" })
    }

    @Test
    fun `chips carry the app's keys, in its order`() {
        assertEquals(
            listOf("all", "new", "recent", "live"),
            HomeChip.forTaste(UserTaste()).map { it.key },
        )
    }

    @Test
    fun `chips carry the viewer's picks between All and the filters`() {
        val taste = UserTaste(topics = listOf("Gaming", "Music", "Pop Music", "Gaming"))

        assertEquals(
            listOf("all", "interest:Gaming", "interest:Music", "new", "recent", "live"),
            HomeChip.forTaste(taste).map { it.key },
        )
    }

    private fun video(
        id: String,
        channelId: String = "UC$id",
        timestamp: Long = 0L,
        isLive: Boolean = false,
        isShort: Boolean = false,
    ): Video =
        Video(
            id = id,
            title = id,
            channelName = "Channel $channelId",
            channelId = channelId,
            thumbnailUrl = "",
            duration = 300,
            viewCount = 0,
            uploadDate = "",
            timestamp = timestamp,
            isLive = isLive,
            isShort = isShort,
        )
}
