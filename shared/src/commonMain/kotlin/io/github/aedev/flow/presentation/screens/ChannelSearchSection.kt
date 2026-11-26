package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.ChannelFeed
import io.github.aedev.flow.presentation.feed.ChannelSearchResults
import io.github.aedev.flow.presentation.feed.ChannelSearchState
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.loadChannelSearch
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.LoadMoreRow
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_retry
import io.github.aedev.flow.resources.channel_search_empty
import io.github.aedev.flow.resources.channel_search_failed
import io.github.aedev.flow.resources.channel_search_hint
import io.github.aedev.flow.ui.theme.Dimensions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val SEARCH_DEBOUNCE_MS = 350L
private const val SEARCH_GRID_COLUMNS = 2
private val SearchResultSpacing = 12.dp
private val SearchFieldInset = 12.dp

/**
 * The channel's own search: a field over that channel's grid, asked as the viewer types. YouTube
 * scopes the answer to the channel, so no client-side filtering is involved; the page's footer
 * reads the next page of the same term.
 */
@Composable
internal fun ChannelSearchSection(
    channel: ChannelFeed,
    search: ChannelSearchState,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
) {
    var attempt by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    LaunchedEffect(search.query, attempt) {
        val term = search.query.trim()
        if (term.isEmpty()) return@LaunchedEffect
        if (attempt == 0) delay(SEARCH_DEBOUNCE_MS)
        search.start(term)
        search.put(
            loadChannelSearch(
                channelId = channel.channelId,
                channelName = channel.name,
                channelThumbnailUrl = channel.avatarUrl.orEmpty(),
                query = term,
            ).fold(
                onSuccess = { LoadState.Ready(it) },
                onFailure = { LoadState.Failed(it.message.orEmpty()) },
            ),
        )
    }

    fun loadMore() {
        val token = search.beginMore() ?: return
        scope.launch {
            loadChannelSearch(
                channelId = channel.channelId,
                channelName = channel.name,
                channelThumbnailUrl = channel.avatarUrl.orEmpty(),
                query = search.query,
                continuation = token,
            ).fold(
                onSuccess = { search.append(it) },
                onFailure = { search.failMore() },
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = search.query,
            onValueChange = { search.type(it) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SearchFieldInset, vertical = 8.dp)
                    .focusRequester(focusRequester),
            singleLine = true,
            shape = CircleShape,
            placeholder = { Text(stringResource(Res.string.channel_search_hint)) },
            leadingIcon = { Icon(imageVector = FlowIcons.Search, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
        )

        when (val current = search.results) {
            null -> {
                Unit
            }

            LoadState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is LoadState.Failed -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Dimensions.SectionSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(Res.string.channel_search_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { attempt++ }) {
                        Text(stringResource(Res.string.action_retry))
                    }
                }
            }

            is LoadState.Ready -> {
                SearchResults(
                    page = current.value,
                    query = search.query,
                    loadingMore = search.loadingMore,
                    onLoadMore = ::loadMore,
                    onOpenVideo = onOpenVideo,
                    onOpenPlaylist = onOpenPlaylist,
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    page: ChannelSearchResults,
    query: String,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
) {
    if (page.isEmpty) {
        Text(
            text = stringResource(Res.string.channel_search_empty, query),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(Dimensions.SectionSpacing),
        )
        return
    }
    val rows = page.entries.chunked(SEARCH_GRID_COLUMNS)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Dimensions.SectionSpacing),
        verticalArrangement = Arrangement.spacedBy(SearchResultSpacing),
    ) {
        items(items = rows, key = { row -> row.joinToString(separator = "|") { it.id } }) { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(SearchResultSpacing),
            ) {
                row.forEach { entry ->
                    Box(modifier = Modifier.weight(1f)) {
                        VideoCard(
                            entry = entry,
                            onOpen = onOpenVideo,
                            onOpenPlaylist = onOpenPlaylist,
                        )
                    }
                }
                if (row.size < SEARCH_GRID_COLUMNS) Spacer(modifier = Modifier.weight(1f))
            }
        }
        if (page.continuation != null) {
            item("search_more") {
                Spacer(modifier = Modifier.height(4.dp))
                LoadMoreRow(loading = loadingMore, onLoadMore = onLoadMore)
            }
        }
    }
}
