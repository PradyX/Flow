package io.github.aedev.flow.presentation.feed

import androidx.compose.foundation.lazy.LazyListState

/**
 * What a channel page keeps while the viewer is elsewhere. Opening a video disposes the page, so
 * without this the way back refetched the landing behind a full-screen spinner and dropped the
 * grid's scroll and open tab. A channel keeps one session for as long as the app runs.
 */
class ChannelBook {
    private class Session(
        val tabs: ChannelTabState = ChannelTabState(),
        val search: ChannelSearchState = ChannelSearchState(),
        val list: LazyListState = LazyListState(),
    ) {
        var feed: ChannelFeed? = null
    }

    private val sessions = mutableMapOf<String, Session>()

    fun feed(channelId: String): ChannelFeed? = sessions[channelId]?.feed

    fun tabs(channelId: String): ChannelTabState = session(channelId).tabs

    fun search(channelId: String): ChannelSearchState = session(channelId).search

    fun listState(channelId: String): LazyListState = session(channelId).list

    fun put(
        channelId: String,
        feed: ChannelFeed,
    ) {
        session(channelId).feed = feed
    }

    fun forget(channelId: String) {
        sessions.remove(channelId)
    }

    private fun session(channelId: String): Session = sessions.getOrPut(channelId) { Session() }
}
