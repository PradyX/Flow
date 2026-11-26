package io.github.aedev.flow.innertube

import io.github.aedev.flow.innertube.models.WatchEndpoint
import io.github.aedev.flow.innertube.models.YouTubeClient.Companion.WEB_REMIX
import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The facade's request surface, end to end through a mock engine.
 *
 * Each case sends a captured-style answer through the real client and asserts both halves: what
 * went on the wire, and what came back as domain objects. The signed-out cases leave
 * [YouTube.cookie] null, which is the app's own signed-out path; nothing is bypassed.
 */
class YouTubeFacadeTest {
    private lateinit var previous: InnerTube
    private var captured: HttpRequestData? = null

    @AfterTest
    fun restoreClient() {
        if (::previous.isInitialized) YouTube.innerTube = previous
    }

    private fun install(
        responseJson: String,
        cookie: String? = null,
    ): InnerTube {
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
        client.cookie = cookie
        previous = YouTube.innerTube
        YouTube.innerTube = client
        return client
    }

    private fun request(): HttpRequestData = assertNotNull(captured)

    private suspend fun bodyText(): String = request().body.toByteArray().decodeToString()

    private fun assertBrowseWire() {
        assertEquals(HttpMethod.Post, request().method)
        assertEquals("music.youtube.com", request().url.host)
        assertEquals("/youtubei/v1/browse", request().url.encodedPath)
        assertEquals(WEB_REMIX.clientId, request().headers["X-YouTube-Client-Name"])
        assertEquals(WEB_REMIX.clientVersion, request().headers["X-YouTube-Client-Version"])
        assertEquals("https://music.youtube.com", request().headers["X-Origin"])
        assertEquals("https://music.youtube.com/", request().headers["Referer"])
        assertEquals("application/json", request().body.contentType?.toString())
        assertEquals(WEB_REMIX.userAgent, request().headers[HttpHeaders.UserAgent])
    }

    @Test
    fun homeParsesShelvesAndContinuation() =
        runTest {
            install(HOME_RESPONSE)

            val page = YouTube.home().getOrThrow()

            assertBrowseWire()
            assertEquals(1, page.sections.size)
            assertEquals("Quick picks", page.sections[0].title)
            assertEquals(1, page.sections[0].items.size)
            assertEquals("HOME_TOKEN_2", page.continuation)
            assertNull(request().headers[HttpHeaders.Cookie])

            assertEquals(CONTEXT_PREFIX + ",\"browseId\":\"FEmusic_home\"}", bodyText())
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    @Test
    fun searchParsesSongsAndContinuation() =
        runTest {
            install(SEARCH_RESPONSE)

            val result = YouTube.search("rick astley", YouTube.SearchFilter.FILTER_SONG).getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("/youtubei/v1/search", request().url.encodedPath)
            assertEquals("67", request().headers["X-YouTube-Client-Name"])
            assertEquals(1, result.items.size)
            assertEquals("SEARCH_TOKEN_2", result.continuation)
            assertNull(request().headers[HttpHeaders.Cookie])

            assertEquals(
                CONTEXT_PREFIX + ",\"query\":\"rick astley\",\"params\":\"EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D\"}",
                bodyText(),
            )
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    @Test
    fun nextParsesTheWatchQueueAndTabs() =
        runTest {
            install(NEXT_RESPONSE)

            val result = YouTube.next(WatchEndpoint(videoId = "dQw4w9WgXcQ")).getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("/youtubei/v1/next", request().url.encodedPath)
            assertEquals("Whenever You Need Somebody", result.title)
            assertEquals(2, result.items.size)
            assertEquals(3 * 60 + 33, result.items[0].duration)
            assertEquals(0, result.currentIndex)
            assertEquals("MPLYtLYRICS", result.lyricsEndpoint?.browseId)
            assertEquals("MPTRtRELATED", result.relatedEndpoint?.browseId)
            assertEquals("NEXT_TOKEN", result.continuation)
            assertNull(request().headers[HttpHeaders.Cookie])

            assertEquals(CONTEXT_PREFIX + ",\"videoId\":\"dQw4w9WgXcQ\",\"contentCheckOk\":true,\"racyCheckOk\":true}", bodyText())
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    @Test
    fun playerParsesVideoDetails() =
        runTest {
            install(PLAYER_RESPONSE)

            val response = YouTube.player("dQw4w9WgXcQ", client = WEB_REMIX).getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("/youtubei/v1/player", request().url.encodedPath)
            assertEquals("OK", response.playabilityStatus.status)
            assertEquals("dQw4w9WgXcQ", response.videoDetails?.videoId)
            assertEquals("Never Gonna Give You Up", response.videoDetails?.title)

            assertEquals(CONTEXT_PREFIX + ",\"videoId\":\"dQw4w9WgXcQ\",\"contentCheckOk\":true,\"racyCheckOk\":true}", bodyText())
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    @Test
    fun likeVideoSendsTheSignedInMutation() =
        runTest {
            install("""{"responseContext":{}}""", cookie = "SID=abc; SAPISID=SAPISID_VALUE")

            YouTube.likeVideo("dQw4w9WgXcQ", like = true).getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("/youtubei/v1/like/like", request().url.encodedPath)
            assertEquals("SID=abc; SAPISID=SAPISID_VALUE", request().headers[HttpHeaders.Cookie])
            assertNotNull(request().headers[HttpHeaders.Authorization])

            assertEquals(
                CONTEXT_PREFIX +
                    ",\"target\":{\"type\":\"io.github.aedev.flow.innertube.models.body.LikeBody.Target.VideoTarget\",\"videoId\":\"dQw4w9WgXcQ\"}}",
                bodyText(),
            )
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    private companion object {
        /** The `context` every WEB_REMIX body starts with, with the app's own Json settings. */
        const val CONTEXT_PREFIX =
            "{\"context\":{\"client\":{\"clientName\":\"WEB_REMIX\",\"clientVersion\":\"1.20260213.01.00\",\"gl\":\"US\",\"hl\":\"en\"}," +
                "\"request\":{\"internalExperimentFlags\":[],\"useSsl\":true}," +
                "\"user\":{\"lockedSafetyMode\":false}}"

        val HOME_RESPONSE =
            """
            {
              "responseContext": {},
              "contents": {
                "singleColumnBrowseResultsRenderer": {
                  "tabs": [
                    {
                      "tabRenderer": {
                        "content": {
                          "sectionListRenderer": {
                            "contents": [
                              {
                                "musicCarouselShelfRenderer": {
                                  "itemSize": "MEDIUM",
                                  "numItemsPerColumn": 2,
                                  "header": {
                                    "musicCarouselShelfBasicHeaderRenderer": {
                                      "title": { "runs": [ { "text": "Quick picks" } ] }
                                    }
                                  },
                                  "contents": [
                                    {
                                      "musicTwoRowItemRenderer": {
                                        "title": { "runs": [ { "text": "Never Gonna Give You Up" } ] },
                                        "subtitle": {
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
                                        },
                                        "thumbnailRenderer": {
                                          "musicThumbnailRenderer": {
                                            "thumbnail": {
                                              "thumbnails": [
                                                { "url": "https://lh3.googleusercontent.com/cover1" }
                                              ]
                                            }
                                          }
                                        },
                                        "navigationEndpoint": {
                                          "watchEndpoint": { "videoId": "dQw4w9WgXcQ" }
                                        }
                                      }
                                    }
                                  ]
                                }
                              }
                            ],
                            "continuations": [
                              { "nextContinuationData": { "continuation": "HOME_TOKEN_2" } }
                            ]
                          }
                        }
                      }
                    }
                  ]
                }
              }
            }
            """.trimIndent()

        val SEARCH_RESPONSE =
            """
            {
              "responseContext": {},
              "contents": {
                "tabbedSearchResultsRenderer": {
                  "tabs": [
                    {
                      "tabRenderer": {
                        "content": {
                          "sectionListRenderer": {
                            "contents": [
                              {
                                "musicShelfRenderer": {
                                  "contents": [
                                    {
                                      "musicResponsiveListItemRenderer": {
                                        "playlistItemData": { "videoId": "dQw4w9WgXcQ" },
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
                                                  },
                                                  { "text": " • " },
                                                  { "text": "3:33" }
                                                ]
                                              }
                                            }
                                          }
                                        ],
                                        "thumbnail": {
                                          "musicThumbnailRenderer": {
                                            "thumbnail": {
                                              "thumbnails": [
                                                { "url": "https://lh3.googleusercontent.com/cover1" }
                                              ]
                                            }
                                          }
                                        }
                                      }
                                    }
                                  ],
                                  "continuations": [
                                    { "nextContinuationData": { "continuation": "SEARCH_TOKEN_2" } }
                                  ]
                                }
                              }
                            ]
                          }
                        }
                      }
                    }
                  ]
                }
              }
            }
            """.trimIndent()

        val NEXT_RESPONSE =
            """
            {
              "responseContext": {},
              "contents": {
                "singleColumnMusicWatchNextResultsRenderer": {
                  "tabbedRenderer": {
                    "watchNextTabbedResultsRenderer": {
                      "tabs": [
                        {
                          "tabRenderer": {
                            "content": {
                              "musicQueueRenderer": {
                                "header": {
                                  "musicQueueHeaderRenderer": {
                                    "subtitle": { "runs": [ { "text": "Whenever You Need Somebody" } ] }
                                  }
                                },
                                "content": {
                                  "playlistPanelRenderer": {
                                    "contents": [
                                      {
                                        "playlistPanelVideoRenderer": {
                                          "videoId": "dQw4w9WgXcQ",
                                          "title": { "runs": [ { "text": "Never Gonna Give You Up" } ] },
                                          "lengthText": { "runs": [ { "text": "3:33" } ] },
                                          "longBylineText": {
                                            "runs": [
                                              {
                                                "text": "Rick Astley",
                                                "navigationEndpoint": {
                                                  "browseEndpoint": { "browseId": "UCuAXFkgsw1L7xaCfnd5JJOw" }
                                                }
                                              },
                                              { "text": " • " },
                                              { "text": "Whenever You Need Somebody" }
                                            ]
                                          },
                                          "selected": true,
                                          "thumbnail": { "thumbnails": [ { "url": "https://i/c1" } ] },
                                          "navigationEndpoint": {
                                            "watchEndpoint": { "videoId": "dQw4w9WgXcQ" }
                                          }
                                        }
                                      },
                                      {
                                        "playlistPanelVideoRenderer": {
                                          "videoId": "aBcDeFgHiJk",
                                          "title": { "runs": [ { "text": "Together Forever" } ] },
                                          "lengthText": { "runs": [ { "text": "4:02" } ] },
                                          "longBylineText": {
                                            "runs": [
                                              {
                                                "text": "Rick Astley",
                                                "navigationEndpoint": {
                                                  "browseEndpoint": { "browseId": "UCuAXFkgsw1L7xaCfnd5JJOw" }
                                                }
                                              },
                                              { "text": " • " },
                                              { "text": "Whenever You Need Somebody" }
                                            ]
                                          },
                                          "selected": false,
                                          "thumbnail": { "thumbnails": [ { "url": "https://i/c2" } ] },
                                          "navigationEndpoint": {
                                            "watchEndpoint": { "videoId": "aBcDeFgHiJk" }
                                          }
                                        }
                                      }
                                    ],
                                    "continuations": [
                                      { "nextContinuationData": { "continuation": "NEXT_TOKEN" } }
                                    ]
                                  }
                                }
                              }
                            }
                          }
                        },
                        {
                          "tabRenderer": {
                            "endpoint": { "browseEndpoint": { "browseId": "MPLYtLYRICS" } }
                          }
                        },
                        {
                          "tabRenderer": {
                            "endpoint": { "browseEndpoint": { "browseId": "MPTRtRELATED" } }
                          }
                        }
                      ]
                    }
                  }
                }
              }
            }
            """.trimIndent()

        val PLAYER_RESPONSE =
            """
            {
              "responseContext": {},
              "playabilityStatus": { "status": "OK" },
              "videoDetails": {
                "videoId": "dQw4w9WgXcQ",
                "title": "Never Gonna Give You Up",
                "author": "Rick Astley",
                "channelId": "UCuAXFkgsw1L7xaCfnd5JJOw"
              }
            }
            """.trimIndent()
    }
}
