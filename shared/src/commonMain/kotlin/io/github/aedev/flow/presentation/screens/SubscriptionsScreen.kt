package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.subscriptions.Subscription
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.SubscribedChannel
import io.github.aedev.flow.presentation.feed.SubscriptionsFeed
import io.github.aedev.flow.presentation.feed.SubscriptionsFeedState
import io.github.aedev.flow.presentation.feed.loadSubscriptions
import io.github.aedev.flow.presentation.ui.ChannelAvatar
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowProfileAction
import io.github.aedev.flow.presentation.ui.FlowSubscribeButton
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.action_undo
import io.github.aedev.flow.resources.channels_count
import io.github.aedev.flow.resources.feed_failed
import io.github.aedev.flow.resources.library_filter_all
import io.github.aedev.flow.resources.no_subscriptions_found
import io.github.aedev.flow.resources.search_subscriptions
import io.github.aedev.flow.resources.subscriptions_empty_body
import io.github.aedev.flow.resources.subscriptions_empty_title
import io.github.aedev.flow.resources.subscriptions_no_videos
import io.github.aedev.flow.resources.subscriptions_quick_access_title
import io.github.aedev.flow.resources.subscriptions_search_placeholder
import io.github.aedev.flow.resources.subscriptions_title
import io.github.aedev.flow.resources.subscriptions_view_mode
import io.github.aedev.flow.resources.unsubscribed_from_template
import io.github.aedev.flow.ui.theme.Dimensions
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val SUBSCRIPTIONS_MAX_COLUMNS = 2
private const val STRIP_ITEM_WIDTH = 64
private const val STRIP_AVATAR_DIAMETER = 56
private const val MANAGE_AVATAR_DIAMETER = 48

private val GridSpacing = 12.dp
private val GridInset = 16.dp
private val StripSpacing = 12.dp
private val StripHorizontalPadding = 12.dp
private val ManageRowPadding = 12.dp
private val ManageAvatarSpacing = 16.dp
private val ManageActionSpacing = 8.dp

/**
 * Subscriptions, the app's tab: a Quick access row of the channels the viewer subscribed to, then
 * their recent uploads merged newest first, in the list rows or the two-column grid the app's own
 * view mode toggle switches between. Searching subscriptions — the app's manage mode — lists one
 * row per channel and unsubscribes from there, with the same Undo the app offers.
 */
@Composable
internal fun SubscriptionsScreen(
    state: SubscriptionsFeedState,
    channels: List<Subscription>,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenChannel: (channelId: String, name: String) -> Unit,
    onOpenProfiles: () -> Unit = {},
    onUnsubscribe: (Subscription) -> Unit = {},
    onRestoreSubscription: (Subscription, index: Int) -> Unit = { _, _ -> },
) {
    var attempt by remember { mutableIntStateOf(0) }
    var managing by remember { mutableStateOf(false) }
    var view by remember { mutableStateOf(SubscriptionsView.ROWS) }
    var query by remember { mutableStateOf("") }

    // Setup is the only place the port subscribes, so its channel list is the whole subscription
    // set; a change there is a different set of channels and re-runs the load.
    LaunchedEffect(channels, attempt) {
        state.feed = LoadState.Loading
        state.feed =
            loadSubscriptions(channels).fold(
                onSuccess = { LoadState.Ready(it) },
                onFailure = { LoadState.Failed(it.message.orEmpty()) },
            )
    }

    if (managing) {
        ManageSubscriptions(
            channels = channels,
            query = query,
            onQueryChange = { query = it },
            onClose = {
                managing = false
                query = ""
            },
            onOpenChannel = onOpenChannel,
            onUnsubscribe = onUnsubscribe,
            onRestoreSubscription = onRestoreSubscription,
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FlowTopBar(
            title = {
                Text(
                    text = stringResource(Res.string.subscriptions_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            actions = {
                IconButton(
                    onClick = {
                        view =
                            if (view == SubscriptionsView.ROWS) {
                                SubscriptionsView.GRID
                            } else {
                                SubscriptionsView.ROWS
                            }
                    },
                ) {
                    Icon(
                        imageVector = if (view == SubscriptionsView.ROWS) FlowIcons.GridView else FlowIcons.ListView,
                        contentDescription = stringResource(Res.string.subscriptions_view_mode),
                    )
                }
                IconButton(onClick = { managing = true }) {
                    Icon(
                        imageVector = FlowIcons.Search,
                        contentDescription = stringResource(Res.string.search_subscriptions),
                    )
                }
                // The app's third disc opens its notifications page. The port has no notifications
                // feed, so that disc's slot stays empty and the discs either side keep its frames.
                Spacer(modifier = Modifier.width(Dimensions.BarActionSlot))
                FlowProfileAction(onClick = onOpenProfiles)
            },
        )

        when (val current = state.feed) {
            LoadState.Loading -> {
                LoadingState()
            }

            is LoadState.Failed -> {
                FailedState(onRetry = { attempt++ })
            }

            is LoadState.Ready -> {
                val feed = current.value
                when {
                    feed.channels.isEmpty() -> {
                        SubscriptionsEmptyState()
                    }

                    feed.videos.isEmpty() && feed.failedChannelCount == feed.channels.size -> {
                        FailedState(onRetry = { attempt++ })
                    }

                    feed.videos.isEmpty() -> {
                        EmptyState(stringResource(Res.string.subscriptions_no_videos))
                    }

                    else -> {
                        SubscriptionsFeed(
                            feed = feed,
                            view = view,
                            onOpenVideo = onOpenVideo,
                            onOpenChannel = onOpenChannel,
                            onViewAll = { managing = true },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Manage mode, the app's `SubscriptionsManageContent`: a search top bar, the count of channels the
 * query leaves, then one row per channel. Unsubscribing from a row offers Undo, which puts the
 * channel back where it was in the subscription order.
 */
@Composable
private fun ManageSubscriptions(
    channels: List<Subscription>,
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onOpenChannel: (channelId: String, name: String) -> Unit,
    onUnsubscribe: (channel: Subscription) -> Unit,
    onRestoreSubscription: (channel: Subscription, index: Int) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(Res.string.action_undo)
    val filtered = manageChannels(channels, query)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ManageTopBar(query = query, onQueryChange = onQueryChange, onClose = onClose)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = GridInset, vertical = GridSpacing),
                verticalArrangement = Arrangement.spacedBy(GridSpacing),
            ) {
                item(key = "count") {
                    Text(
                        text = pluralStringResource(Res.plurals.channels_count, filtered.size, filtered.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                items(filtered, key = { it.id }) { channel ->
                    val index = channels.indexOf(channel)
                    val unsubscribedMessage = stringResource(Res.string.unsubscribed_from_template, channel.name)
                    ManagedChannelRow(
                        channel = channel,
                        onClick = { onOpenChannel(channel.id, channel.name) },
                        onUnsubscribe = {
                            onUnsubscribe(channel)
                            scope.launch {
                                val result =
                                    snackbar.showSnackbar(
                                        message = unsubscribedMessage,
                                        actionLabel = undoLabel,
                                        duration = SnackbarDuration.Short,
                                    )
                                if (result == SnackbarResult.ActionPerformed) {
                                    onRestoreSubscription(channel, index)
                                }
                            }
                        },
                    )
                }
                if (filtered.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(Res.string.no_subscriptions_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = GridInset),
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** The app's search top bar for manage mode: back out of the mode, then the filtering field. */
@Composable
private fun ManageTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    FlowTopBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = CircleShape,
                placeholder = { Text(stringResource(Res.string.subscriptions_search_placeholder)) },
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = FlowIcons.Back,
                    contentDescription = stringResource(Res.string.action_back),
                )
            }
        },
    )
}

@Composable
private fun ManagedChannelRow(
    channel: Subscription,
    onClick: () -> Unit,
    onUnsubscribe: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onClick)
                .padding(ManageRowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChannelAvatar(
            name = channel.name,
            imageUrl = channel.thumbnailUrl,
            diameter = MANAGE_AVATAR_DIAMETER,
        )
        Spacer(modifier = Modifier.width(ManageAvatarSpacing))
        Text(
            text = channel.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(ManageActionSpacing))
        FlowSubscribeButton(
            subscribed = true,
            onSubscribe = {},
            onUnsubscribe = onUnsubscribe,
        )
    }
}

/** The channels manage mode lists: the whole set for a blank query, else names that contain it. */
internal fun manageChannels(
    channels: List<Subscription>,
    query: String,
): List<Subscription> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return channels
    return channels.filter { it.name.contains(trimmed, ignoreCase = true) }
}

/** How the feed lays its uploads out: the app's list rows, or the grid its toggle switches to. */
private enum class SubscriptionsView { ROWS, GRID }

/**
 * The feed under the header band. Its columns come from the window, the same way the app derives
 * them: as many cells as fit at [Dimensions.FeedMinCardWidth], capped at
 * [SUBSCRIPTIONS_MAX_COLUMNS]. A row's still is one of those cells minus its two insets, so a row
 * lines up with the grid above it and its text starts where the grid's second column does.
 */
@Composable
private fun SubscriptionsFeed(
    feed: SubscriptionsFeed,
    view: SubscriptionsView,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenChannel: (channelId: String, name: String) -> Unit,
    onViewAll: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val available = (maxWidth - Dimensions.FeedSideInset * 2).coerceAtLeast(0.dp)
        val columns = (available / Dimensions.FeedMinCardWidth).toInt().coerceIn(1, SUBSCRIPTIONS_MAX_COLUMNS)
        val rowThumbnailWidth = available / columns - Dimensions.ContentPaddingHorizontal * 2
        val rows = view == SubscriptionsView.ROWS
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (rows) 1 else columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = Dimensions.FeedSideInset,
                    end = Dimensions.FeedSideInset,
                    top = Dimensions.FeedTopPadding,
                    bottom = Dimensions.SectionSpacing,
                ),
            verticalArrangement = Arrangement.spacedBy(if (rows) 0.dp else Dimensions.ItemSpacing),
        ) {
            item(key = "quick_access", span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    QuickAccessHeader(
                        channels = feed.channels,
                        onOpenChannel = onOpenChannel,
                        onViewAll = onViewAll,
                    )
                    HorizontalDivider()
                    SubscriptionGroupRow()
                }
            }
            items(feed.videos, key = { video -> video.entry.id }) { video ->
                if (rows) {
                    VideoRowCard(
                        entry = video.entry,
                        onOpen = onOpenVideo,
                        onOpenPlaylist = { _, _, _ -> },
                        thumbnailWidth = rowThumbnailWidth,
                        channelName = video.channelName,
                        onOpenChannel = { onOpenChannel(video.channelId, video.channelName) },
                    )
                } else {
                    VideoCard(
                        entry = video.entry,
                        onOpen = onOpenVideo,
                        channelName = video.channelName,
                        channelAvatarUrl = video.channelAvatarUrl,
                    )
                }
            }
        }
    }
}

/**
 * The app's Quick access header: its label over one avatar per subscription, with View all at the
 * end opening manage mode. Every item keeps the app's 64dp cell, so the row stays even.
 */
@Composable
private fun QuickAccessHeader(
    channels: List<SubscribedChannel>,
    onOpenChannel: (channelId: String, name: String) -> Unit,
    onViewAll: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
    ) {
        Text(
            text = stringResource(Res.string.subscriptions_quick_access_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = StripHorizontalPadding),
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = StripHorizontalPadding, end = 8.dp, top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(StripSpacing),
        ) {
            items(channels, key = { it.id }) { channel ->
                QuickAccessChannel(
                    channel = channel,
                    onClick = { onOpenChannel(channel.id, channel.name) },
                )
            }
            item(key = "view_all") {
                QuickAccessViewAll(onClick = onViewAll)
            }
        }
    }
}

@Composable
private fun QuickAccessChannel(
    channel: SubscribedChannel,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .width(STRIP_ITEM_WIDTH.dp)
                .clickable(onClick = onClick),
    ) {
        ChannelAvatar(
            name = channel.name,
            imageUrl = channel.avatarUrl,
            diameter = STRIP_AVATAR_DIAMETER,
        )
        Text(
            text = channel.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
        )
    }
}

/**
 * The group filter row under the quick access row: the one chip this device's subscription set
 * gives it, All, over the feed beneath. The reference also offers a Posts chip and a control that
 * edits its groups; the port keeps no subscription groups and has no posts feed of its own, so
 * neither has a source here.
 */
@Composable
private fun SubscriptionGroupRow() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Dimensions.ContentPaddingHorizontal,
                    vertical = Dimensions.DenseTileGap,
                ),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.DenseTileGap),
    ) {
        FilterChip(
            selected = true,
            onClick = {},
            label = { Text(stringResource(Res.string.library_filter_all)) },
        )
    }
}

/** The last cell of Quick access: a forward mark that opens the channel list. */
@Composable
private fun QuickAccessViewAll(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .width(STRIP_ITEM_WIDTH.dp)
                .clickable(onClick = onClick),
    ) {
        Box(
            modifier =
                Modifier
                    .size(STRIP_AVATAR_DIAMETER.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = FlowIcons.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(Res.string.library_filter_all),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
        )
    }
}

/** The app's empty Subscriptions page: its mark, then the line that explains what fills it. */
@Composable
private fun SubscriptionsEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Dimensions.ContentPaddingHorizontal),
        ) {
            Icon(
                imageVector = FlowIcons.Subscriptions,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 16.dp).size(80.dp),
            )
            Text(
                text = stringResource(Res.string.subscriptions_empty_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.subscriptions_empty_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
