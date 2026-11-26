package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.aedev.flow.data.history.WatchHistory
import io.github.aedev.flow.data.history.WatchHistoryEntry
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.presentation.ui.durationText
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.history_clear
import io.github.aedev.flow.resources.history_empty
import io.github.aedev.flow.resources.history_position
import io.github.aedev.flow.resources.history_remove
import io.github.aedev.flow.resources.history_title
import io.github.aedev.flow.ui.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

private const val HISTORY_ARTWORK_SIZE = 56

/**
 * The videos this device watched, newest first: where each one stopped, one row per video, a row tap
 * resumes it through the player and the top bar's bin empties the record. The reference app's
 * history page, over the same record the player writes as it plays.
 */
@Composable
internal fun HistoryScreen(
    history: WatchHistory,
    onOpen: (WatchHistoryEntry) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FlowTopBar(
            title = {
                Text(
                    text = stringResource(Res.string.history_title),
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
            actions = {
                if (history.entries.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = FlowIcons.Delete,
                            contentDescription = stringResource(Res.string.history_clear),
                        )
                    }
                }
            },
        )
        if (history.entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Dimensions.SectionSpacing),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(items = history.entries, key = { it.videoId }) { entry ->
                    HistoryRow(
                        entry = entry,
                        onOpen = { onOpen(entry) },
                        onRemove = { onRemove(entry.videoId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: WatchHistoryEntry,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = entry.title.ifBlank { entry.videoId },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column {
                entry.channelName?.takeIf { it.isNotBlank() }?.let { channel ->
                    Text(text = channel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (entry.positionMs > 0) {
                    Text(
                        text =
                            stringResource(
                                Res.string.history_position,
                                durationText((entry.positionMs / 1000).toInt()),
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        leadingContent = {
            AsyncImage(
                model = entry.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(HISTORY_ARTWORK_SIZE.dp)
                        .clip(MaterialTheme.shapes.small),
            )
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = FlowIcons.Close,
                    contentDescription = stringResource(Res.string.history_remove),
                )
            }
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    )
}
