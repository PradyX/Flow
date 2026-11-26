package io.github.aedev.flow.data.downloads

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DownloadsTest {
    private fun entry(
        id: String,
        at: Long = 0,
        size: Long = 1_000,
    ) = DownloadEntry(
        videoId = id,
        title = "Title $id",
        videoUrl = "https://example.test/$id/video",
        sizeBytes = size,
        completedAtMillis = at,
    )

    @Test
    fun theNewestDownloadIsTheFirstRow() {
        val downloads = Downloads().with(entry("a", at = 10)).with(entry("b", at = 20))
        assertEquals(listOf("b", "a"), downloads.entries.map { it.videoId })
    }

    @Test
    fun downloadingTheSameVideoAgainReplacesItsEntry() {
        val downloads =
            Downloads()
                .with(entry("a", at = 10, size = 1_000))
                .with(entry("b", at = 20))
                .with(entry("a", at = 30, size = 2_500))
        assertEquals(listOf("a", "b"), downloads.entries.map { it.videoId })
        assertEquals(1, downloads.entries.count { it.videoId == "a" })
        assertEquals(2_500, downloads.entry("a")?.sizeBytes)
    }

    @Test
    fun aFailedDownloadLeavesNoEntry() {
        // A fetch that aborted is never recorded, and the video can be downloaded again later.
        val downloads = Downloads().with(entry("ok", at = 10))
        assertFalse(downloads.contains("failed"))
        assertNull(downloads.entry("failed"))
        // The retry stores the same id the abort never recorded, so it adds exactly one row.
        val retried = downloads.with(entry("failed", at = 20))
        assertEquals(listOf("failed", "ok"), retried.entries.map { it.videoId })
        assertEquals(1, retried.entries.count { it.videoId == "failed" })
    }

    @Test
    fun aDownloadWithoutAVideoIdIsNotStored() {
        assertTrue(Downloads().with(entry("")).entries.isEmpty())
        assertTrue(Downloads().with(entry("   ")).entries.isEmpty())
    }

    @Test
    fun removingADownloadForgetsOnlyThatVideo() {
        val downloads = Downloads().with(entry("a", at = 10)).with(entry("b", at = 20))
        val left = downloads.without("a")
        assertEquals(listOf("b"), left.entries.map { it.videoId })
        assertTrue(left.without("b").entries.isEmpty())
    }

    @Test
    fun theRecordSaysHowMuchIsOnDisk() {
        val downloads = Downloads().with(entry("a", size = 1_500)).with(entry("b", size = 2_000))
        assertEquals(3_500, downloads.totalBytes())
    }

    @Test
    fun anEntryRemembersTheStreamsItWasFetchedFrom() {
        val decoded = Downloads.decode(Downloads().with(entry("a", at = 5)).encode())
        val one = decoded.entry("a")
        assertEquals("https://example.test/a/video", one?.videoUrl)
        assertNull(one?.audioUrl)
        assertEquals(5, one?.completedAtMillis)
        assertEquals(1, one?.trackCount)
    }

    @Test
    fun aDownloadRoundTripsThroughItsText() {
        val original =
            Downloads()
                .with(entry("a", at = 10))
                .with(
                    entry("b", at = 20).copy(
                        audioUrl = "https://example.test/b/audio",
                        audioMimeType = "audio/mp4",
                        qualityLabel = "720p",
                        userAgent = "agent",
                    ),
                )
        val decoded = Downloads.decode(original.encode())
        assertEquals(original, decoded)
        assertEquals(2, decoded.entry("b")?.trackCount)
        assertEquals("agent", decoded.entry("b")?.userAgent)
        assertEquals("720p", decoded.entry("b")?.qualityLabel)
    }

    @Test
    fun aHandEditedRecordIsCleanedOnRead() {
        val text =
            """{"entries":[{"videoId":"a","title":"A","videoUrl":"u","sizeBytes":-5},""" +
                """{"videoId":"a","title":"A again","videoUrl":"u"},""" +
                """{"videoId":"","title":"nameless","videoUrl":"u"}]}"""
        val decoded = Downloads.decode(text)
        assertEquals(1, decoded.entries.size)
        assertEquals(0, decoded.entry("a")?.sizeBytes)
    }

    @Test
    fun unreadableTextReadsAsAnEmptyRecord() {
        assertTrue(Downloads.decode(null).entries.isEmpty())
        assertTrue(Downloads.decode("not json").entries.isEmpty())
        assertTrue(Downloads.decode("{}").entries.isEmpty())
    }
}
