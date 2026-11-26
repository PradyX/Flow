package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.aedev.flow.data.library.LibraryStore
import io.github.aedev.flow.data.subscriptions.Subscription
import io.github.aedev.flow.data.subscriptions.SubscriptionBook
import io.github.aedev.flow.data.subscriptions.loadSubscriptions
import io.github.aedev.flow.data.subscriptions.saveSubscriptions
import io.github.aedev.flow.presentation.feed.FeedComment
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.Short
import io.github.aedev.flow.presentation.feed.ShortsFeed
import io.github.aedev.flow.presentation.feed.loadCommentReplies
import io.github.aedev.flow.presentation.feed.loadShorts
import io.github.aedev.flow.presentation.feed.loadVideoDetail
import io.github.aedev.flow.presentation.feed.videoWatchUrl
import io.github.aedev.flow.presentation.ui.LocalShareAction
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_copy_link
import io.github.aedev.flow.resources.action_open_channel
import io.github.aedev.flow.resources.action_play
import io.github.aedev.flow.resources.action_retry
import io.github.aedev.flow.resources.action_save
import io.github.aedev.flow.resources.action_saved
import io.github.aedev.flow.resources.comments_title
import io.github.aedev.flow.resources.feed_failed
import io.github.aedev.flow.resources.link_copied
import io.github.aedev.flow.resources.shorts_empty
import io.github.aedev.flow.resources.shorts_title
import io.github.aedev.flow.ui.theme.Dimensions
import io.github.aedev.flow.ui.theme.PlayerScrimContent
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** The pager's tag, so a test can swipe the reel the way a finger does. */
internal const val SHORTS_PAGER_TEST_TAG = "shorts_pager"

/** One short's still, tagged so a test can tell which reel's frame is on screen. */
internal fun shortStillTag(videoId: String): String = "short_still_$videoId"

/** What the comment sheet shows for one reel: its first page and the total its header names. */
private data class ShortComments(
    val comments: List<FeedComment>,
    val countText: String?,
)

/**
 * The reel feed: one short per page, full height, paged by a vertical swipe, with the page's
 * channel, title, views and sound down its left edge and the action rail down its right, the way
 * the reference draws a reel. Loading, failure and an empty feed all draw inside the same frame
 * instead of leaving the screen blank.
 *
 * The reel is a still here: the port has no Shorts playback engine, so the rail carries the actions
 * the port can honour (comments, save, share, more) and the like count is drawn without a toggle,
 * the same call the player's like pair makes.
 */
@OptIn(ExperimentalTime::class)
@Composable
internal fun ShortsScreen(
    libraryStore: LibraryStore,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String) -> Unit,
    onOpenChannel: (channelId: String, name: String) -> Unit,
) {
    var state by remember { mutableStateOf<LoadState<ShortsFeed>>(LoadState.Loading) }
    var attempt by remember { mutableIntStateOf(0) }
    var savedIds by remember { mutableStateOf(emptySet<String>()) }
    var subscriptions by remember { mutableStateOf(loadSubscriptions() ?: SubscriptionBook()) }
    var commentsFor by remember { mutableStateOf<String?>(null) }
    var comments by remember { mutableStateOf<ShortComments?>(null) }
    var commentsLoading by remember { mutableStateOf(false) }
    var repliesByComment by remember { mutableStateOf<Map<String, List<FeedComment>>>(emptyMap()) }
    var expandedReplies by remember { mutableStateOf(emptySet<String>()) }
    var loadingReplies by remember { mutableStateOf(emptySet<String>()) }
    var moreFor by remember { mutableStateOf<Short?>(null) }
    val clipboard = LocalClipboardManager.current
    val share = LocalShareAction.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(Res.string.link_copied)

    LaunchedEffect(libraryStore) {
        savedIds = libraryStore.savedShorts().map { it.videoId }.toSet()
    }

    val onToggleSaved: (Short) -> Unit = { short ->
        scope.launch {
            val isSaved =
                libraryStore.toggleSaved(
                    videoId = short.videoId,
                    title = short.title,
                    channelName = short.channelName,
                    thumbnailUrl = short.stillUrl,
                    nowMillis = Clock.System.now().toEpochMilliseconds(),
                    isShort = true,
                )
            savedIds = if (isSaved) savedIds + short.videoId else savedIds - short.videoId
        }
    }

    val onToggleSubscription: (Short) -> Unit = { short ->
        val channelId = short.channelId
        if (channelId != null) {
            val next =
                if (subscriptions.isSubscribed(channelId)) {
                    subscriptions.without(channelId)
                } else {
                    subscriptions.with(
                        Subscription(
                            id = channelId,
                            name = short.channelName.orEmpty(),
                            thumbnailUrl = short.channelAvatarUrl,
                        ),
                    )
                }
            subscriptions = next
            saveSubscriptions(next)
        }
    }

    val openComments: (Short) -> Unit = { short ->
        commentsFor = short.videoId
        comments = null
        commentsLoading = true
        expandedReplies = emptySet()
        repliesByComment = emptyMap()
        scope.launch {
            comments =
                loadVideoDetail(short.videoId).getOrNull()?.let { detail ->
                    ShortComments(detail.comments, detail.commentCountText)
                }
            commentsLoading = false
        }
    }

    val onToggleReplies: (FeedComment) -> Unit = { comment ->
        if (comment.id in expandedReplies) {
            expandedReplies = expandedReplies - comment.id
        } else {
            expandedReplies = expandedReplies + comment.id
            val token = comment.replyToken
            val videoId = commentsFor
            if (token != null && videoId != null && comment.id !in repliesByComment && comment.id !in loadingReplies) {
                loadingReplies = loadingReplies + comment.id
                scope.launch {
                    loadCommentReplies(token, videoId).onSuccess { fetched ->
                        repliesByComment = repliesByComment + (comment.id to fetched)
                    }
                    loadingReplies = loadingReplies - comment.id
                }
            }
        }
    }

    LaunchedEffect(attempt) {
        state =
            loadShorts().fold(
                onSuccess = { LoadState.Ready(it) },
                onFailure = { LoadState.Failed(it.message.orEmpty()) },
            )
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    verticalArrangement = Arrangement.Center,
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
                if (current.value.shorts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(Res.string.shorts_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    ShortsPager(
                        shorts = current.value.shorts,
                        savedIds = savedIds,
                        subscriptions = subscriptions,
                        onToggleSaved = onToggleSaved,
                        onToggleSubscription = onToggleSubscription,
                        onOpenComments = openComments,
                        onShare = { short ->
                            share(
                                short.title.ifBlank { short.channelName.orEmpty() },
                                videoWatchUrl(short.videoId),
                            )
                        },
                        onMore = { short -> moreFor = short },
                        onOpenVideo = onOpenVideo,
                        onOpenChannel = onOpenChannel,
                    )
                }
            }
        }

        // The tab's own title over the reel, the way the reference labels the Shorts tab.
        Text(
            text = stringResource(Res.string.shorts_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = PlayerScrimContent,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp),
        )

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
        )
    }

    val commentedVideo = commentsFor
    if (commentedVideo != null) {
        BottomSheet(onDismiss = { commentsFor = null }) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item("comments-title") {
                    Text(
                        text = stringResource(Res.string.comments_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    )
                }
                val current = comments
                if (current != null) {
                    commentsSection(
                        comments = current.comments,
                        countText = current.countText,
                        replies = repliesByComment,
                        expanded = expandedReplies,
                        loading = loadingReplies,
                        onToggleReplies = onToggleReplies,
                    )
                } else if (commentsLoading) {
                    item("comments-loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    val moreShort = moreFor
    if (moreShort != null) {
        BottomSheet(onDismiss = { moreFor = null }) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                ShortsSheetRow(stringResource(Res.string.action_play)) {
                    moreFor = null
                    onOpenVideo(moreShort.videoId, moreShort.title, moreShort.stillUrl)
                }
                ShortsSheetRow(
                    stringResource(
                        if (moreShort.videoId in savedIds) Res.string.action_saved else Res.string.action_save,
                    ),
                ) {
                    moreFor = null
                    onToggleSaved(moreShort)
                }
                ShortsSheetRow(stringResource(Res.string.action_copy_link)) {
                    moreFor = null
                    clipboard.setText(AnnotatedString(videoWatchUrl(moreShort.videoId)))
                    scope.launch { snackbar.showSnackbar(copiedMessage) }
                }
                moreShort.channelId?.let { channelId ->
                    ShortsSheetRow(stringResource(Res.string.action_open_channel)) {
                        moreFor = null
                        onOpenChannel(channelId, moreShort.channelName.orEmpty())
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortsSheetRow(
    label: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        content()
    }
}

@Composable
private fun ShortsPager(
    shorts: List<Short>,
    savedIds: Set<String>,
    subscriptions: SubscriptionBook,
    onToggleSaved: (Short) -> Unit,
    onToggleSubscription: (Short) -> Unit,
    onOpenComments: (Short) -> Unit,
    onShare: (Short) -> Unit,
    onMore: (Short) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String) -> Unit,
    onOpenChannel: (channelId: String, name: String) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { shorts.size })
    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().testTag(SHORTS_PAGER_TEST_TAG),
    ) { page ->
        val short = shorts[page]
        ShortPage(
            short = short,
            isSaved = short.videoId in savedIds,
            isSubscribed = short.channelId?.let { subscriptions.isSubscribed(it) } == true,
            onToggleSaved = onToggleSaved,
            onToggleSubscription = onToggleSubscription,
            onOpenComments = onOpenComments,
            onShare = onShare,
            onMore = onMore,
            onOpenVideo = onOpenVideo,
            onOpenChannel = onOpenChannel,
        )
    }
}

@Composable
private fun ShortPage(
    short: Short,
    isSaved: Boolean,
    isSubscribed: Boolean,
    onToggleSaved: (Short) -> Unit,
    onToggleSubscription: (Short) -> Unit,
    onOpenComments: (Short) -> Unit,
    onShare: (Short) -> Unit,
    onMore: (Short) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String) -> Unit,
    onOpenChannel: (channelId: String, name: String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = short.stillUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().testTag(shortStillTag(short.videoId)),
        )
        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(
                        start = ShortsChromeStartPadding,
                        end = ShortsChromeEndPadding,
                        bottom = ShortsControlsBottomOffset,
                    ),
            verticalAlignment = Alignment.Bottom,
        ) {
            ShortsMetadataOverlay(
                short = short,
                isSubscribed = isSubscribed,
                onChannelClick = { short.channelId?.let { onOpenChannel(it, short.channelName.orEmpty()) } },
                onSubscribeToggle = { onToggleSubscription(short) },
                onOpenDescription = { onOpenVideo(short.videoId, short.title, short.stillUrl) },
                modifier = Modifier.weight(1f).padding(end = ShortsMetadataEndPadding),
            )
            ShortsActionRail(
                isLiked = false,
                likeLabel = short.likeCountText.orEmpty(),
                // Liking is an account mutation the port has no session for; the count is drawn and
                // the button is inert, the same call the player's like pair makes.
                onLikeClick = null,
                commentLabel = short.commentCountText.orEmpty(),
                onCommentsClick = { onOpenComments(short) },
                isSaved = isSaved,
                onSaveClick = { onToggleSaved(short) },
                onShareClick = { onShare(short) },
                onMoreClick = { onMore(short) },
                channelAvatarUrl = short.channelAvatarUrl,
                channelName = short.channelName,
            )
        }
    }
}
