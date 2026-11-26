package io.github.aedev.flow.innertube

import io.github.aedev.flow.innertube.models.YouTubeClient.Companion.WEB_REMIX
import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.github.aedev.flow.innertube.utils.sha1
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The ported request layer, driven end to end through a mock engine: what the client puts on the
 * wire, and what it makes of a captured answer.
 *
 * No cookie is set in the request tests, which is how the app's own signed-out browse behaves, so
 * the login headers are simply absent. Nothing about the production path is bypassed or weakened;
 * `sapisidHashHeaderMatchesTheAppFormula` covers the signed-in branch separately.
 */
class InnertubeRequestTest {
    private lateinit var previous: InnerTube

    @AfterTest
    fun restoreClient() {
        if (::previous.isInitialized) YouTube.innerTube = previous
    }

    private fun install(
        responseJson: String,
        capture: (HttpRequestData) -> Unit,
    ): InnerTube {
        val engine =
            MockEngine { request ->
                capture(request)
                respond(
                    content = responseJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        val client = InnerTube(engine)
        client.locale = YouTubeLocale(gl = "US", hl = "en")
        previous = YouTube.innerTube
        YouTube.innerTube = client
        return client
    }

    @Test
    fun sha1MatchesTheFipsVectors() {
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", sha1(""))
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", sha1("abc"))
        assertEquals(
            "84983e441c3bd26ebaae4aa1f95129e5e54670f1",
            sha1("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"),
        )
        assertEquals(
            "2fd4e1c67a2d28fced849ee1bb76e7391b93eb12",
            sha1("The quick brown fox jumps over the lazy dog"),
        )
    }

    @Test
    fun playlistContinuationBuildsTheLegacyRequest() =
        runTest {
            var captured: HttpRequestData? = null
            install(PLAYLIST_CONTINUATION_RESPONSE) { captured = it }

            YouTube.playlistContinuation("TOKEN_PAGE_2").getOrThrow()

            val request = assertNotNull(captured)
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/youtubei/v1/browse", request.url.encodedPath)
            assertEquals("music.youtube.com", request.url.host)
            assertEquals("false", request.url.parameters["prettyPrint"])

            assertEquals("67", request.headers["X-YouTube-Client-Name"])
            assertEquals("1.20260213.01.00", request.headers["X-YouTube-Client-Version"])
            assertEquals("https://music.youtube.com", request.headers["X-Origin"])
            assertEquals("https://music.youtube.com/", request.headers["Referer"])
            assertEquals("application/json", request.body.contentType?.toString())
            assertEquals(WEB_REMIX.userAgent, request.headers[HttpHeaders.UserAgent])
            assertEquals("application/json", request.headers[HttpHeaders.Accept])
            assertEquals("en-US,en;q=0.9", request.headers[HttpHeaders.AcceptLanguage])
            assertEquals("no-cache", request.headers[HttpHeaders.CacheControl])

            // Signed out: the login headers exist only when a cookie does.
            assertEquals(null, request.headers[HttpHeaders.Cookie])
            assertEquals(null, request.headers[HttpHeaders.Authorization])
            assertEquals(null, request.headers["X-Goog-Visitor-Id"])

            val body = request.body.toByteArray().decodeToString()
            assertEquals(expectedBrowseBody(), Json.parseToJsonElement(body))
            assertFalse(body.contains(":null"), "explicitNulls is off: $body")
        }

    @Test
    fun playlistContinuationParsesTheCapturedResponse() =
        runTest {
            install(PLAYLIST_CONTINUATION_RESPONSE) {}

            val page = YouTube.playlistContinuation("TOKEN_PAGE_2").getOrThrow()

            assertEquals(2, page.songs.size)
            val first = page.songs[0]
            assertEquals("dQw4w9WgXcQ", first.id)
            assertEquals("Never Gonna Give You Up", first.title)
            assertEquals(listOf("Rick Astley", "Whenever You Need Somebody"), first.artists.map { it.name })
            assertEquals("UCuAXFkgsw1L7xaCfnd5JJOw", first.artists[0].id)
            assertEquals("Whenever You Need Somebody", first.album?.name)
            assertEquals("MPREb_9a9F4rHiCqN", first.album?.id)
            assertEquals(3 * 60 + 33, first.duration)
            assertEquals("https://lh3.googleusercontent.com/cover1", first.thumbnail)
            assertFalse(first.explicit)

            assertEquals("aBcDeFgHiJk", page.songs[1].id)
            assertEquals(4 * 60 + 2, page.songs[1].duration)

            // The section-list continuation's token, which is what `completed()` walks next.
            assertEquals("TOKEN_PAGE_3", page.continuation)
        }

    @Test
    fun sapisidHashHeaderMatchesTheAppFormula() =
        runTest {
            var captured: HttpRequestData? = null
            val client = install(PLAYLIST_CONTINUATION_RESPONSE) { captured = it }
            client.cookie = "SID=abc; SAPISID=SAPISID_VALUE; HSID=def"

            YouTube.playlistContinuation("TOKEN_PAGE_2").getOrThrow()

            val request = assertNotNull(captured)
            assertEquals("SID=abc; SAPISID=SAPISID_VALUE; HSID=def", request.headers[HttpHeaders.Cookie])
            val authorization = assertNotNull(request.headers[HttpHeaders.Authorization])
            assertTrue(
                authorization.startsWith("SAPISIDHASH "),
                "unexpected authorization: $authorization",
            )
            val payload = authorization.removePrefix("SAPISIDHASH ")
            val timestamp = payload.substringBefore('_')
            val hash = payload.substringAfter('_')
            assertTrue(timestamp.all { it.isDigit() }, "timestamp not seconds: $timestamp")
            assertEquals(
                sha1("$timestamp SAPISID_VALUE https://music.youtube.com"),
                hash,
            )
        }

    /** What the app's `BrowseBody` serializes to for this call, with its own Json settings. */
    private fun expectedBrowseBody() =
        Json.parseToJsonElement(
            """
            {
              "context": {
                "client": {
                  "clientName": "WEB_REMIX",
                  "clientVersion": "1.20260213.01.00",
                  "gl": "US",
                  "hl": "en"
                },
                "request": {
                  "internalExperimentFlags": [],
                  "useSsl": true
                },
                "user": {
                  "lockedSafetyMode": false
                }
              },
              "browseId": "",
              "continuation": "TOKEN_PAGE_2"
            }
            """.trimIndent(),
        )

    private companion object {
        /** A captured `browse` answer for a playlist continuation, trimmed to what is parsed. */
        val PLAYLIST_CONTINUATION_RESPONSE =
            """
            {
              "responseContext": {},
              "continuationContents": {
                "sectionListContinuation": {
                  "contents": [
                    {
                      "musicPlaylistShelfRenderer": {
                        "playlistId": null,
                        "collapsedItemCount": 0,
                        "contents": [
                          {
                            "musicResponsiveListItemRenderer": {
                              "playlistItemData": { "videoId": "dQw4w9WgXcQ", "playlistSetVideoId": "SET_VIDEO_1" },
                              "flexColumns": [
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [ { "text": "Never Gonna Give You Up" } ] }
                                  }
                                },
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": {
                                      "runs": [
                                        {
                                          "text": "Rick Astley",
                                          "navigationEndpoint": {
                                            "browseEndpoint": { "browseId": "UCuAXFkgsw1L7xaCfnd5JJOw" }
                                          }
                                        },
                                        { "text": " • " },
                                        {
                                          "text": "Whenever You Need Somebody",
                                          "navigationEndpoint": {
                                            "browseEndpoint": { "browseId": "MPREb_9a9F4rHiCqN" }
                                          }
                                        }
                                      ]
                                    }
                                  }
                                },
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": {
                                      "runs": [
                                        {
                                          "text": "Whenever You Need Somebody",
                                          "navigationEndpoint": {
                                            "browseEndpoint": { "browseId": "MPREb_9a9F4rHiCqN" }
                                          }
                                        }
                                      ]
                                    }
                                  }
                                }
                              ],
                              "fixedColumns": [
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [ { "text": "3:33" } ] }
                                  }
                                }
                              ],
                              "thumbnail": {
                                "musicThumbnailRenderer": {
                                  "thumbnail": {
                                    "thumbnails": [
                                      {
                                        "url": "https://lh3.googleusercontent.com/cover1",
                                        "width": 60,
                                        "height": 60
                                      }
                                    ]
                                  }
                                }
                              },
                              "musicVideoType": "MUSIC_VIDEO_TYPE_ATV"
                            }
                          },
                          {
                            "musicResponsiveListItemRenderer": {
                              "playlistItemData": { "videoId": "aBcDeFgHiJk", "playlistSetVideoId": "SET_VIDEO_2" },
                              "flexColumns": [
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [ { "text": "Together Forever" } ] }
                                  }
                                },
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": {
                                      "runs": [
                                        {
                                          "text": "Rick Astley",
                                          "navigationEndpoint": {
                                            "browseEndpoint": { "browseId": "UCuAXFkgsw1L7xaCfnd5JJOw" }
                                          }
                                        },
                                        { "text": " • " },
                                        {
                                          "text": "Whenever You Need Somebody",
                                          "navigationEndpoint": {
                                            "browseEndpoint": { "browseId": "MPREb_9a9F4rHiCqN" }
                                          }
                                        }
                                      ]
                                    }
                                  }
                                }
                              ],
                              "fixedColumns": [
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [ { "text": "4:02" } ] }
                                  }
                                }
                              ],
                              "thumbnail": {
                                "musicThumbnailRenderer": {
                                  "thumbnail": {
                                    "thumbnails": [
                                      {
                                        "url": "https://lh3.googleusercontent.com/cover2",
                                        "width": 60,
                                        "height": 60
                                      }
                                    ]
                                  }
                                }
                              },
                              "musicVideoType": "MUSIC_VIDEO_TYPE_ATV"
                            }
                          }
                        ]
                      }
                    }
                  ],
                  "continuations": [
                    { "nextContinuationData": { "continuation": "TOKEN_PAGE_3" } }
                  ]
                }
              }
            }
            """.trimIndent()
    }
}
