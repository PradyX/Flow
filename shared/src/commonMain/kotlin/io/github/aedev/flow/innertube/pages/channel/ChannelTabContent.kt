package io.github.aedev.flow.innertube.pages.channel

import io.github.aedev.flow.innertube.pages.renderer.CommunityPost
import io.github.aedev.flow.innertube.pages.renderer.FeedItem
import io.github.aedev.flow.innertube.pages.renderer.FeedItemOwner
import io.github.aedev.flow.innertube.pages.renderer.FeedShelf

/**
 * One page of one channel tab. Grid tabs fill [items], shelf tabs fill [sections], and the Posts tab
 * fills [posts], which is a shape of its own rather than a card the grids can draw.
 *
 * A sort chip's token is itself a continuation, so switching sort and loading a page are the same
 * request with a different token.
 */
data class ChannelTabContent(
    val kind: ChannelTabKind,
    val items: List<FeedItem> = emptyList(),
    val sections: List<FeedShelf> = emptyList(),
    val posts: List<CommunityPost> = emptyList(),
    val filters: List<ChannelFilterGroup> = emptyList(),
    val continuation: String? = null,
    val owner: FeedItemOwner = FeedItemOwner(),
)
