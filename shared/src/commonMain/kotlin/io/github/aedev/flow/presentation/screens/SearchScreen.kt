package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.search.SearchHistory
import io.github.aedev.flow.data.search.saveSearchHistoryJson
import io.github.aedev.flow.data.search.saveSearchSessionJson
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.SearchFeedState
import io.github.aedev.flow.presentation.feed.SearchSession
import io.github.aedev.flow.presentation.feed.loadSearchContinuation
import io.github.aedev.flow.presentation.feed.loadSearchFeed
import io.github.aedev.flow.presentation.feed.loadSearchMore
import io.github.aedev.flow.presentation.feed.loadSearchSuggestions
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_clear_search
import io.github.aedev.flow.resources.search_empty
import io.github.aedev.flow.resources.search_filters_title
import io.github.aedev.flow.resources.search_hint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Search: the app's search page — a rounded filled field across the top, then either what the user
 * may be looking for (recent searches, YouTube's completions) or what they found. Typing is a mode
 * of this screen, so the results underneath stay put while the panel is up.
 */
@Composable
internal fun SearchScreen(
    state: SearchFeedState,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenChannel: (channelId: String, name: String) -> Unit,
) {
    var attempt by remember { mutableIntStateOf(0) }
    // The continuation of the chip whose results are being fetched, if any.
    var pendingChip by remember { mutableStateOf<String?>(null) }
    var filtersOpen by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    // Focus has to land somewhere that is not the field, or Android keeps the keyboard up.
    val pageFocus = remember { FocusRequester() }

    fun remember(next: SearchHistory) {
        state.history = next
        saveSearchHistoryJson(next.encode())
    }

    fun submit(term: String = state.query) {
        val query = term.trim()
        if (query.isEmpty()) return
        state.query = query
        state.typing = false
        remember(state.history.with(query))
        attempt++
    }

    LaunchedEffect(attempt) {
        // 0 is the untouched initial composition: a returning screen keeps what it already has.
        if (attempt == 0) return@LaunchedEffect
        // Here rather than in the IME callback: hiding the keyboard from inside onSearch is ignored
        // on Android, while this runs on the composition's own coroutine.
        runCatching { pageFocus.requestFocus() }
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        state.chips = emptyList()
        state.results = LoadState.Loading
        state.results =
            loadSearchFeed(state.query.trim(), state.filters.params()).fold(
                onSuccess = {
                    state.chips = it.chips
                    LoadState.Ready(it)
                },
                onFailure = { LoadState.Failed(it.message.orEmpty()) },
            )
    }

    // A session restored without its page (an older save, or a page that never arrived) asks again
    // with the filter it was left with, so a relaunch never lands on the placeholder over a query
    // the user already typed.
    LaunchedEffect(Unit) {
        if (state.results == null && state.query.isNotBlank()) attempt++
    }

    // The page outlives the process: the query, the narrowing, the page itself and the grid
    // position are written a quarter second after the last change, so a scroll writes once rather
    // than once per frame.
    LaunchedEffect(Unit) {
        snapshotFlow { state.session() }
            .distinctUntilChanged()
            .collectLatest { session ->
                delay(SESSION_SAVE_DELAY_MS)
                saveSearchSessionJson(SearchSession.toJson(session))
            }
    }

    // A chip re-runs the search through its own token; the bar stays up from state.chips meanwhile.
    LaunchedEffect(pendingChip) {
        val token = pendingChip ?: return@LaunchedEffect
        state.results = LoadState.Loading
        state.results =
            loadSearchContinuation(token).fold(
                onSuccess = {
                    state.chips = it.chips
                    LoadState.Ready(it)
                },
                onFailure = { LoadState.Failed(it.message.orEmpty()) },
            )
        pendingChip = null
    }

    // One request per settled term: the panel is YouTube's own completion list, asked for after the
    // typing pause rather than on every keystroke.
    LaunchedEffect(state.query, state.typing) {
        if (!state.typing || state.query.isBlank()) {
            state.suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(SUGGESTION_DEBOUNCE_MS)
        state.suggestions = loadSearchSuggestions(state.query.trim()).getOrDefault(emptyList())
    }

    fun loadMore() {
        val ready = state.results as? LoadState.Ready ?: return
        val token = ready.value.continuation ?: return
        if (state.loadingMore) return
        state.loadingMore = true
        scope.launch {
            loadSearchMore(token).onSuccess { next ->
                val kept = (state.results as? LoadState.Ready)?.value ?: return@onSuccess
                state.results = LoadState.Ready(kept.more(next))
            }
            state.loadingMore = false
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .focusRequester(pageFocus)
                .focusable(),
    ) {
        // The app's search page carries the query field in its bar, not under a title bar, and
        // offers the filter dialog only once there are results to narrow.
        FlowTopBar(
            actions = {
                if (!state.typing && state.results is LoadState.Ready) {
                    BadgedBox(
                        badge = {
                            val active = state.filters.activeCount
                            if (active > 0) Badge { Text(active.toString()) }
                        },
                    ) {
                        IconButton(onClick = { filtersOpen = true }) {
                            Icon(
                                imageVector = FlowIcons.Tune,
                                contentDescription = stringResource(Res.string.search_filters_title),
                            )
                        }
                    }
                }
            },
            title = {
                TextField(
                    value = state.query,
                    onValueChange = { typed ->
                        state.query = typed
                        state.typing = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = CircleShape,
                    placeholder = { Text(stringResource(Res.string.search_hint)) },
                    leadingIcon = {
                        Icon(imageVector = FlowIcons.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    state.query = ""
                                    state.typing = true
                                },
                            ) {
                                Icon(
                                    imageVector = FlowIcons.Close,
                                    contentDescription = stringResource(Res.string.action_clear_search),
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit() }),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                )
            },
        )

        if (state.query.isBlank()) {
            // An empty field is what you searched before: the panel is up whether or not the field
            // has focus yet, so opening Search shows somewhere to start rather than a blank page.
            SearchHistoryRows(
                history = state.history,
                onPick = { submit(it) },
                onRemove = { term -> remember(state.history.without(term)) },
                onClear = { remember(state.history.cleared()) },
            )
        } else if (state.typing) {
            SearchSuggestionRows(suggestions = state.suggestions, onPick = { submit(it) })
        } else {
            // YouTube's own filter chips, above the results the way the app's search page shows them.
            if (state.chips.isNotEmpty()) {
                SearchFilterBar(
                    chips = state.chips,
                    onChipClick = { chip -> pendingChip = chip.continuation },
                )
            }

            when (val current = state.results) {
                null -> {
                    Unit
                }

                LoadState.Loading -> {
                    LoadingState()
                }

                is LoadState.Failed -> {
                    FailedState(onRetry = { submit() })
                }

                is LoadState.Ready -> {
                    val feed = current.value
                    if (feed.entries.isEmpty()) {
                        EmptyState(text = stringResource(Res.string.search_empty))
                    } else {
                        SearchResults(
                            feed = feed,
                            gridState = state.gridState,
                            loadingMore = state.loadingMore,
                            onLoadMore = ::loadMore,
                            onOpenVideo = onOpenVideo,
                            onOpenPlaylist = onOpenPlaylist,
                            onOpenChannel = onOpenChannel,
                        )
                    }
                }
            }
        }

        if (filtersOpen) {
            SearchFilterDialog(
                filter = state.filters,
                onApply = { next ->
                    state.filters = next
                    filtersOpen = false
                    submit()
                },
                onDismiss = { filtersOpen = false },
            )
        }
    }
}

/** Long enough that a typing pause settles before the completion list is asked for. */
private const val SUGGESTION_DEBOUNCE_MS = 250L

/** Long enough that a scroll settles before it writes, short enough to survive a quick kill. */
private const val SESSION_SAVE_DELAY_MS = 250L
