package io.github.aedev.flow.innertube

import io.github.aedev.flow.innertube.models.YouTubeClient.Companion.WEB
import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.github.aedev.flow.innertube.pages.renderer.distinctKey
import io.github.aedev.flow.innertube.pages.search.SearchSection
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The main-site (video) endpoints: search, Shorts search, browse and the watch-next document.
 *
 * They use the static WEB client, so `clientVersion` is pinned rather than read from base.js.
 * Every case asserts the wire and a non-empty parse of a captured-shape answer.
 */
class YouTubeVideoFacadeTest {
    private lateinit var previous: InnerTube
    private var captured: HttpRequestData? = null

    @AfterTest
    fun restoreClient() {
        if (::previous.isInitialized) YouTube.innerTube = previous
    }

    private fun install(responseJson: String): InnerTube {
        captured = null
        val engine =
            MockEngine { request ->
                captured = request
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

    private fun request(): HttpRequestData = assertNotNull(captured)

    private suspend fun bodyText(): String = request().body.toByteArray().decodeToString()

    @Test
    fun videoSearchParsesTheFeed() =
        runTest {
            install(SEARCH_RESULTS)

            val page = YouTube.videoSearch(query = "never gonna").getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("www.youtube.com", request().url.host)
            assertEquals("/youtubei/v1/search", request().url.encodedPath)
            assertEquals(WEB.clientId, request().headers["X-YouTube-Client-Name"])
            assertEquals("https://www.youtube.com", request().headers[HttpHeaders.Origin])
            assertEquals("SOCS=CAE=", request().headers[HttpHeaders.Cookie])
            assertEquals(1234L, page.estimatedResults)
            assertTrue(page.sections.isNotEmpty(), "no sections parsed")
            val result = page.sections.filterIsInstance<SearchSection.Result>().firstOrNull()
            assertEquals("v:dQw4w9WgXcQ", result?.item?.distinctKey())

            assertEquals(WEB_CONTEXT + ",\"query\":\"never gonna\"}", bodyText())
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    @Test
    fun searchShortsParsesReels() =
        runTest {
            install(SHORTS_RESULTS)

            val reels = YouTube.searchShorts("never gonna").getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("/youtubei/v1/search", request().url.encodedPath)
            assertEquals(2, reels.size)
            assertEquals("dQw4w9WgXcQ", reels[0].id)
            assertEquals("Never Gonna Give You Up", reels[0].title)

            assertEquals(
                WEB_CONTEXT + ",\"query\":\"never gonna\",\"params\":\"EgIQCQ%3D%3D\"}",
                bodyText(),
            )
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    @Test
    fun webBrowseParsesTheDocument() =
        runTest {
            install(WEB_BROWSE)

            val document = YouTube.webBrowse(browseId = "UCuAXFkgsw1L7xaCfnd5JJOw").getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("/youtubei/v1/browse", request().url.encodedPath)
            assertEquals(WEB.clientId, request().headers["X-YouTube-Client-Name"])
            assertNotNull(document.jsonObject["contents"], "no contents parsed")

            assertEquals(WEB_CONTEXT + ",\"browseId\":\"UCuAXFkgsw1L7xaCfnd5JJOw\"}", bodyText())
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    @Test
    fun watchNextParsesTheDocument() =
        runTest {
            install(WATCH_NEXT)

            val document = YouTube.watchNext(videoId = "dQw4w9WgXcQ").getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("www.youtube.com", request().url.host)
            assertEquals("/youtubei/v1/next", request().url.encodedPath)
            assertEquals(WEB.clientId, request().headers["X-YouTube-Client-Name"])
            val text = document.toString()
            assertTrue(text.contains("Never Gonna Give You Up"), "title missing: $text")

            assertEquals(
                WEB_CONTEXT + ",\"videoId\":\"dQw4w9WgXcQ\",\"contentCheckOk\":true,\"racyCheckOk\":true}",
                bodyText(),
            )
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    @Test
    fun watchMetadataLiteParsesRelatedVideos() =
        runTest {
            install(WATCH_NEXT)

            val metadata = YouTube.watchMetadataLite("dQw4w9WgXcQ").getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("/youtubei/v1/next", request().url.encodedPath)
            val related = metadata.relatedVideos()
            assertEquals(1, related.size)
            assertEquals("aBcDeFgHiJk", related[0].videoId)
        }

    @Test
    fun reelSequenceSkippingAds() =
        runTest {
            install(REEL_SEQUENCE)

            val page = YouTube.reelSequence("SEQ1").getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("/youtubei/v1/reel/reel_watch_sequence", request().url.encodedPath)
            assertEquals(WEB.clientId, request().headers["X-YouTube-Client-Name"])
            assertEquals("https://www.youtube.com", request().headers["X-Origin"])
            assertEquals(1, page.entries.size, "the ad creative was not filtered")
            assertEquals("dQw4w9WgXcQ", page.entries[0].videoId)
            assertEquals("PP1", page.entries[0].playerParams)
            assertEquals("CONT1", page.continuation)

            assertEquals(WEB_CONTEXT + ",\"sequenceParams\":\"SEQ1\"}", bodyText())
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    @Test
    fun reelOverlayParsesTheHeader() =
        runTest {
            install(REEL_OVERLAY)

            val overlay = YouTube.reelOverlay("dQw4w9WgXcQ").getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("/youtubei/v1/reel/reel_item_watch", request().url.encodedPath)
            assertNotNull(overlay, "no overlay parsed")
            assertEquals("Never Gonna Give You Up", overlay.title)
            assertEquals("UCuAXFkgsw1L7xaCfnd5JJOw", overlay.channelId)
            assertEquals("2 days ago", overlay.relativeTimestamp)

            assertEquals(
                WEB_CONTEXT +
                    ",\"playerRequest\":{\"videoId\":\"dQw4w9WgXcQ\",\"contentCheckOk\":true,\"racyCheckOk\":true}," +
                    "\"disablePlayerResponse\":true}",
                bodyText(),
            )
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    private companion object {
        /** The static WEB client body prefix, with the app's own Json settings. */
        const val WEB_CONTEXT =
            "{\"context\":{\"client\":{\"clientName\":\"WEB\",\"clientVersion\":\"2.20260710.06.00\"," +
                "\"originalUrl\":\"https://www.youtube.com\",\"platform\":\"DESKTOP\",\"utcOffsetMinutes\":0," +
                "\"gl\":\"US\",\"hl\":\"en\"}," +
                "\"request\":{\"internalExperimentFlags\":[],\"useSsl\":true}," +
                "\"user\":{\"lockedSafetyMode\":false}}"

        val REEL_SEQUENCE =
            """
            {
              "entries": [
                {
                  "command": {
                    "reelWatchEndpoint": {
                      "videoId": "dQw4w9WgXcQ",
                      "playerParams": "PP1",
                      "params": "PARAMS1",
                      "thumbnail": { "thumbnails": [ { "url": "https://i/poster" } ] }
                    }
                  }
                },
                {
                  "command": {
                    "reelWatchEndpoint": {
                      "videoId": "adReel",
                      "adClientParams": { "isAd": true }
                    }
                  }
                }
              ],
              "continuationEndpoint": { "continuationCommand": { "token": "CONT1" } }
            }
            """.trimIndent()

        val REEL_OVERLAY =
            """
            {
              "overlay": {
                "reelPlayerOverlayRenderer": {
                  "reelPlayerHeaderSupportedRenderers": {
                    "reelPlayerHeaderRenderer": {
                      "reelTitleText": { "simpleText": "Never Gonna Give You Up" },
                      "channelNavigationEndpoint": {
                        "browseEndpoint": { "browseId": "UCuAXFkgsw1L7xaCfnd5JJOw" }
                      },
                      "timestampText": { "simpleText": "2 days ago" }
                    }
                  }
                }
              }
            }
            """.trimIndent()

        val SEARCH_RESULTS =
            """
            {
              "estimatedResults": "1234",
              "contents": {
                "twoColumnSearchResultsRenderer": {
                  "primaryContents": {
                    "sectionListRenderer": {
                      "contents": [
                        {
                          "itemSectionRenderer": {
                            "contents": [
                              {
                                "videoRenderer": {
                                  "videoId": "dQw4w9WgXcQ",
                                  "title": { "runs": [ { "text": "Never Gonna Give You Up" } ] },
                                  "thumbnail": { "thumbnails": [ { "url": "https://i/c1" } ] },
                                  "lengthText": { "simpleText": "3:33" },
                                  "ownerText": { "runs": [ { "text": "Rick Astley" } ] },
                                  "viewCountText": { "simpleText": "1.6B views" }
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  }
                }
              }
            }
            """.trimIndent()

        val SHORTS_RESULTS =
            """
            {
              "contents": {
                "twoColumnSearchResultsRenderer": {
                  "primaryContents": {
                    "sectionListRenderer": {
                      "contents": [
                        {
                          "itemSectionRenderer": {
                            "contents": [
                              {
                                "reelItemRenderer": {
                                  "videoId": "dQw4w9WgXcQ",
                                  "headline": { "simpleText": "Never Gonna Give You Up" },
                                  "viewCountText": { "simpleText": "12M views" }
                                }
                              },
                              {
                                "reelItemRenderer": {
                                  "videoId": "aBcDeFgHiJk",
                                  "headline": { "simpleText": "Together Forever" },
                                  "viewCountText": { "simpleText": "3.1M views" }
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  }
                }
              }
            }
            """.trimIndent()

        val WEB_BROWSE =
            """
            {
              "contents": {
                "twoColumnBrowseResultsRenderer": {
                  "tabs": [
                    { "tabRenderer": { "title": "Home", "selected": true } }
                  ]
                }
              }
            }
            """.trimIndent()

        val WATCH_NEXT =
            """
            {
              "contents": {
                "twoColumnWatchNextResults": {
                  "results": {
                    "results": {
                      "contents": [
                        {
                          "videoPrimaryInfoRenderer": {
                            "title": { "runs": [ { "text": "Never Gonna Give You Up" } ] }
                          }
                        }
                      ]
                    }
                  },
                  "secondaryResults": {
                    "secondaryResults": {
                      "results": [
                        {
                          "compactVideoRenderer": {
                            "videoId": "aBcDeFgHiJk",
                            "title": { "simpleText": "Together Forever" },
                            "lengthText": { "simpleText": "4:02" },
                            "shortBylineText": { "runs": [ { "text": "Rick Astley" } ] },
                            "thumbnail": { "thumbnails": [ { "url": "https://i/c2" } ] }
                          }
                        }
                      ]
                    }
                  }
                }
              }
            }
            """.trimIndent()
    }
}
