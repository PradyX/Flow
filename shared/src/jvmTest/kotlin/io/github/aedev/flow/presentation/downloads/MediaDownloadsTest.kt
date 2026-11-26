package io.github.aedev.flow.presentation.downloads

import io.github.aedev.flow.data.downloads.DownloadEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaDownloadsTest {
    @Test
    fun aDownloadRebuildsTheStreamItWasFetchedFrom() {
        val entry =
            DownloadEntry(
                videoId = "a",
                title = "Title",
                videoUrl = "https://example.test/video",
                audioUrl = "https://example.test/audio",
                userAgent = "agent",
                mimeType = "video/mp4",
                audioMimeType = "audio/mp4",
                qualityLabel = "720p",
                height = 720,
                sizeBytes = 4_000,
            )
        val stream = entry.toPlayableStream()
        assertEquals("https://example.test/video", stream.videoUrl)
        assertEquals("https://example.test/audio", stream.audioUrl)
        assertEquals("agent", stream.userAgent)
        assertEquals("video/mp4", stream.videoMimeType)
        assertEquals("720p", stream.qualityLabel)
        assertEquals(720, stream.height)
        assertEquals(false, stream.isMuxed)
    }

    @Test
    fun aMuxedDownloadNeedsNoAudioTrack() {
        val entry = DownloadEntry(videoId = "a", title = "Title", videoUrl = "https://example.test/one")
        val stream = entry.toPlayableStream()
        assertNull(stream.audioUrl)
        assertEquals(true, stream.isMuxed)
    }
}
