package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.innertube.models.AlbumItem
import io.github.aedev.flow.innertube.models.Artist
import io.github.aedev.flow.innertube.models.PlaylistItem
import io.github.aedev.flow.innertube.models.SongItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A music shelf card opens the page its item names: an album through its playlist id, a playlist
 * through its own, and a song through its watch page. A playlist card must not fall back to its
 * first song, which is what a watch endpoint on a shelf playlist carries.
 */
class MusicEntryTest {
    @Test
    fun anAlbumOpensItsAlbumPage() {
        val album =
            AlbumItem(
                browseId = "MPREb_abc",
                playlistId = "OLAK5uy_abc",
                title = "Jailer 2",
                artists = listOf(Artist("Anirudh Ravichander", null)),
                thumbnail = "https://lh3.googleusercontent.com/album",
            )

        val entry = album.toMusicEntry()

        // The album page is browsed by the album id; OLAK5uy_ does not resolve as a browse id.
        assertEquals("MPREb_abc", entry.playlistId)
        assertNull(entry.videoId)
        assertEquals("Jailer 2", entry.title)
        assertEquals("Anirudh Ravichander", entry.subtitle)
    }

    @Test
    fun aPlaylistKeepsItsOwnIdRatherThanItsFirstSong() {
        val playlist =
            PlaylistItem(
                id = "VLPLabc",
                title = "Old is gold",
                author = Artist("Flow", null),
                songCountText = "50 songs",
                thumbnail = "https://lh3.googleusercontent.com/playlist",
                playEndpoint = null,
                shuffleEndpoint = null,
                radioEndpoint = null,
            )

        val entry = playlist.toMusicEntry()

        assertEquals("VLPLabc", entry.playlistId)
        assertNull(entry.videoId)
        assertEquals("Flow", entry.subtitle)
    }

    @Test
    fun aSongOpensItsWatchPage() {
        val song =
            SongItem(
                id = "fJ9rUzIMcZQ",
                title = "Bohemian Rhapsody",
                artists = listOf(Artist("Queen", null)),
                duration = 355,
                thumbnail = "https://lh3.googleusercontent.com/song",
            )

        val entry = song.toMusicEntry()

        assertNull(entry.playlistId)
        assertEquals("fJ9rUzIMcZQ", entry.videoId)
        assertEquals(355, entry.durationSeconds)
    }
}
