package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.aedev.flow.data.subscriptions.Subscription
import io.github.aedev.flow.innertube.pages.channel.ChannelTabKind
import io.github.aedev.flow.innertube.pages.renderer.CommunityPost
import io.github.aedev.flow.presentation.feed.ChannelFeed
import io.github.aedev.flow.presentation.feed.ChannelTabState
import io.github.aedev.flow.presentation.feed.FeedEntry
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.channelTabRequestKey
import io.github.aedev.flow.presentation.feed.loadChannelTab
import io.github.aedev.flow.presentation.feed.loadChannelTabMore
import io.github.aedev.flow.presentation.ui.ChannelAvatar
import io.github.aedev.flow.presentation.ui.FlowSubscribeButton
import io.github.aedev.flow.presentation.ui.LoadMoreRow
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_retry
import io.github.aedev.flow.resources.channel_tab_empty
import io.github.aedev.flow.resources.feed_failed
import io.github.aedev.flow.ui.theme.Dimensions
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val ChannelContentPadding = 12.dp
private const val CHANNEL_AVATAR_DIAMETER = 72
private val ChannelGridSpacing = 12.dp
private const val CHANNEL_GRID_COLUMNS = 2
private const val BANNER_ASPECT = 6f
private val TabStateMinHeight = 240.dp

/**
 * The channel's banner, header, tabs and the open tab's grid. A tab loads the page its sort names,
 * a sort switch asks again with the chip's own token, and the grid's footer reads the next page in
 * place, the way the app's own grids do.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChannelBody(
    channel: ChannelFeed,
    subscribed: Boolean,
    onSubscriptionChange: (Subscription) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenChannel: (channelId: String, name: String) -> Unit,
    onOpenPost: (CommunityPost) -> Unit,
    tabs: ChannelTabState,
    listState: LazyListState,
) {
    var attempt by remember(channel.channelId) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val visible = channel.tabs
    val selectedKind = tabs.selected ?: visible.firstOrNull()?.kind
    val selectedIndex = visible.indexOfFirst { it.kind == selectedKind }.coerceAtLeast(0)
    val spec = visible.getOrNull(selectedIndex)
    val page = tabs.page(selectedKind)
    val filters = (page as? LoadState.Ready)?.value?.filters.orEmpty()
    val picks = tabs.picks(selectedKind)

    // A tab opens on the page it was first loaded with; a sort or filter choice asks YouTube for the
    // page that choice names, and the guard keeps a recomposition from repeating that request.
    LaunchedEffect(channel.channelId, selectedKind, picks, attempt) {
        val kind = selectedKind ?: return@LaunchedEffect
        val tab = visible.firstOrNull { it.kind == kind } ?: return@LaunchedEffect
        if (tab.isAbout) return@LaunchedEffect
        if (!tabs.beginRequest(kind, channelTabRequestKey(kind, picks))) return@LaunchedEffect
        val previous = tabs.page(kind)
        val previousFilters = (previous as? LoadState.Ready)?.value?.filters.orEmpty()
        if (previous !is LoadState.Ready) tabs.putPage(kind, LoadState.Loading)
        tabs.putPage(
            kind,
            loadChannelTab(channel.channelId, tab.params, kind, previousFilters, picks).fold(
                onSuccess = { LoadState.Ready(it) },
                onFailure = { LoadState.Failed(it.message.orEmpty()) },
            ),
        )
    }

    fun loadMore() {
        val kind = selectedKind ?: return
        val token = tabs.beginMore(kind) ?: return
        scope.launch {
            loadChannelTabMore(channel.channelId, kind, token).fold(
                onSuccess = { tabs.appendPage(kind, it) },
                onFailure = { tabs.failMore(kind) },
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Dimensions.SectionSpacing),
        verticalArrangement = Arrangement.spacedBy(ChannelGridSpacing),
    ) {
        channel.bannerUrl?.takeIf { it.isNotBlank() }?.let { banner ->
            item("banner") {
                AsyncImage(
                    model = banner,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(BANNER_ASPECT),
                )
            }
        }
        item("header") {
            ChannelHeaderBlock(
                channel = channel,
                subscribed = subscribed,
                onSubscriptionChange = onSubscriptionChange,
            )
        }
        if (visible.isNotEmpty()) {
            stickyHeader("tabs") {
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                    ChannelTabRow(
                        selectedIndex = selectedIndex,
                        titles = visible.map { it.title },
                        onSelect = { index -> visible.getOrNull(index)?.let { tabs.select(it.kind) } },
                    )
                    if (spec?.isAbout != true && filters.isNotEmpty()) {
                        ChannelFilterBar(
                            filters = filters,
                            picks = picks,
                            onSelect = { group, option ->
                                selectedKind?.let { tabs.pick(it, group, option) }
                            },
                        )
                    }
                }
            }
        }
        if (spec?.isAbout == true) {
            item("about") { ChannelAboutSection(channel = channel) }
        } else {
            when (page) {
                null, LoadState.Loading -> {
                    item("tab_loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(TabStateMinHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is LoadState.Failed -> {
                    item("tab_failed") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(Dimensions.SectionSpacing),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(Res.string.feed_failed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(
                                onClick = {
                                    selectedKind?.let { tabs.forgetRequest(it) }
                                    attempt++
                                },
                            ) {
                                Text(stringResource(Res.string.action_retry))
                            }
                        }
                    }
                }

                is LoadState.Ready -> {
                    val content = page.value
                    when {
                        spec?.kind == ChannelTabKind.Posts -> {
                            if (content.posts.isEmpty()) item("tab_empty") { EmptyTab() }
                            items(items = content.posts, key = { post -> "post:${post.id}" }) { post ->
                                ChannelPostCard(post = post, onOpen = post.openAction(onOpenPost))
                            }
                        }

                        content.sections.isNotEmpty() -> {
                            content.sections.forEachIndexed { index, shelf ->
                                channelShelfItems(
                                    index = index,
                                    shelf = shelf,
                                    isList = tabs.isList,
                                    onOpenVideo = onOpenVideo,
                                    onOpenPlaylist = onOpenPlaylist,
                                    onOpenChannel = onOpenChannel,
                                    onOpenPost = onOpenPost,
                                )
                            }
                        }

                        tabs.isList -> {
                            if (content.entries.isEmpty()) item("tab_empty") { EmptyTab() }
                            items(items = content.entries, key = { entry -> "row:${entry.id}" }) { entry ->
                                VideoRowCard(
                                    entry = entry,
                                    onOpen = onOpenVideo,
                                    onOpenPlaylist = onOpenPlaylist,
                                )
                            }
                        }

                        else -> {
                            val rows = content.entries.chunked(CHANNEL_GRID_COLUMNS)
                            if (rows.isEmpty()) item("tab_empty") { EmptyTab() }
                            items(
                                items = rows,
                                key = { row -> row.joinToString(separator = "|") { it.id } },
                            ) { row ->
                                ChannelCardRow(row, onOpenVideo, onOpenPlaylist)
                            }
                        }
                    }
                    if (content.continuation != null) {
                        item("tab_more") {
                            LoadMoreRow(
                                loading = tabs.loadingMore(selectedKind),
                                onLoadMore = ::loadMore,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelHeaderBlock(
    channel: ChannelFeed,
    subscribed: Boolean,
    onSubscriptionChange: (Subscription) -> Unit,
) {
    val subscription = Subscription(channel.channelId, channel.name, channel.avatarUrl)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = ChannelContentPadding)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChannelAvatar(
                name = channel.name,
                imageUrl = channel.avatarUrl,
                diameter = CHANNEL_AVATAR_DIAMETER,
            )
            Spacer(modifier = Modifier.weight(1f))
            FlowSubscribeButton(
                subscribed = subscribed,
                onSubscribe = { onSubscriptionChange(subscription) },
                onUnsubscribe = { onSubscriptionChange(subscription) },
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            channel.handle?.takeIf { it.isNotBlank() }?.let { handle ->
                Text(
                    text = handle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            channel.subscribers?.takeIf { it.isNotBlank() }?.let { subscribers ->
                Text(
                    text = subscribers,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        channel.description?.takeIf { it.isNotBlank() }?.let { description ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun EmptyTab() {
    Text(
        text = stringResource(Res.string.channel_tab_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(Dimensions.SectionSpacing),
    )
}
