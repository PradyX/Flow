package io.github.aedev.flow.innertube

import io.github.aedev.flow.innertube.models.YouTubeClient
import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.github.aedev.flow.innertube.models.response.PlayerResponse
import io.github.aedev.flow.innertube.pages.player.playableStream
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Playback resolution over the captured player responses: the token-free VISIONOS rung is asked
 * first on the main site, a refusal falls to the app clients, and a muxed progressive format wins
 * over an adaptive pair when one is served.
 */
class PlayableStreamTest {
    private var previous: InnerTube? = null

    @AfterTest
    fun restoreClient() {
        previous?.let { YouTube.innerTube = it }
    }

    /** Reads the checked-in answer body; a missing file fails rather than skipping the parse. */
    private fun fixture(name: String): String =
        assertNotNull(javaClass.getResourceAsStream("/fixtures/$name"), "missing fixture $name")
            .bufferedReader()
            .use { it.readText() }

    private fun response(name: String): PlayerResponse = Json { ignoreUnknownKeys = true }.decodeFromString<PlayerResponse>(fixture(name))

    private val captured by lazy { fixture("player_android_vr.json") }
    private val response by lazy { Json { ignoreUnknownKeys = true }.decodeFromString<PlayerResponse>(captured) }

    @Test
    fun theCapturedProgressiveFormatIsThePlayableStream() {
        val stream = assertNotNull(response.playableStream(), "the captured response has no progressive format")

        assertEquals("360p", stream.qualityLabel)
        assertEquals(360, stream.height)
        assertEquals(640, stream.width)
        assertTrue(stream.videoMimeType.startsWith("video/mp4"), stream.videoMimeType)
        assertTrue(
            stream.videoUrl.startsWith("https://") && stream.videoUrl.contains("googlevideo.com/videoplayback"),
            stream.videoUrl,
        )
        // The response also carries 2160p video-only and audio-only adaptive formats; the surface
        // must never be handed one of those, because nothing here merges separate tracks.
        val adaptiveUrl =
            response.streamingData
                ?.adaptiveFormats
                ?.first()
                ?.url
        assertNotNull(adaptiveUrl)
        assertNotEquals(adaptiveUrl, stream.videoUrl)
        assertTrue(stream.isMuxed, "the captured progressive format is one file")
        assertEquals(null, stream.audioUrl)
    }

    /** Resolution asks the token-free VISIONOS client first, and always on the main site. */
    @Test
    fun resolutionAsksVisionosFirstOnTheMainSite() =
        runTest {
            val engine = install { captured }
            val client = InnerTube(engine)
            client.locale = YouTubeLocale(gl = "US", hl = "en")
            if (previous == null) previous = YouTube.innerTube
            YouTube.innerTube = client

            val stream = assertNotNull(YouTube.playableStream("dQw4w9WgXcQ").getOrThrow())

            assertEquals(response.playableStream()?.videoUrl, stream.videoUrl)
            val request = engine.requestHistory.single()
            assertEquals("www.youtube.com", request.url.host, "playback resolves on the main site")
            assertTrue(
                request.body
                    .toByteArray()
                    .decodeToString()
                    .contains("\"VISIONOS\""),
                "the first playback client is the token-free VISIONOS build",
            )
            // The player must present the agent of the client that minted the URL, or GVS answers 403.
            assertEquals(YouTubeClient.VISIONOS.userAgent, stream.userAgent)
        }

    /** A client that refuses is skipped: the next one in the chain supplies the stream. */
    @Test
    fun resolutionFallsBackWhenAClientRefuses() =
        runTest {
            var calls = 0
            val engine =
                install {
                    calls++
                    if (calls == 1) "{}" else captured
                }
            val client = InnerTube(engine)
            client.locale = YouTubeLocale(gl = "US", hl = "en")
            if (previous == null) previous = YouTube.innerTube
            YouTube.innerTube = client

            val stream = assertNotNull(YouTube.playableStream("dQw4w9WgXcQ").getOrThrow())

            assertEquals(response.playableStream()?.videoUrl, stream.videoUrl)
            assertEquals(2, engine.requestHistory.size)
            assertTrue(
                engine.requestHistory[1]
                    .body
                    .toByteArray()
                    .decodeToString()
                    .contains("ANDROID_VR"),
                "the second attempt is a VR client",
            )
        }

    /**
     * The captured refusal of VISIONOS for 0J2QdDbelmY, then the captured main-site answer of
     * ANDROID for the same id: the app-client rung still resolves a muxed file on the main site.
     */
    @Test
    fun theAndroidRungServesTheMainSiteStreamVisionosRefused() =
        runTest {
            val refused = fixture("player_visionos_refused_0J2.json")
            val android = fixture("player_android_main_0J2.json")
            val engine =
                install { body ->
                    when {
                        body.contains("VISIONOS") || body.contains("ANDROID_VR") -> refused
                        body.contains("\"ANDROID\"") -> android
                        else -> "{}"
                    }
                }
            val client = InnerTube(engine)
            client.locale = YouTubeLocale(gl = "US", hl = "en")
            if (previous == null) previous = YouTube.innerTube
            YouTube.innerTube = client

            val stream = assertNotNull(YouTube.playableStream("0J2QdDbelmY").getOrThrow())

            assertEquals("360p", stream.qualityLabel)
            assertTrue(stream.isMuxed)
            assertEquals(YouTubeClient.ANDROID.userAgent, stream.userAgent)
            assertTrue(
                engine.requestHistory.all { it.url.host == "www.youtube.com" },
                "every rung asks the main site",
            )
            assertTrue(
                engine.requestHistory.any {
                    it.body
                        .toByteArray()
                        .decodeToString()
                        .contains("\"ANDROID\"")
                },
                "the Android client supplied the stream",
            )
        }

    /** VISIONOS serves separate tracks; the resolver pairs the tallest video with its audio. */
    @Test
    fun theCapturedVisionosResponseResolvesAnAdaptivePair() {
        val stream = assertNotNull(response("player_visionos_direct.json").playableStream())

        assertEquals("1080p", stream.qualityLabel)
        assertEquals(1080, stream.height)
        assertTrue(!stream.isMuxed, "VISIONOS serves separate tracks")
        assertNotNull(stream.audioUrl)
        assertTrue(stream.audioMimeType.orEmpty().startsWith("audio/"), stream.audioMimeType.orEmpty())
        // The taller 2160p track is not in the trimmed fixture; the cap keeps the request at 1080p.
        assertTrue(stream.videoUrl.contains("googlevideo.com/videoplayback"), stream.videoUrl)
    }

    /** Answers every player request with the body [respondWith] chooses for it. */
    private fun install(respondWith: (body: String) -> String): MockEngine {
        val engine =
            MockEngine { request ->
                val body = request.body.toByteArray().decodeToString()
                respond(
                    content = respondWith(body),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        return engine
    }
}
