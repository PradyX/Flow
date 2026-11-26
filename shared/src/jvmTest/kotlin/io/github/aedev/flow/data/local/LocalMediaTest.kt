package io.github.aedev.flow.data.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalMediaTest {
    private fun item(
        uri: String,
        title: String = "File $uri",
        addedAt: Long = 0,
    ) = LocalMediaItem(uri = uri, title = title, mimeType = "video/mp4", sizeBytes = 1_000, addedAtMillis = addedAt)

    @Test
    fun `a picked file is one row, newest first`() {
        val library = LocalMediaLibrary().with(item("content://a", addedAt = 10)).with(item("content://b", addedAt = 20))
        assertEquals(listOf("content://b", "content://a"), library.items.map { it.uri })
    }

    @Test
    fun `picking the same file again moves it up instead of adding a row`() {
        val library =
            LocalMediaLibrary()
                .with(item("content://a", addedAt = 10))
                .with(item("content://b", addedAt = 20))
                .with(item("content://a", title = "Renamed", addedAt = 30))
        assertEquals(listOf("content://a", "content://b"), library.items.map { it.uri })
        assertEquals("Renamed", library.item("content://a")?.title)
    }

    @Test
    fun `a blank uri is not a row`() {
        assertTrue(LocalMediaLibrary().with(item("")).items.isEmpty())
    }

    @Test
    fun `one picker dialog adds every file it returned`() {
        val library = LocalMediaLibrary().withAll(listOf(item("content://a"), item("content://b"), item("content://a")))
        assertEquals(listOf("content://a", "content://b"), library.items.map { it.uri })
    }

    @Test
    fun `the record keeps at most its cap`() {
        val full =
            (1..LocalMediaLibrary.MAX_ITEMS + 5).fold(LocalMediaLibrary()) { acc, index ->
                acc.with(item("content://$index"))
            }
        assertEquals(LocalMediaLibrary.MAX_ITEMS, full.items.size)
        assertEquals("content://${LocalMediaLibrary.MAX_ITEMS + 5}", full.items.first().uri)
    }

    @Test
    fun `removing a file drops only that row`() {
        val library = LocalMediaLibrary().withAll(listOf(item("content://a"), item("content://b"))).without("content://a")
        assertEquals(listOf("content://b"), library.items.map { it.uri })
    }

    @Test
    fun `the record survives a round trip and ignores a broken one`() {
        val library = LocalMediaLibrary().withAll(listOf(item("content://a"), item("content://b")))
        assertEquals(library, LocalMediaLibrary.decode(library.encode()))
        assertTrue(LocalMediaLibrary.decode(null).items.isEmpty())
        assertTrue(LocalMediaLibrary.decode("not json").items.isEmpty())
    }

    @Test
    fun `an imported file plays from its own uri`() {
        val stream = item("content://media/1", title = "Holiday").toPlayableStream()
        assertEquals("content://media/1", stream.videoUrl)
        assertEquals("video/mp4", stream.videoMimeType)
        // One file, so the engine needs no separate audio track and no merge.
        assertTrue(stream.isMuxed)
    }

    @Test
    fun `a file with no reported type still plays as a video`() {
        val stream = LocalMediaItem(uri = "content://media/2", title = "Untitled").toPlayableStream()
        assertEquals("video/*", stream.videoMimeType)
    }

    @Test
    fun `the library resolves only what it holds`() {
        val library = LocalMediaLibrary().with(item("content://a"))
        assertEquals("content://a", library.streamFor("content://a")?.videoUrl)
        assertNull(library.streamFor("content://elsewhere"))
    }
}
