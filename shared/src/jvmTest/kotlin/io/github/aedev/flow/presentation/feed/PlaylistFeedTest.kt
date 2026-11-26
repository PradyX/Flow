package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.innertube.InnerTube
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The playlist page against the captured WEB_REMIX `VL<playlistId>` response: the header YouTube
 * returned becomes the screen's header, and every song becomes an entry that opens its own video.
 */
class PlaylistFeedTest {
    private var previous: InnerTube? = null

    @AfterTest
    fun restoreClient() {
        previous?.let { YouTube.innerTube = it }
    }

    private fun install(status: HttpStatusCode = HttpStatusCode.OK): MockEngine {
        val body =
            assertNotNull(javaClass.getResourceAsStream("/fixtures/playlist_page.json"), "missing fixture")
                .bufferedReader()
                .use { it.readText() }
        val engine =
            MockEngine {
                respond(
                    content = body,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        val client = InnerTube(engine)
        client.locale = YouTubeLocale(gl = "US", hl = "en")
        previous = YouTube.innerTube
        YouTube.innerTube = client
        return engine
    }

    @Test
    fun theCapturedPageBecomesThePlaylistHeaderAndItsSongs() =
        runTest {
            install()

            val playlist = assertNotNull(loadPlaylist("RDCLAK5uy_kiDNaS5nAXxdzsqFElFKKKs0GUEFJE26w").getOrThrow())

            assertEquals("90s Bollywood Dance", playlist.title)
            assertEquals("Playlist", playlist.releaseType, "a playlist is labelled as one")
            assertTrue(playlist.details.orEmpty().contains("59 songs"), playlist.details.orEmpty())
            assertTrue(!playlist.explicit, "the fixture playlist is not explicit")
            assertTrue(!playlist.thumbnailUrl.isNullOrBlank(), "the header keeps its artwork")
            assertEquals(3, playlist.entries.size)

            val first = playlist.entries.first()
            assertEquals("Dg7Z0YPlHY4", first.videoId)
            assertEquals("Saat Samundar Paar", first.title)
            assertTrue(!first.thumbnailUrl.isNullOrBlank(), "the list row keeps its artwork")
            assertTrue((first.durationSeconds ?: 0) > 0, "the list row keeps its length")
            assertEquals(
                listOf("Dg7Z0YPlHY4", "rY_0SOlmrdY", "WlWlGlvN4L4"),
                playlist.entries.map { it.videoId },
            )
        }

    @Test
    fun theRequestIsTheSignedInMusicBrowseOfThePlaylistId() =
        runTest {
            val engine = install()

            loadPlaylist("RDCLAK5uy_kiDNaS5nAXxdzsqFElFKKKs0GUEFJE26w").getOrThrow()

            val request = engine.requestHistory.single()
            assertTrue(request.url.toString().contains("/youtubei/v1/browse"), request.url.toString())
            val body = request.body.toByteArray().decodeToString()
            assertTrue(body.contains("\"browseId\":\"VLRDCLAK5uy_kiDNaS5nAXxdzsqFElFKKKs0GUEFJE26w\""), body.take(200))
            assertTrue(body.contains("\"WEB_REMIX\""), "the music client reads the playlist")
        }

    @Test
    fun aRefusedPlaylistIsAFailureRatherThanAnEmptyPage() =
        runTest {
            install(HttpStatusCode.Forbidden)

            val result = loadPlaylist("RDCLAK5uy_kiDNaS5nAXxdzsqFElFKKKs0GUEFJE26w")

            assertTrue(result.isFailure, "the screen shows a message instead of an empty list")
        }
}
