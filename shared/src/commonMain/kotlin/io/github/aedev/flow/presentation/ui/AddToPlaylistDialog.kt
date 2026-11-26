package io.github.aedev.flow.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.playlists.UserPlaylists
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_cancel
import io.github.aedev.flow.resources.playlist_add_to
import io.github.aedev.flow.resources.playlist_create_action
import io.github.aedev.flow.resources.playlist_name_hint
import io.github.aedev.flow.resources.playlist_video_count
import io.github.aedev.flow.resources.playlists_create
import io.github.aedev.flow.resources.playlists_empty_hint
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val PlaylistPickerMaxHeight = 320.dp

/**
 * Puts the video on a playlist, or takes it off one it is already on: the picker stays open so a
 * video can go to several playlists, and each row shows whether this video is on it.
 */
@Composable
internal fun AddToPlaylistDialog(
    playlists: UserPlaylists,
    videoId: String,
    onToggle: (playlistId: String) -> Unit,
    onCreate: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var naming by remember { mutableStateOf(false) }
    val createLabel = stringResource(Res.string.playlists_create)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.playlist_add_to)) },
        text = {
            Column {
                if (playlists.playlists.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.playlists_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = PlaylistPickerMaxHeight)) {
                        items(playlists.playlists, key = { it.id }) { playlist ->
                            val contains = playlist.videos.any { it.videoId == videoId }
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = playlist.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text =
                                            pluralStringResource(
                                                Res.plurals.playlist_video_count,
                                                playlist.videos.size,
                                                playlist.videos.size,
                                            ),
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = if (contains) FlowIcons.Check else FlowIcons.AddToQueue,
                                        contentDescription = null,
                                        tint =
                                            if (contains) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                    )
                                },
                                modifier = Modifier.clickable { onToggle(playlist.id) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { naming = true }) {
                Icon(imageVector = FlowIcons.Add, contentDescription = null)
                Text(createLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
    if (naming) {
        PlaylistNameDialog(
            title = createLabel,
            initialName = "",
            confirmLabel = stringResource(Res.string.playlist_create_action),
            placeholder = stringResource(Res.string.playlist_name_hint),
            cancelLabel = stringResource(Res.string.action_cancel),
            onConfirm = { name ->
                onCreate(name)
                naming = false
            },
            onDismiss = { naming = false },
        )
    }
}
