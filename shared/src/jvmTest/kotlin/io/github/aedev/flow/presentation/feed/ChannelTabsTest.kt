package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.innertube.pages.channel.ChannelFilterGroup
import io.github.aedev.flow.innertube.pages.channel.ChannelFilterOption
import io.github.aedev.flow.innertube.pages.channel.ChannelHeader
import io.github.aedev.flow.innertube.pages.channel.ChannelLink
import io.github.aedev.flow.innertube.pages.channel.ChannelTabDescriptor
import io.github.aedev.flow.innertube.pages.channel.ChannelTabKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The channel page's tab row: what YouTube published, minus the tabs the port cannot draw. */
class ChannelTabsTest {
    private val header =
        ChannelHeader(
            id = "UCQueen",
            title = "Queen Official",
            handle = "@Queen",
            description = "British rock band",
        )

    private fun tab(
        kind: ChannelTabKind,
        title: String = kind.name,
        params: String? = "token-${kind.name}",
    ) = ChannelTabDescriptor(kind = kind, title = title, params = params)

    @Test
    fun theRowKeepsTheDrawableTabsInYouTubesOrder() {
        val descriptors =
            listOf(
                tab(ChannelTabKind.Home),
                tab(ChannelTabKind.Videos, "Videos"),
                tab(ChannelTabKind.Shorts, "Shorts"),
                tab(ChannelTabKind.Live, "Live"),
                tab(ChannelTabKind.Playlists),
                tab(ChannelTabKind.Posts),
                tab(ChannelTabKind.Store),
                tab(ChannelTabKind.Search),
            )

        val tabs = channelTabs(descriptors, header, "About")

        assertEquals(
            listOf(
                ChannelTabKind.Home,
                ChannelTabKind.Videos,
                ChannelTabKind.Shorts,
                ChannelTabKind.Live,
                ChannelTabKind.Playlists,
                ChannelTabKind.Posts,
                ChannelTabKind.Unknown,
            ),
            tabs.map { it.kind },
        )
        assertEquals(listOf("Home", "Videos", "Shorts", "Live", "Playlists", "Posts", "About"), tabs.map { it.title })
        assertEquals("token-Home", tabs.first().params)
        assertTrue(tabs.last().isAbout)
        assertNull(tabs.last().params)
    }

    @Test
    fun aboutIsOnlyAppendedWhenTheHeaderHasSomethingToShow() {
        val bare = ChannelHeader(id = "UCQueen", title = "Queen Official")
        val descriptors = listOf(tab(ChannelTabKind.Videos, "Videos"))

        assertTrue(channelTabs(descriptors, bare, "About").none { it.isAbout })

        val withLinks = bare.copy(links = listOf(ChannelLink("Site", "queenonline.com", "https://queenonline.com")))
        assertEquals(listOf("Videos", "About"), channelTabs(descriptors, withLinks, "About").map { it.title })
    }

    @Test
    fun aTabWithNoTokenFallsBackToItsKindAndOneWithNoneIsDropped() {
        val descriptors =
            listOf(
                tab(ChannelTabKind.Videos, "Videos", params = null),
                tab(ChannelTabKind.Shorts, "Shorts", params = null),
                tab(ChannelTabKind.Unknown, "Merch", params = null),
                tab(ChannelTabKind.Live, "  ", params = "token-live"),
            )

        val tabs = channelTabs(descriptors, header, "About")

        // Videos and Shorts carry a kind default; the unknown kind and the untitled tab do not.
        assertEquals(listOf(ChannelTabKind.Videos, ChannelTabKind.Shorts), tabs.filterNot { it.isAbout }.map { it.kind })
        assertEquals(ChannelTabKind.Videos.defaultParams, tabs.first().params)
        assertEquals(ChannelTabKind.Shorts.defaultParams, tabs[1].params)
    }

    @Test
    fun aChannelWithOnlyHomeStillShowsThatAndAbout() {
        val tabs = channelTabs(listOf(tab(ChannelTabKind.Home)), header, "About")

        assertEquals(listOf(ChannelTabKind.Home, ChannelTabKind.Unknown), tabs.map { it.kind })
    }

    @Test
    fun aPickedChipRequestsItsContinuation() {
        val filters =
            listOf(
                ChannelFilterGroup(
                    title = null,
                    isDropdown = true,
                    options =
                        listOf(
                            ChannelFilterOption("Latest", selected = true, continuation = "token-latest"),
                            ChannelFilterOption("Popular", continuation = "token-popular"),
                        ),
                ),
            )

        assertNull(channelTabFilterRequest(filters, emptyList()))
        assertEquals(ChannelFilterRequest.Continuation("token-popular"), channelTabFilterRequest(filters, listOf(1)))
    }

    @Test
    fun aPickedMenuEntryRequestsItsParams() {
        val filters =
            listOf(
                ChannelFilterGroup(
                    title = "Sort",
                    isDropdown = true,
                    options = listOf(ChannelFilterOption("Oldest", params = "params-oldest")),
                ),
            )

        assertEquals(ChannelFilterRequest.Params("params-oldest"), channelTabFilterRequest(filters, listOf(0)))
    }

    @Test
    fun aPickOutsideTheOptionsRequestsNothing() {
        val filters =
            listOf(
                ChannelFilterGroup(
                    title = null,
                    isDropdown = false,
                    options = listOf(ChannelFilterOption("Latest", selected = true)),
                ),
            )

        assertNull(channelTabFilterRequest(filters, listOf(0)))
        assertNull(channelTabFilterRequest(filters, listOf(4)))
        assertNull(channelTabFilterRequest(filters, listOf(-1)))
    }

    @Test
    fun theLayoutControlSwitchesBetweenGridAndListAndStartsOnTheGrid() {
        val state = ChannelTabState()

        assertTrue(!state.isList)
        state.toggleList()
        assertTrue(state.isList)
        state.toggleList()
        assertTrue(!state.isList)
    }

    @Test
    fun releasesIsDrawnOnlyWhenTheChannelPublishesIt() {
        val withReleases =
            channelTabs(
                listOf(tab(ChannelTabKind.Videos, "Videos"), tab(ChannelTabKind.Releases, "Releases")),
                header,
                "About",
            )
        assertEquals(listOf(ChannelTabKind.Videos, ChannelTabKind.Releases), withReleases.filterNot { it.isAbout }.map { it.kind })

        val withoutReleases = channelTabs(listOf(tab(ChannelTabKind.Videos, "Videos")), header, "About")
        assertTrue(withoutReleases.none { it.kind == ChannelTabKind.Releases })
    }

    @Test
    fun theChosenIndexPrefersThePickThenYouTubesSelection() {
        val group =
            ChannelFilterGroup(
                title = null,
                isDropdown = true,
                options = listOf(ChannelFilterOption("Latest"), ChannelFilterOption("Popular", selected = true)),
            )

        assertEquals(1, group.chosenIndex(null))
        assertEquals(0, group.chosenIndex(0))
        assertEquals(1, group.chosenIndex(7))
        assertEquals(0, ChannelFilterGroup(null, true, listOf(ChannelFilterOption("Only"))).chosenIndex(null))
    }
}
