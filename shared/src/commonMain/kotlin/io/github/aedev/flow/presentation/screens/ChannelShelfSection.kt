package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.aedev.flow.innertube.pages.renderer.CommunityPost
import io.github.aedev.flow.presentation.feed.ChannelShelf
import io.github.aedev.flow.presentation.feed.FeedEntry
import io.github.aedev.flow.presentation.ui.ChannelNavRow

private val ChannelContentPadding = 12.dp
private val ChannelGridSpacing = 12.dp
private const val CHANNEL_GRID_COLUMNS = 2

/**
 * One shelf of a tab that answers with shelves: Home's featured video and carousels, and a release
 * wall's songs. Cards follow the page's layout choice; a channel shelf draws rows, and a posts
 * shelf draws the channel's posts as they appear on its Posts tab.
 */
internal fun LazyListScope.channelShelfItems(
    index: Int,
    shelf: ChannelShelf,
    isList: Boolean,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenChannel: (channelId: String, name: String) -> Unit,
    onOpenPost: (CommunityPost) -> Unit,
) {
    if (shelf.title.isNotBlank()) {
        item("shelf_title_$index") {
            ChannelShelfHeader(
                title = shelf.title,
                onOpen =
                    shelf.playlistId?.let { id ->
                        { onOpenPlaylist(id, shelf.title, null) }
                    },
            )
        }
    }
    when {
        shelf.isFeatured -> {
            items(items = shelf.entries, key = { entry -> "shelf:$index:${entry.id}" }) { entry ->
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = ChannelContentPadding)) {
                    VideoCard(
                        entry = entry,
                        onOpen = onOpenVideo,
                        onOpenPlaylist = onOpenPlaylist,
                    )
                }
            }
        }

        shelf.posts.isNotEmpty() -> {
            items(items = shelf.posts, key = { post -> "shelf:$index:post:${post.id}" }) { post ->
                ChannelPostCard(post = post, onOpen = post.openAction(onOpenPost))
            }
        }

        shelf.entries.all { it.isChannelRow } -> {
            items(items = shelf.entries, key = { entry -> "shelf:$index:channel:${entry.id}" }) { entry ->
                ChannelNavRow(entry = entry, onOpenChannel = onOpenChannel)
            }
        }

        isList -> {
            items(items = shelf.entries, key = { entry -> "shelf:$index:row:${entry.id}" }) { entry ->
                VideoRowCard(
                    entry = entry,
                    onOpen = onOpenVideo,
                    onOpenPlaylist = onOpenPlaylist,
                )
            }
        }

        else -> {
            val rows = shelf.entries.chunked(CHANNEL_GRID_COLUMNS)
            items(
                items = rows,
                key = { row -> "shelf:$index:" + row.joinToString(separator = "|") { it.id } },
            ) { row ->
                ChannelCardRow(row, onOpenVideo, onOpenPlaylist)
            }
        }
    }
}

/** A channel shelf entry is the row shape, not a card: it has no video, playlist or duration. */
private val FeedEntry.isChannelRow: Boolean
    get() = channelId != null && videoId == null && playlistId == null

/** One shelf's name, which opens a playlist when the shelf is one. */
@Composable
private fun ChannelShelfHeader(
    title: String,
    onOpen: (() -> Unit)?,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
                .padding(horizontal = ChannelContentPadding, vertical = 4.dp),
    )
}

/** One grid row of the open tab: the two-card shape the channel's grids draw. */
@Composable
internal fun ChannelCardRow(
    row: List<FeedEntry>,
    onOpenVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ChannelContentPadding),
        horizontalArrangement = Arrangement.spacedBy(ChannelGridSpacing),
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
        if (row.size < CHANNEL_GRID_COLUMNS) Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * The list layout the grid/list control switches to: the still on the left, the title and its
 * byline on the right, so a row reads top to bottom instead of two cards abreast.
 */
@Composable
internal fun VideoRowCard(
    entry: FeedEntry,
    onOpen: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    onOpenPlaylist: (playlistId: String, title: String, thumbnailUrl: String?) -> Unit,
) {
    val videoId = entry.videoId
    val playlistId = entry.playlistId
    val thumbnailUrl = entry.thumbnailUrl
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    when {
                        videoId != null -> onOpen(videoId, entry.title, thumbnailUrl)
                        playlistId != null -> onOpenPlaylist(playlistId, entry.title, thumbnailUrl)
                    }
                }.padding(horizontal = ChannelContentPadding, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(160.dp).aspectRatio(16f / 9f),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            entry.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
