package io.github.aedev.flow.data.playlists

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserPlaylistsTest {
    private fun video(id: String) = PlaylistVideo(videoId = id, title = "Video $id")

    @Test
    fun `a new playlist goes on top with its name trimmed`() {
        val playlists =
            UserPlaylists()
                .create("Road trip", nowMillis = 10)
                .create("  Late night  ", nowMillis = 20)
        assertEquals(listOf("Late night", "Road trip"), playlists.playlists.map { it.name })
    }

    @Test
    fun `a blank name makes nothing`() {
        assertTrue(UserPlaylists().create("   ", nowMillis = 10).playlists.isEmpty())
    }

    @Test
    fun `two playlists made in the same millisecond get different ids`() {
        val playlists =
            UserPlaylists()
                .create("One", nowMillis = 10)
                .create("Two", nowMillis = 10)
        assertEquals(
            2,
            playlists.playlists
                .map { it.id }
                .toSet()
                .size,
        )
    }

    @Test
    fun `a video is on a playlist once`() {
        val id =
            UserPlaylists()
                .create("Mix", nowMillis = 10)
                .playlists
                .first()
                .id
        val playlists =
            UserPlaylists()
                .create("Mix", nowMillis = 10)
                .add(id, video("a"), nowMillis = 20)
                .add(id, video("a"), nowMillis = 30)
                .add(id, video("b"), nowMillis = 40)
        assertEquals(listOf("a", "b"), playlists.find(id)?.videos?.map { it.videoId })
        assertTrue(playlists.contains(id, "b"))
    }

    @Test
    fun `removing a video leaves the playlist and the other videos`() {
        val id =
            UserPlaylists()
                .create("Mix", nowMillis = 10)
                .playlists
                .first()
                .id
        val playlists =
            UserPlaylists()
                .create("Mix", nowMillis = 10)
                .add(id, video("a"), nowMillis = 20)
                .add(id, video("b"), nowMillis = 30)
                .remove(id, "a", nowMillis = 40)
        assertEquals(listOf("b"), playlists.find(id)?.videos?.map { it.videoId })
        assertTrue(!playlists.contains(id, "a"))
    }

    @Test
    fun `renaming keeps the videos and deleting removes the playlist`() {
        val id =
            UserPlaylists()
                .create("Mix", nowMillis = 10)
                .playlists
                .first()
                .id
        val renamed =
            UserPlaylists()
                .create("Mix", nowMillis = 10)
                .add(id, video("a"), nowMillis = 20)
                .rename(id, "Better mix", nowMillis = 30)
        assertEquals("Better mix", renamed.find(id)?.name)
        assertEquals(listOf("a"), renamed.find(id)?.videos?.map { it.videoId })
        assertNull(renamed.delete(id).find(id))
    }

    @Test
    fun `a playlist survives being written down and read back`() {
        val id =
            UserPlaylists()
                .create("Mix", nowMillis = 10)
                .playlists
                .first()
                .id
        val playlists =
            UserPlaylists()
                .create("Mix", nowMillis = 10)
                .add(id, PlaylistVideo("a", "Video a", channelName = "Channel", thumbnailUrl = "https://i/a.jpg"), 20)
        assertEquals(playlists, UserPlaylists.decode(playlists.encode()))
    }

    @Test
    fun `text the codec cannot read is no playlists`() {
        assertTrue(UserPlaylists.decode("not json").playlists.isEmpty())
        assertTrue(UserPlaylists.decode(null).playlists.isEmpty())
    }

    @Test
    fun `an empty video id is not added`() {
        val id =
            UserPlaylists()
                .create("Mix", nowMillis = 10)
                .playlists
                .first()
                .id
        assertTrue(
            UserPlaylists()
                .create("Mix", nowMillis = 10)
                .add(id, video(""), nowMillis = 20)
                .find(id)
                ?.videos
                .isNullOrEmpty(),
        )
    }
}
