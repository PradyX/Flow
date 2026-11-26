package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.aedev.flow.data.library.LibraryStore
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.Short
import io.github.aedev.flow.presentation.feed.ShortsFeed
import io.github.aedev.flow.presentation.feed.loadShorts
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.action_retry
import io.github.aedev.flow.resources.action_save
import io.github.aedev.flow.resources.action_saved
import io.github.aedev.flow.resources.feed_failed
import io.github.aedev.flow.resources.shorts_empty
import io.github.aedev.flow.ui.theme.Dimensions
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** The pager's tag, so a test can swipe the reel the way a finger does. */
internal const val SHORTS_PAGER_TEST_TAG = "shorts_pager"

/** One short's still, tagged so a test can tell which reel's frame is on screen. */
internal fun shortStillTag(videoId: String): String = "short_still_$videoId"

private val ShortsOverlayPadding = 16.dp
private val ShortsOverlayShape = RoundedCornerShape(12.dp)
private const val SHORTS_OVERLAY_ALPHA = 0.45f
private const val SHORTS_BACK_BUTTON_ALPHA = 0.7f

/**
 * The reel feed: one short per page, full height, paged by a vertical swipe, with the reel's still
 * behind its title and channel and the back button over it. Loading, failure and an empty feed all
 * draw inside the same frame instead of leaving the screen blank.
 */
@OptIn(ExperimentalTime::class)
@Composable
internal fun ShortsScreen(
    libraryStore: LibraryStore,
    onBack: () -> Unit,
) {
    var state by remember { mutableStateOf<LoadState<ShortsFeed>>(LoadState.Loading) }
    var attempt by remember { mutableIntStateOf(0) }
    var savedIds by remember { mutableStateOf(emptySet<String>()) }
    val scope = rememberCoroutineScope()

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
                        onToggleSaved = onToggleSaved,
                    )
                }
            }
        }

        // Over the reel, so it stays reachable on every page.
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(ShortsOverlayPadding),
            shape = ShortsOverlayShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = SHORTS_BACK_BUTTON_ALPHA),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = FlowIcons.Back,
                    contentDescription = stringResource(Res.string.action_back),
                )
            }
        }
    }
}

@Composable
private fun ShortsPager(
    shorts: List<Short>,
    savedIds: Set<String>,
    onToggleSaved: (Short) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { shorts.size })
    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().testTag(SHORTS_PAGER_TEST_TAG),
        ) { page ->
            ShortPage(short = shorts[page])
        }
        val current = shorts.getOrNull(pagerState.currentPage)
        if (current != null) {
            val isSaved = current.videoId in savedIds
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(ShortsOverlayPadding),
                shape = ShortsOverlayShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = SHORTS_BACK_BUTTON_ALPHA),
            ) {
                IconButton(onClick = { onToggleSaved(current) }) {
                    Icon(
                        imageVector = if (isSaved) FlowIcons.Bookmark else FlowIcons.BookmarkBorder,
                        contentDescription =
                            stringResource(if (isSaved) Res.string.action_saved else Res.string.action_save),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortPage(short: Short) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = short.stillUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().testTag(shortStillTag(short.videoId)),
        )
        // The reel's own still is the frame; the scrim under the text is a theme token, not a colour.
        Surface(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(ShortsOverlayPadding),
            shape = ShortsOverlayShape,
            color = MaterialTheme.colorScheme.scrim.copy(alpha = SHORTS_OVERLAY_ALPHA),
        ) {
            Column(modifier = Modifier.padding(ShortsOverlayPadding)) {
                short.channelName?.takeIf { it.isNotBlank() }?.let { channel ->
                    Text(
                        text = channel,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                short.title.takeIf { it.isNotBlank() }?.let { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
