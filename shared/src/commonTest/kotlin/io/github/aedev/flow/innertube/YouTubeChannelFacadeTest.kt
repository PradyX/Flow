package io.github.aedev.flow.innertube

import io.github.aedev.flow.innertube.models.YouTubeLocale
import io.github.aedev.flow.utils.platformUtcOffsetMinutes
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
import kotlin.test.assertTrue

/**
 * The second tranche of facade endpoints: mixes, channels, transcript, account menu, playlist
 * thumbnail and URL resolution.
 *
 * Every case drives the real client through a mock engine and asserts the wire and the parse. The
 * auth-gated ones run signed out (no cookie), matching the app's own signed-out path.
 */
class YouTubeChannelFacadeTest {
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

    private fun assertOrigin(origin: String) {
        assertEquals(origin, request().headers["X-Origin"])
        assertNotNull(request().headers["X-YouTube-Client-Name"])
        assertNotNull(request().headers["X-YouTube-Client-Version"])
    }

    @Test
    fun nextMixParsesTheWatchQueue() =
        runTest {
            install(MIX_RESPONSE)

            val page = YouTube.nextMix("RDdQw4w9WgXcQ").getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("www.youtube.com", request().url.host)
            assertEquals("/youtubei/v1/next", request().url.encodedPath)
            assertEquals("Mix - Rick Astley", page.title)
            assertEquals(1, page.videos.size)
            assertEquals("dQw4w9WgXcQ", page.videos[0].id)
            assertNull(request().headers[HttpHeaders.Cookie])

            assertEquals(
                RESOLVE_CONTEXT +
                    ",\"videoId\":\"dQw4w9WgXcQ\",\"playlistId\":\"RDdQw4w9WgXcQ\"," +
                    "\"contentCheckOk\":true,\"racyCheckOk\":true}",
                bodyText(),
            )
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    @Test
    fun accountMenuParsesTheActiveAccount() =
        runTest {
            install(ACCOUNT_RESPONSE, cookie = "SID=abc")

            val account = YouTube.accountMenu().getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("/youtubei/v1/account/account_menu", request().url.encodedPath)
            assertEquals("Prady", account.name)
            assertEquals("prady@example.com", account.email)
            assertEquals("@prady", account.channelHandle)
            assertEquals("https://i/avatar", account.thumbnailUrl)

            assertEquals(
                REMIX_CONTEXT + ",\"deviceTheme\":\"DEVICE_THEME_SELECTED\",\"userInterfaceTheme\":\"USER_INTERFACE_THEME_DARK\"}",
                bodyText(),
            )
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    @Test
    fun channelParsesTheHeaderAndTabs() =
        runTest {
            install(CHANNEL_RESPONSE)

            val page = YouTube.channel("UCuAXFkgsw1L7xaCfnd5JJOw").getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("/youtubei/v1/browse", request().url.encodedPath)
            assertEquals("UCuAXFkgsw1L7xaCfnd5JJOw", page.header.id)
            assertEquals("Rick Astley", page.header.title)
            assertEquals("@rickastley", page.header.handle)
            assertEquals(2, page.tabs.size)
            assertEquals("Videos", page.tabs[1].title)
            assertNotNull(page.initialTab)
            assertNull(request().headers[HttpHeaders.Cookie])

            assertEquals(WEB_CONTEXT + ",\"browseId\":\"UCuAXFkgsw1L7xaCfnd5JJOw\"}", bodyText())
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    @Test
    fun channelSearchParsesVideos() =
        runTest {
            install(CHANNEL_SEARCH_RESPONSE)

            val result = YouTube.channelSearch("UCuAXFkgsw1L7xaCfnd5JJOw", "Rick Astley", "https://i/ava", "never").getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("/youtubei/v1/browse", request().url.encodedPath)
            assertEquals(1, result.videos.size)
            assertEquals("dQw4w9WgXcQ", result.videos[0].id)
            assertNull(request().headers[HttpHeaders.Cookie])

            assertEquals(
                WEB_CONTEXT +
                    ",\"browseId\":\"UCuAXFkgsw1L7xaCfnd5JJOw\",\"params\":\"EgZzZWFyY2jyBgQKAloA\",\"query\":\"never\"}",
                bodyText(),
            )
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    @Test
    fun transcriptFormatsCueTimes() =
        runTest {
            install(TRANSCRIPT_RESPONSE)

            val transcript = YouTube.transcript("dQw4w9WgXcQ").getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("/youtubei/v1/get_transcript", request().url.encodedPath)
            assertEquals("[00:01.500]never gonna give you up\n[01:02.250]never gonna let you down", transcript)

            assertEquals(REMIX_CONTEXT + ",\"params\":\"CgtkUXc0dzlXZ1hjUQ==\"}", bodyText())
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    @Test
    fun setThumbnailParsesTheNewHeader() =
        runTest {
            install(SET_THUMBNAIL_RESPONSE)

            val url = YouTube.setThumbnailPlaylist("PL123", "BLOB_1").getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("/youtubei/v1/browse/edit_playlist", request().url.encodedPath)
            assertEquals("https://i/new-thumb", url)

            assertEquals(
                REMIX_CONTEXT +
                    ",\"playlistId\":\"PL123\",\"actions\":[{" +
                    "\"type\":\"io.github.aedev.flow.innertube.models.body.Action.SetCustomThumbnailAction\"," +
                    "\"action\":\"ACTION_SET_CUSTOM_THUMBNAIL\"," +
                    "\"addedCustomThumbnail\":{\"imageKey\":{\"name\":\"studio_square_thumbnail\"," +
                    "\"type\":\"PLAYLIST_IMAGE_TYPE_CUSTOM_THUMBNAIL\"}," +
                    "\"playlistScottyEncryptedBlobId\":\"BLOB_1\"}}]}",
                bodyText(),
            )
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    @Test
    fun resolveChannelIdParsesTheBrowseEndpoint() =
        runTest {
            install(RESOLVE_RESPONSE)

            val id = YouTube.resolveChannelId("https://www.youtube.com/@rickastley").getOrThrow()

            assertEquals(HttpMethod.Post, request().method)
            assertEquals("/youtubei/v1/navigation/resolve_url", request().url.encodedPath)
            assertEquals("UCuAXFkgsw1L7xaCfnd5JJOw", id)

            assertEquals(RESOLVE_CONTEXT + ",\"url\":\"https://www.youtube.com/@rickastley\"}", bodyText())
            assertFalse(bodyText().contains(":null"), bodyText())
        }

    @Test
    fun visitorDataParsesTheSwJsPayload() =
        runTest {
            install(SW_JS_DATA)

            val id = YouTube.visitorData().getOrThrow()

            assertEquals(HttpMethod.Get, request().method)
            assertEquals("music.youtube.com", request().url.host)
            assertEquals("/sw.js_data", request().url.encodedPath)
            assertEquals("CgtWSVNJVE9S", id)
        }

    private companion object {
        /** The WEB_REMIX body prefix, with the app's own Json settings. */
        const val REMIX_CONTEXT =
            "{\"context\":{\"client\":{\"clientName\":\"WEB_REMIX\",\"clientVersion\":\"1.20260213.01.00\",\"gl\":\"US\",\"hl\":\"en\"}," +
                "\"request\":{\"internalExperimentFlags\":[],\"useSsl\":true}," +
                "\"user\":{\"lockedSafetyMode\":false}}"

        /** The WEB channel client prefix; the offset is the device's, as the client sends it. */
        val WEB_CONTEXT =
            "{\"context\":{\"client\":{\"clientName\":\"WEB\",\"clientVersion\":\"2.20260710.06.00\"," +
                "\"originalUrl\":\"https://www.youtube.com\",\"platform\":\"DESKTOP\"," +
                "\"utcOffsetMinutes\":${platformUtcOffsetMinutes()},\"gl\":\"US\",\"hl\":\"en\"}," +
                "\"request\":{\"internalExperimentFlags\":[],\"useSsl\":true}," +
                "\"user\":{\"lockedSafetyMode\":false}}"

        /** `resolve_url` is a main-site post: the client keeps its own zero offset there. */
        const val RESOLVE_CONTEXT =
            "{\"context\":{\"client\":{\"clientName\":\"WEB\",\"clientVersion\":\"2.20260710.06.00\"," +
                "\"originalUrl\":\"https://www.youtube.com\",\"platform\":\"DESKTOP\",\"utcOffsetMinutes\":0," +
                "\"gl\":\"US\",\"hl\":\"en\"}," +
                "\"request\":{\"internalExperimentFlags\":[],\"useSsl\":true}," +
                "\"user\":{\"lockedSafetyMode\":false}}"

        const val SW_JS_DATA = ")]}'\n[[null,null,[\"CgtWSVNJVE9S\"]]]"

        val MIX_RESPONSE =
            """
            {
              "contents": {
                "twoColumnWatchNextResults": {
                  "playlist": {
                    "playlist": {
                      "titleText": { "runs": [ { "text": "Mix - Rick Astley" } ] },
                      "ownerName": { "runs": [ { "text": "YouTube Music" } ] },
                      "contents": [
                        {
                          "playlistPanelVideoRenderer": {
                            "videoId": "dQw4w9WgXcQ",
                            "title": { "runs": [ { "text": "Never Gonna Give You Up" } ] },
                            "lengthText": { "runs": [ { "text": "3:33" } ] },
                            "shortBylineText": {
                              "runs": [
                                {
                                  "text": "Rick Astley",
                                  "navigationEndpoint": {
                                    "browseEndpoint": { "browseId": "UCuAXFkgsw1L7xaCfnd5JJOw" }
                                  }
                                }
                              ]
                            },
                            "thumbnail": { "thumbnails": [ { "url": "https://i/c1" } ] }
                          }
                        }
                      ]
                    }
                  }
                }
              }
            }
            """.trimIndent()

        val ACCOUNT_RESPONSE =
            """
            {
              "actions": [
                {
                  "openPopupAction": {
                    "popup": {
                      "multiPageMenuRenderer": {
                        "header": {
                          "activeAccountHeaderRenderer": {
                            "accountName": { "runs": [ { "text": "Prady" } ] },
                            "email": { "runs": [ { "text": "prady@example.com" } ] },
                            "channelHandle": { "runs": [ { "text": "@prady" } ] },
                            "accountPhoto": { "thumbnails": [ { "url": "https://i/avatar" } ] }
                          }
                        }
                      }
                    }
                  }
                }
              ]
            }
            """.trimIndent()

        val CHANNEL_RESPONSE =
            """
            {
              "contents": {
                "twoColumnBrowseResultsRenderer": {
                  "tabs": [
                    { "tabRenderer": { "title": "Home", "selected": "true" } },
                    {
                      "tabRenderer": {
                        "title": "Videos",
                        "endpoint": {
                          "commandMetadata": {
                            "webCommandMetadata": { "url": "/@rickastley/videos" }
                          }
                        }
                      }
                    }
                  ]
                }
              },
              "metadata": {
                "channelMetadataRenderer": {
                  "externalId": "UCuAXFkgsw1L7xaCfnd5JJOw",
                  "title": "Rick Astley",
                  "vanityChannelUrl": "https://www.youtube.com/@rickastley",
                  "avatar": { "thumbnails": [ { "url": "https://i/ava" } ] }
                }
              }
            }
            """.trimIndent()

        val CHANNEL_SEARCH_RESPONSE =
            """
            {
              "contents": {
                "twoColumnBrowseResultsRenderer": {
                  "tabs": [
                    {
                      "tabRenderer": {
                        "selected": true,
                        "endpoint": {
                          "commandMetadata": {
                            "webCommandMetadata": { "url": "/@rickastley/search" }
                          }
                        },
                        "content": {
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
                  ]
                }
              }
            }
            """.trimIndent()

        val TRANSCRIPT_RESPONSE =
            """
            {
              "actions": [
                {
                  "updateEngagementPanelAction": {
                    "content": {
                      "transcriptRenderer": {
                        "body": {
                          "transcriptBodyRenderer": {
                            "cueGroups": [
                              {
                                "transcriptCueGroupRenderer": {
                                  "cues": [
                                    {
                                      "transcriptCueRenderer": {
                                        "cue": { "simpleText": "♪never gonna give you up♪" },
                                        "startOffsetMs": 1500,
                                        "durationMs": 1000
                                      }
                                    }
                                  ]
                                }
                              },
                              {
                                "transcriptCueGroupRenderer": {
                                  "cues": [
                                    {
                                      "transcriptCueRenderer": {
                                        "cue": { "simpleText": "never gonna let you down" },
                                        "startOffsetMs": 62250,
                                        "durationMs": 1000
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
                }
              ]
            }
            """.trimIndent()

        val SET_THUMBNAIL_RESPONSE =
            """
            {
              "newHeader": {
                "musicEditablePlaylistDetailHeaderRenderer": {
                  "editHeader": {},
                  "header": {
                    "musicResponsiveHeaderRenderer": {
                      "buttons": [],
                      "title": { "runs": [ { "text": "My playlist" } ] },
                      "subtitle": { "runs": [ { "text": "Prady" } ] },
                      "thumbnail": {
                        "musicThumbnailRenderer": {
                          "thumbnail": { "thumbnails": [ { "url": "https://i/new-thumb" } ] }
                        }
                      }
                    }
                  }
                }
              }
            }
            """.trimIndent()

        val RESOLVE_RESPONSE =
            """
            {
              "endpoint": {
                "browseEndpoint": { "browseId": "UCuAXFkgsw1L7xaCfnd5JJOw" }
              }
            }
            """.trimIndent()
    }
}
