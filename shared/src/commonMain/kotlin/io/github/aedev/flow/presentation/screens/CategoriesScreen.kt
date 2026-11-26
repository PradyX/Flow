package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.EXPLORE_CATEGORIES
import io.github.aedev.flow.presentation.feed.ExplorePage
import io.github.aedev.flow.presentation.feed.ExploreSection
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.loadExplore
import io.github.aedev.flow.presentation.feed.loadExploreGrid
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.presentation.ui.LoadMoreRow
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_retry
import io.github.aedev.flow.resources.categories_empty
import io.github.aedev.flow.resources.categories_failed
import io.github.aedev.flow.resources.categories_see_all
import io.github.aedev.flow.resources.categories_title
import io.github.aedev.flow.ui.theme.Dimensions
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val ExploreChipSpacing = 8.dp
private val ExploreGridSpacing = 12.dp

/** Three columns, as the app's Home grid uses. */
private const val EXPLORE_COLUMNS = 3

/**
 * Explore: one chip per YouTube destination, each drawing whatever shape its source serves — a page
 * of shelves, the Gaming tab's paged grid, or YouTube Music's trending chart. A shelf's See all
 * pushes the shelf's own grid, so Back returns to the category as it was.
 */
@Composable
internal fun CategoriesScreen(
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenShelf: (browseId: String, params: String, title: String?) -> Unit,
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val category = EXPLORE_CATEGORIES[selectedIndex.coerceIn(0, EXPLORE_CATEGORIES.lastIndex)]
    var state by remember(category) { mutableStateOf<LoadState<ExplorePage>>(LoadState.Loading) }
    var attempt by remember(category) { mutableIntStateOf(0) }
    var loadingMore by remember(category) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(category, attempt) {
        state = LoadState.Loading
        state =
            loadExplore(category.destination).fold(
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
            loadExploreGrid(category.destination.browseId, category.destination.params.orEmpty(), token)
                .onSuccess { page ->
                    val kept = (state as? LoadState.Ready)?.value ?: return@onSuccess
                    state =
                        LoadState.Ready(
                            kept.copy(
                                sections =
                                    kept.sections.map { section ->
                                        section.copy(entries = section.entries + page.entries)
                                    },
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
                    text = stringResource(Res.string.categories_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Dimensions.ContentPaddingHorizontal),
            horizontalArrangement = Arrangement.spacedBy(ExploreChipSpacing),
        ) {
            EXPLORE_CATEGORIES.forEachIndexed { index, candidate ->
                FilterChip(
                    selected = index == selectedIndex,
                    onClick = { selectedIndex = index },
                    label = { Text(stringResource(candidate.label)) },
                )
            }
        }
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
                if (page.sections.all { it.entries.isEmpty() }) {
                    ExploreMessage(message = stringResource(Res.string.categories_empty))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(EXPLORE_COLUMNS),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                            PaddingValues(
                                start = Dimensions.ContentPaddingHorizontal,
                                end = Dimensions.ContentPaddingHorizontal,
                                bottom = Dimensions.SectionSpacing,
                            ),
                        horizontalArrangement = Arrangement.spacedBy(ExploreGridSpacing),
                        verticalArrangement = Arrangement.spacedBy(ExploreGridSpacing),
                    ) {
                        page.sections.forEach { section ->
                            item(key = "head-${section.id}", span = { GridItemSpan(maxLineSpan) }) {
                                ExploreSectionHeader(
                                    section = section,
                                    onSeeAll = { params ->
                                        onOpenShelf(category.destination.browseId, params, section.title)
                                    },
                                )
                            }
                            items(section.entries, key = { entry -> "e-${section.id}-${entry.id}" }) { entry ->
                                VideoCard(
                                    entry = entry,
                                    onOpen = onOpenVideo,
                                    onOpenPlaylist = onOpenPlaylist,
                                )
                            }
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

@Composable
private fun ExploreSectionHeader(
    section: ExploreSection,
    onSeeAll: (params: String) -> Unit,
) {
    val title = section.title ?: return
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.weight(1f))
        section.moreParams?.let { params ->
            TextButton(onClick = { onSeeAll(params) }) {
                Text(stringResource(Res.string.categories_see_all))
            }
        }
    }
}

@Composable
internal fun ExploreMessage(
    message: String,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Dimensions.SectionSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        action?.invoke()
    }
}
