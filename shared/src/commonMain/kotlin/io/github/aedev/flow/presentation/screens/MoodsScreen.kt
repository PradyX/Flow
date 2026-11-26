package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.LoadState
import io.github.aedev.flow.presentation.feed.MoodGenre
import io.github.aedev.flow.presentation.feed.MoodGroup
import io.github.aedev.flow.presentation.feed.loadMoodGroups
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.moods_empty
import io.github.aedev.flow.resources.moods_failed
import io.github.aedev.flow.resources.moods_title
import io.github.aedev.flow.ui.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

private val MoodChipSpacing = 8.dp
private val MoodGroupSpacing = 20.dp

/**
 * YouTube Music's moods and genres: each category as a heading over its buttons, where a button
 * opens the collection behind it. The index is one browse; nothing pages, so the page is a list of
 * groups rather than a feed.
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
                    text = stringResource(Res.string.moods_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = Dimensions.SectionSpacing),
                        verticalArrangement = Arrangement.spacedBy(MoodGroupSpacing),
                    ) {
                        items(groups, key = { it.id }) { group -> MoodGroupBlock(group = group, onOpenMood = onOpenMood) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodGroupBlock(
    group: MoodGroup,
    onOpenMood: (MoodGenre) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = group.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier =
                Modifier.padding(
                    start = Dimensions.ContentPaddingHorizontal,
                    end = Dimensions.ContentPaddingHorizontal,
                    bottom = MoodChipSpacing,
                ),
        )
        FlowRow(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.ContentPaddingHorizontal),
            horizontalArrangement = Arrangement.spacedBy(MoodChipSpacing),
        ) {
            group.genres.forEach { genre ->
                FilterChip(
                    selected = false,
                    onClick = { onOpenMood(genre) },
                    label = { Text(genre.title) },
                )
            }
        }
    }
}
