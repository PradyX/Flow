package io.github.aedev.flow.data.library

import io.github.aedev.flow.presentation.screens.fileLibraryStore
import io.github.aedev.flow.presentation.screens.tempLibraryDatabasePath
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The library's Room store over a real database file: a second store instance, which is what an app
 * restart produces, reads back exactly what the first one wrote.
 */
class LibraryStoreTest {
    private val videoId = "fJ9rUzIMcZQ"
    private val title = "Queen – Bohemian Rhapsody (Official Video Remastered)"
    private val channel = "Queen Official"
    private val thumbnail = "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg"

    @Test
    fun savedAndDownloadedStateSurvivesANewStoreInstance() =
        runTest {
            val path = tempLibraryDatabasePath()

            val first = fileLibraryStore(path)
            assertTrue(first.toggleSaved(videoId, title, channel, thumbnail, nowMillis = 1_700_000_000_000))
            assertTrue(first.toggleDownloaded(videoId, title, channel, thumbnail, nowMillis = 1_700_000_000_000))
            first.close()

            val second = fileLibraryStore(path)
            val restored = second.items()
            assertEquals(1, restored.size)
            val item = restored.single()
            assertEquals(videoId, item.videoId)
            assertEquals(title, item.title)
            assertEquals(channel, item.channelName)
            assertEquals(thumbnail, item.thumbnailUrl)
            assertTrue(item.isDownloaded, "the download flag did not survive the new instance")
            second.close()

            // Removing the download persists too: the row goes, not just its flag.
            val third = fileLibraryStore(path)
            assertFalse(third.toggleDownloaded(videoId, title, channel, thumbnail, nowMillis = 1_700_000_000_000))
            third.close()

            val fourth = fileLibraryStore(path)
            assertNull(fourth.find(videoId))
            assertTrue(fourth.items().isEmpty())
            fourth.close()
        }
}
