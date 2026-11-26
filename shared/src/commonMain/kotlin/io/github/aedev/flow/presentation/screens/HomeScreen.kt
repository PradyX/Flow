package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.data.taste.HomeFeedback
import io.github.aedev.flow.data.taste.TasteChannel
import io.github.aedev.flow.data.taste.UserTaste
import io.github.aedev.flow.presentation.feed.HomeChip
import io.github.aedev.flow.presentation.feed.HomeFeedState
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.loadHomeChip
import io.github.aedev.flow.presentation.feed.videoCardEntry
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** The app's Home grid: three columns of video cards, with its chip row above them. */
private const val HOME_COLUMNS = 3

private val GridSpacing = 12.dp
private val ChipSpacing = 8.dp
private val GridInset = 16.dp

@Composable
internal fun HomeScreen(
    state: HomeFeedState,
    taste: UserTaste,
    feedback: HomeFeedback,
    onFeedback: (HomeFeedback) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
) {
    var attempt by remember { mutableIntStateOf(0) }
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
    LaunchedEffect(state.selectedChip, attempt, taste, feedback) {
        val chip = state.selectedChip
        state.chipFeeds[chip]?.let { cached ->
            state.feed = LoadState.Ready(cached)
            return@LaunchedEffect
        }
        state.feed = LoadState.Loading
        state.feed =
            loadHomeChip(chip, mixState, taste).fold(
                onSuccess = { videos ->
                    state.chipFeeds[chip] = videos
                    LoadState.Ready(videos)
                },
                onFailure = { LoadState.Failed(it.message.orEmpty()) },
            )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(HOME_COLUMNS),
            state = state.gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = GridInset, vertical = GridSpacing),
            horizontalArrangement = Arrangement.spacedBy(GridSpacing),
            verticalArrangement = Arrangement.spacedBy(GridSpacing),
        ) {
            item(key = "chips", span = { GridItemSpan(maxLineSpan) }) {
                HomeChipRow(
                    selected = state.selectedChip,
                    onSelect = { chip -> state.selectedChip = chip },
                )
            }

            when (val current = state.feed) {
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
                    items(current.value, key = { video -> video.id }) { video ->
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
    selected: HomeChip,
    onSelect: (HomeChip) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
        contentPadding = PaddingValues(bottom = GridSpacing),
    ) {
        items(HomeChip.entries.toList(), key = { it.key }) { chip ->
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
    ).joinToString(" · ").takeIf { it.isNotBlank() }

@Composable
private fun chipLabel(chip: HomeChip): String =
    stringResource(
        when (chip) {
            HomeChip.All -> Res.string.home_chip_all
            HomeChip.NewToYou -> Res.string.home_chip_new_to_you
            HomeChip.RecentlyUploaded -> Res.string.home_chip_recently_uploaded
            HomeChip.Live -> Res.string.home_chip_live
        },
    )
