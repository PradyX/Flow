package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.playlists.PlaylistVideo
import io.github.aedev.flow.data.playlists.UserPlaylist
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowTopBar
import io.github.aedev.flow.presentation.ui.MediaArtwork
import io.github.aedev.flow.presentation.ui.PlaylistNameDialog
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_back
import io.github.aedev.flow.resources.action_cancel
import io.github.aedev.flow.resources.playlist_delete
import io.github.aedev.flow.resources.playlist_delete_text
import io.github.aedev.flow.resources.playlist_delete_title
import io.github.aedev.flow.resources.playlist_empty
import io.github.aedev.flow.resources.playlist_more_options
import io.github.aedev.flow.resources.playlist_name_hint
import io.github.aedev.flow.resources.playlist_play
import io.github.aedev.flow.resources.playlist_remove_video
import io.github.aedev.flow.resources.playlist_rename
import io.github.aedev.flow.resources.playlist_shuffle
import io.github.aedev.flow.resources.playlist_video_count
import io.github.aedev.flow.ui.theme.Dimensions
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val UserPlaylistRowArtwork = 56.dp
private val UserPlaylistActionSpacing = 8.dp
private const val USER_PLAYLIST_ROW_SPACING = 4

/**
 * One playlist the viewer made: Play and Shuffle over its videos, then the videos themselves. A row
 * starts playback at that video through the queue the rest of the app plays through; its close
 * control takes the video off this playlist and leaves it wherever else it lives.
 */
@Composable
internal fun UserPlaylistScreen(
    playlist: UserPlaylist,
    onPlay: (shuffled: Boolean) -> Unit,
    onOpenIndex: (index: Int) -> Unit,
    onRemoveVideo: (videoId: String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        FlowTopBar(
            title = {
                Text(
                    text = playlist.name,
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
            actions = {
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(
                            imageVector = FlowIcons.MoreVert,
                            contentDescription = stringResource(Res.string.playlist_more_options),
                        )
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.playlist_rename)) },
                            onClick = {
                                menu = false
                                renaming = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.playlist_delete)) },
                            leadingIcon = { Icon(imageVector = FlowIcons.Delete, contentDescription = null) },
                            onClick = {
                                menu = false
                                deleting = true
                            },
                        )
                    }
                }
            },
        )

        if (playlist.videos.isNotEmpty()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.ContentPaddingHorizontal),
                horizontalArrangement = Arrangement.spacedBy(UserPlaylistActionSpacing),
            ) {
                Button(
                    onClick = { onPlay(false) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(imageVector = FlowIcons.Play, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(Res.string.playlist_play), style = MaterialTheme.typography.labelLarge)
                }
                FilledTonalButton(
                    onClick = { onPlay(true) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(imageVector = FlowIcons.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(Res.string.playlist_shuffle), style = MaterialTheme.typography.labelLarge)
                }
            }
            Text(
                text =
                    pluralStringResource(
                        Res.plurals.playlist_video_count,
                        playlist.videos.size,
                        playlist.videos.size,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.padding(
                        start = Dimensions.ContentPaddingHorizontal,
                        top = 12.dp,
                        bottom = 4.dp,
                    ),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Dimensions.SectionSpacing),
                verticalArrangement = Arrangement.spacedBy(USER_PLAYLIST_ROW_SPACING.dp),
            ) {
                itemsIndexed(playlist.videos, key = { _, video -> video.videoId }) { index, video ->
                    PlaylistVideoRow(
                        video = video,
                        onClick = { onOpenIndex(index) },
                        onRemove = { onRemoveVideo(video.videoId) },
                    )
                }
            }
        } else {
            Text(
                text = stringResource(Res.string.playlist_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(Dimensions.SectionSpacing),
            )
        }
    }

    if (renaming) {
        PlaylistNameDialog(
            title = stringResource(Res.string.playlist_rename),
            initialName = playlist.name,
            confirmLabel = stringResource(Res.string.playlist_rename),
            placeholder = stringResource(Res.string.playlist_name_hint),
            cancelLabel = stringResource(Res.string.action_cancel),
            onConfirm = { name ->
                onRename(name)
                renaming = false
            },
            onDismiss = { renaming = false },
        )
    }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(stringResource(Res.string.playlist_delete_title)) },
            text = { Text(stringResource(Res.string.playlist_delete_text, playlist.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleting = false
                        onDelete()
                    },
                ) {
                    Text(stringResource(Res.string.playlist_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun PlaylistVideoRow(
    video: PlaylistVideo,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = video.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            video.channelName?.takeIf { it.isNotBlank() }?.let { channel ->
                Text(
                    text = channel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = {
            MediaArtwork(
                title = video.title,
                imageUrl = video.thumbnailUrl,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(UserPlaylistRowArtwork),
            )
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = FlowIcons.Close,
                    contentDescription = stringResource(Res.string.playlist_remove_video),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
