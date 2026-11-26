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
import androidx.compose.ui.text.style.TextOverflow
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.MoodPage
import io.github.aedev.flow.presentation.feed.loadMoodPage
import io.github.aedev.flow.presentation.feed.loadMoodPageMore
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.mood_empty
import io.github.aedev.flow.resources.moods_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * One mood or genre: the shelves YouTube Music publishes for it, each opening its albums,
 * playlists, artists and songs the way the music tab does. The page pages itself through its own
 * token as the footer comes into view.
 */
@Composable
internal fun MoodScreen(
    browseId: String,
    params: String?,
    title: String?,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenArtist: (artistId: String, name: String, thumbnailUrl: String?) -> Unit,
    onBack: () -> Unit,
) {
    var state by remember(browseId, params) { mutableStateOf<LoadState<MoodPage>>(LoadState.Loading) }
    var attempt by remember(browseId, params) { mutableIntStateOf(0) }
    var loadingMore by remember(browseId, params) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(browseId, params, attempt) {
        state = LoadState.Loading
        state =
            loadMoodPage(browseId, params).fold(
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
            loadMoodPageMore(token).onSuccess { page ->
                val kept = (state as? LoadState.Ready)?.value ?: return@onSuccess
                state = LoadState.Ready(kept.more(page))
            }
            loadingMore = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FlowTopBar(
            title = {
                Text(
                    text = title?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.moods_title),
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
                LoadingState()
            }

            is LoadState.Failed -> {
                FailedState(onRetry = { attempt++ })
            }

            is LoadState.Ready -> {
                val page = current.value
                if (page.isEmpty) {
                    EmptyState(text = stringResource(Res.string.mood_empty))
                } else {
                    MusicSectionList(
                        sections = page.sections,
                        continuation = page.continuation,
                        loadingMore = loadingMore,
                        onLoadMore = ::loadMore,
                        onOpenPlaylist = onOpenPlaylist,
                        onOpenVideo = onOpenVideo,
                        onOpenArtist = onOpenArtist,
                    )
                }
            }
        }
    }
}
