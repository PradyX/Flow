package io.github.aedev.flow.presentation.nav

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The navigation stack's stored form: a relaunch reopens the tab and the detail pages the last run
 * left, and anything that cannot be reopened degrades to a stack that starts on a tab root rather
 * than to a route the app cannot draw.
 */
class NavigationStateTest {
    @Test
    fun everyTabRootComesBack() {
        listOf(
            AppDestination.Home,
            AppDestination.Music,
            AppDestination.Search,
            AppDestination.Subscriptions,
            AppDestination.Library,
        ).forEach { tab ->
            val restored = restoredNavigationStack(navigationStateJson(listOf(tab)))

            assertEquals(listOf(tab), restored)
            assertEquals(1, AppRouter(restored.first()).depth)
        }
    }

    @Test
    fun aChannelComesBackWithItsIdentifiers() {
        val channel = AppDestination.Channel("UCiMhD4jzUqG-IgPzUmmytRQ", "Queen Official")

        val restored = restoredNavigationStack(navigationStateJson(listOf(AppDestination.Search, channel)))

        assertEquals(listOf(AppDestination.Search, channel), restored)
    }

    @Test
    fun aPlayerComesBackWithItsIdentifiers() {
        val player =
            AppDestination.Player(
                videoId = "fJ9rUzIMcZQ",
                title = "Queen – Bohemian Rhapsody",
                thumbnailUrl = "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg",
            )

        val restored = restoredNavigationStack(navigationStateJson(listOf(AppDestination.Home, player)))

        assertEquals(listOf(AppDestination.Home, player), restored)
        assertEquals(2, AppRouter(restored.first(), restored.drop(1)).depth)
    }

    @Test
    fun aTabNameFromTheTabOnlyFormatStillOpens() {
        assertEquals(listOf(AppDestination.Library), restoredNavigationStack("library"))
        assertEquals(listOf(AppDestination.Music), restoredNavigationStack("music"))
        assertEquals(listOf(AppDestination.Search), restoredNavigationStack("  search "))
    }

    @Test
    fun aStoredValueThatIsNotARouteIsIgnored() {
        assertTrue(restoredNavigationStack(null).isEmpty())
        assertTrue(restoredNavigationStack("").isEmpty())
        assertTrue(restoredNavigationStack("player").isEmpty())
        assertTrue(restoredNavigationStack("not json at all").isEmpty())
        assertTrue(restoredNavigationStack("""{"routes":[]}""").isEmpty())
        assertTrue(restoredNavigationStack("""{"routes":[{"tab":"settings"}]}""").isEmpty())
        assertTrue(restoredNavigationStack("""{"routes":[{"channelName":"No Id"}]}""").isEmpty())
        assertTrue(restoredNavigationStack("""{"routes":[{"videoTitle":"No Id"}]}""").isEmpty())
    }

    @Test
    fun aStackThatDoesNotStartOnATabRootIsIgnored() {
        val player = AppDestination.Player("fJ9rUzIMcZQ", "Bohemian Rhapsody")
        val channel = AppDestination.Channel("UCiMhD4jzUqG-IgPzUmmytRQ", "Queen Official")

        // A detail page is only restorable over the tab it was opened from.
        assertTrue(restoredNavigationStack(navigationStateJson(listOf(player))).isEmpty())
        assertTrue(restoredNavigationStack(navigationStateJson(listOf(channel, player))).isEmpty())
    }

    @Test
    fun aPlaylistComesBackWithItsIdentifiers() {
        val playlist =
            AppDestination.Playlist(
                playlistId = "PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI",
                title = "Top Tracks",
                thumbnailUrl = "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg",
            )

        val restored = restoredNavigationStack(navigationStateJson(listOf(AppDestination.Search, playlist)))

        assertEquals(listOf(AppDestination.Search, playlist), restored)
        assertEquals(2, AppRouter(restored.first(), restored.drop(1)).depth)
    }

    @Test
    fun anArtistComesBackWithItsIdentifiers() {
        val artist =
            AppDestination.Artist(
                artistId = "UCVH0sKQ0aF1G5yA9IW1H2qQ",
                name = "Anirudh Ravichander",
                thumbnailUrl = "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg",
            )

        val restored = restoredNavigationStack(navigationStateJson(listOf(AppDestination.Music, artist)))

        assertEquals(listOf(AppDestination.Music, artist), restored)
        assertEquals(2, AppRouter(restored.first(), restored.drop(1)).depth)
    }

    @Test
    fun theReelFeedComesBack() {
        val restored =
            restoredNavigationStack(navigationStateJson(listOf(AppDestination.Home, AppDestination.Shorts)))

        assertEquals(listOf(AppDestination.Home, AppDestination.Shorts), restored)
    }

    @Test
    fun aPlaylistWithNoIdentifiersEndsTheChain() {
        val restored =
            restoredNavigationStack(
                """{"routes":[{"tab":"home"},{"playlistTitle":"No Id"},{"videoId":"fJ9rUzIMcZQ"}]}""",
            )

        assertEquals(listOf(AppDestination.Home), restored)
    }

    @Test
    fun aRestoredDetailDropsToTheTabItWasOpenedFrom() {
        val router =
            AppRouter(
                start = AppDestination.Search,
                restoredDetail = listOf(AppDestination.Channel("UCiMhD4jzUqG-IgPzUmmytRQ", "Queen Official")),
            )

        assertEquals(2, router.depth)

        router.dropToTabRoot()

        assertEquals(listOf(AppDestination.Search), router.stack)
        assertEquals(AppDestination.Search, router.current)
        assertFalse(router.canGoBack)
    }
}
