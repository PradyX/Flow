package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.ExploreGridPage
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.loadExploreGrid
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.presentation.ui.LoadMoreRow
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.action_retry
import io.github.aedev.flow.resources.categories_empty
import io.github.aedev.flow.resources.categories_failed
import io.github.aedev.flow.resources.categories_title
import io.github.aedev.flow.ui.theme.Dimensions
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val ExploreShelfSpacing = 12.dp

/** Three columns, as the app's Home grid uses. */
private const val EXPLORE_SHELF_COLUMNS = 3

/**
 * A shelf's See all: the shelf's own videos, paged, opened from the category it belongs to. Back
 * returns to the category with its chip selection intact.
 */
@Composable
internal fun ExploreShelfScreen(
    browseId: String,
    params: String,
    title: String?,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onBack: () -> Unit,
) {
    var state by remember(browseId, params) { mutableStateOf<LoadState<ExploreGridPage>>(LoadState.Loading) }
    var attempt by remember(browseId, params) { mutableIntStateOf(0) }
    var loadingMore by remember(browseId, params) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(browseId, params, attempt) {
        state = LoadState.Loading
        state =
            loadExploreGrid(browseId, params).fold(
                onSuccess = { page -> LoadState.Ready(page) },
                onFailure = { error -> LoadState.Failed(error.message.orEmpty()) },
            )
    }

    fun loadMore() {
        val ready = state as? LoadState.Ready ?: return
        val token = ready.value.continuation ?: return
        if (loadingMore) return
        loadingMore = true
        scope.launch {
            loadExploreGrid(browseId, params, token)
                .onSuccess { page ->
                    val kept = (state as? LoadState.Ready)?.value ?: return@onSuccess
                    state =
                        LoadState.Ready(
                            kept.copy(
                                entries = kept.entries + page.entries,
                                continuation = page.continuation,
                            ),
                        )
                }
            loadingMore = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FlowTopBar(
            title = {
                Text(
                    text = title?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.categories_title),
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
        )
        when (val current = state) {
            LoadState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is LoadState.Failed -> {
                ExploreMessage(
                    message = stringResource(Res.string.categories_failed),
                    action = {
                        TextButton(onClick = { attempt++ }) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    },
                )
            }

            is LoadState.Ready -> {
                val page = current.value
                if (page.entries.isEmpty()) {
                    ExploreMessage(message = stringResource(Res.string.categories_empty))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(EXPLORE_SHELF_COLUMNS),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                            PaddingValues(
                                horizontal = Dimensions.ContentPaddingHorizontal,
                                vertical = ExploreShelfSpacing,
                            ),
                        horizontalArrangement = Arrangement.spacedBy(ExploreShelfSpacing),
                        verticalArrangement = Arrangement.spacedBy(ExploreShelfSpacing),
                    ) {
                        items(page.entries, key = { it.id }) { entry ->
                            VideoCard(
                                entry = entry,
                                onOpen = onOpenVideo,
                                onOpenPlaylist = onOpenPlaylist,
                            )
                        }
                        if (page.continuation != null) {
                            item(key = "more", span = { GridItemSpan(maxLineSpan) }) {
                                LoadMoreRow(loading = loadingMore, onLoadMore = ::loadMore)
                            }
                        }
                    }
                }
            }
        }
    }
}
