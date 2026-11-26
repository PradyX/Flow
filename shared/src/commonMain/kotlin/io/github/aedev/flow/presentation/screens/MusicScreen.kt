package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.MusicChip
import io.github.aedev.flow.presentation.feed.MusicFeedState
import io.github.aedev.flow.presentation.feed.MusicHomeFeed
import io.github.aedev.flow.presentation.feed.loadMusicHome
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowProfileAction
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_search
import io.github.aedev.flow.resources.moods_title
import io.github.aedev.flow.resources.music_title
import io.github.aedev.flow.resources.settings_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The app's music tab: YouTube Music's own shelves, each a heading over a row of square cards, in
 * the app's order. A card opens the playlist page its album or playlist carries, its song, or its
 * artist — every card lands somewhere.
 */
@Composable
internal fun MusicScreen(
    state: MusicFeedState,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenArtist: (artistId: String, name: String, thumbnailUrl: String?) -> Unit,
    onSearch: () -> Unit,
    onOpenMoods: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfiles: () -> Unit,
) {
    var attempt by remember { mutableIntStateOf(0) }
    var selectedChip by remember { mutableStateOf<MusicChip?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(attempt, selectedChip) {
        // A returning screen keeps the shelves it already has; a retry or a chip choice refetches.
        if (attempt == 0 && selectedChip == null && state.feed is LoadState.Ready) return@LaunchedEffect
        state.feed = LoadState.Loading
        state.feed =
            loadMusicHome(chip = selectedChip).fold(
                onSuccess = { LoadState.Ready(it) },
                onFailure = { LoadState.Failed(it.message.orEmpty()) },
            )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FlowTopBar(
            title = {
                Text(
                    text = stringResource(Res.string.music_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            actions = {
                IconButton(onClick = onSearch) {
                    Icon(
                        imageVector = FlowIcons.Search,
                        contentDescription = stringResource(Res.string.action_search),
                    )
                }
                IconButton(onClick = onOpenMoods) {
                    Icon(
                        imageVector = FlowIcons.Genres,
                        contentDescription = stringResource(Res.string.moods_title),
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = FlowIcons.SettingsOutlined,
                        contentDescription = stringResource(Res.string.settings_title),
                    )
                }
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
                MusicShelves(
                    feed = current.value,
                    selectedChip = selectedChip,
                    onChipSelected = { selectedChip = it },
                    loadingMore = state.loadingMore,
                    onLoadMore = {
                        val ready = state.feed as? LoadState.Ready ?: return@MusicShelves
                        val token = ready.value.continuation ?: return@MusicShelves
                        if (state.loadingMore) return@MusicShelves
                        state.loadingMore = true
                        scope.launch {
                            loadMusicHome(chip = selectedChip, continuation = token).onSuccess { next ->
                                val shown = state.feed as? LoadState.Ready ?: return@launch
                                state.feed = LoadState.Ready(shown.value.more(next))
                            }
                            state.loadingMore = false
                        }
                    },
                    onOpenPlaylist = onOpenPlaylist,
                    onOpenVideo = onOpenVideo,
                    onOpenArtist = onOpenArtist,
                )
            }
        }
    }
}

@Composable
private fun MusicShelves(
    feed: MusicHomeFeed,
    selectedChip: MusicChip?,
    onChipSelected: (MusicChip?) -> Unit,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenArtist: (artistId: String, name: String, thumbnailUrl: String?) -> Unit,
) {
    MusicSectionList(
        sections = feed.sections,
        chips = feed.chips,
        selectedChip = selectedChip,
        onChipSelected = onChipSelected,
        continuation = feed.continuation,
        loadingMore = loadingMore,
        onLoadMore = onLoadMore,
        onOpenPlaylist = onOpenPlaylist,
        onOpenVideo = onOpenVideo,
        onOpenArtist = onOpenArtist,
    )
}
