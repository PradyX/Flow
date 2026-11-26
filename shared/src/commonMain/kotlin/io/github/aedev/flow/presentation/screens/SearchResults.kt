package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.innertube.pages.search.SearchChip
import io.github.aedev.flow.presentation.feed.SearchFeed
import io.github.aedev.flow.presentation.ui.ChannelNavRow
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.LoadMoreRow
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.search_results_estimated
import io.github.aedev.flow.ui.theme.Dimensions
import io.github.aedev.flow.utils.formatViewCount
import org.jetbrains.compose.resources.stringResource

/** How many cards the app lays out across its results, as its own feed does. */
private const val WIDE_GRID_COLUMNS = 3

/**
 * What a search found: YouTube's own estimate, the channels that matched drawn as rows, then every
 * video and playlist in the same card grid the app's feeds use. The page keeps asking for the next
 * one as its footer comes into view.
 */
@Composable
internal fun SearchResults(
    feed: SearchFeed,
    gridState: LazyGridState,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenChannel: (channelId: String, name: String) -> Unit,
) {
    val channels = feed.entries.filter { it.channelId != null }
    val cards = feed.entries.filter { it.channelId == null }
    LazyVerticalGrid(
        columns = GridCells.Fixed(WIDE_GRID_COLUMNS),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimensions.ContentPaddingHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SectionSpacing),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SectionSpacing),
    ) {
        feed.estimatedResults?.takeIf { it > 0 }?.let { estimate ->
            item(key = "estimate", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(Res.string.search_results_estimated, formatViewCount(estimate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }

        if (channels.isNotEmpty()) {
            item(key = "channels", span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    channels.forEach { entry ->
                        ChannelNavRow(entry = entry, onOpenChannel = onOpenChannel)
                    }
                }
            }
        }

        items(cards, key = { it.id }) { entry ->
            VideoCard(entry = entry, onOpen = onOpenVideo, onOpenPlaylist = onOpenPlaylist)
        }

        if (feed.continuation != null) {
            item(key = "more", span = { GridItemSpan(maxLineSpan) }) {
                LaunchedEffectOnce(continuation = feed.continuation, loadingMore = loadingMore, onLoadMore = onLoadMore)
                LoadMoreRow(loading = loadingMore, onLoadMore = onLoadMore)
            }
        }
    }
}

/** Asks for the next page once, when the footer is composed. */
@Composable
private fun LaunchedEffectOnce(
    continuation: String,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(continuation) {
        if (!loadingMore) onLoadMore()
    }
}

/**
 * The chips YouTube puts above a search: the same order and the same labels the site sends, since a
 * chip is a token, not a local enum. A selected chip carries the app's own check mark.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SearchFilterBar(
    chips: List<SearchChip>,
    onChipClick: (SearchChip) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FilterChipSpacing),
        contentPadding = PaddingValues(horizontal = Dimensions.ContentPaddingHorizontal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(chips, key = { it.label }) { chip ->
            FilterChip(
                selected = chip.selected,
                onClick = { onChipClick(chip) },
                label = {
                    Text(
                        text = chip.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                shapes = FilterChipDefaults.shapes(),
                leadingIcon =
                    if (chip.selected) {
                        {
                            Icon(
                                imageVector = FlowIcons.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
            )
        }
    }
}

private val FilterChipSpacing = 8.dp
