package io.github.aedev.flow.presentation.feed

import io.github.aedev.flow.innertube.pages.channel.ChannelTabKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The channel page's retained session: a feed, its tabs and its scroll outlive the page itself, so
 * going back to it restores the grid instead of asking YouTube again.
 */
class ChannelBookTest {
    private val feed =
        ChannelFeed(
            channelId = "UCqueen",
            name = "Queen Official",
            handle = null,
            avatarUrl = null,
            bannerUrl = null,
            subscribers = null,
            description = null,
        )

    @Test
    fun aChannelRemembersWhatItLoaded() {
        val book = ChannelBook()
        assertNull(book.feed("UCqueen"))

        book.put("UCqueen", feed)

        assertEquals(feed, book.feed("UCqueen"))
    }

    @Test
    fun theSameChannelGetsTheSameTabsAndScroll() {
        val book = ChannelBook()

        assertSame(book.tabs("UCqueen"), book.tabs("UCqueen"))
        assertSame(book.listState("UCqueen"), book.listState("UCqueen"))
    }

    @Test
    fun oneChannelsGridNeverLeaksIntoAnother() {
        val book = ChannelBook()
        book.tabs("UCqueen").select(ChannelTabKind.Live)

        assertEquals(ChannelTabKind.Live, book.tabs("UCqueen").selected)
        assertNull(book.tabs("UCzeemusic").selected)
        assertNull(book.feed("UCzeemusic"))
    }

    @Test
    fun tabsKeepTheirPagesAndSortAcrossVideos() {
        val book = ChannelBook()
        book.tabs("UCqueen").seed(
            kind = ChannelTabKind.Videos,
            page = LoadState.Ready(ChannelTabPage(entries = emptyList())),
            key = "videos",
        )

        val reopened = book.tabs("UCqueen")

        assertTrue(reopened.page(ChannelTabKind.Videos) is LoadState.Ready)
        assertFalse(reopened.beginRequest(ChannelTabKind.Videos, "videos"))
    }

    @Test
    fun forgettingAChannelDropsItsFeedAndScroll() {
        val book = ChannelBook()
        book.put("UCqueen", feed)
        val list = book.listState("UCqueen")

        book.forget("UCqueen")

        assertNull(book.feed("UCqueen"))
        assertFalse(book.listState("UCqueen") === list)
    }
}
