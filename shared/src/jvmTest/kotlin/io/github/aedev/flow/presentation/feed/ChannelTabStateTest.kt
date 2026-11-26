package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.innertube.pages.channel.ChannelTabKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The tab state: which tab is open, each tab's page, picks and last request. */
class ChannelTabStateTest {
    private val videosPage = LoadState.Ready(ChannelTabPage(entries = emptyList()))
    private val shortsPage = LoadState.Ready(ChannelTabPage(entries = emptyList()))

    @Test
    fun seedingOpensTheVideosTabOnThePageTheLandingReturned() {
        val state = ChannelTabState()

        state.seed(ChannelTabKind.Videos, videosPage, "Videos:")

        assertEquals(ChannelTabKind.Videos, state.selected)
        assertEquals(videosPage, state.page(ChannelTabKind.Videos))
        assertEquals(emptyList(), state.picks(ChannelTabKind.Videos))
    }

    @Test
    fun aRepeatedRequestIsSkippedAndAChangedOneIsNot() {
        val state = ChannelTabState()

        assertTrue(state.beginRequest(ChannelTabKind.Videos, "Videos:"))
        assertFalse(state.beginRequest(ChannelTabKind.Videos, "Videos:"))
        assertTrue(state.beginRequest(ChannelTabKind.Videos, "Videos:1"))

        // The other tab has its own request, not the Videos tab's.
        assertTrue(state.beginRequest(ChannelTabKind.Shorts, "Videos:"))
    }

    @Test
    fun forgettingARequestLetsTheSameOneRunAgain() {
        val state = ChannelTabState()
        state.beginRequest(ChannelTabKind.Videos, "Videos:1")

        state.forgetRequest(ChannelTabKind.Videos)

        assertTrue(state.beginRequest(ChannelTabKind.Videos, "Videos:1"))
    }

    @Test
    fun eachTabKeepsItsOwnPicks() {
        val state = ChannelTabState()

        state.pick(ChannelTabKind.Videos, 0, 2)
        state.pick(ChannelTabKind.Shorts, 0, 1)

        assertEquals(listOf(2), state.picks(ChannelTabKind.Videos))
        assertEquals(listOf(1), state.picks(ChannelTabKind.Shorts))
        assertEquals(emptyList(), state.picks(ChannelTabKind.Live))
    }

    @Test
    fun aPickForALaterGroupLeavesTheEarlierOnesUnchosen() {
        val state = ChannelTabState()

        state.pick(ChannelTabKind.Videos, 1, 3)

        assertEquals(listOf(-1, 3), state.picks(ChannelTabKind.Videos))

        state.pick(ChannelTabKind.Videos, 0, 1)

        assertEquals(listOf(1, 3), state.picks(ChannelTabKind.Videos))
    }

    @Test
    fun switchingTabsKeepsThePagesAlreadyLoaded() {
        val state = ChannelTabState()
        state.seed(ChannelTabKind.Videos, videosPage, "Videos:")
        state.putPage(ChannelTabKind.Shorts, shortsPage)

        state.select(ChannelTabKind.Shorts)
        state.select(ChannelTabKind.Videos)

        assertEquals(ChannelTabKind.Videos, state.selected)
        assertEquals(videosPage, state.page(ChannelTabKind.Videos))
        assertEquals(shortsPage, state.page(ChannelTabKind.Shorts))
    }

    @Test
    fun theAboutTabHasNoPageOfItsOwn() {
        val state = ChannelTabState()
        state.seed(ChannelTabKind.Videos, videosPage, "Videos:")

        state.select(ChannelTabKind.Unknown)

        assertEquals(ChannelTabKind.Unknown, state.selected)
        assertNull(state.page(ChannelTabKind.Unknown))
    }

    @Test
    fun resetClearsTheSelectionPagesPicksAndRequests() {
        val state = ChannelTabState()
        state.seed(ChannelTabKind.Videos, videosPage, "Videos:")
        state.pick(ChannelTabKind.Videos, 0, 1)

        state.reset()

        assertNull(state.selected)
        assertNull(state.page(ChannelTabKind.Videos))
        assertEquals(emptyList(), state.picks(ChannelTabKind.Videos))
        assertTrue(state.beginRequest(ChannelTabKind.Videos, "Videos:"))
    }
}
