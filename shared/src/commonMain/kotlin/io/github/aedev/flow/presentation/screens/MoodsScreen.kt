package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.MoodGenre
import io.github.aedev.flow.presentation.feed.MoodGroup
import io.github.aedev.flow.presentation.feed.loadMoodGroups
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.presentation.ui.MusicMoodButton
import io.github.aedev.flow.presentation.ui.MusicMoodTone
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.moods_empty
import io.github.aedev.flow.resources.moods_failed
import io.github.aedev.flow.resources.moods_index_title
import io.github.aedev.flow.ui.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

private val MoodCategoryInset = 6.dp
private val MoodButtonRowSpacing = 12.dp

/**
 * YouTube Music's moods and genres, the app's `MoodsAndGenresScreen`: each category as a heading
 * over a grid of its buttons, where a button opens the collection behind it. The index is one
 * browse; nothing pages, so the page is a list of groups rather than a feed.
 */
@Composable
internal fun MoodsScreen(
    onBack: () -> Unit,
    onOpenMood: (MoodGenre) -> Unit,
) {
    var state by remember { mutableStateOf<LoadState<List<MoodGroup>>>(LoadState.Loading) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        state = LoadState.Loading
        state =
            loadMoodGroups().fold(
                onSuccess = { groups -> LoadState.Ready(groups) },
                onFailure = { error -> LoadState.Failed(error.message.orEmpty()) },
            )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FlowTopBar(
            title = {
                Text(
                    text = stringResource(Res.string.moods_index_title),
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
                val groups = current.value
                if (groups.isEmpty()) {
                    EmptyState(text = stringResource(Res.string.moods_empty))
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        // The app sizes the index's buttons by window class: two across a phone,
                        // three on a medium window, four on an expanded one.
                        val columns =
                            when {
                                maxWidth >= 840.dp -> 4
                                maxWidth >= 600.dp -> 3
                                else -> 2
                            }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = Dimensions.SectionSpacing),
                        ) {
                            items(groups, key = { it.id }) { group ->
                                MoodCategoryBlock(
                                    group = group,
                                    columns = columns,
                                    onOpenMood = onOpenMood,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodCategoryBlock(
    group: MoodGroup,
    columns: Int,
    onOpenMood: (MoodGenre) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MoodCategoryInset),
    ) {
        Text(
            text = group.title,
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier.padding(
                    horizontal = Dimensions.ContentPaddingHorizontal,
                    vertical = Dimensions.ContentPaddingVertical,
                ),
        )
        group.genres.chunked(columns).forEachIndexed { rowIndex, row ->
            Row(
                modifier =
                    Modifier.padding(vertical = MoodCategoryInset).padding(horizontal = MoodCategoryInset),
                horizontalArrangement = Arrangement.spacedBy(MoodButtonRowSpacing),
            ) {
                row.forEachIndexed { columnIndex, genre ->
                    MusicMoodButton(
                        title = genre.title,
                        onClick = { onOpenMood(genre) },
                        modifier = Modifier.weight(1f),
                        tone = MusicMoodTone.forIndex(rowIndex * columns + columnIndex),
                    )
                }
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
