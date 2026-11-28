package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.core.common.currentTimeMillis
import io.github.aedev.flow.data.history.WatchHistoryEntry
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.data.taste.HomeFeedback
import io.github.aedev.flow.data.taste.TasteChannel
import io.github.aedev.flow.data.taste.UserTaste
import io.github.aedev.flow.presentation.feed.HomeChip
import io.github.aedev.flow.presentation.feed.HomeFeedState
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.loadHomeChip
import io.github.aedev.flow.presentation.feed.videoCardEntry
import io.github.aedev.flow.presentation.playback.HostResumeEffect
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_undo
import io.github.aedev.flow.resources.home_card_live_now
import io.github.aedev.flow.resources.home_card_views
import io.github.aedev.flow.resources.home_chip_all
import io.github.aedev.flow.resources.home_chip_live
import io.github.aedev.flow.resources.home_chip_new_to_you
import io.github.aedev.flow.resources.home_chip_recently_uploaded
import io.github.aedev.flow.resources.home_feedback_channel_hidden
import io.github.aedev.flow.resources.home_feedback_liked
import io.github.aedev.flow.resources.home_feedback_not_interested
import io.github.aedev.flow.resources.home_new_videos
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** The app's Home grid: as many columns of video cards as the window fits, chips above them. */
private const val MAX_AUTO_COLUMNS = 4

/** Narrowest a cell may be before the grid drops a column, the reference app's own minimum. */
private val MinCardWidth = 272.dp

/** The card's own side inset, repeated by the grid as its window margin. */
private val CardInset = 12.dp
private val CompactBreakpoint = 600.dp
private val LargeBreakpoint = 840.dp
private val GridSpacing = 12.dp
private val ChipSpacing = 8.dp
private val ChipRowHeight = 48.dp
private val GridVerticalInset = 0.dp

/** The gap between a card row and the next; cards carry their own bottom inset. */
private val GridRowSpacing = 4.dp

/** The feed's own gap under the header, as the reference grid carries. */
private val GridTopGap = 4.dp

/** How old a loaded feed may be before returning to Home asks for it again. */
private const val HOME_REFRESH_AFTER_MS = 5L * 60L * 1000L
private val ChipRowInset = 12.dp

/** One Home grid's column count, window margin and row spacing, derived from the window width. */
private class HomeGrid(
    val columns: Int,
    val contentPadding: Dp,
)

private fun homeGrid(width: Dp): HomeGrid {
    val compact = width < CompactBreakpoint
    val margin =
        when {
            compact -> 0.dp
            width >= LargeBreakpoint -> 24.dp
            else -> 16.dp
        }
    val contentPadding = (margin - CardInset).coerceAtLeast(0.dp)
    val available = (width - contentPadding * 2).coerceAtLeast(0.dp)
    val columns =
        if (compact) {
            1
        } else {
            (available / MinCardWidth).toInt().coerceIn(1, MAX_AUTO_COLUMNS)
        }
    return HomeGrid(
        columns = columns,
        contentPadding = contentPadding,
    )
}

@Composable
internal fun HomeScreen(
    state: HomeFeedState,
    taste: UserTaste,
    feedback: HomeFeedback,
    onFeedback: (HomeFeedback) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    /** The shell's Home bar, composed inside the header so it can leave with the feed. */
    topBar: @Composable () -> Unit = {},
    continueWatching: List<WatchHistoryEntry> = emptyList(),
    onOpenHistoryEntry: (WatchHistoryEntry) -> Unit = {},
    onRemoveHistoryEntry: (String) -> Unit = {},
    onOpenHistory: () -> Unit = {},
) {
    var attempt by remember { mutableIntStateOf(0) }
    val chips = remember(taste) { HomeChip.forTaste(taste) }
    // A list that arrived while the viewer was scrolled down waits here instead of replacing
    // what they are looking at.
    var pendingFeed by remember { mutableStateOf<List<Video>?>(null) }
    var lastLoadAt by remember { mutableStateOf(0L) }
    // Coming back to a feed that has had time to go stale asks for it again; the list that lands
    // then waits behind the cards on screen instead of replacing them.
    HostResumeEffect {
        val now = currentTimeMillis()
        if (lastLoadAt > 0 && now - lastLoadAt > HOME_REFRESH_AFTER_MS) {
            state.chipFeeds.remove(state.selectedChip)
            attempt++
        }
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val mixState = remember(taste, feedback) { feedback.toMixState(taste.toMixState()) }
    val undoLabel = stringResource(Res.string.action_undo)
    val likedMessage = stringResource(Res.string.home_feedback_liked)
    val notInterestedMessage = stringResource(Res.string.home_feedback_not_interested)
    val channelHiddenMessage = stringResource(Res.string.home_feedback_channel_hidden)

    // A hidden card leaves the grid, so the menu cannot offer its undo any more: the snackbar is
    // how every mark stays reversible, restoring the exact feedback that was in place before it.
    fun mark(
        next: HomeFeedback,
        message: String,
    ) {
        val previous = feedback
        onFeedback(next)
        scope.launch {
            val result = snackbar.showSnackbar(message = message, actionLabel = undoLabel, withDismissAction = false)
            if (result == SnackbarResult.ActionPerformed) onFeedback(previous)
        }
    }

    /** Taking a mark back needs no undo of its own, so it applies without a snackbar. */
    fun clear(next: HomeFeedback) = onFeedback(next)
    // Setup's taste or a card menu changing the suppressions invalidates every chip: each one is
    // ranked with them, so a cached chip would keep showing what was just hidden.
    LaunchedEffect(taste, feedback) { state.chipFeeds.clear() }
    LaunchedEffect(chips) {
        if (chips.none { it.key == state.selectedChip.key }) state.selectedChip = HomeChip.All
        pendingFeed = null
    }
    LaunchedEffect(state.selectedChip, attempt, taste, feedback) {
        val chip = state.selectedChip
        state.chipFeeds[chip]?.let { cached ->
            state.feed = LoadState.Ready(cached)
            return@LaunchedEffect
        }
        // Cards already on screen stay put while a refresh runs: the list that lands waits behind
        // them, and the pill is how the viewer takes it.
        val holdCards = state.gridState.firstVisibleItemIndex > 0 && state.feed is LoadState.Ready
        if (!holdCards) state.feed = LoadState.Loading
        state.feed =
            loadHomeChip(chip, mixState, taste).fold(
                onSuccess = { videos ->
                    state.chipFeeds[chip] = videos
                    lastLoadAt = currentTimeMillis()
                    if (holdCards) {
                        pendingFeed = videos
                        state.feed
                    } else {
                        LoadState.Ready(videos)
                    }
                },
                onFailure = {
                    if (holdCards) state.feed else LoadState.Failed(it.message.orEmpty())
                },
            )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val grid = homeGrid(maxWidth)
        // The header is the shell's own bar plus the chips, held over the feed the way the app
        // keeps every other page's bar: the cards scroll beneath it instead of moving away with
        // it and leaving the strip it occupies blank.
        var headerHeightPx by remember { mutableIntStateOf(0) }
        val headerHeight = with(LocalDensity.current) { headerHeightPx.toDp() }
        val readyVideos = (state.feed as? LoadState.Ready)?.value.orEmpty()
        // The shelf spans the row after the first full one, the place the reference app puts it,
        // so the row above it is never left with holes.
        val slots =
            remember(readyVideos, grid.columns, continueWatching.isNotEmpty()) {
                buildList<Video?> {
                    val columns = grid.columns.coerceAtLeast(1)
                    addAll(readyVideos.take(columns))
                    if (continueWatching.isNotEmpty()) add(null)
                    addAll(readyVideos.drop(columns))
                }
            }
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(grid.columns),
                state = state.gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = grid.contentPadding,
                        top = headerHeight + GridTopGap,
                        end = grid.contentPadding,
                        bottom = GridVerticalInset,
                    ),
                verticalArrangement = Arrangement.spacedBy(GridRowSpacing),
            ) {
                when (state.feed) {
                    LoadState.Loading -> {
                        item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                            LoadingState()
                        }
                    }

                    is LoadState.Failed -> {
                        item(key = "failed", span = { GridItemSpan(maxLineSpan) }) {
                            FailedState(
                                onRetry = {
                                    state.chipFeeds.remove(state.selectedChip)
                                    attempt++
                                },
                            )
                        }
                    }

                    is LoadState.Ready -> {
                        itemsIndexed(
                            items = slots,
                            key = { _, video -> video?.id ?: "continue_watching" },
                            span = { _, video -> if (video == null) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
                        ) { _, video ->
                            if (video == null) {
                                HomeContinueWatching(
                                    entries = continueWatching,
                                    onOpen = onOpenHistoryEntry,
                                    onRemove = onRemoveHistoryEntry,
                                    onSeeAll = onOpenHistory,
                                )
                                return@itemsIndexed
                            }
                            VideoCard(
                                entry = videoCardEntry(video).copy(subtitle = homeMeta(video)),
                                onOpen = onOpenVideo,
                                channelName = video.channelName.takeIf { it.isNotBlank() },
                                channelAvatarUrl = video.channelThumbnailUrl.takeIf { it.isNotBlank() },
                                liked = feedback.isLikedVideo(video.id),
                                notInterested = feedback.isSuppressedVideo(video.id),
                                channelBlocked = feedback.isSuppressedChannel(video.channelId),
                                onMoreLikeThis = {
                                    val channel = video.channel()
                                    if (feedback.isLikedVideo(video.id)) {
                                        clear(feedback.withoutVideoLike(video.id, video.channelId))
                                    } else {
                                        mark(feedback.withVideoLike(video.id, channel), likedMessage)
                                    }
                                },
                                onNotInterested = {
                                    if (feedback.isSuppressedVideo(video.id)) {
                                        clear(feedback.restoreVideo(video.id))
                                    } else {
                                        mark(feedback.withoutVideo(video.id), notInterestedMessage)
                                    }
                                },
                                onDontRecommendChannel = {
                                    val channel = video.channel()
                                    if (feedback.isSuppressedChannel(video.channelId)) {
                                        clear(feedback.restoreChannel(video.channelId))
                                    } else {
                                        mark(feedback.withoutChannel(channel), channelHiddenMessage)
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .onSizeChanged { size -> headerHeightPx = size.height },
            ) {
                topBar()
                HomeChipRow(
                    chips = chips,
                    selected = state.selectedChip,
                    onSelect = { chip ->
                        if (chip.key == state.selectedChip.key) {
                            state.chipFeeds.remove(chip)
                            attempt++
                        } else {
                            state.selectedChip = chip
                        }
                    },
                    horizontalInset = ChipRowInset,
                )
            }

            // A fresh list the viewer has not seen yet waits behind the cards on screen; the pill
            // rides up with the header and stops under the status bar, as the reference one does.
            pendingFeed?.let { fresh ->
                Button(
                    onClick = {
                        state.chipFeeds[state.selectedChip] = fresh
                        state.feed = LoadState.Ready(fresh)
                        pendingFeed = null
                        scope.launch { state.gridState.scrollToItem(0) }
                    },
                    contentPadding = ButtonDefaults.ExtraSmallContentPadding,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset { IntOffset(x = 0, y = headerHeightPx) },
                ) {
                    Icon(
                        imageVector = FlowIcons.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.ExtraSmallIconSize),
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.ExtraSmallIconSpacing))
                    Text(text = stringResource(Res.string.home_new_videos))
                }
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** The video's channel as the feedback store keeps it. */
private fun Video.channel(): TasteChannel = TasteChannel(channelId, channelName, channelThumbnailUrl)

/** The app's chip row: one filter chip per chip, the selected one filled. */
@Composable
private fun HomeChipRow(
    chips: List<HomeChip>,
    selected: HomeChip,
    onSelect: (HomeChip) -> Unit,
    horizontalInset: Dp,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().height(ChipRowHeight),
        horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
        contentPadding =
            PaddingValues(start = horizontalInset, end = horizontalInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(chips, key = { it.key }) { chip ->
            FilterChip(
                selected = chip == selected,
                onClick = { onSelect(chip) },
                label = { Text(chipLabel(chip)) },
            )
        }
    }
}

/** The card's meta line: what the app shows under the channel row. */
@Composable
private fun homeMeta(video: io.github.aedev.flow.data.model.Video): String? =
    listOfNotNull(
        when {
            video.isLive -> {
                stringResource(Res.string.home_card_live_now)
            }

            video.viewCount > 0 -> {
                stringResource(
                    Res.string.home_card_views,
                    io.github.aedev.flow.utils
                        .formatViewCount(video.viewCount),
                )
            }

            else -> {
                null
            }
        },
        video.uploadDate.takeIf { it.isNotBlank() },
    ).joinToString(" • ").takeIf { it.isNotBlank() }

@Composable
private fun chipLabel(chip: HomeChip): String =
    when (chip) {
        is HomeChip.Interest -> {
            chip.topic
        }

        else -> {
            stringResource(
                when (chip) {
                    HomeChip.All -> Res.string.home_chip_all
                    HomeChip.NewToYou -> Res.string.home_chip_new_to_you
                    HomeChip.RecentlyUploaded -> Res.string.home_chip_recently_uploaded
                    HomeChip.Live -> Res.string.home_chip_live
                    is HomeChip.Interest -> Res.string.home_chip_all
                },
            )
        }
    }
