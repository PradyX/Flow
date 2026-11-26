package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.history.WatchHistoryEntry
import io.github.aedev.flow.presentation.ui.DurationBadge
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.MediaArtwork
import io.github.aedev.flow.presentation.ui.durationText
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.continue_watching_title
import io.github.aedev.flow.resources.history_remove
import org.jetbrains.compose.resources.stringResource

private val ShelfCardWidth = 350.dp
private val ShelfCardSpacing = 10.dp
private val ShelfInset = 12.dp
private val ShelfHeaderSpacing = 8.dp

/**
 * The Home shelf of videos the viewer left part-way: a header that opens History, then a row of
 * wide cards, each removable in place.
 */
@Composable
internal fun HomeContinueWatching(
    entries: List<WatchHistoryEntry>,
    onOpen: (WatchHistoryEntry) -> Unit,
    onRemove: (String) -> Unit,
    onSeeAll: () -> Unit,
    cardWidth: androidx.compose.ui.unit.Dp = ShelfCardWidth,
) {
    val unique = entries.distinctBy { it.videoId }
    if (unique.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = ShelfInset, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = FlowIcons.PlayCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(ShelfHeaderSpacing))
            Text(
                text = stringResource(Res.string.continue_watching_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = FlowIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = ShelfInset),
            horizontalArrangement = Arrangement.spacedBy(ShelfCardSpacing),
        ) {
            items(unique, key = { it.videoId }) { entry ->
                ContinueWatchingCard(
                    entry = entry,
                    cardWidth = cardWidth,
                    onClick = { onOpen(entry) },
                    onRemove = { onRemove(entry.videoId) },
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    entry: WatchHistoryEntry,
    cardWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val progress =
        if (entry.durationMs > 0) (entry.positionMs.toFloat() / entry.durationMs).coerceIn(0f, 1f) else null
    val remaining =
        entry.durationMs.takeIf { it > 0 }?.let { duration ->
            durationText(((duration - entry.positionMs) / 1000).coerceAtLeast(0).toInt())
        }
    Column(modifier = Modifier.width(cardWidth).clickable(onClick = onClick)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            MediaArtwork(
                title = entry.title,
                imageUrl = entry.thumbnailUrl,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
            if (remaining != null) {
                DurationBadge(
                    text = remaining,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomStart),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.4f),
                    drawStopIndicator = {},
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                entry.channelName?.takeIf { it.isNotBlank() }?.let { channel ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = channel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = FlowIcons.Close,
                    modifier = Modifier.size(16.dp),
                    contentDescription = stringResource(Res.string.history_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
