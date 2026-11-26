package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.subscriptions.Subscription
import io.github.aedev.flow.innertube.pages.channel.ChannelTabKind
import io.github.aedev.flow.innertube.pages.renderer.CommunityPost
import io.github.aedev.flow.presentation.feed.ChannelBook
import io.github.aedev.flow.presentation.feed.ChannelFeed
import io.github.aedev.flow.presentation.feed.ChannelTabPage
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.channelTabRequestKey
import io.github.aedev.flow.presentation.feed.loadChannel
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.action_cancel
import io.github.aedev.flow.resources.action_retry
import io.github.aedev.flow.resources.channel_search_action
import io.github.aedev.flow.resources.channel_tab_about
import io.github.aedev.flow.resources.channel_view_grid
import io.github.aedev.flow.resources.channel_view_list
import io.github.aedev.flow.resources.feed_failed
import io.github.aedev.flow.ui.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

/**
 * A channel: the app's channel header, the tabs YouTube published for it, and the open tab's grid.
 * The landing fetch opens the Videos tab, which is the one the port's channel cards show; About is
 * appended from the header the same request returns.
 *
 * The page's feed, tabs, search and scroll live in [ChannelBook], so a video opened from here and
 * closed again comes back to the same grid instead of a refetching blank page.
 */
@Composable
internal fun ChannelScreen(
    channelId: String,
    fallbackName: String,
    subscribed: Boolean,
    onSubscriptionChange: (Subscription) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenChannel: (channelId: String, name: String) -> Unit,
    onOpenPost: (CommunityPost) -> Unit,
    onBack: () -> Unit,
    book: ChannelBook,
    /** A relaunch reopened this page: if it no longer resolves, the tab beneath it takes over. */
    onUnavailable: (() -> Unit)? = null,
) {
    val aboutTitle = stringResource(Res.string.channel_tab_about)
    val tabs = remember(channelId) { book.tabs(channelId) }
    val search = remember(channelId) { book.search(channelId) }
    val listState = remember(channelId) { book.listState(channelId) }
    var state by
        remember(channelId) {
            mutableStateOf<LoadState<ChannelFeed>>(
                book.feed(channelId)?.let { LoadState.Ready(it) } ?: LoadState.Loading,
            )
        }
    var attempt by remember(channelId) { mutableIntStateOf(0) }

    LaunchedEffect(channelId, attempt) {
        // The retained feed is what the viewer left; only an explicit retry asks again.
        if (attempt == 0 && book.feed(channelId) != null) return@LaunchedEffect
        state = LoadState.Loading
        tabs.reset()
        state =
            loadChannel(channelId, aboutTitle).fold(
                onSuccess = { feed ->
                    // A channel YouTube no longer knows answers without a header rather than with an
                    // error, so a nameless header is the unresolvable case.
                    if (onUnavailable != null && feed.name.isBlank()) {
                        onUnavailable()
                        LoadState.Failed("")
                    } else {
                        book.put(channelId, feed)
                        // The landing fetch carries the Videos tab's own first page, so that tab
                        // opens on it; the channel itself opens on Home, which loads its shelves.
                        feed.tabs.firstOrNull { it.kind == ChannelTabKind.Videos }?.let { videos ->
                            tabs.seed(
                                kind = videos.kind,
                                page =
                                    LoadState.Ready(
                                        ChannelTabPage(
                                            entries = feed.videos,
                                            filters = feed.filters,
                                            continuation = feed.videosContinuation,
                                        ),
                                    ),
                                key = channelTabRequestKey(videos.kind, emptyList()),
                            )
                        }
                        LoadState.Ready(feed)
                    }
                },
                onFailure = { error ->
                    onUnavailable?.invoke()
                    LoadState.Failed(error.message.orEmpty())
                },
            )
    }

    val ready = state as? LoadState.Ready

    Column(modifier = Modifier.fillMaxSize()) {
        FlowTopBar(
            title = {
                Text(
                    text = ready?.value?.name ?: fallbackName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = FlowIcons.Back,
                        contentDescription = stringResource(Res.string.action_back),
                    )
                }
            },
            actions = {
                // The grid/list control belongs to the grid tabs; About has no layout to switch.
                val openTab = ready?.value?.tabs?.firstOrNull { it.kind == tabs.selected }
                if (ready != null && openTab?.isAbout != true) {
                    IconButton(onClick = { tabs.toggleList() }) {
                        Icon(
                            imageVector = if (tabs.isList) FlowIcons.GridView else FlowIcons.ListView,
                            contentDescription =
                                stringResource(
                                    if (tabs.isList) Res.string.channel_view_grid else Res.string.channel_view_list,
                                ),
                        )
                    }
                }
                // The reference's magnifier sits on the channel's own bar and searches that channel,
                // which is why the tab row hides YouTube's Search tab.
                if (ready != null) {
                    if (search.active) {
                        IconButton(onClick = { search.close() }) {
                            Icon(
                                imageVector = FlowIcons.Close,
                                contentDescription = stringResource(Res.string.action_cancel),
                            )
                        }
                    } else {
                        IconButton(onClick = { search.open() }) {
                            Icon(
                                imageVector = FlowIcons.Search,
                                contentDescription = stringResource(Res.string.channel_search_action),
                            )
                        }
                    }
                }
            },
        )

        when (val current = state) {
            LoadState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is LoadState.Failed -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(Dimensions.SectionSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(Res.string.feed_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { attempt++ }) {
                        Text(stringResource(Res.string.action_retry))
                    }
                }
            }

            is LoadState.Ready -> {
                if (search.active) {
                    ChannelSearchSection(
                        channel = current.value,
                        search = search,
                        onOpenVideo = onOpenVideo,
                        onOpenPlaylist = onOpenPlaylist,
                    )
                } else {
                    ChannelBody(
                        channel = current.value,
                        subscribed = subscribed,
                        onSubscriptionChange = onSubscriptionChange,
                        onOpenVideo = onOpenVideo,
                        onOpenPlaylist = onOpenPlaylist,
                        onOpenChannel = onOpenChannel,
                        onOpenPost = onOpenPost,
                        tabs = tabs,
                        listState = listState,
                    )
                }
            }
        }
    }
}
